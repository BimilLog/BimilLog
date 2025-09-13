-- V1에서 V2 아키텍처로 전체 마이그레이션
-- 날짜: 2025-01-20
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

-- 새로운 캐시 플래그 컬럼 추가
ALTER TABLE `post`
  ADD COLUMN `post_cache_flag` VARCHAR(20) AFTER `popular_flag`;

-- 기존 데이터 마이그레이션
UPDATE `post`
SET `post_cache_flag` =
  CASE `popular_flag`
    WHEN 'LEGEND' THEN 'LEGEND'
    WHEN 'REALTIME' THEN 'POPULAR'
    WHEN 'WEEKLY' THEN 'POPULAR'
    ELSE NULL
  END
WHERE `popular_flag` IS NOT NULL;

-- 기존 인덱스 삭제
ALTER TABLE `post`
  DROP INDEX `idx_post_created_at_popular`,
  DROP INDEX `idx_post_popular_flag`;

-- post_cache_flag를 사용한 새 인덱스 생성
ALTER TABLE `post`
  ADD INDEX `idx_post_created_at_cache` (`created_at`, `post_cache_flag`),
  ADD INDEX `idx_post_cache_flag` (`post_cache_flag`);

-- ============================================
-- 파트 3: 구 컬럼 정리
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

-- black_list 테이블 기본 키 업데이트
ALTER TABLE `black_list`
  DROP PRIMARY KEY,
  DROP COLUMN `kakao_id`,
  ADD PRIMARY KEY (`provider`, `social_id`);

-- ============================================
-- 파트 4: 메시지 테이블 컬럼 수정
-- ============================================

-- JPA 엔티티와 일치하도록 컬럼명 변경
ALTER TABLE `message`
  CHANGE COLUMN `receiver_id` `user_id` BIGINT NOT NULL,
  CHANGE COLUMN `sender_name` `anonymity` VARCHAR(255) NOT NULL,
  CHANGE COLUMN `grid_x` `x` INT NOT NULL,
  CHANGE COLUMN `grid_y` `y` INT NOT NULL;

-- 올바른 컬럼명으로 유니크 제약조건 업데이트
ALTER TABLE `message`
  DROP INDEX `unique_user_x_y`,
  ADD UNIQUE INDEX `unique_user_x_y` (`user_id`, `x`, `y`);

-- ============================================
-- 파트 5: V2 아키텍처를 위한 테이블명 변경
-- ============================================

-- JPA 엔티티 명명 규칙에 맞게 테이블명 변경
RENAME TABLE `users` TO `user`;
RENAME TABLE `black_list` TO `blacklist`;

-- ============================================
-- 파트 6: 누락된 인덱스 추가
-- ============================================

-- CommentClosure 계층 쿼리를 위한 인덱스 추가
CREATE INDEX idx_comment_closure_ancestor_depth
ON comment_closure (descendant_id, depth);

-- PostLike 사용자-게시글 조회를 위한 인덱스 추가
CREATE INDEX idx_postlike_user_post
ON post_like (user_id, post_id);

-- ============================================
-- 파트 7: 타임스탬프 정밀도 업데이트 (선택사항이지만 권장)
-- ============================================

-- 모든 타임스탬프 컬럼을 마이크로초 정밀도를 위해 TIMESTAMP(6)로 업데이트
ALTER TABLE `user`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `post`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `comment`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `message`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `notification`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `report`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `blacklist`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `token`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `fcm_token`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

ALTER TABLE `post_like`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL,
  MODIFY COLUMN `modified_at` TIMESTAMP(6) NULL DEFAULT NULL;

-- ============================================
-- 마이그레이션 완료
-- ============================================
SELECT '마이그레이션 V2.0이 성공적으로 완료되었습니다. 데이터베이스 스키마가 이제 V2 JPA 엔티티와 일치합니다.' AS status;