-- =============================================================================
-- Post 테이블 비정규화: CQRS Read Model 제거 (H2)
-- =============================================================================

-- 1. post 테이블에 비정규화 컬럼 추가
ALTER TABLE post
    ADD COLUMN like_count INT NOT NULL DEFAULT 0;
ALTER TABLE post
    ADD COLUMN comment_count INT NOT NULL DEFAULT 0;
ALTER TABLE post
    ADD COLUMN member_name VARCHAR(50) DEFAULT '익명';

-- 2. post_read_model에서 기존 데이터 마이그레이션 (H2 MERGE 구문)
UPDATE post p SET
    p.like_count = (SELECT prm.like_count FROM post_read_model prm WHERE prm.post_id = p.post_id),
    p.comment_count = (SELECT prm.comment_count FROM post_read_model prm WHERE prm.post_id = p.post_id),
    p.member_name = (SELECT prm.member_name FROM post_read_model prm WHERE prm.post_id = p.post_id)
WHERE EXISTS (SELECT 1 FROM post_read_model prm WHERE prm.post_id = p.post_id);

-- 3. CQRS 테이블 제거
DROP TABLE IF EXISTS post_read_model_dlq;
DROP TABLE IF EXISTS post_read_model;
