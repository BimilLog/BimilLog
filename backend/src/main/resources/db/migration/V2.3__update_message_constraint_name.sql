-- ============================================
-- V2.3: Message 테이블 유니크 제약조건명 변경
-- ============================================
-- Date: 2025-01-09
-- Description:
--   JPA Entity 어노테이션과 DB 제약조건명을 일치시키기 위해
--   unique_user_x_y → unique_member_x_y로 변경
--   (V2.1에서 이미 user_id → member_id 컬럼 변경 완료)
-- ============================================

-- message 테이블의 유니크 제약조건명 변경
ALTER TABLE `message`
  DROP INDEX `unique_user_x_y`,
  ADD UNIQUE INDEX `unique_member_x_y` (`member_id`, `x`, `y`);

-- ============================================
-- Migration Verification
-- ============================================

-- 새 제약조건 존재 확인
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN 'SUCCESS: unique_member_x_y constraint exists'
        ELSE 'ERROR: unique_member_x_y constraint not found'
    END AS constraint_status
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'message'
  AND INDEX_NAME = 'unique_member_x_y';

-- 제약조건이 올바른 컬럼을 참조하는지 확인
SELECT
    CASE
        WHEN GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'member_id,x,y'
        THEN 'SUCCESS: Constraint columns are correct (member_id, x, y)'
        ELSE CONCAT('ERROR: Unexpected columns: ', GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX))
    END AS columns_status
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'message'
  AND INDEX_NAME = 'unique_member_x_y'
GROUP BY INDEX_NAME;

-- ============================================
-- Migration Complete
-- ============================================
SELECT 'V2.3 마이그레이션 완료: unique_member_x_y 제약조건이 성공적으로 생성되었습니다.' AS status;
