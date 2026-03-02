-- V2.23: like 테이블 인덱스 정리 및 comment 인덱스 추가

-- post_like: idx_postlike_member_post 제거 (uk_postlike_member_post UNIQUE와 중복)
DROP INDEX IF EXISTS idx_postlike_member_post ON post_like;

-- comment_like: 기존 인덱스 및 유니크 제약조건 제거 후 UNIQUE (member_id, comment_id) 순서로 교체
DROP INDEX IF EXISTS idx_comment_like_member_comment ON comment_like;
ALTER TABLE comment_like DROP CONSTRAINT IF EXISTS uk_comment_like_member_comment;
ALTER TABLE comment_like ADD CONSTRAINT uk_comment_like_member_comment UNIQUE (member_id, comment_id);

-- comment: member_id + post_id 복합 인덱스 추가
CREATE INDEX idx_comment_member_post ON comment (member_id, post_id);
