-- =============================================================================
-- Post 테이블에 featured_type 컬럼 추가, featured_post 테이블 제거
-- 공지/주간/레전드 상태를 Post 엔티티에서 직접 관리
-- =============================================================================

-- 1. post 테이블에 featured_type 컬럼 추가
ALTER TABLE post ADD COLUMN featured_type VARCHAR(20) NULL;

-- 2. featured_post 테이블에서 데이터 마이그레이션 (우선순위: NOTICE > LEGEND > WEEKLY)
-- NOTICE 먼저 (최고 우선순위)
UPDATE post p
    INNER JOIN featured_post fp ON p.post_id = fp.post_id
SET p.featured_type = 'NOTICE'
WHERE fp.type = 'NOTICE';

-- LEGEND (NOTICE가 아닌 글만)
UPDATE post p
    INNER JOIN featured_post fp ON p.post_id = fp.post_id
SET p.featured_type = 'LEGEND'
WHERE fp.type = 'LEGEND' AND p.featured_type IS NULL;

-- WEEKLY (NOTICE, LEGEND가 아닌 글만)
UPDATE post p
    INNER JOIN featured_post fp ON p.post_id = fp.post_id
SET p.featured_type = 'WEEKLY'
WHERE fp.type = 'WEEKLY' AND p.featured_type IS NULL;

-- 3. 인덱스 추가
CREATE INDEX idx_post_featured_type ON post (featured_type);

-- 4. featured_post 테이블 제거
DROP TABLE IF EXISTS featured_post;
