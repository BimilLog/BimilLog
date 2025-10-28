-- V2.2: PostLike 테이블에 UNIQUE 제약조건 추가 및 중복 데이터 정리
-- 작성일: 2025-10-13
-- 목적: member_id + post_id 중복 방지를 위한 UNIQUE 제약조건 추가

-- Step 1: 중복 데이터 확인 (로그용 - 실제로는 실행되지 않음)
-- SELECT member_id, post_id, COUNT(*) as cnt
-- FROM post_like
-- GROUP BY member_id, post_id
-- HAVING cnt > 1;

-- Step 2: 중복 데이터 정리 (가장 최근 createdAt 기준으로 1개만 남기고 삭제)
DELETE pl1 FROM post_like pl1
INNER JOIN post_like pl2
WHERE pl1.member_id = pl2.member_id
  AND pl1.post_id = pl2.post_id
  AND pl1.post_like_id < pl2.post_like_id;

-- Step 3: UNIQUE 제약조건 추가
ALTER TABLE post_like
ADD CONSTRAINT uk_postlike_member_post UNIQUE (member_id, post_id);
