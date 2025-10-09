-- ================================================================================================
-- Comment Content Length Update: VARCHAR(255) -> VARCHAR(1000)
-- Date: 2025-10-09
-- Version: 2.4
-- ================================================================================================
-- Description:
--   사용자는 255자까지 입력 가능하지만, 서버는 줄바꿈 및 특수문자 처리를 위해
--   1000자까지 수용하도록 DB 컬럼 길이 변경
-- ================================================================================================

-- comment.content 컬럼 길이 변경
ALTER TABLE `comment`
  MODIFY COLUMN `content` VARCHAR(1000) NOT NULL COMMENT '댓글 내용 (사용자 입력 기준 255자, 서버 저장 기준 1000자)';

-- 검증
SELECT
    CASE
        WHEN CHARACTER_MAXIMUM_LENGTH = 1000 THEN 'SUCCESS: comment.content is VARCHAR(1000)'
        ELSE CONCAT('ERROR: comment.content is VARCHAR(', CHARACTER_MAXIMUM_LENGTH, ')')
    END AS validation_result
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'comment'
  AND COLUMN_NAME = 'content';
