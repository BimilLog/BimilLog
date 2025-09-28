-- Comment Closure 테이블 ID 생성 전략 변경
-- GenerationType.IDENTITY 지원을 위한 AUTO_INCREMENT 추가
-- 날짜: 2025-09-28

-- ============================================
-- comment_closure 테이블의 id를 AUTO_INCREMENT로 변경
-- ============================================
ALTER TABLE `comment_closure`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT;

-- ============================================
-- 더 이상 필요 없는 시퀀스 테이블 삭제
-- ============================================
DROP TABLE IF EXISTS `comment_closure_seq`;

-- ============================================
-- 마이그레이션 검증
-- ============================================

-- AUTO_INCREMENT 설정 확인
SELECT
    CASE
        WHEN EXTRA LIKE '%auto_increment%' THEN 'SUCCESS: comment_closure.id is AUTO_INCREMENT'
        ELSE 'ERROR: comment_closure.id is not AUTO_INCREMENT'
    END AS auto_increment_status
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'comment_closure'
    AND COLUMN_NAME = 'id';

-- comment_closure_seq 테이블 삭제 확인
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN 'SUCCESS: comment_closure_seq table removed'
        ELSE 'ERROR: comment_closure_seq table still exists'
    END AS seq_table_status
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'comment_closure_seq';

-- 마이그레이션 완료 메시지
SELECT 'V2.1 마이그레이션 완료: comment_closure 테이블이 AUTO_INCREMENT를 사용하도록 변경되었습니다.' AS migration_status;