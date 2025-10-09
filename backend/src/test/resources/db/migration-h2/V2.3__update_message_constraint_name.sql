-- ============================================
-- V2.3: Message 테이블 유니크 제약조건명 변경 (H2 Test DB)
-- ============================================

-- H2에서는 제약조건을 직접 변경할 수 없으므로 DROP 후 ADD
ALTER TABLE message DROP CONSTRAINT IF EXISTS unique_user_x_y;
ALTER TABLE message ADD CONSTRAINT unique_member_x_y UNIQUE (member_id, x, y);
