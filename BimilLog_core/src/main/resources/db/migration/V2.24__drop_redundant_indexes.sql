-- V2.24: 중복/불필요 인덱스 제거
-- 작성일: 2026-03-24
-- IF EXISTS 사용: repair 후 재실행 시 이미 삭제된 인덱스 오류 방지

-- 1. post: idx_post_created_at_popular 제거
--    popular_flag 컬럼 삭제(V2.20) 후 created_at만 남아 idx_post_created와 완전 중복
ALTER TABLE post DROP INDEX IF EXISTS idx_post_created_at_popular;

-- 2. member: idx_user_username 제거
--    UKk8d0f2n7n88w1a16yhua64onx (UNIQUE member_name)가 동일 컬럼 커버
ALTER TABLE member DROP INDEX IF EXISTS idx_user_username;

-- 3. friendship: idx_friendship_friend 제거
--    idx_friendship_friend_member (friend_id, member_id) 복합 인덱스의 선행 컬럼으로 커버
ALTER TABLE friendship DROP INDEX IF EXISTS idx_friendship_friend;

-- 4. comment: FKqm52p1v3o13hy268he0wcngr5 (member_id) 제거
--    idx_comment_member_post (member_id, post_id) 복합 인덱스의 선행 컬럼으로 커버
ALTER TABLE comment DROP INDEX IF EXISTS FKqm52p1v3o13hy268he0wcngr5;

-- 5. comment_like: FKl5wrmp8eoy5uegdo3473jqqi (member_id) 제거
--    uk_comment_like_member_comment (member_id, comment_id) UNIQUE 인덱스의 선행 컬럼으로 커버
ALTER TABLE comment_like DROP INDEX IF EXISTS FKl5wrmp8eoy5uegdo3473jqqi;

-- 6. post_like: FKj7iy0k7n3d0vkh8o7ibjna884 (post_id) 제거
--    idx_post_like_post_id_id (post_id, post_like_id) 복합 인덱스의 선행 컬럼으로 커버
ALTER TABLE post_like DROP INDEX IF EXISTS FKj7iy0k7n3d0vkh8o7ibjna884;

-- 7. member_blacklist: idx_member_blacklist_request (request_member_id) 제거
--    uk_member_blacklist_request_black (request_member_id, black_member_id) UNIQUE의 선행 컬럼으로 커버
ALTER TABLE member_blacklist DROP INDEX IF EXISTS idx_member_blacklist_request;
