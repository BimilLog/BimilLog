-- V2.24: 중복/불필요 인덱스 제거 (H2)

-- friendship: idx_friendship_friend 제거 (idx_friendship_friend_member 복합 인덱스로 커버)
DROP INDEX IF EXISTS idx_friendship_friend ON friendship;

-- member_blacklist: idx_member_blacklist_request 제거 (uk_member_blacklist_request_black UNIQUE로 커버)
DROP INDEX IF EXISTS idx_member_blacklist_request ON member_blacklist;

-- 나머지 인덱스(idx_post_created_at_popular, idx_user_username, FK 자동생성 인덱스)는
-- H2 테스트 환경에 존재하지 않으므로 생략
