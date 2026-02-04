-- =============================================================================
-- Post Read Model 테이블 (CQRS 조회 전용)
-- 게시판 목록 조회 성능 최적화를 위한 비정규화 테이블
-- post 삭제 시 CASCADE로 자동 삭제됨
-- =============================================================================

CREATE TABLE post_read_model (
    post_id BIGINT NOT NULL,
    title VARCHAR(30) NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    member_id BIGINT NULL,
    member_name VARCHAR(50) DEFAULT '익명',
    created_at TIMESTAMP(6) NOT NULL,
    modified_at TIMESTAMP(6) NULL,

    PRIMARY KEY (post_id),
    CONSTRAINT fk_post_read_model_post FOREIGN KEY (post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    INDEX idx_post_read_model_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기존 데이터 마이그레이션
INSERT INTO post_read_model (post_id, title, view_count, like_count, comment_count, member_id, member_name, created_at, modified_at)
SELECT
    p.post_id,
    p.title,
    p.views,
    COALESCE(pl.cnt, 0),
    COALESCE(c.cnt, 0),
    m.member_id,
    COALESCE(m.member_name, '익명'),
    p.created_at,
    p.modified_at
FROM post p
LEFT JOIN member m ON p.member_id = m.member_id
LEFT JOIN (SELECT post_id, COUNT(*) cnt FROM post_like GROUP BY post_id) pl ON p.post_id = pl.post_id
LEFT JOIN (SELECT post_id, COUNT(*) cnt FROM comment GROUP BY post_id) c ON p.post_id = c.post_id;


-- =============================================================================
-- Post Read Model DLQ 테이블 (이벤트 처리 실패 시 재처리용)
-- =============================================================================

CREATE TABLE post_read_model_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    event_type ENUM('POST_CREATED','POST_UPDATED','LIKE_INCREMENT','LIKE_DECREMENT','COMMENT_INCREMENT','COMMENT_DECREMENT','VIEW_INCREMENT') NOT NULL,
    post_id BIGINT NOT NULL,
    title VARCHAR(30) NULL,
    member_id BIGINT NULL,
    member_name VARCHAR(50) NULL,
    delta_value INT NULL COMMENT '증감값 (view_count 벌크 업데이트용)',
    status ENUM('PENDING','PROCESSED','FAILED') NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_post_dlq_status_created (status, created_at),
    UNIQUE KEY uk_post_dlq_event_status (event_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================================
-- 처리된 이벤트 테이블 (멱등성 보장용)
-- =============================================================================

CREATE TABLE processed_event (
    event_id VARCHAR(64) PRIMARY KEY,
    event_type VARCHAR(30) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_processed_event_type_at (event_type, processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
