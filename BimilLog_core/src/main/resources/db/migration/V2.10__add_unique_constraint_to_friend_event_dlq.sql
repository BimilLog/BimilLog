-- Friend Event DLQ 테이블에 멱등성을 위한 unique constraint 추가
-- PENDING 상태에서 동일한 이벤트가 중복 저장되는 것을 방지

-- 기존 중복 데이터 정리 (가장 최신 것만 유지)
DELETE t1 FROM friend_event_dlq t1
INNER JOIN friend_event_dlq t2
WHERE t1.id < t2.id
  AND t1.type = t2.type
  AND t1.member_id = t2.member_id
  AND t1.target_id = t2.target_id
  AND t1.status = 'PENDING'
  AND t2.status = 'PENDING';

-- unique index 추가 (PENDING 상태인 경우에만 유니크)
-- MySQL 8.0에서는 부분 인덱스를 지원하지 않으므로, 전체 조합에 대해 인덱스 생성
-- 애플리케이션 레벨에서 PENDING 상태 체크 후 중복 방지
CREATE UNIQUE INDEX uk_dlq_pending_event
    ON friend_event_dlq (type, member_id, target_id, status);
