-- V1에서 V2 아키텍처로 전체 마이그레이션
-- 날짜: 2025-01-14
-- 이 마이그레이션은 운영 데이터베이스를 V2 JPA 엔티티와 일치하도록 업데이트합니다

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
-- 파트 2: 게시글 캐시 플래그 마이그레이션
-- ============================================

-- 새로운 캐시 플래그 컬럼 추가 (ENUM 타입으로 정의)
ALTER TABLE `post`
  ADD COLUMN `post_cache_flag` ENUM('REALTIME', 'WEEKLY', 'LEGEND', 'NOTICE') AFTER `popular_flag`;

-- 기존 데이터 마이그레이션
UPDATE `post`
SET `post_cache_flag` =
  CASE `popular_flag`
    WHEN 'LEGEND' THEN 'LEGEND'
    WHEN 'REALTIME' THEN 'REALTIME'
    WHEN 'WEEKLY' THEN 'WEEKLY'
    ELSE NULL
  END
WHERE `popular_flag` IS NOT NULL;

-- 기존 인덱스 삭제
ALTER TABLE `post`
  DROP INDEX `idx_post_created_at_popular`,
  DROP INDEX `idx_post_popular_flag`;

-- post_cache_flag를 사용한 새 인덱스 생성
ALTER TABLE `post`
  ADD INDEX `idx_post_created_at_popular` (`created_at`, `post_cache_flag`),
  ADD INDEX `idx_post_popular_flag` (`post_cache_flag`);

-- ============================================
-- 파트 3: 메시지 테이블 컬럼 수정
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
-- 파트 4: 알림 테이블 컬럼 수정
-- ============================================

-- JPA 엔티티와 일치하도록 컬럼명 변경 (data → content)
ALTER TABLE `notification`
  CHANGE COLUMN `data` `content` VARCHAR(255) NOT NULL;

-- notification_type enum 값 변경 (FARM → MESSAGE)
UPDATE `notification`
SET `notification_type` = 'MESSAGE'
WHERE `notification_type` = 'FARM';

-- notification_type enum 재정의
ALTER TABLE `notification`
  MODIFY COLUMN `notification_type` ENUM('COMMENT','COMMENT_FEATURED','MESSAGE','POST_FEATURED','ADMIN','INITIATE') NOT NULL;

-- ============================================
-- 파트 5: FCM 토큰 테이블 수정
-- ============================================

-- JPA 엔티티에 유니크 제약조건이 없으므로 추가하지 않음
-- fcm_registration_token 컬럼은 이미 NOT NULL 상태

-- ============================================
-- 파트 6: 토큰 테이블 구조 수정
-- ============================================

-- JPA 엔티티와 일치하도록 컬럼명 변경
-- Token 엔티티는 실제로 accessToken, refreshToken 필드를 가지고 있음
-- 이는 카카오 토큰을 저장하는 용도임 (JWT는 별도 관리)
ALTER TABLE `token`
  CHANGE COLUMN `kakao_access_token` `access_token` VARCHAR(255) NOT NULL,
  CHANGE COLUMN `kakao_refresh_token` `refresh_token` VARCHAR(255) NOT NULL,
  DROP COLUMN `jwt_refresh_token`;

-- 인덱스 추가
ALTER TABLE `token`
  ADD INDEX `idx_token_user` (`user_id`),
  ADD INDEX `idx_token_refresh` (`refresh_token`);

-- ============================================
-- 파트 7: 좋아요 테이블 PK 컬럼명 수정
-- ============================================

-- comment_like 테이블 PK 컬럼명 수정
ALTER TABLE `comment_like`
  CHANGE COLUMN `comment_like_id` `CommentLike_id` BIGINT NOT NULL AUTO_INCREMENT;

-- 유니크 제약조건은 이미 V1에 존재하므로 추가하지 않음

