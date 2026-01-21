-- Friend Event DLQ 테이블에 멱등성을 위한 unique constraint 추가
-- PENDING 상태에서 동일한 이벤트가 중복 저장되는 것을 방지

-- unique index 추가
CREATE UNIQUE INDEX uk_dlq_pending_event
    ON friend_event_dlq (type, member_id, target_id, status);
