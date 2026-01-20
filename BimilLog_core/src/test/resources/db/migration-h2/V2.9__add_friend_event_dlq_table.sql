-- Friend Event DLQ (Dead Letter Queue) 테이블
-- 친구 관련 Redis 이벤트 처리 실패 시 임시 저장하여 재처리하기 위한 테이블

CREATE TABLE friend_event_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    member_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    score DOUBLE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_dlq_status_created ON friend_event_dlq (status, created_at);
