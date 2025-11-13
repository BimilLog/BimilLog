-- ============================================
-- V2.4: FCM 토큰을 AuthToken 테이블로 통합
-- ============================================
-- 목적: 기기별 세션 정보 통합 (JWT Refresh Token + FCM Registration Token)
-- 작성일: 2025-11-13
--
-- 배경:
--   - AuthToken은 이미 "기기별 세션"을 표현하는 엔티티
--   - FCM 토큰도 기기별로 관리되어야 함
--   - 로그아웃 시 AuthToken 삭제 = FCM 토큰도 삭제 (생명주기 일치)
--
-- 장점:
--   - JWT에 fcmTokenId 불필요 (authTokenId로 FCM 조회 가능)
--   - 별도 JOIN 불필요
--   - NULL 처리 자연스러움 (FCM 미지원/거부 기기는 NULL 유지)
--   - 코드 복잡도 감소
-- ============================================

-- Step 1: auth_token 테이블에 fcm_registration_token 컬럼 추가
ALTER TABLE `auth_token`
ADD COLUMN `fcm_registration_token` VARCHAR(255) NULL
COMMENT 'Firebase Cloud Messaging 토큰 (기기별 Push 알림용, NULL 허용)'
AFTER `use_count`;

-- Step 2: fcm_token 테이블 삭제
-- 주의: 기존 FCM 토큰 데이터는 마이그레이션하지 않음
-- 이유: fcm_token과 auth_token의 1:N 관계가 일치하지 않아 완벽한 매핑 불가능
-- 결과: 사용자는 앱 재실행 시 FCM 토큰 재등록 필요 (자동 처리됨)
DROP TABLE IF EXISTS `fcm_token`;

-- ============================================
-- 마이그레이션 후 데이터 검증 쿼리 (실행하지 말 것, 참고용)
-- ============================================
-- SELECT auth_token_id, member_id, fcm_registration_token, created_at
-- FROM auth_token
-- WHERE fcm_registration_token IS NOT NULL;
