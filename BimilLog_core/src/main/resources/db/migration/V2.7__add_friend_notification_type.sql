-- ============================================
-- V2.7: 친구 알림 타입 추가
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
--   - notification 테이블의 notification_type ENUM 확장
--   - FRIEND 타입으로 친구 요청 알림 저장 가능
--   - 기존 데이터는 기본값 TRUE로 설정 (기본적으로 알림 허용)
-- ============================================

-- ============================================
-- Step 1: setting 테이블에 friend_send_notification 컬럼 추가
-- ============================================
-- 친구 요청 알림 수신 여부 설정
-- 기본값: TRUE (알림 허용)
ALTER TABLE `setting`
  ADD COLUMN `friend_send_notification` TINYINT(1) NOT NULL DEFAULT '1'
  COMMENT '친구 요청 알림 수신 여부';

-- ============================================
-- Step 2: notification 테이블의 notification_type ENUM 확장
-- ============================================
-- ENUM 목록에 FRIEND 추가
-- 기존 값들은 유지되고 새로운 값만 추가됨
ALTER TABLE `notification`
  MODIFY COLUMN `notification_type` ENUM(
    'COMMENT',
    'COMMENT_FEATURED',
    'MESSAGE',
    'POST_FEATURED',
    'ADMIN',
    'INITIATE',
    'FRIEND'
  ) NOT NULL
  COMMENT '알림 유형 (댓글, 댓글 추천, 메시지, 게시글 추천, 관리자, 초기화, 친구)';
