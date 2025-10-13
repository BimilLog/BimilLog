-- ================================================================================================
-- Date: 2025-10-13
-- Version: 2.2 (H2 Test)
-- ================================================================================================
-- Description:
--  PostLike 테이블에 UNIQUE 제약조건 추가 및 중복 데이터 정리
--  member_id + post_id 중복 방지를 위한 UNIQUE 제약조건 추가
-- ================================================================================================

-- Step 1: 중복 데이터 정리 (H2 호환 방식 - 가장 최근 post_like_id만 남기고 삭제)
DELETE FROM post_like
WHERE post_like_id NOT IN (
    SELECT MAX(post_like_id)
    FROM post_like
    GROUP BY member_id, post_id
);

-- Step 2: UNIQUE 제약조건 추가
ALTER TABLE post_like
ADD CONSTRAINT uk_postlike_member_post UNIQUE (member_id, post_id);
