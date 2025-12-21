-- ============================================
-- V2.8: POST_FEATURED 알림 타입 세분화 (H2 테스트용)
-- ============================================
-- 목적: 인기글 알림 타입을 구체적으로 분류
-- 작성일: 2025-12-14
--
-- 배경:
--   - Java NotificationType enum이 POST_FEATURED에서 세분화됨
--   - POST_FEATURED_WEEKLY: 주간 인기글
--   - POST_FEATURED_LEGEND: 명예의 전당
--   - POST_FEATURED_REALTIME: 실시간 인기글
--   - 사용자에게 더 명확한 알림 정보 제공
--   - FCM 푸시 알림 메시지 차별화
--
-- 주요 기능:
--   - notification 테이블의 notification_type ENUM 확장
--   - POST_FEATURED를 3개 타입으로 분리
--   - 기존 POST_FEATURED 데이터는 POST_FEATURED_WEEKLY로 마이그레이션
--
-- H2 특이사항:
--   - H2는 MySQL ENUM을 VARCHAR로 처리
--   - 데이터 마이그레이션만 수행
-- ============================================

-- ============================================
-- Step 1: 기존 POST_FEATURED 데이터를 POST_FEATURED_WEEKLY로 마이그레이션
-- ============================================
-- 기존 데이터의 의도가 명확하지 않으므로 주간 인기글로 간주
UPDATE notification
SET notification_type = 'POST_FEATURED_WEEKLY'
WHERE notification_type = 'POST_FEATURED';

-- ============================================
-- 참고: H2에서는 ENUM 제약조건이 없으므로 별도 ALTER TABLE 불필요
-- ============================================
-- H2는 notification_type을 VARCHAR로 처리하므로
-- 'POST_FEATURED_WEEKLY', 'POST_FEATURED_LEGEND', 'POST_FEATURED_REALTIME' 값을
-- 자유롭게 저장할 수 있음
