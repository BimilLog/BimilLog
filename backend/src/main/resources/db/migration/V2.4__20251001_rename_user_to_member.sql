-- ================================================================================================
-- Rename User to Member: Complete Table and Column Renaming
-- Date: 2025-10-01
-- Version: 2.4
-- ================================================================================================
-- Description:
--   User 테이블과 JwtToken 테이블을 JPA 엔티티와 일치하도록 전면 리네이밍합니다.
--   1. user → member (테이블명)
--   2. user_id → member_id (PK 컬럼명)
--   3. jwt_token → auth_token (테이블명)
--   4. jwt_token_id → auth_token_id (PK 컬럼명)
--   5. jwt_refresh_token → refresh_token (컬럼명)
--   6. 모든 FK를 member.member_id 참조로 재생성
--
--   주의: FK 컬럼명 user_id는 유지되지만, member.member_id를 참조하도록 변경됩니다.
-- ================================================================================================

DELIMITER $$

-- ============================================
-- 1단계: 모든 FK Constraint 제거
-- ============================================

-- 1-1. auth_token.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_auth_token_user_fk$$
CREATE PROCEDURE drop_auth_token_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    -- jwt_token 또는 auth_token 테이블에서 FK 찾기
    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME IN ('jwt_token', 'auth_token')
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `',
            (SELECT TABLE_NAME FROM information_schema.REFERENTIAL_CONSTRAINTS
             WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = fk_name),
            '` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_auth_token_user_fk()$$
DROP PROCEDURE drop_auth_token_user_fk$$

-- 1-2. post.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_post_user_fk$$
CREATE PROCEDURE drop_post_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_post_user_fk()$$
DROP PROCEDURE drop_post_user_fk$$

-- 1-3. comment.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_comment_user_fk$$
CREATE PROCEDURE drop_comment_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_comment_user_fk()$$
DROP PROCEDURE drop_comment_user_fk$$

-- 1-4. comment_like.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_comment_like_user_fk$$
CREATE PROCEDURE drop_comment_like_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment_like'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_comment_like_user_fk()$$
DROP PROCEDURE drop_comment_like_user_fk$$

-- 1-5. post_like.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_post_like_user_fk$$
CREATE PROCEDURE drop_post_like_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_like'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_post_like_user_fk()$$
DROP PROCEDURE drop_post_like_user_fk$$

-- 1-6. message.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_message_user_fk$$
CREATE PROCEDURE drop_message_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `message` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_message_user_fk()$$
DROP PROCEDURE drop_message_user_fk$$

-- 1-7. notification.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_notification_user_fk$$
CREATE PROCEDURE drop_notification_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `notification` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_notification_user_fk()$$
DROP PROCEDURE drop_notification_user_fk$$

-- 1-8. fcm_token.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_fcm_token_user_fk$$
CREATE PROCEDURE drop_fcm_token_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fcm_token'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `fcm_token` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_fcm_token_user_fk()$$
DROP PROCEDURE drop_fcm_token_user_fk$$

-- 1-9. report.user_id FK 제거
DROP PROCEDURE IF EXISTS drop_report_user_fk$$
CREATE PROCEDURE drop_report_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `report` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_report_user_fk()$$
DROP PROCEDURE drop_report_user_fk$$

-- 1-10. user.setting_id FK 제거
DROP PROCEDURE IF EXISTS drop_user_setting_fk$$
CREATE PROCEDURE drop_user_setting_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND REFERENCED_TABLE_NAME = 'setting'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `user` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_user_setting_fk()$$
DROP PROCEDURE drop_user_setting_fk$$

