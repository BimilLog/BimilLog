-- ================================================================================================
-- Remove All CASCADE Constraints from Database (Dynamic FK Detection)
-- Date: 2025-09-30
-- Version: 2.3
-- ================================================================================================
-- Description:
--   JPA 엔티티에는 cascade 설정이 없으므로, DB 레벨의 모든 ON DELETE CASCADE를 제거합니다.
--   삭제 로직은 애플리케이션 레이어에서 명시적으로 처리됩니다.
--
--   이 마이그레이션은:
--   1. 현재 DB의 FK 이름을 동적으로 감지
--   2. CASCADE가 있는 FK만 DROP 후 재생성
--   3. 새 DB와 기존 DB 모두 호환
-- ================================================================================================

DELIMITER $$

-- ============================================
-- 1. post.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_post_user_fk$$
CREATE PROCEDURE migrate_post_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    -- 현재 FK 이름과 CASCADE 여부 조회
    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    -- CASCADE가 있는 경우만 재생성
    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `post`
          ADD CONSTRAINT `fk_post_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_post_user_fk()$$
DROP PROCEDURE migrate_post_user_fk$$

-- ============================================
-- 2. comment.post_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_comment_post_fk$$
CREATE PROCEDURE migrate_comment_post_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment'
      AND REFERENCED_TABLE_NAME = 'post'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `comment`
          ADD CONSTRAINT `fk_comment_post`
          FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`);
    END IF;
END$$

CALL migrate_comment_post_fk()$$
DROP PROCEDURE migrate_comment_post_fk$$

-- ============================================
-- 3. comment_like.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_comment_like_user_fk$$
CREATE PROCEDURE migrate_comment_like_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment_like'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `comment_like`
          ADD CONSTRAINT `fk_comment_like_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_comment_like_user_fk()$$
DROP PROCEDURE migrate_comment_like_user_fk$$

-- ============================================
-- 4. comment_like.comment_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_comment_like_comment_fk$$
CREATE PROCEDURE migrate_comment_like_comment_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment_like'
      AND REFERENCED_TABLE_NAME = 'comment'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `comment_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `comment_like`
          ADD CONSTRAINT `fk_comment_like_comment`
          FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`);
    END IF;
END$$

CALL migrate_comment_like_comment_fk()$$
DROP PROCEDURE migrate_comment_like_comment_fk$$

-- ============================================
-- 5. post_like.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_post_like_user_fk$$
CREATE PROCEDURE migrate_post_like_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_like'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `post_like`
          ADD CONSTRAINT `fk_post_like_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_post_like_user_fk()$$
DROP PROCEDURE migrate_post_like_user_fk$$

-- ============================================
-- 6. post_like.post_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_post_like_post_fk$$
CREATE PROCEDURE migrate_post_like_post_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_like'
      AND REFERENCED_TABLE_NAME = 'post'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `post_like` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `post_like`
          ADD CONSTRAINT `fk_post_like_post`
          FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`);
    END IF;
END$$

CALL migrate_post_like_post_fk()$$
DROP PROCEDURE migrate_post_like_post_fk$$

-- ============================================
-- 7. message.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_message_user_fk$$
CREATE PROCEDURE migrate_message_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'message'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `message` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `message`
          ADD CONSTRAINT `fk_message_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_message_user_fk()$$
DROP PROCEDURE migrate_message_user_fk$$

-- ============================================
-- 8. notification.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_notification_user_fk$$
CREATE PROCEDURE migrate_notification_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `notification` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `notification`
          ADD CONSTRAINT `fk_notification_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_notification_user_fk()$$
DROP PROCEDURE migrate_notification_user_fk$$

-- ============================================
-- 9. fcm_token.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_fcm_token_user_fk$$
CREATE PROCEDURE migrate_fcm_token_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'fcm_token'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `fcm_token` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `fcm_token`
          ADD CONSTRAINT `fk_fcm_token_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_fcm_token_user_fk()$$
DROP PROCEDURE migrate_fcm_token_user_fk$$

-- ============================================
-- 10. report.user_id FK (CASCADE 제거)
-- ============================================
DROP PROCEDURE IF EXISTS migrate_report_user_fk$$
CREATE PROCEDURE migrate_report_user_fk()
BEGIN
    DECLARE fk_name VARCHAR(64);
    DECLARE has_cascade VARCHAR(10);

    SELECT CONSTRAINT_NAME, DELETE_RULE INTO fk_name, has_cascade
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report'
      AND REFERENCED_TABLE_NAME = 'user'
    LIMIT 1;

    IF has_cascade = 'CASCADE' THEN
        SET @drop_sql = CONCAT('ALTER TABLE `report` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        ALTER TABLE `report`
          ADD CONSTRAINT `fk_report_user`
          FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`);
    END IF;
END$$

CALL migrate_report_user_fk()$$
DROP PROCEDURE migrate_report_user_fk$$

DELIMITER ;

-- ============================================
-- 마이그레이션 검증
-- ============================================
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN 'SUCCESS: All CASCADE constraints removed'
        ELSE CONCAT('WARNING: ', COUNT(*), ' CASCADE constraints still exist')
    END AS migration_status,
    GROUP_CONCAT(CONCAT(TABLE_NAME, '.', CONSTRAINT_NAME) SEPARATOR ', ') AS remaining_cascades
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND DELETE_RULE = 'CASCADE';

-- ================================================================================================
-- Migration Notes:
-- 1. 동적 FK 이름 감지: 현재 DB의 FK 이름을 자동으로 찾아 처리
-- 2. CASCADE가 있는 FK만 재생성: 불필요한 작업 방지
-- 3. 멱등성 보장: 여러 번 실행해도 안전
-- 4. 새 DB 호환: V1.0 FK 이름 (FKxxx)도 자동 감지
-- 5. 기존 DB 호환: 이미 변경된 FK 이름 (fk_xxx)도 자동 감지
-- 6. 삭제 로직: 애플리케이션 레이어(Service)에서 명시적으로 처리 필요
-- ================================================================================================