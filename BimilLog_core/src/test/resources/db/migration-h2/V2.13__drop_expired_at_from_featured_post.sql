-- H2 전용: featured_post 테이블에서 expired_at 제거 및 비정규화 컬럼 추가
ALTER TABLE featured_post DROP COLUMN expired_at;

ALTER TABLE featured_post ADD COLUMN author_name VARCHAR(255) NULL;
ALTER TABLE featured_post ADD COLUMN title VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE featured_post ADD COLUMN view_count INT NOT NULL DEFAULT 0;
ALTER TABLE featured_post ADD COLUMN like_count INT NOT NULL DEFAULT 0;
ALTER TABLE featured_post ADD COLUMN comment_count INT NOT NULL DEFAULT 0;

-- 기존 데이터 비정규화 컬럼 채우기
UPDATE featured_post fp
SET title = (SELECT p.title FROM post p WHERE p.post_id = fp.post_id),
    view_count = (SELECT p.views FROM post p WHERE p.post_id = fp.post_id),
    author_name = (SELECT m.member_name FROM member m INNER JOIN post p ON p.member_id = m.member_id WHERE p.post_id = fp.post_id);

UPDATE featured_post fp
SET like_count = (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = fp.post_id);

UPDATE featured_post fp
SET comment_count = (SELECT COUNT(*) FROM comment c WHERE c.post_id = fp.post_id);
