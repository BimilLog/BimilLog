-- ============================================
-- V2.7: 친구 알림 타입 추가 (H2)
-- ============================================
-- 목적: 친구 요청 알림 기능 완성
-- 작성일: 2025-11-27
--
-- 배경:
--   - Java NotificationType enum에 FRIEND 타입이 정의됨
--   - Setting 엔티티에 friendSendNotification 필드가 추가됨
--   - DB 스키마와의 동기화 필요
--   - 친구 요청 관련 알림 처리를 위해 필수
--
-- 주요 기능:
--   - setting 테이블에 friend_send_notification 컬럼 추가
--   - H2에서는 notification_type이 VARCHAR(50)으로 정의되어 있어 ENUM 변경 불필요
--   - 기존 데이터는 기본값 TRUE로 설정 (기본적으로 알림 허용)
-- ============================================

-- ============================================
-- Step 1: setting 테이블에 friend_send_notification 컬럼 추가
-- ============================================
-- 친구 요청 알림 수신 여부 설정
-- 기본값: TRUE (알림 허용)
ALTER TABLE setting
  ADD COLUMN friend_send_notification BOOLEAN NOT NULL DEFAULT TRUE;

-- ============================================
-- Step 2: notification_type은 VARCHAR(50)이므로 변경 불필요
-- ============================================
-- H2에서는 notification_type이 VARCHAR(50)으로 정의되어 있음
-- VARCHAR는 모든 문자열 값을 허용하므로 스키마 변경 불필요
-- No schema changes needed for notification_type (VARCHAR(50), not ENUM)
