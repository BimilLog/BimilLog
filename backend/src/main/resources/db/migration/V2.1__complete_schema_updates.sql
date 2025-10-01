-- ================================================================================================
-- Complete Schema Updates: V2.1 to V2.5 Consolidated
-- Date: 2025-10-01
-- Version: 2.1 (consolidates V2.1, V2.2, V2.4, V2.5)
-- ================================================================================================
-- Description:
--   V2.1~V2.5의 모든 변경사항을 통합한 마이그레이션
--   - V2.1: comment_closure AUTO_INCREMENT 변경
--   - V2.2: token 테이블 분리 (auth_token, kakao_token)
--   - V2.4: user → member 전면 리네이밍
--   - V2.5: CASCADE 설정 및 일부 FK 제거
--   (V2.3의 CASCADE 제거 단계는 생략 - 최종 상태만 반영)
-- ================================================================================================

-- ============================================
-- PART 1: Comment Closure Table Update (V2.1)
-- ============================================

-- comment_closure_seq 테이블 삭제
DROP TABLE IF EXISTS `comment_closure_seq`;

-- comment_closure.id를 AUTO_INCREMENT로 변경
ALTER TABLE `comment_closure`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

-- ============================================
-- PART 2: Token Table Split (V2.2)
-- ============================================

-- 2-1. kakao_token 테이블 생성 (User와 1:1 관계)
CREATE TABLE IF NOT EXISTS `kakao_token` (
  `kakao_token_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Kakao 토큰 ID (PK)',
  `kakao_access_token` VARCHAR(500) NOT NULL COMMENT 'Kakao OAuth 액세스 토큰',
  `kakao_refresh_token` VARCHAR(500) NOT NULL COMMENT 'Kakao OAuth 리프레시 토큰',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `modified_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`kakao_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='카카오 OAuth 토큰 테이블 (User 1:1)';

-- 2-2. jwt_token 테이블 생성 (User와 1:N 관계)
CREATE TABLE IF NOT EXISTS `jwt_token` (
  `jwt_token_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'JWT 토큰 ID (PK)',
  `user_id` BIGINT NOT NULL COMMENT '사용자 ID (FK)',
  `jwt_refresh_token` VARCHAR(500) COMMENT 'JWT 리프레시 토큰',
  `last_used_at` DATETIME COMMENT '토큰 마지막 사용 시각',
  `use_count` INT DEFAULT 0 COMMENT '토큰 사용 횟수',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `modified_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`jwt_token_id`),
  INDEX `idx_jwt_token_user` (`user_id`),
  INDEX `idx_jwt_token_refresh` (`jwt_refresh_token`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='JWT 리프레시 토큰 테이블 (User 1:N)';

-- 2-3. 기존 token 데이터 마이그레이션
INSERT INTO `kakao_token` (`kakao_access_token`, `kakao_refresh_token`, `created_at`, `modified_at`)
SELECT
  t.access_token,
  t.refresh_token,
  t.created_at,
  t.modified_at
FROM `token` t
INNER JOIN (
  SELECT user_id, MAX(token_id) as latest_token_id
  FROM `token`
  GROUP BY user_id
) latest ON t.token_id = latest.latest_token_id
ORDER BY t.user_id;

-- 2-4. user 테이블에 kakao_token_id 컬럼 추가
ALTER TABLE `user`
  ADD COLUMN `kakao_token_id` BIGINT COMMENT 'Kakao 토큰 ID (1:1)' AFTER `setting_id`;

-- 2-5. user와 kakao_token 매핑
UPDATE `user` u
INNER JOIN `token` t ON u.user_id = t.user_id
INNER JOIN (
  SELECT user_id, MAX(token_id) as latest_token_id
  FROM `token`
  GROUP BY user_id
) latest ON t.user_id = latest.user_id AND t.token_id = latest.latest_token_id
INNER JOIN `kakao_token` kt ON
  kt.kakao_access_token COLLATE utf8mb4_0900_ai_ci = t.access_token COLLATE utf8mb4_0900_ai_ci AND
  kt.kakao_refresh_token COLLATE utf8mb4_0900_ai_ci = t.refresh_token COLLATE utf8mb4_0900_ai_ci
SET u.kakao_token_id = kt.kakao_token_id;

-- 2-6. kakao_token_id를 NOT NULL로 변경
ALTER TABLE `user`
  MODIFY COLUMN `kakao_token_id` BIGINT NOT NULL COMMENT 'Kakao 토큰 ID (1:1)';

-- 2-7. jwt_token 데이터 마이그레이션
INSERT INTO `jwt_token` (`user_id`, `jwt_refresh_token`, `last_used_at`, `use_count`, `created_at`, `modified_at`)
SELECT
  user_id,
  '' as jwt_refresh_token,
  NULL as last_used_at,
  0 as use_count,
  created_at,
  modified_at
FROM `token`
ORDER BY token_id;

-- 2-8. 기존 token 테이블 삭제
DROP TABLE IF EXISTS `token`;

-- 2-9. 인덱스 추가
ALTER TABLE `user`
  ADD INDEX `idx_user_kakao_token` (`kakao_token_id`);

-- ============================================
-- PART 3: User to Member Renaming (V2.4)
-- ============================================

-- 3-1. 모든 FK Constraint 제거 (동적 감지)
DELIMITER $$

DROP PROCEDURE IF EXISTS drop_all_user_fks$$
CREATE PROCEDURE drop_all_user_fks()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE fk_name VARCHAR(64);
    DECLARE tbl_name VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT CONSTRAINT_NAME, TABLE_NAME
        FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND (TABLE_NAME IN ('jwt_token', 'post', 'comment', 'comment_like', 'post_like', 'message', 'notification', 'fcm_token', 'report', 'user')
               AND REFERENCED_TABLE_NAME = 'user')
           OR (TABLE_NAME = 'user' AND REFERENCED_TABLE_NAME IN ('setting', 'kakao_token'));
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO fk_name, tbl_name;
        IF done THEN
            LEAVE read_loop;
        END IF;
        SET @drop_sql = CONCAT('ALTER TABLE `', tbl_name, '` DROP FOREIGN KEY `', fk_name, '`');
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END$$

CALL drop_all_user_fks()$$
DROP PROCEDURE drop_all_user_fks$$

DELIMITER ;

-- 3-2. user 테이블 변경
ALTER TABLE `user` CHANGE COLUMN `user_id` `member_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `user` RENAME TO `member`;
ALTER TABLE `member` CHANGE COLUMN `user_name` `member_name` VARCHAR(255) NOT NULL;

-- 3-3. jwt_token 테이블 변경 및 리네이밍
ALTER TABLE `jwt_token`
  CHANGE COLUMN `jwt_token_id` `auth_token_id` BIGINT NOT NULL AUTO_INCREMENT,
  CHANGE COLUMN `user_id` `member_id` BIGINT NOT NULL,
  CHANGE COLUMN `jwt_refresh_token` `refresh_token` VARCHAR(500);

ALTER TABLE `jwt_token` RENAME TO `auth_token`;

-- 3-4. FK 컬럼명 변경 (user_id → member_id)
ALTER TABLE `post` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `comment` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `comment_like` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `post_like` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `message` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `notification` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `fcm_token` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `report` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- ============================================
-- PART 4: Final CASCADE and FK Configuration (V2.5)
-- ============================================

-- 4-1. CASCADE 추가 및 FK 재생성
ALTER TABLE `comment_like`
  ADD CONSTRAINT `fk_comment_like_comment`
  FOREIGN KEY (`comment_id`) REFERENCES `comment` (`comment_id`)
  ON DELETE CASCADE,
  ADD CONSTRAINT `fk_comment_like_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
  ON DELETE CASCADE;

ALTER TABLE `comment`
  ADD CONSTRAINT `fk_comment_post`
  FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`)
  ON DELETE CASCADE,
  ADD CONSTRAINT `fk_comment_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

ALTER TABLE `post_like`
  ADD CONSTRAINT `fk_post_like_post`
  FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`)
  ON DELETE CASCADE,
  ADD CONSTRAINT `fk_post_like_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
  ON DELETE CASCADE;

ALTER TABLE `post`
  ADD CONSTRAINT `fk_post_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

