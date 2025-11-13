-- ================================================================================================
-- Social Token Migration: Kakao Token → Social Token (Multi-Provider Support)
-- Date: 2025-11-11
-- Version: 2.3
-- ================================================================================================
-- Description:
--   카카오 전용 토큰 테이블을 멀티 프로바이더 지원 소셜 토큰 테이블로 마이그레이션
--   - kakao_token → social_token (테이블명 변경)
--   - member.kakao_token_id → member.social_token_id (컬럼명 변경)
--   - provider 구분은 member 테이블의 provider 컬럼 사용 (1:1 관계)
--
--   ⚠️ 주의: 이 마이그레이션은 V2.2 이후에 실행됩니다
--   - kakao_token 테이블이 존재해야 합니다
-- ================================================================================================

-- ============================================
-- 1. social_token 테이블 생성
-- ============================================

CREATE TABLE IF NOT EXISTS `social_token` (
  `social_token_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '소셜 토큰 ID (PK)',
  `access_token` VARCHAR(500) COMMENT '소셜 플랫폼 액세스 토큰',
  `refresh_token` VARCHAR(500) COMMENT '소셜 플랫폼 리프레시 토큰',
  `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
  `modified_at` TIMESTAMP(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',
  PRIMARY KEY (`social_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='소셜 플랫폼 OAuth 토큰 테이블 (멀티 프로바이더 지원, provider는 member 테이블 참조)';

-- ============================================
-- 2. 기존 kakao_token 데이터를 social_token으로 마이그레이션
-- ============================================

INSERT INTO `social_token` (
  `social_token_id`,
  `access_token`,
  `refresh_token`,
  `created_at`,
  `modified_at`
)
SELECT
  `kakao_token_id` AS `social_token_id`,
  `kakao_access_token` AS `access_token`,
  `kakao_refresh_token` AS `refresh_token`,
  `created_at`,
  `modified_at`
FROM `kakao_token`;

-- ============================================
-- 3. member 테이블의 kakao_token_id를 social_token_id로 변경
-- ============================================

-- 3-1. 새 컬럼 추가 (임시)
ALTER TABLE `member`
  ADD COLUMN `social_token_id` BIGINT NULL COMMENT '소셜 토큰 ID (1:1)' AFTER `setting_id`;

-- 3-2. 기존 kakao_token_id 값을 social_token_id로 복사
UPDATE `member`
SET `social_token_id` = `kakao_token_id`
WHERE `kakao_token_id` IS NOT NULL;

-- 3-3. 기존 kakao_token_id 컬럼 제거
ALTER TABLE `member`
  DROP COLUMN `kakao_token_id`;

-- ============================================
-- 4. 인덱스 업데이트
-- ============================================

-- 기존 인덱스 제거 (존재하는 경우)
SET @exist_idx_user_kakao_token := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'member'
    AND INDEX_NAME = 'idx_user_kakao_token'
);

SET @sql_drop_idx = IF(@exist_idx_user_kakao_token > 0,
  'ALTER TABLE `member` DROP INDEX `idx_user_kakao_token`;',
  'SELECT "Index idx_user_kakao_token does not exist";'
);

PREPARE stmt FROM @sql_drop_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새 인덱스 추가
ALTER TABLE `member`
  ADD INDEX `idx_member_social_token` (`social_token_id`);

-- ============================================
-- 5. kakao_token 테이블 삭제
-- ============================================

DROP TABLE IF EXISTS `kakao_token`;

-- ================================================================================================
-- 마이그레이션 완료
-- ================================================================================================
-- 결과:
--   ✓ social_token 테이블 생성 (멀티 프로바이더 지원, provider는 member 테이블 참조)
--   ✓ 기존 카카오 토큰 데이터 마이그레이션
--   ✓ member.kakao_token_id → member.social_token_id 변경
--   ✓ 인덱스 업데이트 (idx_member_social_token)
--   ✓ kakao_token 테이블 삭제
--
-- 다음 단계:
--   - 애플리케이션 코드에서 KakaoToken → SocialToken 변경
--   - 네이버, 구글 등 추가 프로바이더 지원
--   - Provider 구분은 member.provider 컬럼 사용 (1:1 관계)
-- ================================================================================================
