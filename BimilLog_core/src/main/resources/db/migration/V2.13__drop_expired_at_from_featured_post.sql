-- featured_post 테이블에서 사용하지 않는 expired_at 컬럼 제거
ALTER TABLE `featured_post` DROP COLUMN `expired_at`;

-- Post 참조 제거: 비정규화 컬럼 추가 (author_name, title, view_count, like_count, comment_count)
ALTER TABLE `featured_post`
    ADD COLUMN `author_name` VARCHAR(255) NULL AFTER `post_id`,
    ADD COLUMN `title` VARCHAR(30) NOT NULL DEFAULT '' AFTER `author_name`,
    ADD COLUMN `view_count` INT NOT NULL DEFAULT 0 AFTER `title`,
    ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 AFTER `view_count`,
    ADD COLUMN `comment_count` INT NOT NULL DEFAULT 0 AFTER `like_count`;

-- 기존 featured_post 데이터에 post/member 테이블에서 비정규화 컬럼 채우기
UPDATE `featured_post` fp
    INNER JOIN `post` p ON fp.`post_id` = p.`post_id`
    LEFT JOIN `member` m ON p.`member_id` = m.`member_id`
SET fp.`title` = p.`title`,
    fp.`view_count` = p.`views`,
    fp.`author_name` = m.`member_name`;

-- like_count, comment_count는 집계 쿼리로 업데이트
UPDATE `featured_post` fp
SET fp.`like_count` = (
    SELECT COUNT(*) FROM `post_like` pl WHERE pl.`post_id` = fp.`post_id`
);

UPDATE `featured_post` fp
SET fp.`comment_count` = (
    SELECT COUNT(*) FROM `comment` c WHERE c.`post_id` = fp.`post_id`
);

-- FK 제약조건은 유지 (ON DELETE CASCADE)
