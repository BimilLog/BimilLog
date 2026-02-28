-- friendship 테이블 (friend_id, member_id) 복합 인덱스 (H2)
CREATE INDEX idx_friendship_friend_member ON friendship (friend_id, member_id);
