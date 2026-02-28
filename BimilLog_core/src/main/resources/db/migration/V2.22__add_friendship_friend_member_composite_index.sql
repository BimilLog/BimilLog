-- friendship 테이블 (friend_id, member_id) 복합 인덱스
-- friend_id로 조회 시 member_id까지 커버링 인덱스로 처리하여
-- 배치 친구 관계 재구축 쿼리 성능을 최적화합니다.
CREATE INDEX idx_friendship_friend_member ON friendship (friend_id, member_id);
