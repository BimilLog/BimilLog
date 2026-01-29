-- H2 전용: featured_post 테이블 생성
CREATE TABLE IF NOT EXISTS featured_post (
    featured_post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    featured_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expired_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_at TIMESTAMP(6) NULL,

    CONSTRAINT uk_featured_post_type UNIQUE (post_id, type),
    CONSTRAINT fk_featured_post_post FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE
);

CREATE INDEX idx_featured_post_type_featured ON featured_post (type, featured_at DESC);

-- 기존 공지사항 마이그레이션
INSERT INTO featured_post (post_id, type, featured_at)
SELECT post_id, 'NOTICE', created_at
FROM post WHERE is_notice = TRUE;
