-- ============================================
-- V2.8: POST_FEATURED 알림 타입 세분화
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
-- ============================================

-- ============================================
-- Step 1: ENUM에 새로운 타입들을 임시로 추가
-- ============================================
-- POST_FEATURED를 유지하면서 새로운 타입들을 추가
ALTER TABLE `notification`
  MODIFY COLUMN `notification_type` ENUM(
    'COMMENT',
    'COMMENT_FEATURED',
    'MESSAGE',
    'POST_FEATURED',
    'POST_FEATURED_WEEKLY',
    'POST_FEATURED_LEGEND',
    'POST_FEATURED_REALTIME',
    'ADMIN',
    'INITIATE',
    'FRIEND'
  ) NOT NULL
  COMMENT '알림 유형 (댓글, 댓글 추천, 메시지, 인기글, 주간 인기글, 명예의 전당, 실시간 인기글, 관리자, 초기화, 친구)';

-- ============================================
-- Step 2: 기존 POST_FEATURED 데이터를 POST_FEATURED_WEEKLY로 마이그레이션
-- ============================================
-- 기존 데이터의 의도가 명확하지 않으므로 주간 인기글로 간주
UPDATE `notification`
SET `notification_type` = 'POST_FEATURED_WEEKLY'
WHERE `notification_type` = 'POST_FEATURED';

-- ============================================
-- Step 3: POST_FEATURED 제거하고 최종 ENUM 확정
-- ============================================
-- POST_FEATURED는 더 이상 사용하지 않으므로 제거
ALTER TABLE `notification`
  MODIFY COLUMN `notification_type` ENUM(
    'COMMENT',
    'COMMENT_FEATURED',
    'MESSAGE',
    'POST_FEATURED_WEEKLY',
    'POST_FEATURED_LEGEND',
    'POST_FEATURED_REALTIME',
    'ADMIN',
    'INITIATE',
    'FRIEND'
  ) NOT NULL
  COMMENT '알림 유형 (댓글, 댓글 추천, 메시지, 주간 인기글, 명예의 전당, 실시간 인기글, 관리자, 초기화, 친구)';
