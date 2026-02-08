-- =============================================================================
-- Post 테이블 비정규화: CQRS Read Model 제거
-- post 테이블에 like_count, comment_count, member_name 컬럼 추가
-- post_read_model, post_read_model_dlq 테이블 제거
-- =============================================================================

-- 1. post 테이블에 비정규화 컬럼 추가
ALTER TABLE post
    ADD COLUMN like_count INT NOT NULL DEFAULT 0,
    ADD COLUMN comment_count INT NOT NULL DEFAULT 0,
    ADD COLUMN member_name VARCHAR(50) DEFAULT '익명';

-- 2. post_read_model에서 기존 데이터 마이그레이션
UPDATE post p
    JOIN post_read_model prm ON p.post_id = prm.post_id
SET p.like_count    = prm.like_count,
    p.comment_count = prm.comment_count,
    p.member_name   = prm.member_name;

-- 3. CQRS 테이블 제거
DROP TABLE IF EXISTS post_read_model_dlq;
DROP TABLE IF EXISTS post_read_model;
