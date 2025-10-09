-- ============================================
-- V2.4: Comment Content Length Update (H2 Test DB)
-- ============================================
-- Description:
--   사용자는 255자까지 입력 가능하지만, 서버는 줄바꿈 및 특수문자 처리를 위해
--   1000자까지 수용하도록 DB 컬럼 길이 변경

-- H2에서는 ALTER COLUMN 구문 사용 (MySQL의 MODIFY COLUMN과 다름)
ALTER TABLE comment ALTER COLUMN content VARCHAR(1000) NOT NULL;
