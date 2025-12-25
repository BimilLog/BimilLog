-- ====================================================================================
-- bimillogTest DB 데이터 정리 스크립트
-- 목적: 성능 테스트에 필요한 데이터만 남기고 나머지 삭제
--
-- 보존할 데이터:
--   - 일반 게시글 (post_id 1~10000) - 비회원 게시글로 변경
--   - 주간 인기글 (post_id 10001~10010) + 추천 데이터
--   - 레전드 인기글 (post_id 10011~10060) + 추천 데이터
--   - 인기글에 추천한 회원들만 보존
--
-- 삭제할 데이터:
--   - 모든 댓글 (comment)
--   - 모든 댓글 클로저 (comment_closure)
--   - 모든 댓글 추천 (comment_like)
--   - 일반 게시글 추천 (post_id 1~10000)
--   - 인기글에 추천하지 않은 회원
--
-- 실행 방법:
--   mysql -h localhost -u root -p bimillogTest < performance/cleanup-test-data.sql
-- ====================================================================================

USE bimillogTest;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 댓글 추천 전체 삭제
TRUNCATE TABLE comment_like;
SELECT '1. comment_like 테이블 삭제 완료' AS status;

-- 2. 댓글 클로저 전체 삭제
TRUNCATE TABLE comment_closure;
SELECT '2. comment_closure 테이블 삭제 완료' AS status;

-- 3. 댓글 전체 삭제
TRUNCATE TABLE comment;
SELECT '3. comment 테이블 삭제 완료' AS status;

-- 4. 일반 게시글 추천 삭제 (post_id 1~10000)
DELETE FROM post_like WHERE post_id BETWEEN 1 AND 10000;
SELECT '4. 일반 게시글 추천 삭제 완료' AS status;

-- 5. 일반 게시글을 비회원 게시글로 변경 (post_id 1~10000)
UPDATE post
SET member_id = NULL
WHERE post_id BETWEEN 1 AND 10000;
SELECT '5. 일반 게시글 (1~10000)을 비회원 게시글로 변경 완료' AS status;

-- 6. 인기글에 추천한 회원 ID를 테이블에 저장
DROP TABLE IF EXISTS keep_members_temp;
CREATE TABLE keep_members_temp (
    member_id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

-- 주간/레전드 인기글에 추천한 회원 ID 추출
INSERT IGNORE INTO keep_members_temp (member_id)
SELECT DISTINCT member_id
FROM post_like
WHERE post_id BETWEEN 10001 AND 10060;

SELECT CONCAT('6. 보존할 회원 수: ', COUNT(*), '명') AS status
FROM keep_members_temp;

-- 7. 보존할 회원 외의 소셜 토큰 삭제
-- member 테이블에서 social_token_id를 통해 연결됨
DELETE FROM social_token
WHERE social_token_id NOT IN (
    SELECT social_token_id
    FROM member
    WHERE member_id IN (SELECT member_id FROM keep_members_temp)
      AND social_token_id IS NOT NULL
);
SELECT '7. 불필요한 소셜 토큰 삭제 완료' AS status;

-- 8. 보존할 회원 외의 인증 토큰 삭제
DELETE FROM auth_token
WHERE member_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '8. 불필요한 인증 토큰 삭제 완료' AS status;

-- 9. 보존할 회원 외의 알림 삭제
DELETE FROM notification
WHERE member_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '9. 불필요한 알림 삭제 완료' AS status;

-- 10. 보존할 회원 외의 블랙리스트 삭제
DELETE FROM member_blacklist
WHERE request_member_id NOT IN (SELECT member_id FROM keep_members_temp)
   OR black_member_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '10. 불필요한 블랙리스트 삭제 완료' AS status;

-- 11. 보존할 회원 외의 친구 관계 삭제
DELETE FROM friendship
WHERE member_id NOT IN (SELECT member_id FROM keep_members_temp)
   OR friend_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '11. 불필요한 친구 관계 삭제 완료' AS status;

-- 12. 보존할 회원 외의 친구 요청 삭제
DELETE FROM friend_request
WHERE sender_id NOT IN (SELECT member_id FROM keep_members_temp)
   OR receiver_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '12. 불필요한 친구 요청 삭제 완료' AS status;

-- 13. 보존할 회원 외의 롤링페이퍼 메시지 삭제
DELETE FROM message
WHERE member_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '13. 불필요한 롤링페이퍼 메시지 삭제 완료' AS status;

-- 14. 보존할 회원 외의 신고 삭제
DELETE FROM report
WHERE member_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '14. 불필요한 신고 삭제 완료' AS status;

-- 15. 보존할 회원 외의 회원 삭제 (setting도 CASCADE로 삭제됨)
DELETE FROM member
WHERE member_id NOT IN (SELECT member_id FROM keep_members_temp);
SELECT '15. 불필요한 회원 삭제 완료' AS status;

-- 16. 최종 결과 확인
SELECT '================ 최종 데이터 요약 ================' AS '';
SELECT CONCAT('보존된 회원 수: ', COUNT(*), '명') AS result FROM member;
SELECT CONCAT('전체 게시글 수: ', COUNT(*), '개') AS result FROM post;
SELECT CONCAT('  - 비회원 게시글 (1~10000): ', COUNT(*), '개') AS result
FROM post WHERE post_id BETWEEN 1 AND 10000;
SELECT CONCAT('  - 주간 인기글 (10001~10010): ', COUNT(*), '개') AS result
FROM post WHERE post_id BETWEEN 10001 AND 10010;
SELECT CONCAT('  - 레전드 인기글 (10011~10060): ', COUNT(*), '개') AS result
FROM post WHERE post_id BETWEEN 10011 AND 10060;
SELECT CONCAT('보존된 게시글 추천 수: ', COUNT(*), '개 (인기글만)') AS result FROM post_like;
SELECT CONCAT('남은 댓글 수: ', COUNT(*), '개') AS result FROM comment;
SELECT CONCAT('남은 댓글 추천 수: ', COUNT(*), '개') AS result FROM comment_like;

-- 17. 임시 테이블 정리
DROP TABLE IF EXISTS keep_members_temp;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '================ 정리 완료 ================' AS '';
