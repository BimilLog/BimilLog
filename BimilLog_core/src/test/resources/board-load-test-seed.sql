-- =====================================================
-- BimilLog 게시판 부하 테스트용 시드 데이터
-- 글 10만개, 추천 100만개 (글당 10개)
-- 실행 방법: mysql -u root -p bimillog < board-load-test-seed.sql
-- =====================================================

-- 실행 전 설정 (대량 삽입 최적화)
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;
SET sql_log_bin = 0;

-- 진행 상황 출력
SELECT '시드 데이터 생성 시작...' AS status;

-- =====================================================
-- 1. 테스트용 회원 생성 (1,000명)
-- =====================================================
SELECT '1. 테스트 회원 1,000명 생성 중...' AS status;

-- setting 테이블 먼저 생성
INSERT INTO setting (comment_notification, message_notification, post_featured_notification)
SELECT 1, 1, 1
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
         (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
         (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
    WHERE a.N + b.N * 10 + c.N * 100 < 1000
) numbers;

COMMIT;

-- 생성된 setting_id 범위 확인
SET @min_setting_id = (SELECT MIN(setting_id) FROM setting WHERE setting_id >= (SELECT COALESCE(MAX(setting_id), 0) FROM setting) - 1000);
SET @max_setting_id = (SELECT MAX(setting_id) FROM setting);

-- member 테이블 생성
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at, modified_at)
SELECT
    s.setting_id,
    CONCAT('loadtest_user_', ROW_NUMBER() OVER (ORDER BY s.setting_id)),
    CONCAT('loadtest_', ROW_NUMBER() OVER (ORDER BY s.setting_id)),
    'KAKAO',
    CONCAT('테스트유저', ROW_NUMBER() OVER (ORDER BY s.setting_id)),
    'USER',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
    NULL
FROM setting s
WHERE s.setting_id BETWEEN @min_setting_id AND @max_setting_id
ORDER BY s.setting_id
LIMIT 1000;

COMMIT;

SELECT CONCAT('회원 생성 완료: ', COUNT(*), '명') AS status FROM member WHERE member_name LIKE 'loadtest_user_%';

-- =====================================================
-- 2. 게시글 10만개 생성
-- =====================================================
SELECT '2. 게시글 10만개 생성 중... (약 2-5분 소요)' AS status;

-- 테스트 회원 ID 범위 저장
SET @min_member_id = (SELECT MIN(member_id) FROM member WHERE member_name LIKE 'loadtest_user_%');
SET @max_member_id = (SELECT MAX(member_id) FROM member WHERE member_name LIKE 'loadtest_user_%');

-- 프로시저로 배치 삽입
DROP PROCEDURE IF EXISTS insert_posts;

DELIMITER $$