-- post_like 테이블 PK 컬럼명 수정
ALTER TABLE `post_like`
  CHANGE COLUMN `post_like_id` `postLike_id` BIGINT NOT NULL AUTO_INCREMENT;

-- ============================================
-- 파트 8: 구 컬럼 정리
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
-- 파트 9: 테이블명 변경 (JPA 엔티티명과 일치)
-- ============================================

-- users → user 테이블명 변경
RENAME TABLE `users` TO `user`;

-- black_list → blacklist 테이블명 변경
RENAME TABLE `black_list` TO `blacklist`;

-- ============================================
-- 파트 10: 인덱스 확인
-- ============================================

-- 필요한 인덱스들은 이미 V1에서 생성되어 있음
-- comment_closure: idx_comment_closure_ancestor_depth
-- post_like: idx_postlike_user_post
-- comment_like: uk_comment_like_user_comment (인덱스 역할도 수행)

-- ============================================
-- 파트 11: Full-text 인덱스 재생성 (ngram parser)
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
-- 제목 전용 인덱스
ALTER TABLE `post` ADD FULLTEXT INDEX `post_title_IDX` (`title`) WITH PARSER ngram;

-- 제목+내용 복합 인덱스
ALTER TABLE `post` ADD FULLTEXT INDEX `post_title_content_IDX` (`title`, `content`) WITH PARSER ngram;

-- 풀텍스트 인덱스 생성 확인
SELECT
    CASE
        WHEN COUNT(*) = 2 THEN 'Fulltext indexes created successfully'
        ELSE CONCAT('ERROR: Expected 2 fulltext indexes, found ', COUNT(*))
    END AS fulltext_status,
    GROUP_CONCAT(INDEX_NAME ORDER BY INDEX_NAME) AS index_names
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'post'
    AND INDEX_TYPE = 'FULLTEXT';

-- ============================================
-- 파트 12: 마이그레이션 검증
-- ============================================

-- 테이블 이름 변경 확인
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: user table exists'
        ELSE 'ERROR: user table not found'
    END AS user_table_status
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user';

SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: blacklist table exists'
        ELSE 'ERROR: blacklist table not found'
    END AS blacklist_table_status
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blacklist';

-- 주요 컬럼 변경 확인
SELECT
    CASE
        WHEN COUNT(*) = 2 THEN 'SUCCESS: social_id and provider columns exist in user table'
        ELSE 'ERROR: social columns not found in user table'
    END AS social_columns_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME IN ('social_id', 'provider');

SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: post_cache_flag column exists'
        ELSE 'ERROR: post_cache_flag column not found'
    END AS cache_flag_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'post'
    AND COLUMN_NAME = 'post_cache_flag';

-- 좋아요 테이블 PK 컬럼명 변경 확인
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: CommentLike_id column exists'
        ELSE 'ERROR: CommentLike_id column not found'
    END AS comment_like_pk_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'comment_like'
    AND COLUMN_NAME = 'CommentLike_id';

SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: postLike_id column exists'
        ELSE 'ERROR: postLike_id column not found'
    END AS post_like_pk_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'post_like'
    AND COLUMN_NAME = 'postLike_id';

-- 메시지 테이블 x, y 컬럼 확인
SELECT
    CASE
        WHEN COUNT(*) = 2 THEN 'SUCCESS: x and y columns exist in message table'
        ELSE 'ERROR: x/y columns not found in message table'
    END AS message_coordinates_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'message'
    AND COLUMN_NAME IN ('x', 'y');

-- 알림 테이블 content 컬럼 확인
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: content column exists in notification table'
        ELSE 'ERROR: content column not found in notification table'
    END AS notification_content_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'notification'
    AND COLUMN_NAME = 'content';

-- ============================================
-- 마이그레이션 완료
-- ============================================
SELECT '마이그레이션 V2.0이 성공적으로 완료되었습니다. 데이터베이스 스키마가 이제 V2 JPA 엔티티와 일치합니다.' AS status;