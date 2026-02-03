-- Post 테이블에서 is_notice 컬럼 및 인덱스 제거 (H2용)
-- 공지사항 여부는 featured_post(type=NOTICE) 테이블에서 관리

-- 인덱스 제거 (H2 문법)
DROP INDEX IF EXISTS idx_post_notice_created;

-- is_notice 컬럼 제거
ALTER TABLE post DROP COLUMN IF EXISTS is_notice;