CREATE PROCEDURE insert_posts()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_posts INT DEFAULT 100000;
    DECLARE random_member_id BIGINT;
    DECLARE random_views INT;
    DECLARE random_date DATETIME;

    WHILE i < total_posts DO
        -- 10,000개씩 배치 삽입
        INSERT INTO post (member_id, title, content, views, password, is_notice, created_at, modified_at)
        SELECT
            -- 80%는 회원 글, 20%는 익명 글
            CASE WHEN RAND() < 0.8
                THEN @min_member_id + FLOOR(RAND() * (@max_member_id - @min_member_id + 1))
                ELSE NULL
            END AS member_id,
            CONCAT('테스트 게시글 #', i + numbers.n) AS title,
            CONCAT(
                '<p>부하 테스트용 게시글 내용입니다. ',
                '이 글은 자동 생성된 테스트 데이터입니다. ',
                '글 번호: ', i + numbers.n, ', ',
                '생성 시각: ', NOW(), '</p>',
                '<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. ',
                'Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>'
            ) AS content,
            FLOOR(RAND() * 10000) AS views,
            CASE WHEN RAND() < 0.2 THEN FLOOR(1000 + RAND() * 9000) ELSE NULL END AS password,
            0 AS is_notice,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) AS created_at,
            NULL AS modified_at
        FROM (
            SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 + 1 AS n
            FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
                 (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
                 (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
                 (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d
            WHERE a.N + b.N * 10 + c.N * 100 + d.N * 1000 < batch_size
        ) numbers;

        SET i = i + batch_size;
        COMMIT;

        -- 진행 상황 출력
        SELECT CONCAT('게시글 생성 중: ', i, '/', total_posts) AS progress;
    END WHILE;
END$$

DELIMITER ;

CALL insert_posts();
DROP PROCEDURE IF EXISTS insert_posts;

COMMIT;

SELECT CONCAT('게시글 생성 완료: ', COUNT(*), '개') AS status FROM post WHERE title LIKE '테스트 게시글%';

-- =====================================================
-- 3. 추천 데이터 100만개 생성 (글당 10개)
-- =====================================================
SELECT '3. 추천 데이터 100만개 생성 중... (약 5-10분 소요)' AS status;

-- 테스트 게시글 ID 범위 저장
SET @min_post_id = (SELECT MIN(post_id) FROM post WHERE title LIKE '테스트 게시글%');
SET @max_post_id = (SELECT MAX(post_id) FROM post WHERE title LIKE '테스트 게시글%');

-- 프로시저로 배치 삽입
DROP PROCEDURE IF EXISTS insert_post_likes;

DELIMITER $$

CREATE PROCEDURE insert_post_likes()
BEGIN
    DECLARE current_post_id BIGINT;
    DECLARE likes_per_post INT DEFAULT 10;
    DECLARE batch_count INT DEFAULT 0;
    DECLARE done INT DEFAULT FALSE;
    DECLARE post_cursor CURSOR FOR
        SELECT post_id FROM post
        WHERE title LIKE '테스트 게시글%'
        ORDER BY post_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN post_cursor;

    read_loop: LOOP
        FETCH post_cursor INTO current_post_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 각 게시글에 10개 추천 추가 (랜덤 회원)
        INSERT IGNORE INTO post_like (member_id, post_id, created_at, modified_at)
        SELECT
            @min_member_id + FLOOR(RAND() * (@max_member_id - @min_member_id + 1)) AS member_id,
            current_post_id AS post_id,
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY) AS created_at,
            NULL AS modified_at
        FROM (
            SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
            UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
        ) numbers;

        SET batch_count = batch_count + 1;

        -- 1,000개 게시글마다 커밋
        IF batch_count % 1000 = 0 THEN
            COMMIT;
            SELECT CONCAT('추천 생성 중: ', batch_count * likes_per_post, '/1,000,000 (게시글 ', batch_count, '/', 100000, ')') AS progress;
        END IF;
    END LOOP;

    CLOSE post_cursor;
    COMMIT;
END$$

DELIMITER ;

CALL insert_post_likes();
DROP PROCEDURE IF EXISTS insert_post_likes;

COMMIT;

SELECT CONCAT('추천 생성 완료: ', COUNT(*), '개') AS status FROM post_like WHERE post_id >= @min_post_id;

-- =====================================================
-- 4. 인덱스 재구성 및 통계 갱신
-- =====================================================
SELECT '4. 인덱스 및 통계 최적화 중...' AS status;

ANALYZE TABLE post;
ANALYZE TABLE post_like;
ANALYZE TABLE member;

-- 설정 복원
SET autocommit = 1;
SET unique_checks = 1;
SET foreign_key_checks = 1;
SET sql_log_bin = 1;

-- =====================================================
-- 5. 최종 통계
-- =====================================================
SELECT '===== 시드 데이터 생성 완료 =====' AS status;
SELECT CONCAT('총 회원: ', COUNT(*), '명') AS result FROM member WHERE member_name LIKE 'loadtest_user_%';
SELECT CONCAT('총 게시글: ', COUNT(*), '개') AS result FROM post WHERE title LIKE '테스트 게시글%';
SELECT CONCAT('총 추천: ', COUNT(*), '개') AS result FROM post_like WHERE post_id >= @min_post_id;
SELECT CONCAT('평균 추천/글: ', ROUND(COUNT(*) / 100000, 2), '개') AS result FROM post_like WHERE post_id >= @min_post_id;
