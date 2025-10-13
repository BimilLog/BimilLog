-- ================================================================================================
-- Date: 2025-10-12
-- Version: 2.1
-- ================================================================================================
-- Description:
--  post_like와 comment_like테이블에 created_at, modified_at 추가합니다.
--  기존 데이터는 id 순서대로 6개월 전부터 1분씩 증가하는 타임스탬프를 할당합니다.
-- ================================================================================================

-- ============================================
-- 파트 1: post_like 마이그레이션
-- ============================================

-- 1-1. 컬럼 추가 (NULL 허용)
ALTER TABLE `post_like`
  ADD COLUMN `created_at` TIMESTAMP(6) NULL,
  ADD COLUMN `modified_at` TIMESTAMP(6) NULL;

-- 1-2. 기존 데이터 타임스탬프 생성 (id 순서대로 6개월 전부터 1분씩 증가)
SET @row_num = 0;
UPDATE `post_like`
SET
    `created_at` = TIMESTAMPADD(MINUTE, (@row_num := @row_num + 1), DATE_SUB(NOW(), INTERVAL 6 MONTH)),
    `modified_at` = `created_at`
WHERE `created_at` IS NULL
ORDER BY `post_like_id`;

-- 1-3. NOT NULL 제약 조건 적용
ALTER TABLE `post_like`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL;

-- ============================================
-- 파트 2: comment_like 마이그레이션
-- ============================================

-- 2-1. 컬럼 추가 (NULL 허용)
ALTER TABLE `comment_like`
  ADD COLUMN `created_at` TIMESTAMP(6) NULL,
  ADD COLUMN `modified_at` TIMESTAMP(6) NULL;

-- 2-2. 기존 데이터 타임스탬프 생성 (id 순서대로 6개월 전부터 1분씩 증가)
SET @row_num = 0;
UPDATE `comment_like`
SET
    `created_at` = TIMESTAMPADD(MINUTE, (@row_num := @row_num + 1), DATE_SUB(NOW(), INTERVAL 6 MONTH)),
    `modified_at` = `created_at`
WHERE `created_at` IS NULL
ORDER BY `comment_like_id`;

-- 2-3. NOT NULL 제약 조건 적용
ALTER TABLE `comment_like`
  MODIFY COLUMN `created_at` TIMESTAMP(6) NOT NULL;

