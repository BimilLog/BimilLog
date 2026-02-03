-- ====================================================================================
-- bimillogTest DB 시드 스크립트 (게시판 부하 테스트용)
--  - 게시글 100,000개 (모두 익명)
--
-- 실행 방법:
--   mysql -u <user> -p bimillogTest < src/test/resources/bimillogTest-seed.sql
-- ====================================================================================

USE bimillogTest;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET @previous_autocommit = @@autocommit;
SET autocommit = 0;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE post_like;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE comment_closure;
TRUNCATE TABLE comment;
TRUNCATE TABLE post;

SET SESSION cte_max_recursion_depth = 200000;

-- ====================================================================================
-- 숫자 시퀀스 테이블 (10,000개)
-- ====================================================================================
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);

INSERT INTO tmp_seq (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10000
)
SELECT n FROM seq;

SET @base_now := NOW(6);

-- ====================================================================================
-- Post (100,000개) - 모두 익명, 10번 × 10,000개 배치
-- ====================================================================================
SELECT 'Creating 100,000 anonymous posts...' AS progress;

-- Batch 1: 1 ~ 10,000
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT
    NULL,
    CONCAT('테스트 글 ', ts.n),
    CONCAT('<p>부하테스트 게시글 #', ts.n, ' - 익명 글입니다. Lorem ipsum dolor sit amet.</p>'),
    ts.n MOD 10000,
    1234,
    DATE_SUB(@base_now, INTERVAL (ts.n MOD 180) DAY),
    NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 10,000 done' AS progress;

-- Batch 2
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 10000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 10000 + ts.n, '</p>'),
    (10000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((10000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 20,000 done' AS progress;

-- Batch 3
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 20000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 20000 + ts.n, '</p>'),
    (20000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((20000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 30,000 done' AS progress;

-- Batch 4
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 30000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 30000 + ts.n, '</p>'),
    (30000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((30000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 40,000 done' AS progress;

-- Batch 5
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 40000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 40000 + ts.n, '</p>'),
    (40000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((40000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 50,000 done' AS progress;

-- Batch 6
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 50000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 50000 + ts.n, '</p>'),
    (50000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((50000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 60,000 done' AS progress;

-- Batch 7
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 60000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 60000 + ts.n, '</p>'),
    (60000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((60000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 70,000 done' AS progress;

-- Batch 8
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 70000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 70000 + ts.n, '</p>'),
    (70000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((70000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 80,000 done' AS progress;

-- Batch 9
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 80000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 80000 + ts.n, '</p>'),
    (80000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((80000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 90,000 done' AS progress;

-- Batch 10
START TRANSACTION;
INSERT INTO post (member_id, title, content, views, password, created_at, modified_at)
SELECT NULL, CONCAT('테스트 글 ', 90000 + ts.n),
    CONCAT('<p>부하테스트 게시글 #', 90000 + ts.n, '</p>'),
    (90000 + ts.n) MOD 10000, 1234,
    DATE_SUB(@base_now, INTERVAL ((90000 + ts.n) MOD 180) DAY), NULL
FROM tmp_seq ts WHERE ts.n <= 10000;
COMMIT;
SELECT 'Posts: 100,000 done' AS progress;

-- ====================================================================================
-- Cleanup
-- ====================================================================================
DROP TEMPORARY TABLE IF EXISTS tmp_seq;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET autocommit = @previous_autocommit;

ANALYZE TABLE post;

-- ====================================================================================
-- Final Report
-- ====================================================================================
SELECT '========================================' AS '';
SELECT 'SEED DATA COMPLETE' AS result;
SELECT '========================================' AS '';
SELECT COUNT(*) AS 'Total Posts' FROM post;
