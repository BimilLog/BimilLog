-- V2.23: like 테이블 인덱스 정리 및 comment 인덱스 추가
-- 작성일: 2026-03-02

-- post_like: idx_postlike_user_post 제거 (uk_postlike_member_post UNIQUE와 중복)
DROP INDEX idx_postlike_user_post ON post_like;

-- comment_like: 기존 일반 인덱스 제거 후 UNIQUE 제약조건으로 교체 (member_id, comment_id 순서)
DROP INDEX uk_comment_like_user_comment ON comment_like;
ALTER TABLE comment_like ADD CONSTRAINT uk_comment_like_member_comment UNIQUE (member_id, comment_id);

-- comment: member_id + post_id 복합 인덱스 추가
CREATE INDEX idx_comment_member_post ON comment (member_id, post_id);
