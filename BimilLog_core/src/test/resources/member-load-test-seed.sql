-- =====================================================
-- BimilLog 멤버 캐시 부하 테스트용 시드 데이터
-- 멤버 10만명 (Setting 10만개 + Member 10만개)
-- 실행 방법: mysql -u root -p bimillog2 < member-load-test-seed.sql
-- 예상 소요 시간: 1-2분
-- =====================================================

USE bimillog2;

-- 대량 삽입 최적화 설정
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

SELECT NOW() AS '시작 시각', '멤버 시드 데이터 생성 시작' AS status;

-- =====================================================
-- 1. 숫자 테이블 생성 (0~9999, 10,000개)
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
-- 2. Setting 10만개 생성 (10배치 × 10,000개)
-- =====================================================

-- 배치 1 (1 ~ 10,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 2 (10,001 ~ 20,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 3 (20,001 ~ 30,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 4 (30,001 ~ 40,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 5 (40,001 ~ 50,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 6 (50,001 ~ 60,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 7 (60,001 ~ 70,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 8 (70,001 ~ 80,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 9 (80,001 ~ 90,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

-- 배치 10 (90,001 ~ 100,000)
INSERT INTO setting (comment_notification, message_notification, post_featured_notification, friend_send_notification)
SELECT 1, 1, 1, 1 FROM _numbers;
COMMIT;

SELECT NOW() AS '완료 시각', CONCAT('Setting 생성 완료: ', (SELECT COUNT(*) FROM setting), '개') AS status;

-- =====================================================
-- 3. Member 10만개 생성 (10배치 × 10,000개)
--    member_name: 'lt_member_{절대번호}' (기존 'lt_user_'와 충돌 방지)
--    setting_id:  방금 생성한 setting과 1:1 매핑
--    created_at:  1년치 분산
-- =====================================================

-- setting_id 시작값: 방금 생성된 10만개의 첫 번째 id
SET @setting_start = (SELECT MAX(setting_id) - 99999 FROM setting);

-- 배치 1 (n = 0 ~ 9,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + n,
    CONCAT('lt_member_', n),
    CONCAT('lt_member_social_', n),
    'KAKAO',
    CONCAT('테스터멤버', n),
    'USER',
    DATE_SUB(NOW(), INTERVAL (n % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 2 (n = 10,000 ~ 19,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 10000 + n,
    CONCAT('lt_member_', 10000 + n),
    CONCAT('lt_member_social_', 10000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 10000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((10000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 3 (n = 20,000 ~ 29,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 20000 + n,
    CONCAT('lt_member_', 20000 + n),
    CONCAT('lt_member_social_', 20000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 20000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((20000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 4 (n = 30,000 ~ 39,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 30000 + n,
    CONCAT('lt_member_', 30000 + n),
    CONCAT('lt_member_social_', 30000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 30000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((30000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 5 (n = 40,000 ~ 49,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 40000 + n,
    CONCAT('lt_member_', 40000 + n),
    CONCAT('lt_member_social_', 40000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 40000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((40000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 6 (n = 50,000 ~ 59,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 50000 + n,
    CONCAT('lt_member_', 50000 + n),
    CONCAT('lt_member_social_', 50000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 50000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((50000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 7 (n = 60,000 ~ 69,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 60000 + n,
    CONCAT('lt_member_', 60000 + n),
    CONCAT('lt_member_social_', 60000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 60000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((60000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 8 (n = 70,000 ~ 79,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 70000 + n,
    CONCAT('lt_member_', 70000 + n),
    CONCAT('lt_member_social_', 70000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 70000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((70000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 9 (n = 80,000 ~ 89,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 80000 + n,
    CONCAT('lt_member_', 80000 + n),
    CONCAT('lt_member_social_', 80000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 80000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((80000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- 배치 10 (n = 90,000 ~ 99,999)
INSERT INTO member (setting_id, member_name, social_id, provider, social_nickname, role, created_at)
SELECT
    @setting_start + 90000 + n,
    CONCAT('lt_member_', 90000 + n),
    CONCAT('lt_member_social_', 90000 + n),
    'KAKAO',
    CONCAT('테스터멤버', 90000 + n),
    'USER',
    DATE_SUB(NOW(), INTERVAL ((90000 + n) % 365) DAY)
FROM _numbers;
COMMIT;

-- =====================================================
-- 4. 정리
-- =====================================================
DROP TABLE IF EXISTS _numbers;

-- 원래 설정 복구
SET unique_checks = 1;
SET foreign_key_checks = 1;
SET autocommit = 1;

-- 결과 확인
SELECT NOW() AS '완료 시각',
       (SELECT COUNT(*) FROM member WHERE member_name LIKE 'lt_member_%') AS '생성된 멤버 수',
       '멤버 시드 완료' AS status;
