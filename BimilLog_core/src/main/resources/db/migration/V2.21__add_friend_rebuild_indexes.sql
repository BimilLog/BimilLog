-- 친구 Redis 재구축 쿼리 최적화 인덱스
-- 드라이빙 테이블(post, comment)에서 조인 테이블(post_like, comment, comment_like)로 이어지는
-- 복합 keyset 페이지네이션을 위한 인덱스입니다.

-- post_like: post를 드라이빙으로 삼아 post_id로 조인 후 id 기준 정렬
-- (post_id, id) → post.id = ? 조건으로 해당 post의 좋아요를 id 순으로 효율적으로 스캔
CREATE INDEX idx_post_like_post_id_id ON post_like (post_id, id);

-- comment: post를 드라이빙으로 삼아 post_id로 조인 후 id 기준 정렬
-- (post_id, id) → post.id = ? 조건으로 해당 post의 댓글을 id 순으로 효율적으로 스캔
CREATE INDEX idx_comment_post_id_id ON comment (post_id, id);

-- comment_like: comment를 드라이빙으로 삼아 comment_id로 조인 후 id 기준 정렬
-- (comment_id, id) → comment.id = ? 조건으로 해당 댓글의 좋아요를 id 순으로 효율적으로 스캔
CREATE INDEX idx_comment_like_comment_id_id ON comment_like (comment_id, id);