ALTER TABLE `message`
  ADD CONSTRAINT `fk_message_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

ALTER TABLE `notification`
  ADD CONSTRAINT `fk_notification_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);

ALTER TABLE `fcm_token`
  ADD CONSTRAINT `fk_fcm_token_member`
  FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
  ON DELETE CASCADE;

ALTER TABLE `member`
  ADD CONSTRAINT `fk_member_setting`
  FOREIGN KEY (`setting_id`) REFERENCES `setting` (`setting_id`)
  ON DELETE CASCADE;

-- 4-2. member.kakao_token_id를 nullable로 변경 (FK 제거)
ALTER TABLE `member`
  MODIFY COLUMN `kakao_token_id` BIGINT NULL;

-- auth_token.member_id FK는 추가하지 않음 (보안 감사용)
-- report.member_id FK는 추가하지 않음 (법적 보관용)
-- member.kakao_token_id FK는 추가하지 않음 (독립 라이프사이클)

-- ============================================
-- Migration Verification
-- ============================================

-- 테이블 존재 확인
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: member table exists'
        ELSE 'ERROR: member table not found'
    END AS member_table_status
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member';

SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: auth_token table exists'
        ELSE 'ERROR: auth_token table not found'
    END AS auth_token_table_status
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'auth_token';

SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: kakao_token table exists'
        ELSE 'ERROR: kakao_token table not found'
    END AS kakao_token_table_status
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kakao_token';

-- CASCADE 설정 확인 (7개)
SELECT
    CASE
        WHEN COUNT(*) = 7 THEN 'SUCCESS: 7 CASCADE constraints configured'
        ELSE CONCAT('WARNING: Expected 7 CASCADE, found ', COUNT(*))
    END AS cascade_status
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND DELETE_RULE = 'CASCADE'
  AND CONSTRAINT_NAME IN (
    'fk_comment_like_comment',
    'fk_comment_like_member',
    'fk_comment_post',
    'fk_post_like_post',
    'fk_post_like_member',
    'fk_member_setting',
    'fk_fcm_token_member'
  );

-- FK 없는 컬럼 확인 (3개)
SELECT
    'INFO: auth_token.member_id, report.member_id, member.kakao_token_id have NO FK constraint (by design)' AS fk_info;

-- ================================================================================================
-- Migration Notes:
-- 1. V2.1: comment_closure AUTO_INCREMENT 변경
-- 2. V2.2: token → auth_token & kakao_token 분리 (다중 기기, 보안 강화)
-- 3. V2.4: user → member 전면 리네이밍 (테이블, 컬럼, FK)
-- 4. V2.5: 최종 CASCADE 설정 (6개) 및 FK 제거 (3개)
-- 5. V2.3의 "모든 CASCADE 제거" 단계는 생략 (최종 상태만 반영)
--
-- CASCADE가 있는 FK (7개):
--   - comment_like.comment_id, comment_like.member_id
--   - comment.post_id
--   - post_like.post_id, post_like.member_id
--   - member.setting_id
--   - fcm_token.member_id
--
-- FK가 없는 컬럼 (3개):
--   - auth_token.member_id (보안 감사 이력 보존)
--   - report.member_id (법적 보관)
--   - member.kakao_token_id (독립 라이프사이클)
-- ================================================================================================
