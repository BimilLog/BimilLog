-- ================================================================================================
-- Remove post_cache_flag Column and Update Indexes
-- Date: 2025-10-05
-- Version: 2.2
-- ================================================================================================
-- Description:
--   post 테이블에서 post_cache_flag 컬럼 제거
--   - 캐싱 전략 변경: DB 플래그 대신 Redis만 사용
--   - 모든 게시글에 캐시 어사이드 패턴 적용
--   - is_notice 플래그는 유지 (공지사항 구분용)
-- ================================================================================================

-- 1. post_cache_flag 관련 인덱스 제거
-- idx_post_created_at_popular 인덱스 제거 (존재하는 경우에만)
SET @index_exists1 = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'post'
    AND INDEX_NAME = 'idx_post_created_at_popular'
);

SET @drop_index_sql1 = IF(@index_exists1 > 0,
  'ALTER TABLE `post` DROP INDEX `idx_post_created_at_popular`',
  'SELECT "Index idx_post_created_at_popular does not exist, skipping drop" AS info'
);

PREPARE stmt1 FROM @drop_index_sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- idx_post_popular_flag 인덱스 제거 (존재하는 경우에만)
SET @index_exists2 = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'post'
    AND INDEX_NAME = 'idx_post_popular_flag'
);

SET @drop_index_sql2 = IF(@index_exists2 > 0,
  'ALTER TABLE `post` DROP INDEX `idx_post_popular_flag`',
  'SELECT "Index idx_post_popular_flag does not exist, skipping drop" AS info'
);

PREPARE stmt2 FROM @drop_index_sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 2. post_cache_flag 컬럼 제거 (존재하는 경우에만)
SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'post'
    AND COLUMN_NAME = 'post_cache_flag'
);

SET @drop_column_sql = IF(@column_exists > 0,
  'ALTER TABLE `post` DROP COLUMN `post_cache_flag`',
  'SELECT "Column post_cache_flag does not exist, skipping drop" AS info'
);

PREPARE stmt FROM @drop_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ================================================================================================
-- Migration Verification
-- ================================================================================================

-- post_cache_flag 컬럼이 제거되었는지 확인
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN 'SUCCESS: post_cache_flag column removed'
        ELSE 'ERROR: post_cache_flag column still exists'
    END AS column_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'post'
  AND COLUMN_NAME = 'post_cache_flag';

-- is_notice 컬럼이 유지되는지 확인
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: is_notice column exists'
        ELSE 'ERROR: is_notice column not found'
    END AS is_notice_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'post'
  AND COLUMN_NAME = 'is_notice';

-- 유지되는 인덱스 확인
SELECT
    CASE
        WHEN COUNT(*) = 2 THEN 'SUCCESS: Required indexes exist (idx_post_notice_created, idx_post_created)'
        ELSE CONCAT('WARNING: Expected 2 indexes, found ', COUNT(*))
    END AS index_status
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'post'
  AND INDEX_NAME IN ('idx_post_notice_created', 'idx_post_created');

-- ================================================================================================
-- Migration Notes:
-- 1. post_cache_flag 컬럼 제거 (DB 플래그 미사용)
-- 2. idx_post_created_at_popular, idx_post_popular_flag 인덱스 제거
-- 3. idx_post_notice_created, idx_post_created 인덱스 유지
-- 4. is_notice 플래그는 공지사항 구분을 위해 유지
--
-- 새로운 캐싱 전략:
--   - 모든 게시글: Redis 캐시 어사이드 패턴
--   - 인기글/공지사항: Redis에 postId만 저장, 조회 시 캐시 활용
-- ================================================================================================
