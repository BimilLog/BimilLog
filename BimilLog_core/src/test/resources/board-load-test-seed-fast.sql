-- =====================================================
-- BimilLog 게시판 부하 테스트용 시드 데이터 (고속 버전)
-- 글 10만개, 추천 100만개 (글당 10개)
-- 실행 방법: mysql -u root -p bimillog < board-load-test-seed-fast.sql
-- 예상 소요 시간: 3-5분
-- =====================================================

-- 대량 삽입 최적화 설정
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;
SET sql_log_bin = 0;
SET SESSION bulk_insert_buffer_size = 256 * 1024 * 1024;

SELECT NOW() AS '시작 시각', '시드 데이터 생성 시작' AS status;

-- =====================================================
-- 1. 숫자 테이블 생성 (재사용)
-- =====================================================
DROP TABLE IF EXISTS _numbers;
CREATE TABLE _numbers (n INT PRIMARY KEY);

INSERT INTO _numbers (n)
SELECT a.N + b.N * 10 + c.N * 100 + d.N * 1000 AS n
FROM
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
    (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
     UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d;

COMMIT;

SELECT NOW() AS '완료 시각', '숫자 테이블 생성 완료 (10,000개)' AS status;

-- =====================================================
-- 2. 테스트용 회원 1,000명 생성
-- =====================================================

-- setting 먼저 생성
INSERT INTO setting (comment_notification, message_notification, post_featured_notification)
SELECT 1, 1, 1 FROM _numbers WHERE n < 1000;

COMMIT;

-- 마지막 1000개 setting_id 사용
SET @setting_start = (SELECT MAX(setting_id) - 999 FROM setting);

-- member 생성
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + n,
    CONCAT('lt_user_', n),
    CONCAT('lt_social_', n),
    'KAKAO',
    CONCAT('테스터', n),
    'USER',
    DATE_SUB(NOW(), INTERVAL (n % 365) DAY)
FROM _numbers
WHERE n < 1000;

COMMIT;

SET @member_start = (SELECT MIN(member_id) FROM member WHERE member_name LIKE 'lt_user_%');
SET @member_end = (SELECT MAX(member_id) FROM member WHERE member_name LIKE 'lt_user_%');

SELECT NOW() AS '완료 시각', CONCAT('회원 생성 완료: ', @member_end - @member_start + 1, '명') AS status;

-- =====================================================
-- 3. 게시글 10만개 생성 (10번 × 10,000개)
-- =====================================================

-- 1차 (0 ~ 9,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', n),
    CONCAT('<p>부하테스트 게시글 #', n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    n % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 10,000개 완료' AS progress;

-- 2차 (10,000 ~ 19,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 10000 + n),
    CONCAT('<p>부하테스트 게시글 #', 10000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (10000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 20,000개 완료' AS progress;

-- 3차 (20,000 ~ 29,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 20000 + n),
    CONCAT('<p>부하테스트 게시글 #', 20000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (20000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 30,000개 완료' AS progress;

-- 4차 (30,000 ~ 39,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 30000 + n),
    CONCAT('<p>부하테스트 게시글 #', 30000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (30000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 40,000개 완료' AS progress;

-- 5차 (40,000 ~ 49,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 40000 + n),
    CONCAT('<p>부하테스트 게시글 #', 40000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (40000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 50,000개 완료' AS progress;

-- 6차 (50,000 ~ 59,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 50000 + n),
    CONCAT('<p>부하테스트 게시글 #', 50000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (50000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 60,000개 완료' AS progress;

-- 7차 (60,000 ~ 69,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 60000 + n),
    CONCAT('<p>부하테스트 게시글 #', 60000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (60000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 70,000개 완료' AS progress;

-- 8차 (70,000 ~ 79,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 70000 + n),
    CONCAT('<p>부하테스트 게시글 #', 70000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (70000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 80,000개 완료' AS progress;

-- 9차 (80,000 ~ 89,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 80000 + n),
    CONCAT('<p>부하테스트 게시글 #', 80000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (80000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;
SELECT NOW() AS '시각', '게시글 90,000개 완료' AS progress;

-- 10차 (90,000 ~ 99,999)
INSERT INTO post (member_id, title, content, views, password, is_notice, created_at)
SELECT
    CASE WHEN n % 5 = 0 THEN NULL ELSE @member_start + (n % 1000) END,
    CONCAT('테스트 글 ', 90000 + n),
    CONCAT('<p>부하테스트 게시글 #', 90000 + n, ' - 자동 생성됨. Lorem ipsum dolor sit amet.</p>'),
    (90000 + n) % 10000,
    CASE WHEN n % 5 = 0 THEN 1234 ELSE NULL END,
    0,
    DATE_SUB(NOW(), INTERVAL (n % 180) DAY)
FROM _numbers WHERE n < 10000;
COMMIT;

SET @post_start = (SELECT MIN(post_id) FROM post WHERE title LIKE '테스트 글%');
SET @post_end = (SELECT MAX(post_id) FROM post WHERE title LIKE '테스트 글%');

SELECT NOW() AS '완료 시각', CONCAT('게시글 생성 완료: ', @post_end - @post_start + 1, '개') AS status;

-- =====================================================
-- 4. 추천 데이터 100만개 생성 (글당 10개)
-- 10만 글 × 10개 = 100만개
-- =====================================================

-- 숫자 테이블 확장 (10만개)
DROP TABLE IF EXISTS _numbers_100k;
CREATE TABLE _numbers_100k (n INT PRIMARY KEY);

INSERT INTO _numbers_100k (n)
SELECT n1.n + n2.n * 10000 AS n
FROM _numbers n1,
     (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
      UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) n2
WHERE n1.n + n2.n * 10000 < 100000;

COMMIT;

SELECT NOW() AS '시각', '추천 데이터 생성 시작...' AS status;

-- 추천 1차 (글당 1번째 추천)
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + (n % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL (n % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 100,000개 완료 (1/10)' AS progress;

-- 추천 2차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 100) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 10) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 200,000개 완료 (2/10)' AS progress;

-- 추천 3차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 200) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 20) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 300,000개 완료 (3/10)' AS progress;

-- 추천 4차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 300) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 30) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 400,000개 완료 (4/10)' AS progress;

-- 추천 5차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 400) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 40) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 500,000개 완료 (5/10)' AS progress;

-- 추천 6차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 500) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 50) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 600,000개 완료 (6/10)' AS progress;

-- 추천 7차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 600) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 60) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 700,000개 완료 (7/10)' AS progress;

-- 추천 8차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 700) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 70) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 800,000개 완료 (8/10)' AS progress;

-- 추천 9차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 800) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 80) % 90) DAY)
FROM _numbers_100k;
COMMIT;
SELECT NOW() AS '시각', '추천 900,000개 완료 (9/10)' AS progress;

-- 추천 10차
INSERT IGNORE INTO post_like (member_id, post_id, created_at)
SELECT
    @member_start + ((n + 900) % 1000),
    @post_start + n,
    DATE_SUB(NOW(), INTERVAL ((n + 85) % 90) DAY)
FROM _numbers_100k;
COMMIT;

SELECT NOW() AS '완료 시각', '추천 1,000,000개 완료 (10/10)' AS status;

-- =====================================================
-- 5. 정리 및 최적화
-- =====================================================

-- 임시 테이블 삭제
DROP TABLE IF EXISTS _numbers;
DROP TABLE IF EXISTS _numbers_100k;

-- 통계 갱신
ANALYZE TABLE post;
ANALYZE TABLE post_like;
ANALYZE TABLE member;

-- 설정 복원
SET autocommit = 1;
SET unique_checks = 1;
SET foreign_key_checks = 1;
SET sql_log_bin = 1;

-- =====================================================
-- 6. 최종 결과
-- =====================================================
SELECT '========================================' AS '';
SELECT '시드 데이터 생성 완료' AS 결과;
SELECT '========================================' AS '';
SELECT COUNT(*) AS '총 회원 수' FROM member WHERE member_name LIKE 'lt_user_%';
SELECT COUNT(*) AS '총 게시글 수' FROM post WHERE title LIKE '테스트 글%';
SELECT COUNT(*) AS '총 추천 수' FROM post_like WHERE post_id BETWEEN @post_start AND @post_end;
SELECT ROUND(COUNT(*) / 100000, 2) AS '평균 추천/글' FROM post_like WHERE post_id BETWEEN @post_start AND @post_end;
SELECT NOW() AS '종료 시각';
