-- processed_event 테이블 제거
-- 멱등성은 DLQ의 event_id 유니크 제약으로 보장하므로 별도 테이블 불필요

DROP TABLE IF EXISTS processed_event;

-- Post DLQ 유니크 제약 변경: (event_id, status) -> event_id만

ALTER TABLE post_read_model_dlq DROP CONSTRAINT IF EXISTS uk_post_dlq_event_status;

ALTER TABLE post_read_model_dlq ADD UNIQUE (event_id);
