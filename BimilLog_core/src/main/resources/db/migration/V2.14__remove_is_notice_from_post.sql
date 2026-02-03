-- Post 테이블에서 is_notice 컬럼 및 인덱스 제거
-- 공지사항 여부는 featured_post(type=NOTICE) 테이블에서 관리

-- 인덱스 제거 (존재하는 경우에만)
DROP INDEX idx_post_notice_created ON post;

-- is_notice 컬럼 제거
ALTER TABLE post DROP COLUMN is_notice;
