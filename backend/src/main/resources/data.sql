-- 비밀로그 샘플 데이터 스크립트
-- 사용자 3명, 각종 게시글 및 댓글, 롤링페이퍼 메시지, 추천 등을 포함
-- 전체 트랜잭션으로 묶어 하나라도 실패시 전체 롤백

-- 트랜잭션 시작
START TRANSACTION;

-- 에러 발생시 롤백 처리를 위한 설정
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

-- 사용자 설정 데이터 (3개)
INSERT INTO setting (setting_id, message_notification, comment_notification, post_featured_notification)
VALUES
    (2, 1, 1, 1),
    (3, 1, 0, 1),
    (4, 0, 1, 0);

-- 사용자 데이터 (3명) - 테이블명 "user" 사용
INSERT INTO user (user_id, setting_id, social_id, provider, user_name, role, social_nickname, thumbnail_image, created_at, modified_at)
VALUES
    (2, 2, '1234561111', 'KAKAO', '비밀농부1', 'USER', '카카오닉네임1', 'https://example.com/thumb1.jpg', NOW(6), NOW(6)),
    (3, 3, '7890121111', 'KAKAO', '익명작가', 'USER', '카카오닉네임2', 'https://example.com/thumb2.jpg', NOW(6), NOW(6)),
    (4, 4, '4456781111', 'KAKAO', '롤링메신저', 'ADMIN', '카카오닉네임3', 'https://example.com/thumb3.jpg', NOW(6), NOW(6));

-- 토큰 데이터 (사용자 2, 3, 4)
INSERT INTO token (token_id, user_id, access_token, refresh_token, created_at, modified_at)
VALUES
    (2, 2, 'kakao_access_token_user2_sample123456', 'kakao_refresh_token_user2_sample789012', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 6 HOUR)),
    (3, 3, 'kakao_access_token_user3_sample234567', 'kakao_refresh_token_user3_sample890123', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 8 HOUR)),
    (4, 4, 'kakao_access_token_user4_sample345678', 'kakao_refresh_token_user4_sample901234', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 12 HOUR));

