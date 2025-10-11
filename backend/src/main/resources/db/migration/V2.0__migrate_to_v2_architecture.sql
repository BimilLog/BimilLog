-- ================================================================================================
-- Complete V2 Architecture Migration: V2.0~V2.4 Consolidated
-- Date: 2025-10-10
-- Version: 2.0 (consolidates V2.0, V2.1, V2.2, V2.3, V2.4)
-- ================================================================================================
-- Description:
--   V1에서 V2 아키텍처로의 전체 마이그레이션 (V2.0~V2.4 통합본)
--   - V2.0: 소셜 로그인 멀티 프로바이더 지원, 테이블/컬럼명 변경
--   - V2.1: comment_closure AUTO_INCREMENT, token 분리, user→member 리네이밍
--   - V2.2: post_cache_flag 제거 (캐싱 전략 변경)
--   - V2.3: message 제약조건명 변경
--   - V2.4: comment.content 길이 변경 (VARCHAR(1000))
--
--   ⚠️ 주의: 이 마이그레이션은 V1.0 베이스라인에서 실행됩니다
--   - V1.0의 `users` 테이블이 존재해야 합니다
--   - 최종적으로 `member` 테이블로 변경됩니다
-- ================================================================================================

-- ============================================
-- 파트 1: 소셜 로그인 마이그레이션 (카카오 → 멀티 프로바이더)
-- ============================================

-- 소셜 로그인 지원을 위한 새 컬럼 추가
ALTER TABLE `users`
  ADD COLUMN `social_id` VARCHAR(255) AFTER `kakao_id`,
  ADD COLUMN `provider` VARCHAR(20) DEFAULT 'KAKAO' AFTER `social_id`,
  ADD COLUMN `social_nickname` VARCHAR(255) AFTER `provider`;

-- 기존 카카오 데이터를 새 컬럼으로 마이그레이션
UPDATE `users`
SET
  `social_id` = CAST(`kakao_id` AS CHAR),
  `provider` = 'KAKAO',
  `social_nickname` = `kakao_nickname`
WHERE `social_id` IS NULL;

-- 데이터 마이그레이션 후 새 컬럼을 NOT NULL로 변경
ALTER TABLE `users`
  MODIFY COLUMN `social_id` VARCHAR(255) NOT NULL,
  MODIFY COLUMN `provider` VARCHAR(20) NOT NULL;

-- 멀티 프로바이더 지원을 위한 복합 유니크 인덱스 추가
ALTER TABLE `users`
  ADD UNIQUE INDEX `uk_provider_social_id` (`provider`, `social_id`);

-- 블랙리스트 테이블을 멀티 프로바이더 지원하도록 업데이트
ALTER TABLE `black_list`
  ADD COLUMN `social_id` VARCHAR(255) AFTER `kakao_id`,
  ADD COLUMN `provider` VARCHAR(20) DEFAULT 'KAKAO' AFTER `social_id`;

-- 기존 블랙리스트 데이터 마이그레이션
UPDATE `black_list`
SET
  `social_id` = CAST(`kakao_id` AS CHAR),
  `provider` = 'KAKAO'
WHERE `social_id` IS NULL;

-- 컬럼을 NOT NULL로 변경
ALTER TABLE `black_list`
  MODIFY COLUMN `social_id` VARCHAR(255) NOT NULL,
  MODIFY COLUMN `provider` VARCHAR(20) NOT NULL;

-- ============================================
-- 파트 2: 메시지 테이블 컬럼 수정
-- ============================================

-- JPA 엔티티와 일치하도록 컬럼명 변경 (width/height → x/y)
ALTER TABLE `message`
  CHANGE COLUMN `width` `x` INT NOT NULL,
  CHANGE COLUMN `height` `y` INT NOT NULL;

-- 올바른 컬럼명으로 유니크 제약조건 업데이트
ALTER TABLE `message`
  DROP INDEX `unique_user_x_y`,
  ADD UNIQUE INDEX `unique_user_x_y` (`user_id`, `x`, `y`);

-- ============================================
-- 파트 3: 알림 테이블 컬럼 수정
-- ============================================

-- JPA 엔티티와 일치하도록 컬럼명 변경 (data → content)
ALTER TABLE `notification`
  CHANGE COLUMN `data` `content` VARCHAR(255) NOT NULL;

