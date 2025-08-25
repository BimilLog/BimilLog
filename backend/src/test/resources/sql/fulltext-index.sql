-- FULLTEXT 인덱스 생성 스크립트 (DatabaseInitializer 방식 적용)
-- PostFulltextRepositoryTest에서 사용
-- MySQL 8.0 ngram 파서를 사용한 한글/영문 통합 FULLTEXT 검색

-- 기존 인덱스 존재 여부 확인 후 삭제
SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics 
    WHERE table_schema = DATABASE() 
    AND table_name = 'post' 
    AND index_name = 'idx_post_title'
);

SET @sql = CASE 
    WHEN @index_exists > 0 THEN 'ALTER TABLE post DROP INDEX idx_post_title'
    ELSE 'SELECT "No idx_post_title to drop" as info'
END;
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics 
    WHERE table_schema = DATABASE() 
    AND table_name = 'post' 
    AND index_name = 'idx_post_title_content'
);

SET @sql = CASE 
    WHEN @index_exists > 0 THEN 'ALTER TABLE post DROP INDEX idx_post_title_content'
    ELSE 'SELECT "No idx_post_title_content to drop" as info'
END;
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- FULLTEXT 인덱스 생성 (DatabaseInitializer와 동일한 방식)
-- MySQL 8.0의 ngram 파서 사용으로 한글 검색 최적화

-- 1. 제목 전용 FULLTEXT 인덱스 (한글/영문 통합)
ALTER TABLE post ADD FULLTEXT INDEX idx_post_title (title) WITH PARSER ngram;

-- 2. 제목 + 내용 통합 FULLTEXT 인덱스 (한글/영문 통합)
ALTER TABLE post ADD FULLTEXT INDEX idx_post_title_content (title, content) WITH PARSER ngram;

-- 인덱스 최적화
OPTIMIZE TABLE post;

-- 생성된 인덱스 확인 (디버깅용)
SELECT 
    INDEX_NAME,
    COLUMN_NAME, 
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'post' 
AND INDEX_TYPE = 'FULLTEXT'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;