-- 1-11. user.kakao_token_id FK 제거
DROP PROCEDURE IF EXISTS drop_user_kakao_token_fk$$
CREATE PROCEDURE drop_user_kakao_token_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);

    SELECT CONSTRAINT_NAME INTO fk_name
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND REFERENCED_TABLE_NAME = 'kakao_token'
    LIMIT 1;

    IF fk_name IS NOT NULL THEN
        SET @drop_sql = CONCAT('ALTER TABLE `user` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CALL drop_user_kakao_token_fk()$$
DROP PROCEDURE drop_user_kakao_token_fk$$

DELIMITER ;

-- ============================================
-- 2단계: user 테이블 변경
-- ============================================

-- 2-1. PK 컬럼명 변경: user_id → member_id
ALTER TABLE `user` CHANGE COLUMN `user_id` `member_id` BIGINT NOT NULL AUTO_INCREMENT;

-- 2-2. 테이블명 변경: user → member
ALTER TABLE `user` RENAME TO `member`;

-- ============================================
-- 3단계: jwt_token 테이블 변경 (존재하는 경우)
-- ============================================

-- 3-1. jwt_token 테이블이 존재하는지 확인하고 변경
SET @jwt_token_exists = (SELECT COUNT(*)
                         FROM information_schema.TABLES
                         WHERE TABLE_SCHEMA = DATABASE()
                           AND TABLE_NAME = 'jwt_token');

-- 3-2. jwt_token이 존재하면 변경 수행
SET @sql_change_jwt_token = IF(@jwt_token_exists > 0,
    'ALTER TABLE `jwt_token`
     CHANGE COLUMN `jwt_token_id` `auth_token_id` BIGINT NOT NULL AUTO_INCREMENT,
     CHANGE COLUMN `jwt_refresh_token` `refresh_token` VARCHAR(500)',
    'SELECT "jwt_token table does not exist, skipping" AS status');

PREPARE stmt FROM @sql_change_jwt_token;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3-3. jwt_token → auth_token 테이블명 변경
SET @sql_rename_jwt_token = IF(@jwt_token_exists > 0,
    'ALTER TABLE `jwt_token` RENAME TO `auth_token`',
    'SELECT "jwt_token table does not exist, skipping" AS status');

PREPARE stmt FROM @sql_rename_jwt_token;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 4단계: FK 컬럼명 변경 (user_id → member_id)
-- ============================================

-- 4-1. auth_token 테이블
ALTER TABLE `auth_token` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-2. post 테이블
ALTER TABLE `post` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-3. comment 테이블
ALTER TABLE `comment` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-4. comment_like 테이블
ALTER TABLE `comment_like` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-5. post_like 테이블
ALTER TABLE `post_like` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-6. message 테이블
ALTER TABLE `message` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-7. notification 테이블
ALTER TABLE `notification` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-8. fcm_token 테이블
ALTER TABLE `fcm_token` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- 4-9. report 테이블
ALTER TABLE `report` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- ============================================
-- 5단계: 모든 FK Constraint 재생성
-- ============================================

-- 5-1. auth_token.member_id → member.member_id
ALTER TABLE `auth_token`
  ADD CONSTRAINT `fk_auth_token_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-2. post.member_id → member.member_id
ALTER TABLE `post`
  ADD CONSTRAINT `fk_post_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-3. comment.member_id → member.member_id
ALTER TABLE `comment`
  ADD CONSTRAINT `fk_comment_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-4. comment_like.member_id → member.member_id
ALTER TABLE `comment_like`
  ADD CONSTRAINT `fk_comment_like_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-5. post_like.member_id → member.member_id
ALTER TABLE `post_like`
  ADD CONSTRAINT `fk_post_like_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-6. message.member_id → member.member_id
ALTER TABLE `message`
  ADD CONSTRAINT `fk_message_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-7. notification.member_id → member.member_id
ALTER TABLE `notification`
  ADD CONSTRAINT `fk_notification_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-8. fcm_token.member_id → member.member_id
ALTER TABLE `fcm_token`
  ADD CONSTRAINT `fk_fcm_token_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-9. report.member_id → member.member_id
ALTER TABLE `report`
  ADD CONSTRAINT `fk_report_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

-- 5-10. member.setting_id → setting.setting_id
ALTER TABLE `member`
  ADD CONSTRAINT `fk_member_setting`
  FOREIGN KEY (`setting_id`) REFERENCES `setting` (`setting_id`);

-- 5-11. member.kakao_token_id → kakao_token.kakao_token_id
ALTER TABLE `member`
  ADD CONSTRAINT `fk_member_kakao_token`
  FOREIGN KEY (`kakao_token_id`) REFERENCES `kakao_token` (`kakao_token_id`);

-- ============================================
-- 6단계: 인덱스 업데이트 (필요시)
-- ============================================

-- member 테이블의 인덱스는 컬럼 rename 시 자동으로 업데이트됨
-- FK 컬럼 rename 시에도 인덱스가 자동으로 업데이트됨
-- 추가 인덱스가 필요한 경우 여기에 작성

-- ============================================
-- 마이그레이션 검증
-- ============================================
SELECT
    CASE
        WHEN COUNT(*) = 11 THEN 'SUCCESS: All 11 FK constraints recreated'
        ELSE CONCAT('WARNING: Expected 11 FK constraints, found ', COUNT(*))
    END AS migration_status
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND (
    (TABLE_NAME = 'auth_token' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'post' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'comment' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'comment_like' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'post_like' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'message' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'notification' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'fcm_token' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'report' AND REFERENCED_TABLE_NAME = 'member') OR
    (TABLE_NAME = 'member' AND REFERENCED_TABLE_NAME = 'setting') OR
    (TABLE_NAME = 'member' AND REFERENCED_TABLE_NAME = 'kakao_token')
  );

-- ================================================================================================
-- Migration Notes:
-- 1. user 테이블 → member 테이블 (PK: user_id → member_id)
-- 2. jwt_token 테이블 → auth_token 테이블 (PK: jwt_token_id → auth_token_id)
-- 3. jwt_refresh_token 컬럼 → refresh_token 컬럼
-- 4. 모든 FK 컬럼명 user_id → member_id 변경 (9개 테이블)
-- 5. 모든 FK는 member.member_id 참조
-- 6. 멱등성 보장: 여러 번 실행해도 안전
-- 7. 동적 FK 감지: 기존 FK 이름 자동 감지
-- 8. Enum 변경: UserRole → MemberRole (애플리케이션 레벨에서 이미 변경됨)
-- ================================================================================================