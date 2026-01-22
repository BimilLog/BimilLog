-- Friend Event DLQ 테이블에 event_id 컬럼 추가
-- 각 이벤트를 개별적으로 추적하여 좋아요 취소/재좋아요 시나리오 지원

-- 1. 기존 유니크 인덱스 삭제
DROP INDEX uk_dlq_pending_event ON friend_event_dlq;

-- 2. event_id 컬럼 추가 (임시로 nullable)
ALTER TABLE friend_event_dlq ADD COLUMN event_id VARCHAR(255) NULL AFTER id;

-- 3. 기존 데이터에 event_id 값 채우기
-- FRIEND_ADD/REMOVE: deterministic key 생성
-- SCORE_UP: UUID 생성
UPDATE friend_event_dlq
SET event_id = CASE
    WHEN type = 'FRIEND_ADD' THEN CONCAT('FRIEND_ADD:', member_id, ':', target_id)
    WHEN type = 'FRIEND_REMOVE' THEN CONCAT('FRIEND_REMOVE:', member_id, ':', target_id)
    ELSE UUID()
END
WHERE event_id IS NULL;

-- 4. event_id 컬럼을 NOT NULL로 변경
ALTER TABLE friend_event_dlq MODIFY COLUMN event_id VARCHAR(255) NOT NULL;

-- 5. 새 유니크 인덱스 추가 (event_id, status)
CREATE UNIQUE INDEX uk_dlq_event_status ON friend_event_dlq (event_id, status);
