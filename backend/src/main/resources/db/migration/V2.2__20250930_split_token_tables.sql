-- ================================================================================================
-- Token Table Split: JWT Token & Kakao Token Separation
-- Date: 2025-09-30
-- Version: 2.2
-- ================================================================================================
-- Description:
--   기존 token 테이블을 jwt_token과 kakao_token으로 분리
--   - jwt_token: User 1:N 관계 (다중 기기 지원, JWT 리프레시 토큰 관리)
--   - kakao_token: User 1:1 관계 (카카오 OAuth 토큰 관리)
--   - 설계 개선: 토큰 책임 분리, 정규화, 보안 강화
-- ================================================================================================

-- ============================================
-- 1. kakao_token 테이블 생성 (User와 1:1 관계)
-- ============================================
CREATE TABLE IF NOT EXISTS `kakao_token` (
  `kakao_token_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Kakao 토큰 ID (PK)',
  `kakao_access_token` VARCHAR(500) NOT NULL COMMENT 'Kakao OAuth 액세스 토큰',
  `kakao_refresh_token` VARCHAR(500) NOT NULL COMMENT 'Kakao OAuth 리프레시 토큰',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `modified_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`kakao_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='카카오 OAuth 토큰 테이블 (User 1:1)';

-- ============================================
-- 2. jwt_token 테이블 생성 (User와 1:N 관계, 다중 기기 지원)
-- ============================================
CREATE TABLE IF NOT EXISTS `jwt_token` (
  `jwt_token_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'JWT 토큰 ID (PK)',
  `user_id` BIGINT NOT NULL COMMENT '사용자 ID (FK)',
  `jwt_refresh_token` VARCHAR(500) COMMENT 'JWT 리프레시 토큰 (Token Rotation)',
  `last_used_at` DATETIME COMMENT '토큰 마지막 사용 시각',
  `use_count` INT DEFAULT 0 COMMENT '토큰 사용 횟수 (재사용 공격 감지)',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 시각',
  `modified_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 시각',
  PRIMARY KEY (`jwt_token_id`),
  INDEX `idx_jwt_token_user` (`user_id`),
  INDEX `idx_jwt_token_refresh` (`jwt_refresh_token`(255)),
  CONSTRAINT `fk_jwt_token_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='JWT 리프레시 토큰 테이블 (User 1:N, 다중 기기)';

-- ============================================
-- 3. 기존 token 테이블 데이터 마이그레이션
-- ============================================

-- 3-1. kakao_token 테이블에 데이터 삽입 (사용자당 가장 최신 토큰 사용)
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

-- 3-2. users 테이블에 kakao_token_id 컬럼 추가 (1:1 관계)
ALTER TABLE `users`
  ADD COLUMN `kakao_token_id` BIGINT COMMENT 'Kakao 토큰 ID (1:1)' AFTER `setting_id`;

-- 3-3. users와 kakao_token 매핑 (user_id 순서로 1:1 매핑)
SET @row_number := 0;
UPDATE `users` u
INNER JOIN (
  SELECT
    kt.kakao_token_id,
    (@row_number := @row_number + 1) as row_num
  FROM `kakao_token` kt
  ORDER BY kt.kakao_token_id
) kt_ordered ON kt_ordered.row_num = (
  SELECT (@row_user := @row_user + 1)
  FROM (SELECT @row_user := 0) init
  WHERE u.user_id >= (SELECT MIN(user_id) FROM `users`)
  LIMIT 1
)
SET u.kakao_token_id = kt_ordered.kakao_token_id;

-- 더 안전한 매핑 방법: token 테이블의 user_id를 통한 직접 매핑
UPDATE `users` u
INNER JOIN `token` t ON u.user_id = t.user_id
INNER JOIN (
  SELECT user_id, MAX(token_id) as latest_token_id
  FROM `token`
  GROUP BY user_id
) latest ON t.user_id = latest.user_id AND t.token_id = latest.latest_token_id
INNER JOIN `kakao_token` kt ON
  kt.kakao_access_token = t.access_token AND
  kt.kakao_refresh_token = t.refresh_token
SET u.kakao_token_id = kt.kakao_token_id;

-- 3-4. kakao_token_id를 NOT NULL로 변경 및 FK 제약조건 추가
ALTER TABLE `users`
  MODIFY COLUMN `kakao_token_id` BIGINT NOT NULL COMMENT 'Kakao 토큰 ID (1:1)',
  ADD CONSTRAINT `fk_user_kakao_token` FOREIGN KEY (`kakao_token_id`)
    REFERENCES `kakao_token` (`kakao_token_id`);

-- 3-5. jwt_token 테이블에 모든 기존 token 레코드 마이그레이션
INSERT INTO `jwt_token` (`user_id`, `jwt_refresh_token`, `last_used_at`, `use_count`, `created_at`, `modified_at`)
SELECT
  user_id,
  '' as jwt_refresh_token,  -- 빈 문자열로 초기화 (서비스에서 업데이트)
  NULL as last_used_at,
  0 as use_count,
  created_at,
  modified_at
FROM `token`
ORDER BY token_id;

-- ============================================
-- 4. 기존 token 테이블 삭제
-- ============================================
DROP TABLE IF EXISTS `token`;

-- ============================================
-- 5. 인덱스 추가 (성능 최적화)
-- ============================================
ALTER TABLE `users`
  ADD INDEX `idx_user_kakao_token` (`kakao_token_id`);

-- ================================================================================================
-- Migration Notes:
-- 1. kakao_token: 사용자당 가장 최신 token의 카카오 토큰 사용 (1:1 관계)
-- 2. jwt_token: 모든 기존 token 레코드를 마이그레이션 (1:N 관계, 다중 기기)
-- 3. jwt_refresh_token: 빈 문자열로 초기화 (SocialLoginService에서 실제 값 설정)
-- 4. Token Rotation: use_count와 last_used_at을 활용한 재사용 공격 감지
-- 5. 보안 강화: JWT 리프레시 토큰 DB 저장 및 비교 검증
-- ================================================================================================