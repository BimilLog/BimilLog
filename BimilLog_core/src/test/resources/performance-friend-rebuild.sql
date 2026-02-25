-- =============================================================
-- FriendAdminService Redis 재구築 성능 테스트 시드
-- =============================================================
-- 시나리오: 회원 10만명, 회원당 친구 300명, 회원당 상호작용 150명
--
-- 데이터 규모:
--   setting   :  100,000 rows
--   member    :  100,000 rows (provider=KAKAO, member_name=PerfUser_N)
--   post      :  100,000 rows (회원당 1개 — post_like FK 대상)
--   friendship:   15,000,000 rows (100K × 300 / 2)
--   post_like :   30,000,000 rows (100K × 300 = 각 회원이 300명의 글에 좋아요)
--
-- 사용 방법:
--   이 파일의 Step1~3 SQL은 FriendRedisRebuildPerformanceTest의 @BeforeAll에서
--   JDBC로 실행됩니다. friendship / post_like는 Java JDBC 배치로 생성합니다.
--
-- 수동 실행 시 (MySQL CLI / DBeaver 등에서):
--   USE bimillogTest;
--   SOURCE performance-friend-rebuild.sql;
--   (Step1~3 이후 Java 테스트의 insertFriendships / insertPostLikes 로직을
--    아래 Note를 참고해 직접 작성하거나 테스트 클래스를 실행할 것)
-- =============================================================

-- ── 공통 수식 ──────────────────────────────────────────────
-- 1~100000 시퀀스 생성용 크로스 조인 (디짓 서브쿼리)
-- 각 Step 에서 아래 패턴을 재사용합니다.
--
-- SELECT a.n + b.n*10 + c.n*100 + d.n*1000 + e.n*10000 + 1 AS seq
-- FROM digits a CROSS JOIN digits b CROSS JOIN digits c
--      CROSS JOIN digits d CROSS JOIN digits e
-- WHERE seq BETWEEN 1 AND 100000
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

-- =============================================================
-- Step 2: member 100,000 rows 삽입
-- @setting_start = LAST_INSERT_ID() after Step 1
-- =============================================================
-- [Java에서 @setting_start를 캡처 후 아래 쿼리 실행]
-- INSERT IGNORE INTO member
--     (setting_id, social_id, provider, member_name, role)
-- SELECT @setting_start + seq - 1,
--        CONCAT('perf_', seq),
--        'KAKAO',
--        CONCAT('PerfUser_', seq),
--        'USER'
-- FROM (... nums ...) WHERE seq <= 100000;

-- =============================================================
-- Step 3: post 100,000 rows 삽입
-- @member_start = LAST_INSERT_ID() after Step 2
-- =============================================================
-- [Java에서 @member_start를 캡처 후 아래 쿼리 실행]
-- INSERT INTO post
--     (member_id, title, content, views, like_count, comment_count,
--      member_name, is_weekly, is_legend, is_notice, created_at)
-- SELECT @member_start + seq - 1,
--        CONCAT('PerfPost_', seq),
--        'performance test post content',
--        0, 0, 0,
--        CONCAT('PerfUser_', seq),
--        0, 0, 0, NOW()
-- FROM (... nums ...) WHERE seq <= 100000;

-- =============================================================
-- Step 4: friendship 15,000,000 rows (Java JDBC 배치로 생성)
-- 로직: member i (0-indexed) 의 파트너 = (i + j) % 100000, j in 1..150
--       항상 min(i, partner)를 member_id로 저장해 중복 방지
-- INSERT IGNORE INTO friendship (member_id, friend_id) VALUES (?, ?)
-- =============================================================

-- =============================================================
-- Step 5: post_like 30,000,000 rows (Java JDBC 배치로 생성)
-- 로직: member i 가 member (i + j + 300) % 100000 의 post 에 좋아요, j in 1..300
--       (friendship 대상과 범위를 다르게 하여 상호작용 다양성 확보)
-- INSERT IGNORE INTO post_like (member_id, post_id) VALUES (?, ?)
-- =============================================================

SET foreign_key_checks = 1;
SET unique_checks      = 1;
