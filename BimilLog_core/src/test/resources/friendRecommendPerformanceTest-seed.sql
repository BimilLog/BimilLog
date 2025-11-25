-- ========================================
-- 친구 추천 성능 테스트 시드 데이터
-- 총 회원: 1,000명
-- 평균 친구: 15명/회원
-- 평균 게시글: 1개/회원
-- 평균 게시글 추천: 1개/회원
-- 평균 댓글: 5개/회원
-- 평균 댓글 추천: 10개/회원
-- ========================================

SET FOREIGN_KEY_CHECKS = 0;
SET autocommit = 0;
START TRANSACTION;

-- ========================================
-- 1. 시퀀스 테이블 생성 (1~10,000)
-- ========================================
DROP TABLE IF EXISTS tmp_seq;
CREATE TABLE tmp_seq (n INT PRIMARY KEY);

INSERT INTO tmp_seq (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 10000
)
SELECT n FROM seq;

-- ========================================
-- 2. 설정 1,000개 생성 (먼저 생성)
-- ========================================
INSERT INTO setting (setting_id)
SELECT n
FROM tmp_seq
WHERE n <= 1000;

-- ========================================
-- 3. 회원 1,000명 생성
-- ========================================
INSERT INTO member (member_id, social_id, member_name, social_nickname, provider, setting_id, role, created_at, modified_at)
SELECT
    n,
    CONCAT('perf_test_', LPAD(n, 6, '0')),
    CONCAT('회원_', n),
    CONCAT('user_', n),
    CASE (n % 3)
        WHEN 0 THEN 'KAKAO'
        WHEN 1 THEN 'NAVER'
        ELSE 'GOOGLE'
    END,
    n,
    'USER',
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 12345) * 365) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 12345) * 365) DAY)
FROM tmp_seq
WHERE n <= 1000;

-- ========================================
-- 4. 친구 관계 생성 (15,000건, 양방향)
-- ========================================
-- 각 회원당 평균 15명 친구 (클러스터링 적용)
INSERT INTO friendship (friendship_id, member_id, friend_id, created_at)
SELECT
    ROW_NUMBER() OVER () AS id,
    member_id,
    friend_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(member_id + friend_id) * 180) DAY)
FROM (
    SELECT DISTINCT
        t1.n AS member_id,
        t2.n AS friend_id
    FROM tmp_seq t1
    CROSS JOIN tmp_seq t2
    WHERE t1.n <= 1000
      AND t2.n <= 1000
      AND t1.n < t2.n
      AND (
          -- 클러스터링: 같은 그룹 내 친구 확률 높음 (그룹 크기: 50명)
          (FLOOR((t1.n - 1) / 50) = FLOOR((t2.n - 1) / 50) AND RAND(t1.n * t2.n) < 0.55)
          -- 다른 그룹 간 친구 확률 낮음
          OR (FLOOR((t1.n - 1) / 50) != FLOOR((t2.n - 1) / 50) AND RAND(t1.n + t2.n) < 0.15)
      )
    ORDER BY RAND(54321)
    LIMIT 15000
) friendship_data;

-- ========================================
-- 5. 게시글 1,000개 생성
-- ========================================
INSERT INTO post (post_id, member_id, title, content, is_notice, views, created_at, modified_at)
SELECT
    n,
    n,
    CONCAT('성능 테스트 게시글 ', n),
    CONCAT('이것은 성능 테스트를 위한 게시글입니다. 번호: ', n, '\n\n',
           '내용: 친구 추천 알고리즘의 성능을 측정하기 위해 생성된 테스트 게시글입니다.\n',
           'BFS 알고리즘과 Redis 캐싱 성능을 검증합니다.'),
    IF(n <= 5, 1, 0),
    FLOOR(RAND(n * 456) * 1000),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 111) * 365) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 222) * 180) DAY)
FROM tmp_seq
WHERE n <= 1000;

-- ========================================
-- 6. 게시글 추천 1,000개
-- ========================================
INSERT INTO post_like (post_like_id, member_id, post_id, created_at, modified_at)
SELECT
    n,
    CASE
        WHEN n % 1000 = 0 THEN 1
        ELSE (n % 1000) + 1
    END,
    n,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 333) * 180) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 444) * 180) DAY)
FROM tmp_seq
WHERE n <= 1000;

-- ========================================
-- 7. 댓글 5,000개 생성 (부모 댓글만, 단순화)
-- ========================================
INSERT INTO comment (comment_id, member_id, post_id, content, deleted, created_at, modified_at)
SELECT
    t1.n,
    ((t1.n - 1) % 1000) + 1,
    ((t1.n - 1) % 1000) + 1,
    CONCAT('성능 테스트 댓글 내용 ', t1.n, '. 친구 추천 성능 측정을 위한 테스트 댓글입니다.'),
    0,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(t1.n * 666) * 180) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(t1.n * 777) * 180) DAY)
FROM tmp_seq t1
WHERE t1.n <= 5000;

-- ========================================
-- 8. 댓글 closure table (자기 자신)
-- ========================================
INSERT INTO comment_closure (id, ancestor_id, descendant_id, depth)
SELECT comment_id, comment_id, comment_id, 0
FROM comment
WHERE comment_id <= 5000;

-- ========================================
-- 9. 댓글 추천 10,000개
-- ========================================
INSERT INTO comment_like (comment_like_id, member_id, comment_id, created_at, modified_at)
SELECT
    t1.n,
    ((t1.n - 1) % 1000) + 1,
    ((t1.n - 1) % 5000) + 1,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(t1.n * 888) * 180) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(t1.n * 999) * 180) DAY)
FROM tmp_seq t1
WHERE t1.n <= 10000;

-- ========================================
-- 10. 블랙리스트 100건 (10% 회원이 평균 1명 차단)
-- ========================================
INSERT INTO member_blacklist (member_black_list_id, request_member_id, black_member_id, created_at, modified_at)
SELECT
    n,
    n,
    CASE
        WHEN n + 500 > 1000 THEN n + 500 - 1000
        ELSE n + 500
    END,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 1111) * 180) DAY),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 1111) * 180) DAY)
FROM tmp_seq
WHERE n <= 100;

-- ========================================
-- 완료 및 정리
-- ========================================
COMMIT;
SET FOREIGN_KEY_CHECKS = 1;

-- 임시 테이블 삭제
DROP TABLE IF EXISTS tmp_seq;

-- 통계 출력
SELECT '시드 데이터 로드 완료!' AS status;
SELECT COUNT(*) AS member_count FROM member;
SELECT COUNT(*) AS friendship_count FROM friendship;
SELECT COUNT(*) AS post_count FROM post;
SELECT COUNT(*) AS post_like_count FROM post_like;
SELECT COUNT(*) AS comment_count FROM comment;
SELECT COUNT(*) AS comment_like_count FROM comment_like;
SELECT COUNT(*) AS blacklist_count FROM member_blacklist;
