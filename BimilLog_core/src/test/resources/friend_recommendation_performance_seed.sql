-- ====================================================================================
-- Friend Recommendation Performance Test Seed Script
--  - Members: 1,000
--  - Friendships: ~7,500 (avg 15 friends per member, bidirectional)
--  - Posts: 1,000
--  - Comments: 5,000 (root comments only)
--  - Post Likes: 5,000
--  - Comment Likes: 10,000
-- ====================================================================================

USE bimillogTest;

SET @previous_autocommit = @@autocommit;
SET autocommit = 0;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing data
TRUNCATE TABLE friend_recommendation;
TRUNCATE TABLE friendship;
TRUNCATE TABLE friend_request;
TRUNCATE TABLE post_like;
TRUNCATE TABLE comment_like;
TRUNCATE TABLE comment_closure;
TRUNCATE TABLE comment;
TRUNCATE TABLE post;
TRUNCATE TABLE member;
TRUNCATE TABLE setting;

SET SESSION cte_max_recursion_depth = 200000;

-- Create temporary sequence table
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);

INSERT INTO tmp_seq (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 20000
)
SELECT n FROM seq;

-- Variables
SET @member_cnt := 1000;
SET @friendship_per_member := 15;
SET @base_now := NOW(6);

START TRANSACTION;

-- 1. Setting (1000)
INSERT INTO setting (message_notification, comment_notification, post_featured_notification)
SELECT 1, 1, 1
FROM tmp_seq
WHERE n <= @member_cnt;

-- 2. Member (1000)
INSERT INTO member (
    setting_id,
    social_id,
    provider,
    member_name,
    role,
    social_nickname,
    thumbnail_image,
    created_at,
    modified_at
)
SELECT
    n,
    CONCAT('perf_s', LPAD(n, 6, '0')),
    'KAKAO',
    CONCAT('perf_m', LPAD(n, 4, '0')),
    'USER',
    CONCAT('PerfUser', LPAD(n, 4, '0')),
    CONCAT('https://cdn.test/avatar/', LPAD(n, 4, '0')),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 13) * 365) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 17) * 180) DAY)
FROM tmp_seq
WHERE n <= @member_cnt;

-- 3. Friendship (~7500, bidirectional)
-- Create separate temp tables to avoid reopen issues
DROP TEMPORARY TABLE IF EXISTS tmp_members;
CREATE TEMPORARY TABLE tmp_members (n INT);
INSERT INTO tmp_members SELECT n FROM tmp_seq WHERE n <= @member_cnt;

DROP TEMPORARY TABLE IF EXISTS tmp_friend_idx;
CREATE TEMPORARY TABLE tmp_friend_idx (n INT);
INSERT INTO tmp_friend_idx SELECT n FROM tmp_seq WHERE n <= @friendship_per_member;

-- Insert friendships (forward direction)
INSERT IGNORE INTO friendship (member_id, friend_id, created_at, modified_at)
SELECT DISTINCT
    m.n AS member_id,
    1 + MOD(m.n * 23 + f.n * 29, @member_cnt) AS friend_id,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(m.n * 31 + f.n * 37) * 180) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(m.n * 41 + f.n * 43) * 90) DAY)
FROM tmp_members m
JOIN tmp_friend_idx f
WHERE m.n != 1 + MOD(m.n * 23 + f.n * 29, @member_cnt);

-- Insert friendships (reverse direction for bidirectional relationship)
INSERT IGNORE INTO friendship (member_id, friend_id, created_at, modified_at)
SELECT
    friend_id,
    member_id,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(friend_id * 31 + member_id * 37) * 180) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(friend_id * 41 + member_id * 43) * 90) DAY)
FROM friendship;

DROP TEMPORARY TABLE tmp_members;
DROP TEMPORARY TABLE tmp_friend_idx;

-- 4. Post (1000)
INSERT INTO post (member_id, title, content, views, is_notice, password, created_at, modified_at)
SELECT
    1 + MOD(n - 1, @member_cnt),
    CONCAT('Performance Test Post ', n),
    CONCAT('Test post content ', n, ' for friend recommendation testing.'),
    FLOOR(RAND(n * 47) * 1000),
    0,
    NULL,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 53) * 180) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 59) * 90) DAY)
FROM tmp_seq
WHERE n <= 1000;

-- 5. Comment (5000, root comments only)
INSERT INTO comment (post_id, member_id, content, deleted, password, created_at, modified_at)
SELECT
    1 + MOD(FLOOR((n - 1) / 5), 1000),
    1 + MOD(n - 1, @member_cnt),
    CONCAT('Test comment ', n, ' for interaction scoring.'),
    0,
    NULL,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 71) * 120) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 73) * 60) DAY)
FROM tmp_seq
WHERE n <= 5000;

-- 6. Comment Closure (depth=0, self-reference)
INSERT INTO comment_closure (ancestor_id, descendant_id, depth)
SELECT comment_id, comment_id, 0
FROM comment;

-- 7. Post Like (5000)
INSERT IGNORE INTO post_like (post_id, member_id, created_at, modified_at)
SELECT
    1 + MOD(n - 1, 1000),
    1 + MOD(n * 7 - 1, @member_cnt),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 89) * 90) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 97) * 60) DAY)
FROM tmp_seq
WHERE n <= 5000;

-- 8. Comment Like (10000)
INSERT IGNORE INTO comment_like (comment_id, member_id, created_at, modified_at)
SELECT
    1 + MOD(n - 1, 5000),
    1 + MOD(n * 11 - 1, @member_cnt),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 107) * 90) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(n * 109) * 60) DAY)
FROM tmp_seq
WHERE n <= 10000;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
SET autocommit = @previous_autocommit;

-- Statistics output
SELECT '========== Seed Data Loading Complete ==========' AS '';
SELECT CONCAT('Members: ', COUNT(*)) AS result FROM member;
SELECT CONCAT('Friendships: ', COUNT(*), ' (bidirectional)') AS result FROM friendship;
SELECT CONCAT('Posts: ', COUNT(*)) AS result FROM post;
SELECT CONCAT('Comments: ', COUNT(*)) AS result FROM comment;
SELECT CONCAT('Post Likes: ', COUNT(*)) AS result FROM post_like;
SELECT CONCAT('Comment Likes: ', COUNT(*)) AS result FROM comment_like;
SELECT '===================================================' AS '';
