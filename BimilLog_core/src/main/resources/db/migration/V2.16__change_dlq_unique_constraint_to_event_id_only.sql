-- Friend Event DLQ 유니크 제약 변경: (event_id, status) → event_id만

DROP INDEX uk_dlq_event_status ON friend_event_dlq;

ALTER TABLE friend_event_dlq ADD UNIQUE (event_id);
