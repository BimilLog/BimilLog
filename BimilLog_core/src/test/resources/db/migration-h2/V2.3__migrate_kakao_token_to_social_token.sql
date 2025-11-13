-- ================================================================================================
-- Social Token Migration: Kakao Token → Social Token (Multi-Provider Support) - H2 Version
-- Date: 2025-11-11
-- Version: 2.3
-- ================================================================================================

-- ============================================
-- 1. social_token 테이블 생성
-- ============================================

CREATE TABLE IF NOT EXISTS social_token (
  social_token_id BIGINT NOT NULL AUTO_INCREMENT,
  access_token VARCHAR(500),
  refresh_token VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (social_token_id)
);

-- ============================================
-- 2. 기존 kakao_token 데이터를 social_token으로 마이그레이션
-- ============================================

INSERT INTO social_token (
  social_token_id,
  access_token,
  refresh_token,
  created_at,
  modified_at
)
SELECT
  kakao_token_id AS social_token_id,
  kakao_access_token AS access_token,
  kakao_refresh_token AS refresh_token,
  created_at,
  modified_at
FROM kakao_token;

-- ============================================
-- 3. member 테이블의 kakao_token_id를 social_token_id로 변경
-- ============================================

-- 3-1. 새 컬럼 추가 (임시)
ALTER TABLE member
  ADD COLUMN social_token_id BIGINT NULL;

-- 3-2. 기존 kakao_token_id 값을 social_token_id로 복사
UPDATE member
SET social_token_id = kakao_token_id
WHERE kakao_token_id IS NOT NULL;

-- 3-3. 기존 kakao_token_id 컬럼 제거
ALTER TABLE member
  DROP COLUMN kakao_token_id;

-- ============================================
-- 4. kakao_token 테이블 삭제
-- ============================================

DROP TABLE IF EXISTS kakao_token;
