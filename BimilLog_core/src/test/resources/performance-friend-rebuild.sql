-- =============================================================
-- FriendAdminService Redis 재구築 성능 테스트 시드
-- =============================================================
-- 시나리오: 회원 10만명, 회원당 친구 300명, 회원당 상호작용 300명
--          (회원당 각 글에 글 추천 100개 댓글 작성 100개 댓글 추천 100개)
--
-- 데이터 규모:
--   setting      :    100,000 rows
--   member       :    100,000 rows (provider=KAKAO, member_name=PerfUser_N)
--   post         :    100,000 rows (회원당 1개 — post_like FK 대상)
--   friendship   : 15,000,000 rows (100K × 150)
--   post_like    : 10,000,000 rows (100K × 100)
--   comment      : 10,000,000 rows (100K × 100)
--   comment_like : 10,000,000 rows (100K × 100)
--
-- 실행 방법:
--   mysql -u root -p bimillog2 < performance-friend-rebuild.sql
-- =============================================================

SET foreign_key_checks = 0;
SET unique_checks      = 0;
SET autocommit         = 0;

-- =============================================================
-- Step 1: setting 100,000 rows 삽입
-- =============================================================
INSERT INTO setting
    (message_notification, comment_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1
FROM (
    SELECT a.n + b.n*10 + c.n*100 + d.n*1000 + e.n*10000 + 1 AS seq
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
               UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
) nums
WHERE seq <= 100000;

COMMIT;

-- setting_start 변수 캡처
SET @setting_start = LAST_INSERT_ID();
SELECT @setting_start AS setting_start;

-- =============================================================
-- Step 2: member 100,000 rows 삽입
-- =============================================================
INSERT IGNORE INTO member
    (setting_id, social_id, provider, member_name, role)
SELECT @setting_start + seq - 1,
       CONCAT('perf_', seq),
       'KAKAO',
       CONCAT('PerfUser_', seq),
       'USER'
FROM (
    SELECT a.n + b.n*10 + c.n*100 + d.n*1000 + e.n*10000 + 1 AS seq
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
               UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
) nums
WHERE seq <= 100000;

COMMIT;

-- member_start 변수 캡처
SET @member_start = LAST_INSERT_ID();
SELECT @member_start AS member_start;

-- =============================================================
-- Step 3: post 100,000 rows 삽입
-- =============================================================
INSERT INTO post
    (member_id, title, content, views, like_count, comment_count,
     member_name, is_weekly, is_legend, is_notice, created_at)
SELECT @member_start + seq - 1,
       CONCAT('PerfPost_', seq),
       'performance test content',
       0, 0, 0,
       CONCAT('PerfUser_', seq),
       0, 0, 0, NOW()
FROM (
    SELECT a.n + b.n*10 + c.n*100 + d.n*1000 + e.n*10000 + 1 AS seq
    FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
               UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
    CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e
) nums
WHERE seq <= 100000;

COMMIT;

-- post_start 변수 캡처
SET @post_start = LAST_INSERT_ID();
SELECT @post_start AS post_start;

-- =============================================================
-- Step 4: friendship ~15,000,000 rows (저장 프로시저 배치 삽입)
-- 로직: member i (0-indexed) 의 파트너 = (i + j) % 100000, j in 1..150
--       항상 min(i, partner)를 member_id로 저장해 중복 방지
-- =============================================================
DROP PROCEDURE IF EXISTS insert_friendships;

DELIMITER $$
CREATE PROCEDURE insert_friendships(IN p_member_start BIGINT)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT;
    DECLARE v_partner INT;
    DECLARE v_batch_count INT DEFAULT 0;
    DECLARE v_member_count INT DEFAULT 100000;
    DECLARE v_half INT DEFAULT 150;

    DROP TEMPORARY TABLE IF EXISTS tmp_friendship;
    CREATE TEMPORARY TABLE tmp_friendship (
        member_id BIGINT NOT NULL,
        friend_id BIGINT NOT NULL
    ) ENGINE=MEMORY;

    WHILE v_i < v_member_count DO
        SET v_j = 1;
        WHILE v_j <= v_half DO
            SET v_partner = (v_i + v_j) % v_member_count;
            INSERT INTO tmp_friendship (member_id, friend_id)
            VALUES (
                p_member_start + LEAST(v_i, v_partner),
                p_member_start + GREATEST(v_i, v_partner)
            );
            SET v_batch_count = v_batch_count + 1;

            IF v_batch_count % 10000 = 0 THEN
                INSERT IGNORE INTO friendship (member_id, friend_id)
                SELECT member_id, friend_id FROM tmp_friendship;
                TRUNCATE TABLE tmp_friendship;

                IF v_batch_count % 1000000 = 0 THEN
                    COMMIT;
                END IF;
            END IF;

            SET v_j = v_j + 1;
        END WHILE;
        SET v_i = v_i + 1;
    END WHILE;

    -- 나머지 flush
    INSERT IGNORE INTO friendship (member_id, friend_id)
    SELECT member_id, friend_id FROM tmp_friendship;
    COMMIT;

    DROP TEMPORARY TABLE IF EXISTS tmp_friendship;
END$$
DELIMITER ;

SELECT 'Step 4: friendship 삽입 시작...' AS progress;
CALL insert_friendships(@member_start);
SELECT 'Step 4: friendship 삽입 완료' AS progress;

-- =============================================================
-- Step 5: post_like 10,000,000 rows (저장 프로시저 배치 삽입)
-- 로직: member i 가 member (i + j + 300) % 100000 의 post 에 좋아요, j in 1..100
-- =============================================================
DROP PROCEDURE IF EXISTS insert_post_likes;

DELIMITER $$
CREATE PROCEDURE insert_post_likes(IN p_member_start BIGINT, IN p_post_start BIGINT)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT;
    DECLARE v_author_idx INT;
    DECLARE v_batch_count INT DEFAULT 0;
    DECLARE v_member_count INT DEFAULT 100000;
    DECLARE v_like_per_member INT DEFAULT 100;

    DROP TEMPORARY TABLE IF EXISTS tmp_post_like;
    CREATE TEMPORARY TABLE tmp_post_like (
        member_id BIGINT NOT NULL,
        post_id BIGINT NOT NULL
    ) ENGINE=MEMORY;

    WHILE v_i < v_member_count DO
        SET v_j = 1;
        WHILE v_j <= v_like_per_member DO
            SET v_author_idx = (v_i + v_j + 300) % v_member_count;
            INSERT INTO tmp_post_like (member_id, post_id)
            VALUES (p_member_start + v_i, p_post_start + v_author_idx);
            SET v_batch_count = v_batch_count + 1;

            IF v_batch_count % 10000 = 0 THEN
                INSERT IGNORE INTO post_like (member_id, post_id, created_at)
                SELECT member_id, post_id, NOW() FROM tmp_post_like;
                TRUNCATE TABLE tmp_post_like;

                IF v_batch_count % 1000000 = 0 THEN
                    COMMIT;
                END IF;
            END IF;

            SET v_j = v_j + 1;
        END WHILE;
        SET v_i = v_i + 1;
    END WHILE;

    -- 나머지 flush
    INSERT IGNORE INTO post_like (member_id, post_id, created_at)
    SELECT member_id, post_id, NOW() FROM tmp_post_like;
    COMMIT;

    DROP TEMPORARY TABLE IF EXISTS tmp_post_like;
END$$
DELIMITER ;

SELECT 'Step 5: post_like 삽입 시작...' AS progress;
CALL insert_post_likes(@member_start, @post_start);
SELECT 'Step 5: post_like 삽입 완료' AS progress;

-- =============================================================
-- Step 6: comment 10,000,000 rows (저장 프로시저 배치 삽입)
-- 로직: member i → post of member (i + j + 400) % 100000, j in 1..100
-- comment_closure는 interaction score 쿼리에 불필요하므로 미삽입
-- =============================================================
DROP PROCEDURE IF EXISTS insert_comments;

DELIMITER $$
CREATE PROCEDURE insert_comments(IN p_member_start BIGINT, IN p_post_start BIGINT)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT;
    DECLARE v_post_author_idx INT;
    DECLARE v_batch_count INT DEFAULT 0;
    DECLARE v_member_count INT DEFAULT 100000;
    DECLARE v_comment_per_member INT DEFAULT 100;

    DROP TEMPORARY TABLE IF EXISTS tmp_comment;
    CREATE TEMPORARY TABLE tmp_comment (
        member_id BIGINT NOT NULL,
        post_id BIGINT NOT NULL
    ) ENGINE=MEMORY;

    WHILE v_i < v_member_count DO
        SET v_j = 1;
        WHILE v_j <= v_comment_per_member DO
            SET v_post_author_idx = (v_i + v_j + 400) % v_member_count;
            INSERT INTO tmp_comment (member_id, post_id)
            VALUES (p_member_start + v_i, p_post_start + v_post_author_idx);
            SET v_batch_count = v_batch_count + 1;

            IF v_batch_count % 10000 = 0 THEN
                INSERT INTO comment (member_id, post_id, content, deleted, created_at)
                SELECT member_id, post_id, 'perf', 0, NOW() FROM tmp_comment;
                TRUNCATE TABLE tmp_comment;

                IF v_batch_count % 1000000 = 0 THEN
                    COMMIT;
                END IF;
            END IF;

            SET v_j = v_j + 1;
        END WHILE;
        SET v_i = v_i + 1;
    END WHILE;

    -- 나머지 flush
    INSERT INTO comment (member_id, post_id, content, deleted, created_at)
    SELECT member_id, post_id, 'perf', 0, NOW() FROM tmp_comment;
    COMMIT;

    DROP TEMPORARY TABLE IF EXISTS tmp_comment;
END$$
DELIMITER ;

SELECT 'Step 6: comment 삽입 시작...' AS progress;
CALL insert_comments(@member_start, @post_start);
SELECT 'Step 6: comment 삽입 완료' AS progress;

-- comment_start 캡처 (comment_like에서 사용)
SET @comment_start = (SELECT MIN(comment_id) FROM comment WHERE member_id = @member_start);
SELECT @comment_start AS comment_start;

-- =============================================================
-- Step 7: comment_like 10,000,000 rows (저장 프로시저 배치 삽입)
-- 로직: member i → 댓글 작성자 member k = (i + j + 500) % 100000, j in 1..100
--       해당 member k의 첫 번째 댓글: comment_start + k * 100
-- =============================================================
DROP PROCEDURE IF EXISTS insert_comment_likes;

DELIMITER $$
CREATE PROCEDURE insert_comment_likes(
    IN p_member_start BIGINT,
    IN p_comment_start BIGINT
)
BEGIN
    DECLARE v_i INT DEFAULT 0;
    DECLARE v_j INT;
    DECLARE v_comment_author_idx INT;
    DECLARE v_batch_count INT DEFAULT 0;
    DECLARE v_member_count INT DEFAULT 100000;
    DECLARE v_comment_like_per_member INT DEFAULT 100;
    DECLARE v_comment_per_member INT DEFAULT 100;

    DROP TEMPORARY TABLE IF EXISTS tmp_comment_like;
    CREATE TEMPORARY TABLE tmp_comment_like (
        member_id BIGINT NOT NULL,
        comment_id BIGINT NOT NULL
    ) ENGINE=MEMORY;

    WHILE v_i < v_member_count DO
        SET v_j = 1;
        WHILE v_j <= v_comment_like_per_member DO
            SET v_comment_author_idx = (v_i + v_j + 500) % v_member_count;
            INSERT INTO tmp_comment_like (member_id, comment_id)
            VALUES (
                p_member_start + v_i,
                p_comment_start + CAST(v_comment_author_idx AS SIGNED) * v_comment_per_member
            );
            SET v_batch_count = v_batch_count + 1;

            IF v_batch_count % 10000 = 0 THEN
                INSERT IGNORE INTO comment_like (member_id, comment_id, created_at)
                SELECT member_id, comment_id, NOW() FROM tmp_comment_like;
                TRUNCATE TABLE tmp_comment_like;

                IF v_batch_count % 1000000 = 0 THEN
                    COMMIT;
                END IF;
            END IF;

            SET v_j = v_j + 1;
        END WHILE;
        SET v_i = v_i + 1;
    END WHILE;

    -- 나머지 flush
    INSERT IGNORE INTO comment_like (member_id, comment_id, created_at)
    SELECT member_id, comment_id, NOW() FROM tmp_comment_like;
    COMMIT;

    DROP TEMPORARY TABLE IF EXISTS tmp_comment_like;
END$$
DELIMITER ;

SELECT 'Step 7: comment_like 삽입 시작...' AS progress;
CALL insert_comment_likes(@member_start, @comment_start);
SELECT 'Step 7: comment_like 삽입 완료' AS progress;

-- =============================================================
-- 정리: FK/Unique 체크 복원 및 프로시저 삭제
-- =============================================================
SET foreign_key_checks = 1;
SET unique_checks      = 1;
SET autocommit         = 1;

DROP PROCEDURE IF EXISTS insert_friendships;
DROP PROCEDURE IF EXISTS insert_post_likes;
DROP PROCEDURE IF EXISTS insert_comments;
DROP PROCEDURE IF EXISTS insert_comment_likes;

SELECT '=== 모든 시드 데이터 삽입 완료 ===' AS result;