-- 게시글 데이터 (로그인/비로그인 사용자 혼합, 총 30개)
-- 사용자2의 게시글 (ERROR 유형 8개) + 익명 게시글 2개
INSERT INTO post (post_id, user_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (1, 2, '로그인 버그 발견', '카카오 로그인 시 무한 로딩이 발생합니다.', 15, 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (2, null, '댓글 작성 오류', '댓글 작성 후 새로고침하면 댓글이 사라져요.', 23, 0, '1234', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (3, 2, '이미지 업로드 실패', '프로필 이미지 변경이 되지 않습니다.', 8, 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (4, 2, '알림 설정 버그', '알림 설정을 변경해도 적용이 안 돼요.', 12, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (5, 2, '롤링페이퍼 오류', '메시지 작성 후 위치가 겹쳐서 보입니다.', 31, 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (6, null, '검색 기능 버그', '한글 검색이 제대로 작동하지 않아요.', 19, 0, '5678', DATE_SUB(NOW(6), INTERVAL 6 HOUR), DATE_SUB(NOW(6), INTERVAL 6 HOUR)),
    (7, 2, '모바일 화면 깨짐', '스마트폰에서 레이아웃이 깨져 보여요.', 27, 0, null, DATE_SUB(NOW(6), INTERVAL 5 HOUR), DATE_SUB(NOW(6), INTERVAL 5 HOUR)),
    (8, 2, '추천 기능 오류', '글 추천을 눌러도 반응이 없습니다.', 14, 0, null, DATE_SUB(NOW(6), INTERVAL 4 HOUR), DATE_SUB(NOW(6), INTERVAL 4 HOUR)),
    (9, 2, '페이지네이션 버그', '다음 페이지로 넘어가지 않아요.', 9, 0, null, DATE_SUB(NOW(6), INTERVAL 3 HOUR), DATE_SUB(NOW(6), INTERVAL 3 HOUR)),
    (10, 2, '로그아웃 오류', '로그아웃 후에도 로그인 상태가 유지돼요.', 21, 0, null, DATE_SUB(NOW(6), INTERVAL 2 HOUR), DATE_SUB(NOW(6), INTERVAL 2 HOUR));

-- 사용자3의 게시글 (IMPROVEMENT 유형 7개) + 익명 게시글 3개
INSERT INTO post (post_id, user_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (11, 3, '다크모드 기능 요청', '밤에 사용할 때 다크모드가 있으면 좋겠어요.', 45, 0, null, DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (12, null, '태그 기능 추가', '게시글에 태그를 달 수 있으면 좋을 것 같아요.', 33, 0, '9999', DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (13, 3, '북마크 기능', '마음에 드는 글을 저장할 수 있는 기능이 있으면 좋겠어요.', 28, 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (14, null, '글자 크기 조절', '글자 크기를 조절할 수 있는 옵션이 있으면 좋겠습니다.', 17, 0, '1111', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (15, 3, '이모티콘 추가', '댓글에 이모티콘을 사용할 수 있으면 더 재미있을 것 같아요.', 52, 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (16, 3, '검색 필터 기능', '날짜, 작성자별로 검색할 수 있는 필터가 있으면 좋겠어요.', 26, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (17, 3, '알림 소리 설정', '알림 소리를 선택할 수 있는 기능을 추가해주세요.', 39, 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (18, null, '게시글 임시저장', '작성 중인 글을 임시저장할 수 있으면 좋겠어요.', 22, 0, '2222', DATE_SUB(NOW(6), INTERVAL 8 HOUR), DATE_SUB(NOW(6), INTERVAL 8 HOUR)),
    (19, 3, '사용자 차단 기능', '특정 사용자를 차단할 수 있는 기능이 필요해요.', 41, 0, null, DATE_SUB(NOW(6), INTERVAL 7 HOUR), DATE_SUB(NOW(6), INTERVAL 7 HOUR)),
    (20, 3, '테마 변경 기능', '다양한 테마를 선택할 수 있으면 좋겠습니다.', 34, 0, null, DATE_SUB(NOW(6), INTERVAL 6 HOUR), DATE_SUB(NOW(6), INTERVAL 6 HOUR));

-- 사용자4의 게시글 (POST, COMMENT 유형 6개) + 익명 게시글 4개
INSERT INTO post (post_id, user_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (21, 4, '부적절한 게시글 신고', '욕설이 포함된 게시글을 발견했습니다.', 18, 0, null, DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY)),
    (22, null, '스팸 댓글 신고', '같은 내용의 댓글을 반복해서 작성하는 사용자가 있어요.', 29, 0, '3333', DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (23, 4, '광고성 게시글', '상업적 목적의 게시글이 올라왔어요.', 16, 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (24, null, '악성 댓글 신고', '개인 공격성 댓글을 신고합니다.', 24, 0, '4444', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (25, 4, '도배성 게시글', '의미없는 글을 반복해서 작성하고 있어요.', 13, 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (26, null, '비방 댓글', '특정인을 비방하는 댓글이 있습니다.', 31, 0, '5555', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (27, 4, '허위정보 게시글', '잘못된 정보를 퍼뜨리는 글이 있어요.', 27, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (28, null, '음란성 내용', '부적절한 내용이 포함된 게시글입니다.', 19, 0, '6666', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (29, 4, '혐오 발언', '특정 집단을 혐오하는 발언이 담긴 댓글이에요.', 22, 0, null, DATE_SUB(NOW(6), INTERVAL 10 HOUR), DATE_SUB(NOW(6), INTERVAL 10 HOUR)),
    (30, 4, '개인정보 노출', '타인의 개인정보가 노출된 글이 있습니다.', 35, 0, null, DATE_SUB(NOW(6), INTERVAL 9 HOUR), DATE_SUB(NOW(6), INTERVAL 9 HOUR));

-- 댓글 데이터 (각 게시글당 3-4개씩, 총 22개)
-- 게시글 1번의 댓글들
INSERT INTO comment (comment_id, post_id, user_id, content, deleted, password, created_at, modified_at)
VALUES
    (1, 1, 3, '저도 같은 문제를 겪었어요. 브라우저 캐시를 지우니까 해결됐어요!', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (2, 1, 4, '이 버그는 개발팀에서 확인했습니다. 곧 수정될 예정이에요.', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (3, 1, null, '익명 사용자도 같은 문제입니다.', 0, '1234', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY));

-- 게시글 2번의 댓글들
INSERT INTO comment (comment_id, post_id, user_id, content, deleted, password, created_at, modified_at)
VALUES
    (4, 2, 4, '댓글 작성 후 페이지 새로고침 대신 F5를 눌러보세요.', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (5, 2, 2, '네, 시도해봤는데도 계속 문제가 발생해요.', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (6, 2, null, '모바일에서도 같은 증상이 있어요.', 0, '5678', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (7, 2, 3, '저는 크롬에서만 문제가 발생하고 파이어폭스에서는 정상이에요.', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY));

-- 계속해서 각 게시글마다 댓글 추가
INSERT INTO comment (comment_id, post_id, user_id, content, deleted, password, created_at, modified_at)
VALUES
    (8, 3, 3, '이미지 용량이 너무 큰 건 아닐까요?', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (9, 3, 4, 'jpg, png 파일만 지원됩니다.', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (10, 3, null, '저도 같은 문제예요.', 0, '9999', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),

    (11, 4, 2, '알림 설정을 변경한 후 로그아웃 후 재로그인해보세요.', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (12, 4, 3, '저는 정상 작동해요.', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (13, 4, null, '브라우저 문제일 수도 있어요.', 0, '1111', DATE_SUB(NOW(6), INTERVAL 12 HOUR), DATE_SUB(NOW(6), INTERVAL 12 HOUR)),

    (14, 5, 3, '롤링페이퍼 메시지 위치는 자동으로 배치됩니다.', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (15, 5, 4, '새로고침하면 정상 위치로 표시될 거예요.', 0, null, DATE_SUB(NOW(6), INTERVAL 18 HOUR), DATE_SUB(NOW(6), INTERVAL 18 HOUR)),
    (16, 5, null, '감사합니다! 해결됐어요.', 0, '2222', DATE_SUB(NOW(6), INTERVAL 12 HOUR), DATE_SUB(NOW(6), INTERVAL 12 HOUR));

-- 더 많은 댓글들 (나머지 게시글들)
INSERT INTO comment (comment_id, post_id, user_id, content, deleted, password, created_at, modified_at)
VALUES
    (17, 11, 2, '다크모드 정말 필요해요! 찬성합니다.', 0, null, DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (18, 11, 4, '다크모드 개발 검토 중입니다.', 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (19, 11, null, '빨리 추가되길 바라요!', 0, '3333', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),

    (20, 12, 4, '태그 기능은 좋은 아이디어네요.', 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (21, 12, 2, '카테고리 기능도 함께 추가되면 좋을 것 같아요.', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (22, 12, null, '해시태그 형태면 더 좋겠어요.', 0, '4444', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY));

-- 댓글 클로저 테이블 (계층형 댓글 구조) - TEMPORARILY DISABLED
-- INSERT INTO comment_closure (ancestor_id, descendant_id, depth)
-- VALUES
--     -- 각 댓글의 자기 자신 참조 (depth 0)
--     (1, 1, 0), (2, 2, 0), (3, 3, 0), (4, 4, 0), (5, 5, 0), (6, 6, 0), (7, 7, 0), (8, 8, 0), (9, 9, 0), (10, 10, 0),
--     (11, 11, 0), (12, 12, 0), (13, 13, 0), (14, 14, 0), (15, 15, 0), (16, 16, 0), (17, 17, 0), (18, 18, 0), (19, 19, 0), (20, 20, 0),
--     (21, 21, 0), (22, 22, 0),
--
--     -- 대댓글 관계 (예시: 2번 댓글에 대한 5번 댓글의 답글)
--     (2, 5, 1),
--     -- 3번 댓글에 대한 6번 댓글의 답글
--     (3, 6, 1),
--     -- 4번 댓글에 대한 12번 댓글의 답글
--     (4, 12, 1);

-- 게시글 추천 데이터
INSERT INTO post_like (post_like_id, user_id, post_id)
VALUES
    (1, 2, 11), (2, 2, 15), (3, 2, 17), (4, 2, 19),  -- 사용자2가 추천한 글들
    (5, 3, 1), (6, 3, 5), (7, 3, 21), (8, 3, 25),   -- 사용자3이 추천한 글들
    (9, 4, 2), (10, 4, 12), (11, 4, 16), (12, 4, 20),  -- 사용자4가 추천한 글들
    (13, 2, 22), (14, 3, 3), (15, 4, 13);           -- 추가 추천들

-- 댓글 추천 데이터
INSERT INTO comment_like (comment_like_id, comment_id, user_id)
VALUES
    (1, 1, 3), (2, 1, 4),          -- 1번 댓글 추천
    (3, 2, 2), (4, 2, 3),          -- 2번 댓글 추천
    (5, 4, 2),                  -- 4번 댓글 추천
    (6, 8, 3), (7, 8, 4),          -- 8번 댓글 추천
    (8, 11, 3),                 -- 11번 댓글 추천
    (9, 17, 3), (10, 17, 4),        -- 17번 댓글 추천
    (11, 18, 2),                 -- 18번 댓글 추천
    (12, 20, 2), (13, 20, 3);        -- 20번 댓글 추천

-- 롤링페이퍼 메시지 데이터 (x, y 컬럼 사용) - 암호화된 content
INSERT INTO message (message_id, user_id, x, y, anonymity, content, deco_type, created_at, modified_at)
VALUES
    (1, 2, 0, 0, '농부', 'oXEYvoqn1bXQDWiwZS1b8/P9chdaVGVoQa+OqGOwmyULLxy4/ROg8QjvoWhKN6cUKu2G5VlguEY0nPMhUjDLmhZMUxvgSRkWfoIqmQ+pTPIHOzUe6pcs7eB8JDdmdypEjCRHVvZy/UImwbxaTTm8vA==', 'STRAWBERRY', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (2, 2, 0, 1, '당근', 'dMY54tYCdd1lRuE2dzKWebeFNIYAaDQNLLg6N9F+txgutn/bTc4Hh9tB6+pkilUxrQc24WmYq1PnHC5pothjG2j6l+50GKLcF2l1P+M2i2nOdUJoEPEWCrHWIMFfQJfp', 'CARROT', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (3, 2, 0, 2, '커피', 'Wp/swJel1cUa0JZmYKCGxdY8q3/vI3uf+Lvj83nJVza5kjuKR7nAR2gWVW6QW2neWPdnf2Mb20fYf2dFI2dcMvFA702Hp3S8xaK7BOQT/h8=', 'COFFEE', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (4, 2, 1, 0, '고양이', '+UPgH81T8oH/JdM5yBAZ8J9d/XYaS5zFa9W+Qb+S0KwH88UGS5XvnVjaPzRq8n/QkR0ItXO11k+Z9yn2pt9smF10er2QXnNSw8Qn3idqujlJ2gwBv+UHnOue07MpsCY8', 'CAT', DATE_SUB(NOW(6), INTERVAL 12 HOUR), DATE_SUB(NOW(6), INTERVAL 12 HOUR)),
    (5, 2, 1, 1, '별빛', 'vpk43LiTn33AZIahk/Bkdrfeikmj4HFNy5/PYX03rtD2xIuGlazREeJ4ZP14EicJCIzhFTbyBt9/skvFDpb9WQ==', 'STAR', DATE_SUB(NOW(6), INTERVAL 6 HOUR), DATE_SUB(NOW(6), INTERVAL 6 HOUR)),

    (6, 3, 0, 3, '사과', 'vct16trR+lcKu6WZe3fKUQDhBrIvv3IaNj9arbyMw3yT1Dol/XG4MvCmdvwfF+LyCvXwMjl+vpbat9nDsk7AuaJWtWsXVjczIGrwnr4txAvEhBOr7IDyHl5IQlYDctTi', 'APPLE', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (7, 3, 0, 4, '멍멍', 'C8MLY30ITER+6kNL62vAQ9Zcohv8RNjTnIDcK2Hl448Cmv1Rc/EYZc0giVee3vpZwui84PvPiBpWU+b/7P1AdZ+rCCgPIgsA9lCGVdv3+T4=', 'DOG', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (8, 3, 0, 5, '달빛', 'DEi6y1BYrryfJVYYi2pLPE+dV8/pXm+ksR0dSmQl6m+OQ0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJLjBNjDdWdbwwR42BDNZN7G4ri4amv2s=', 'MOON', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (9, 3, 1, 3, '풍선', 'yc3c9B4g3dChnsK/NxW8sPOfyzG5QTuFMIpO45wiD8hKKic4HXjKBb4FoI+0QI+PNpCQbpL7MYpmJoDGeUayzqS0Ua1akFrJa5JQGs/Ck1Q=', 'BALLOON', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (10, 3, 1, 4, '해바라', 'oogDmwzcTYhe9B+tEb/4wvYWmKf9WF2Nf8WV3wf1D1cjIYSBxr6PZkJVgm/xjRTLukJuBod6DCKzh2V0mNOLMw==', 'SUNFLOWER', DATE_SUB(NOW(6), INTERVAL 8 HOUR), DATE_SUB(NOW(6), INTERVAL 8 HOUR)),

    (11, 4, 2, 0, '용용', 'kPqh+eGx/xXCZebRvMQSmsXYcyog/aX0lHH54i6KmgR4EUv1nuHxKpZGRmY7uxaP0taL9I0lYyB4IJ7GESDN3dQ84+RHlQ2ezdjRg4KMUDlfFYhX4ZTj6wqJfw8j5amDIoioijAStHWw1PFwqQlzgA==', 'DRAGON', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (12, 4, 2, 1, '천사', 'Swc6933Sj9qA1x4vlm5RZLp1FEpbeQlUfkL2DoGUWU4q7YblWWC4RjSc8yFSMMua7nrN0brJeAN/YLBokRKrjXNTMoGwpG9vCNnssGurFaw=', 'ANGEL', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (13, 4, 2, 2, '불새', 'aLWa04MllNYa9DIO2TvO34SDVUF5/q0dGLubE19h4CjVV3UqrNh8i5xSu5TWmCY5dIuuKzR7KbJ10/tbILx+ywwtHvORBnDLnDwE2ftq7Wc=', 'PHOENIX', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (14, 4, 2, 3, '왕관', 'LVoSznMzfeX7KTRfNOxYbR2hyAm4/Y1FDQwWy7AkTz9lfCzayutPyO2OiSq98incGpUdGqfLmx94tjhhzIZml6S0Ua1akFrJa5JQGs/Ck1Q=', 'STAR', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (15, 4, 2, 4, '무지개', 'H98D1yYWvFAmnJBHJ8xP5DsoulerLLZErer4DxM1XBFQbLkRZaYsqMb08bZXZ+HJfjnzJ4fMa/9/N+FJrV52aQ==', 'RAINBOW', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY));

-- 신고 데이터 (각 신고 유형별로)
INSERT INTO report (report_id, user_id, report_type, target_id, content, created_at, modified_at)
VALUES
    /* ERROR 유형 신고 - target_id는 NULL */
    (1, 2, 'ERROR', NULL, '카카오 로그인 시 무한 로딩 문제', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (2, 3, 'ERROR', NULL, '댓글 작성 후 새로고침하면 사라지는 문제', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (3, 4, 'ERROR', NULL, '프로필 이미지 변경 불가 문제', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),

    /* IMPROVEMENT 유형 신고 - target_id는 NULL */
    (4, 2, 'IMPROVEMENT', NULL, '야간 사용을 위한 다크모드 기능 추가 건의', DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (5, 3, 'IMPROVEMENT', NULL, '게시글 분류를 위한 태그 기능 건의', DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (6, 4, 'IMPROVEMENT', NULL, '좋은 글 저장을 위한 북마크 기능 건의', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),

    /* POST 유형 신고 - target_id는 게시글 ID */
    (7, 2, 'POST', 21, '욕설이 포함된 게시글 신고', DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY)),
    (8, 4, 'POST', 22, '광고성 내용의 반복 게시글 신고', DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (9, 2, 'POST', 23, '잘못된 정보를 담은 게시글 신고', DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),

    /* COMMENT 유형 신고 - target_id는 댓글 ID */
    (10, 2, 'COMMENT', 1, '개인 공격성 댓글 신고', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (11, 3, 'COMMENT', 5, '같은 내용 반복 댓글 신고', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (12, 4, 'COMMENT', 10, '특정인 비방 댓글 신고', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY));

-- 외래 키 제약조건 재활성화
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;

-- 트랜잭션 커밋
COMMIT;