-- enum 목록을 확장해 새로운 타입(MESSAGE)을 미리 허용
ALTER TABLE `notification`
  MODIFY COLUMN `notification_type` ENUM('COMMENT','COMMENT_FEATURED','FARM','POST_FEATURED','ADMIN','INITIATE','MESSAGE') NOT NULL;

-- notification_type enum 값 변경 (FARM → MESSAGE)
UPDATE `notification`
SET `notification_type` = 'MESSAGE'
WHERE `notification_type` = 'FARM';

-- notification_type enum 재정의
ALTER TABLE `notification`
  MODIFY COLUMN `notification_type` ENUM('COMMENT','COMMENT_FEATURED','MESSAGE','POST_FEATURED','ADMIN','INITIATE') NOT NULL;

-- ============================================
-- 파트 4: 토큰 테이블 구조 수정
-- ============================================

-- JPA 엔티티와 일치하도록 컬럼명 변경
ALTER TABLE `token`
  CHANGE COLUMN `kakao_access_token` `access_token` VARCHAR(255) NOT NULL,
  CHANGE COLUMN `kakao_refresh_token` `refresh_token` VARCHAR(255) NOT NULL,
  DROP COLUMN `jwt_refresh_token`;

-- 인덱스 추가
ALTER TABLE `token`
  ADD INDEX `idx_token_user` (`user_id`),
  ADD INDEX `idx_token_refresh` (`refresh_token`);

-- ============================================
-- 파트 5: 구 컬럼 정리
-- ============================================

-- users 테이블에서 기존 카카오 전용 컬럼 제거
ALTER TABLE `users`
  DROP INDEX `UKk4ycaj27putgcujmehwbsrmmc`,
  DROP INDEX `idx_user_kakao_id_username`,
  DROP COLUMN `kakao_id`,
  DROP COLUMN `kakao_nickname`;

-- post 테이블에서 기존 popular_flag 컬럼 제거
ALTER TABLE `post`
  DROP COLUMN `popular_flag`;

-- black_list 테이블 기본 키 업데이트 및 id 컬럼 추가
ALTER TABLE `black_list`
  DROP PRIMARY KEY,
  ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
  DROP COLUMN `kakao_id`,
  ADD UNIQUE KEY (`provider`, `social_id`);

-- ============================================
-- 파트 6: 테이블명 변경 (JPA 엔티티명과 일치)
-- ============================================

-- users → user 테이블명 변경
RENAME TABLE `users` TO `user`;

-- black_list → blacklist 테이블명 변경
RENAME TABLE `black_list` TO `blacklist`;

-- ============================================
-- 파트 7: Full-text 인덱스 재생성 (ngram parser)
-- ============================================

-- 기존 풀텍스트 인덱스 삭제 (존재하는 경우)
SET @drop_idx_title = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'post'
        AND INDEX_NAME = 'post_title_IDX'
    ),
    'ALTER TABLE `post` DROP INDEX `post_title_IDX`',
    'SELECT "post_title_IDX not exists"'
);
PREPARE stmt FROM @drop_idx_title;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_idx_title_content = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'post'
        AND INDEX_NAME = 'post_title_content_IDX'
    ),
    'ALTER TABLE `post` DROP INDEX `post_title_content_IDX`',
    'SELECT "post_title_content_IDX not exists"'
);
PREPARE stmt FROM @drop_idx_title_content;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 풀텍스트 인덱스 재생성 (ngram_token_size=2 사용)
ALTER TABLE `post` ADD FULLTEXT INDEX `post_title_IDX` (`title`) WITH PARSER ngram;
ALTER TABLE `post` ADD FULLTEXT INDEX `post_title_content_IDX` (`title`, `content`) WITH PARSER ngram;

-- ============================================
-- 파트 8: V2.1 통합 - Comment Closure Table Update
-- ============================================

-- comment_closure_seq 테이블 삭제
DROP TABLE IF EXISTS `comment_closure_seq`;

-- comment_closure.id를 AUTO_INCREMENT로 변경
ALTER TABLE `comment_closure`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

-- ============================================
-- 파트 9: V2.1 통합 - Token Table Split
-- ============================================

