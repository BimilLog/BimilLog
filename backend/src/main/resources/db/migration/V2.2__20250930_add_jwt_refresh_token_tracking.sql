-- ================================================================================================
-- JWT Refresh Token Tracking & Security Enhancement
-- Date: 2025-09-30
-- Version: 2.2
-- ================================================================================================
-- Description:
--   JWT 리프레시 토큰 보안 강화를 위한 스키마 변경
--   - DB에 JWT 리프레시 토큰 저장 (클라이언트 토큰과 비교 검증)
--   - 토큰 사용 이력 추적 (재사용 공격 감지)
--   - Token Rotation 지원
-- ================================================================================================

-- 1. token 테이블에 JWT 리프레시 토큰 저장 컬럼 추가
ALTER TABLE token ADD COLUMN jwt_refresh_token VARCHAR(500) COMMENT 'JWT 리프레시 토큰 (DB 저장용, 탈취 감지)';

-- 2. 토큰 마지막 사용 시각 추가 (Token Rotation 관리)
ALTER TABLE token ADD COLUMN last_used_at DATETIME COMMENT '토큰 마지막 사용 시각';

-- 3. 토큰 사용 횟수 추가 (재사용 공격 감지)
ALTER TABLE token ADD COLUMN use_count INT DEFAULT 0 COMMENT '토큰 사용 횟수 (Rotation 감지용)';

-- 4. jwt_refresh_token 컬럼에 인덱스 추가 (검색 성능 향상)
CREATE INDEX idx_token_jwt_refresh ON token(jwt_refresh_token(255));

-- ================================================================================================
-- Security Enhancement Notes:
-- 1. DB에 저장된 JWT 리프레시 토큰과 클라이언트 토큰 비교 검증
-- 2. use_count > 0 인 토큰 재사용 시도 → 탈취 의심 → 모든 세션 무효화
-- 3. Token Rotation: 리프레시 시 새 토큰 발급 및 DB 업데이트
-- 4. last_used_at: 마지막 토큰 사용 시각 기록
-- ================================================================================================