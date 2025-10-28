-- ================================================================================================
-- Complete V2 Architecture Migration for H2 Test DB: V2.0~V2.4 Consolidated
-- Date: 2025-10-10
-- Version: 2.0 (consolidates V2.0, V2.1, V2.2, V2.3, V2.4)
-- ================================================================================================
-- Description:
--   H2 테스트 환경에서의 V2 아키텍처 마이그레이션 (V2.0~V2.4 통합본)
--   - V2.0: 초기 스키마에 이미 반영되어 있어 별도 작업 불필요
--   - V2.1: post_like, comment_like에 created_at, modified_at 추가 (BaseEntity 상속)
--   - V2.2: post_cache_flag 제거 (캐싱 전략 변경)
--   - V2.3: message 제약조건명 변경
--   - V2.4: comment.content 길이 변경
-- ================================================================================================

-- ============================================
-- V2.1 통합: post_like와 comment_like에 BaseEntity 추가
-- ============================================

-- 파트 1: post_like 마이그레이션
-- 1-1. 컬럼 추가 (NULL 허용)
ALTER TABLE post_like ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6) NULL;
ALTER TABLE post_like ADD COLUMN IF NOT EXISTS modified_at TIMESTAMP(6) NULL;

-- 1-2. 기존 데이터 타임스탬프 생성 (id를 사용하여 6개월 전부터 1분씩 증가)
UPDATE post_like
SET created_at = DATEADD('MINUTE', post_like_id, DATEADD('MONTH', -6, CURRENT_TIMESTAMP())),
    modified_at = DATEADD('MINUTE', post_like_id, DATEADD('MONTH', -6, CURRENT_TIMESTAMP()))
WHERE created_at IS NULL;

-- 1-3. NOT NULL 제약 조건 적용 (H2 구문)
ALTER TABLE post_like ALTER COLUMN created_at TIMESTAMP(6) NOT NULL;

-- 파트 2: comment_like 마이그레이션
-- 2-1. 컬럼 추가 (NULL 허용)
ALTER TABLE comment_like ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6) NULL;
ALTER TABLE comment_like ADD COLUMN IF NOT EXISTS modified_at TIMESTAMP(6) NULL;

-- 2-2. 기존 데이터 타임스탬프 생성 (id를 사용하여 6개월 전부터 1분씩 증가)
UPDATE comment_like
SET created_at = DATEADD('MINUTE', comment_like_id, DATEADD('MONTH', -6, CURRENT_TIMESTAMP())),
    modified_at = DATEADD('MINUTE', comment_like_id, DATEADD('MONTH', -6, CURRENT_TIMESTAMP()))
WHERE created_at IS NULL;

-- 2-3. NOT NULL 제약 조건 적용 (H2 구문)
ALTER TABLE comment_like ALTER COLUMN created_at TIMESTAMP(6) NOT NULL;

-- ============================================
-- V2.2 통합: post_cache_flag 제거
-- ============================================

-- post_cache_flag 관련 인덱스 제거 (H2 구문)
DROP INDEX IF EXISTS idx_post_created_at_popular;
DROP INDEX IF EXISTS idx_post_popular_flag;

-- post_cache_flag 컬럼 제거 (H2 구문)
ALTER TABLE post DROP COLUMN IF EXISTS post_cache_flag;

-- ============================================
-- V2.3 통합: Message 테이블 유니크 제약조건명 변경
-- ============================================

-- H2에서는 제약조건을 직접 변경할 수 없으므로 DROP 후 ADD
ALTER TABLE message DROP CONSTRAINT IF EXISTS unique_user_x_y;
ALTER TABLE message ADD CONSTRAINT unique_member_x_y UNIQUE (member_id, x, y);

-- ============================================
-- V2.4 통합: Comment Content Length Update
-- ============================================

-- H2에서는 ALTER COLUMN 구문 사용 (MySQL의 MODIFY COLUMN과 다름)
ALTER TABLE comment ALTER COLUMN content VARCHAR(1000) NOT NULL;

-- ================================================================================================
-- 마이그레이션 완료
-- ================================================================================================
-- 통합 마이그레이션 V2.0이 성공적으로 완료되었습니다. (V2.0~V2.4 통합)