-- 9-1. kakao_token 테이블 생성 (User와 1:1 관계)
CREATE TABLE IF NOT EXISTS `kakao_token` (
  `kakao_token_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Kakao 토큰 ID (PK)',
  `kakao_access_token` VARCHAR(500) NOT NULL COMMENT 'Kakao OAuth 액세스 토큰',
  `kakao_refresh_token` VARCHAR(500) NOT NULL COMMENT 'Kakao OAuth 리프레시 토큰',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `modified_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`kakao_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='카카오 OAuth 토큰 테이블 (User 1:1)';

-- 9-2. jwt_token 테이블 생성 (User와 1:N 관계)
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

-- 9-3. 기존 token 데이터 마이그레이션
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

-- 9-4. user 테이블에 kakao_token_id 컬럼 추가
ALTER TABLE `user`
  ADD COLUMN `kakao_token_id` BIGINT COMMENT 'Kakao 토큰 ID (1:1)' AFTER `setting_id`;

-- 9-5. user와 kakao_token 매핑
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

-- 9-6. kakao_token_id를 NOT NULL로 변경
ALTER TABLE `user`
  MODIFY COLUMN `kakao_token_id` BIGINT NOT NULL COMMENT 'Kakao 토큰 ID (1:1)';

-- 9-7. jwt_token 데이터 마이그레이션
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

-- 9-8. 기존 token 테이블 삭제
DROP TABLE IF EXISTS `token`;

-- 9-9. 인덱스 추가
ALTER TABLE `user`
  ADD INDEX `idx_user_kakao_token` (`kakao_token_id`);

-- ============================================
-- 파트 10: V2.1 통합 - User to Member Renaming
-- ============================================

-- 10-1. 모든 FK Constraint 제거 (동적 감지)
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

-- 10-2. user 테이블 변경
ALTER TABLE `user` CHANGE COLUMN `user_id` `member_id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `user` RENAME TO `member`;
ALTER TABLE `member` CHANGE COLUMN `user_name` `member_name` VARCHAR(255) NOT NULL;

-- 10-3. jwt_token 테이블 변경 및 리네이밍
ALTER TABLE `jwt_token`
  CHANGE COLUMN `jwt_token_id` `auth_token_id` BIGINT NOT NULL AUTO_INCREMENT,
  CHANGE COLUMN `user_id` `member_id` BIGINT NOT NULL,
  CHANGE COLUMN `jwt_refresh_token` `refresh_token` VARCHAR(500);

ALTER TABLE `jwt_token` RENAME TO `auth_token`;

-- 10-4. FK 컬럼명 변경 (user_id → member_id)
ALTER TABLE `post` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `comment` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `comment_like` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `post_like` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `message` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `notification` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `fcm_token` CHANGE COLUMN `user_id` `member_id` BIGINT;
ALTER TABLE `report` CHANGE COLUMN `user_id` `member_id` BIGINT;

-- ============================================
-- 파트 11: V2.1 통합 - Final CASCADE and FK Configuration
-- ============================================

-- CASCADE 추가 및 FK 재생성
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

-- member.kakao_token_id를 nullable로 변경 (FK 제거)
ALTER TABLE `member`
  MODIFY COLUMN `kakao_token_id` BIGINT NULL;

-- auth_token.member_id FK는 추가하지 않음 (보안 감사용)
-- report.member_id FK는 추가하지 않음 (법적 보관용)
-- member.kakao_token_id FK는 추가하지 않음 (독립 라이프사이클)

-- ============================================
-- 파트 12: V2.3 통합 - Message 테이블 유니크 제약조건명 변경
-- ============================================

-- message 테이블의 유니크 제약조건명 변경
ALTER TABLE `message`
  DROP INDEX `unique_user_x_y`,
  ADD UNIQUE INDEX `unique_member_x_y` (`member_id`, `x`, `y`);

-- ============================================
-- 파트 13: V2.4 통합 - Comment Content Length Update
-- ============================================

-- comment.content 컬럼 길이 변경
ALTER TABLE `comment`
  MODIFY COLUMN `content` VARCHAR(1000) NOT NULL COMMENT '댓글 내용 (사용자 입력 기준 255자, 서버 저장 기준 1000자)';

-- ================================================================================================
-- 마이그레이션 완료
-- ================================================================================================
SELECT '통합 마이그레이션 V2.0이 성공적으로 완료되었습니다. (V2.0~V2.4 통합)' AS status;
