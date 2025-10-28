-- ====================================================================================
-- bimillogTest DB 시드 스크립트 (고성능 버전)
--  - 회원 5,000명
--  - 게시글 10,000개
--  - 댓글 100,000개 (모두 루트 댓글)
--  - 댓글 클로저 100,000행 (depth=0)
--  - 댓글 추천 500,000개 (일반 분배 480,000개 + 타겟 회원 20,000개)
--
-- 실행 방법:
--   mysql -u <user> -p < backend/src/test/resources/bimillogTest-seed.sql
--
-- ⚠️ 주의: 스크립트는 대상 테이블을 TRUNCATE 하므로 실행 전에 백업을 권장합니다.
-- ====================================================================================

USE bimillogTest;

SET @previous_autocommit = @@autocommit;
SET autocommit = 0;

SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE comment_like;
TRUNCATE TABLE comment_closure;
TRUNCATE TABLE comment;
TRUNCATE TABLE post;
TRUNCATE TABLE member;
TRUNCATE TABLE setting;

SET SESSION cte_max_recursion_depth = 200000;

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);

INSERT INTO tmp_seq (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT n FROM seq;

DROP TEMPORARY TABLE IF EXISTS tmp_slots;
CREATE TEMPORARY TABLE tmp_slots (slot TINYINT PRIMARY KEY);
INSERT INTO tmp_slots VALUES (0),(1),(2),(3),(4);

SET @member_cnt := 5000;
SET @post_cnt := 10000;
SET @comment_cnt := 100000;
SET @like_seed_comment_cnt := 100000;
SET @target_member_id := 1;
SET @base_now := NOW(6);

START TRANSACTION;

-- 1. Setting
INSERT INTO setting (message_notification, comment_notification, post_featured_notification)
SELECT 1, 1, 1
FROM tmp_seq
WHERE n <= @member_cnt;

-- 2. Member (설정과 1:1 매칭)
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
    s.setting_id,
    CONCAT('s', LPAD(ts.n, 6, '0')),
    'KAKAO',
    CONCAT('m', LPAD(ts.n, 4, '0')),
    'USER',
    CONCAT('닉네임', LPAD(ts.n, 4, '0')),
    CONCAT('https://cdn.bimillog.test/avatar/', LPAD(ts.n, 4, '0')),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(ts.n * 13) * 365) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(ts.n * 17) * 180) DAY)
FROM tmp_seq ts
JOIN setting s ON s.setting_id = ts.n
WHERE ts.n <= @member_cnt;

-- 3. Post
INSERT INTO post (
    member_id,
    title,
    content,
    views,
    is_notice,
    password,
    created_at,
    modified_at
)
SELECT
    1 + FLOOR(RAND(ts.n * 23) * @member_cnt),
    CONCAT('시드 게시글 제목 ', ts.n),
    CONCAT('시드 게시글 콘텐츠 ', ts.n, ' - 성능 검증용 텍스트입니다.'),
    FLOOR(RAND(ts.n * 29) * 50000),
    0,
    NULL,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(ts.n * 31) * 365) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(ts.n * 37) * 180) DAY)
FROM tmp_seq ts
WHERE ts.n <= @post_cnt;

-- 4. Comment
INSERT INTO comment (
    post_id,
    member_id,
    content,
    deleted,
    password,
    created_at,
    modified_at
)
SELECT
    1 + FLOOR(RAND(ts.n * 41) * @post_cnt),
    1 + FLOOR(RAND(ts.n * 43) * @member_cnt),
    CONCAT('이것은 시드 댓글 ', ts.n, ' 입니다. 성능 테스트용으로 생성되었습니다.'),
    0,
    NULL,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(ts.n * 47) * 120) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(ts.n * 53) * 120) DAY)
FROM tmp_seq ts
WHERE ts.n <= @comment_cnt;

-- 5. Comment Closure (depth=0)
INSERT INTO comment_closure (ancestor_id, descendant_id, depth)
SELECT comment_id, comment_id, 0
FROM comment;

-- 6. Comment Like - 일반 회원 분배 (댓글 1 ~ 20,000, 댓글당 4개)
INSERT INTO comment_like (
    comment_id,
    member_id,
    created_at,
    modified_at
)
SELECT
    c.comment_id,
    (( (c.comment_id - 1) * 5 + slots.slot) MOD (@member_cnt - 1)) + 2,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(c.comment_id * 59 + slots.slot) * 90) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(c.comment_id * 61 + slots.slot) * 90) DAY)
FROM comment c
JOIN tmp_slots slots ON slots.slot < 4
WHERE c.comment_id BETWEEN 1 AND 20000;

-- 7. Comment Like - 일반 회원 분배 (댓글 20,001 ~ 100,000, 댓글당 5개)
INSERT INTO comment_like (
    comment_id,
    member_id,
    created_at,
    modified_at
)
SELECT
    c.comment_id,
    (( (c.comment_id - 1) * 5 + slots.slot) MOD (@member_cnt - 1)) + 2,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(c.comment_id * 67 + slots.slot) * 90) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(c.comment_id * 71 + slots.slot) * 90) DAY)
FROM comment c
JOIN tmp_slots slots
WHERE c.comment_id BETWEEN 20001 AND @like_seed_comment_cnt;

-- 8. Comment Like - 타겟 회원 20,000개 집중 분배
INSERT INTO comment_like (
    comment_id,
    member_id,
    created_at,
    modified_at
)
SELECT
    c.comment_id,
    @target_member_id,
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(c.comment_id * 73) * 90) DAY),
    DATE_SUB(@base_now, INTERVAL FLOOR(RAND(c.comment_id * 79) * 90) DAY)
FROM comment c
WHERE c.comment_id BETWEEN 1 AND 20000;

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
DROP TEMPORARY TABLE IF EXISTS tmp_slots;

SET autocommit = @previous_autocommit;
