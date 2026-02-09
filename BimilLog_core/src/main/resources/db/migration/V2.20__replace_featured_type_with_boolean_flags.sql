-- =============================================================================
-- featured_type VARCHAR → is_weekly, is_legend, is_notice BOOLEAN 전환
-- 한 게시글이 복수 카테고리에 동시 소속 가능하도록 변경
-- =============================================================================

-- 1. boolean 컬럼 추가
ALTER TABLE post ADD COLUMN is_weekly BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE post ADD COLUMN is_legend BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE post ADD COLUMN is_notice BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. 기존 데이터 마이그레이션
UPDATE post SET is_weekly = TRUE WHERE featured_type = 'WEEKLY';
UPDATE post SET is_legend = TRUE WHERE featured_type = 'LEGEND';
UPDATE post SET is_notice = TRUE WHERE featured_type = 'NOTICE';

-- 3. 기존 컬럼 및 인덱스 제거
DROP INDEX idx_post_featured_type ON post;
ALTER TABLE post DROP COLUMN featured_type;

-- 4. 새 인덱스 추가
CREATE INDEX idx_post_is_weekly ON post (is_weekly);
CREATE INDEX idx_post_is_legend ON post (is_legend);
CREATE INDEX idx_post_is_notice ON post (is_notice);
