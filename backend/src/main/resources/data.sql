-- 비밀로그 샘플 데이터 스크립트
-- 사용자 4명, 각종 게시글 및 댓글, 롤링페이퍼 메시지, 추천 등을 포함
-- 전체 트랜잭션으로 묶어 하나라도 실패시 전체 롤백

-- 트랜잭션 시작
START TRANSACTION;

-- 에러 발생시 롤백 처리를 위한 설정
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;

-- 사용자 설정 데이터 (4개)
INSERT INTO setting (setting_id, message_notification, comment_notification, post_featured_notification)
VALUES
    (1, 1, 1, 1),
    (2, 1, 1, 1),
    (3, 1, 0, 1),
    (4, 0, 1, 0);

-- 카카오 토큰 데이터 (4개)
INSERT INTO kakao_token (kakao_token_id, kakao_access_token, kakao_refresh_token, created_at, modified_at)
VALUES
    (1, 'kakao_access_token_user1_sample123456', 'kakao_refresh_token_user1_sample789012', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 6 HOUR)),
    (2, 'kakao_access_token_user2_sample123456', 'kakao_refresh_token_user2_sample789012', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 6 HOUR)),
    (3, 'kakao_access_token_user3_sample234567', 'kakao_refresh_token_user3_sample890123', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 8 HOUR)),
    (4, 'kakao_access_token_user4_sample345678', 'kakao_refresh_token_user4_sample901234', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 12 HOUR));

-- 사용자 데이터 (4명) - member 테이블 사용
INSERT INTO member (member_id, setting_id, kakao_token_id, social_id, provider, member_name, role, social_nickname, thumbnail_image, created_at, modified_at)
VALUES
    (1, 1, 1, '3973639063', 'KAKAO', '비밀로그개발자', 'ADMIN', '정재익', 'http://k.kakaocdn.net/dn/bIks3d/btsMMBlpal1/dPX58KruRPDmnFO8LcauM0/img_110x110.jpg', NOW(6), NOW(6)),
    (2, 2, 2, '1234561111', 'KAKAO', '비밀농부1', 'USER', '카카오닉네임1', 'https://example.com/thumb1.jpg', NOW(6), NOW(6)),
    (3, 3, 3, '7890121111', 'KAKAO', '익명작가', 'USER', '카카오닉네임2', 'https://example.com/thumb2.jpg', NOW(6), NOW(6)),
    (4, 4, 4, '4456781111', 'KAKAO', '롤링메신저', 'ADMIN', '카카오닉네임3', 'https://example.com/thumb3.jpg', NOW(6), NOW(6));

-- 게시글 데이터 (로그인/비로그인 사용자 혼합, 총 30개)
-- 사용자2의 게시글 (ERROR 유형 8개) + 익명 게시글 2개
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (1, 2, '로그인 버그 발견', '<p><strong style="color: rgb(230, 0, 0);">카카오 로그인 시 무한 로딩이 발생합니다.</strong></p><p>로그인 버튼을 눌렀는데 <em>계속 로딩</em>만 돌아가요...</p>', 15, 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (2, null, '댓글 작성 오류', '<p><u>댓글 작성 후 새로고침하면 댓글이 사라져요.</u></p><p>정말 <strong style="color: rgb(204, 0, 0);">불편</strong>합니다.</p>', 23, 0, '1234', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (3, 2, '이미지 업로드 실패', '<h2>프로필 이미지 변경 불가</h2><p>프로필 이미지 변경이 <strong>되지 않습니다</strong>. 계속 <em>실패</em>한다고 나와요.</p>', 8, 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (4, 2, '알림 설정 버그', '<p>알림 설정을 변경해도 <span style="color: rgb(230, 0, 0);">적용이 안 돼요</span>.</p><p>설정에서 알림을 <strong>꺼놨는데</strong> 계속 알림이 와요.</p>', 12, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (5, 2, '롤링페이퍼 오류', '<p><strong style="color: rgb(0, 102, 204);">롤링페이퍼 메시지 위치 겹침 현상</strong></p><p>메시지 작성 후 위치가 <em>겹쳐서</em> 보입니다. 다른 메시지와 겹쳐요...</p>', 31, 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (6, null, '검색 기능 버그', '<p>한글 검색이 <u>제대로 작동하지 않아요</u>.</p><p><strong>영어는 되는데</strong> 한글만 안 돼요.</p>', 19, 0, '5678', DATE_SUB(NOW(6), INTERVAL 6 HOUR), DATE_SUB(NOW(6), INTERVAL 6 HOUR)),
    (7, 2, '모바일 화면 깨짐', '<h3>모바일 레이아웃 버그</h3><p>스마트폰에서 레이아웃이 <span style="color: rgb(230, 0, 0);">깨져 보여요</span>.</p><ul><li>메뉴가 겹침</li><li>글자가 잘림</li><li>버튼이 화면 밖으로</li></ul>', 27, 0, null, DATE_SUB(NOW(6), INTERVAL 5 HOUR), DATE_SUB(NOW(6), INTERVAL 5 HOUR)),
    (8, 2, '추천 기능 오류', '<p>글 추천을 눌러도 <strong style="color: rgb(204, 0, 0);">반응이 없습니다</strong>.</p><p>버튼을 <em>여러 번 눌러봤는데</em> 소용없어요.</p>', 14, 0, null, DATE_SUB(NOW(6), INTERVAL 4 HOUR), DATE_SUB(NOW(6), INTERVAL 4 HOUR)),
    (9, 2, '페이지네이션 버그', '<p><u>다음 페이지로 넘어가지 않아요</u>.</p><p>페이지 번호를 눌러도 <strong>계속 첫 페이지</strong>만 보입니다.</p>', 9, 0, null, DATE_SUB(NOW(6), INTERVAL 3 HOUR), DATE_SUB(NOW(6), INTERVAL 3 HOUR)),
    (10, 2, '로그아웃 오류', '<p><strong style="color: rgb(230, 0, 0);">로그아웃 후에도 로그인 상태가 유지돼요</strong>.</p><p>로그아웃 버튼을 눌렀는데 <em>여전히 로그인</em> 되어있어요.</p>', 21, 0, null, DATE_SUB(NOW(6), INTERVAL 2 HOUR), DATE_SUB(NOW(6), INTERVAL 2 HOUR));

-- 사용자3의 게시글 (IMPROVEMENT 유형 7개) + 익명 게시글 3개
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (11, 3, '다크모드 기능 요청', '<h2 style="color: rgb(102, 60, 187);">다크모드 추가 요청</h2><p>밤에 사용할 때 <strong>다크모드</strong>가 있으면 좋겠어요.</p><p>눈이 <em>너무 부셔서</em> 불편해요.</p>', 45, 0, null, DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (12, null, '태그 기능 추가', '<p>게시글에 <strong style="color: rgb(0, 138, 0);">#태그</strong>를 달 수 있으면 좋을 것 같아요.</p><p>글 분류가 <em>훨씬 편할</em> 것 같습니다.</p>', 33, 0, '9999', DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (13, 3, '북마크 기능', '<p><u>마음에 드는 글을 저장할 수 있는 기능</u>이 있으면 좋겠어요.</p><p>나중에 <strong>다시 보기</strong> 편하게요!</p>', 28, 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (14, null, '글자 크기 조절', '<h3>글자 크기 조절 옵션</h3><p>글자 크기를 <span style="color: rgb(0, 102, 204);">조절할 수 있는 옵션</span>이 있으면 좋겠습니다.</p><ol><li>작게</li><li>보통</li><li>크게</li></ol>', 17, 0, '1111', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (15, 3, '이모티콘 추가', '<p>댓글에 <strong style="color: rgb(250, 197, 28);">이모티콘</strong>을 사용할 수 있으면 더 재미있을 것 같아요.</p><p>감정 표현이 <em>훨씬 풍부</em>해질 거예요.</p>', 52, 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (16, 3, '검색 필터 기능', '<p><strong>검색 필터 기능</strong> 제안드립니다.</p><ul><li>날짜별 검색</li><li>작성자별 검색</li><li>태그별 검색</li></ul><p>이렇게 있으면 <span style="color: rgb(0, 138, 0);">훨씬 편할</span> 것 같아요.</p>', 26, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (17, 3, '알림 소리 설정', '<h2>알림 소리 커스터마이징</h2><p>알림 소리를 <strong style="color: rgb(147, 101, 184);">선택</strong>할 수 있는 기능을 추가해주세요.</p><p>기본 소리가 <em>너무 단조로워요</em>.</p>', 39, 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (18, null, '게시글 임시저장', '<p>작성 중인 글을 <u>임시저장</u>할 수 있으면 좋겠어요.</p><p>실수로 나갔을 때 <strong style="color: rgb(230, 0, 0);">내용이 다 사라져서</strong> 속상했어요.</p>', 22, 0, '2222', DATE_SUB(NOW(6), INTERVAL 8 HOUR), DATE_SUB(NOW(6), INTERVAL 8 HOUR)),
    (19, 3, '사용자 차단 기능', '<p>특정 사용자를 <strong>차단</strong>할 수 있는 기능이 필요해요.</p><p><span style="color: rgb(230, 0, 0);">불쾌한 댓글</span>을 다는 사람을 안 보고 싶어요.</p>', 41, 0, null, DATE_SUB(NOW(6), INTERVAL 7 HOUR), DATE_SUB(NOW(6), INTERVAL 7 HOUR)),
    (20, 3, '테마 변경 기능', '<h3 style="color: rgb(147, 101, 184);">다양한 테마 제공</h3><p>다양한 <strong>테마</strong>를 선택할 수 있으면 좋겠습니다.</p><p>기본, 다크, <em>파스텔</em> 등 여러 가지로요!</p>', 34, 0, null, DATE_SUB(NOW(6), INTERVAL 6 HOUR), DATE_SUB(NOW(6), INTERVAL 6 HOUR));

-- 사용자4의 게시글 (POST, COMMENT 유형 6개) + 익명 게시글 4개
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (21, 4, '부적절한 게시글 신고', '<p><strong style="color: rgb(230, 0, 0);">욕설이 포함된 게시글</strong>을 발견했습니다.</p><p>커뮤니티 규칙을 <em>위반</em>하는 내용이에요.</p>', 18, 0, null, DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY)),
    (22, null, '스팸 댓글 신고', '<p>같은 내용의 댓글을 <u>반복해서 작성</u>하는 사용자가 있어요.</p><p><strong>도배성 댓글</strong>이 너무 많습니다.</p>', 29, 0, '3333', DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (23, 4, '광고성 게시글', '<h3>상업적 광고 게시글 신고</h3><p><span style="color: rgb(230, 0, 0);">상업적 목적</span>의 게시글이 올라왔어요.</p><p>계속 <strong>제품 홍보</strong>를 하고 있습니다.</p>', 16, 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (24, null, '악성 댓글 신고', '<p><strong style="color: rgb(204, 0, 0);">개인 공격성 댓글</strong>을 신고합니다.</p><p>특정 사용자를 <em>지속적으로 비난</em>하고 있어요.</p>', 24, 0, '4444', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (25, 4, '도배성 게시글', '<p>의미없는 글을 <u>반복해서 작성</u>하고 있어요.</p><p>게시판이 <strong style="color: rgb(230, 0, 0);">도배</strong>되고 있습니다.</p>', 13, 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (26, null, '비방 댓글', '<p>특정인을 <strong>비방</strong>하는 댓글이 있습니다.</p><p><span style="color: rgb(230, 0, 0);">인신공격성 내용</span>이 포함되어 있어요.</p>', 31, 0, '5555', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (27, 4, '허위정보 게시글', '<h2 style="color: rgb(204, 0, 0);">허위 정보 유포</h2><p><strong>잘못된 정보</strong>를 퍼뜨리는 글이 있어요.</p><p>많은 사람들이 <em>오해</em>할 수 있습니다.</p>', 27, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (28, null, '음란성 내용', '<p><span style="color: rgb(230, 0, 0);">부적절한 내용</span>이 포함된 게시글입니다.</p><p>청소년에게 <strong>유해한 내용</strong>이에요.</p>', 19, 0, '6666', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (29, 4, '혐오 발언', '<p>특정 집단을 <strong style="color: rgb(204, 0, 0);">혐오</strong>하는 발언이 담긴 댓글이에요.</p><p><u>차별적인 표현</u>이 사용되었습니다.</p>', 22, 0, null, DATE_SUB(NOW(6), INTERVAL 10 HOUR), DATE_SUB(NOW(6), INTERVAL 10 HOUR)),
    (30, 4, '개인정보 노출', '<h3 style="color: rgb(230, 0, 0);">개인정보 유출 신고</h3><p>타인의 <strong>개인정보</strong>가 노출된 글이 있습니다.</p><p>전화번호와 주소가 <em>공개</em>되어 있어요.</p>', 35, 0, null, DATE_SUB(NOW(6), INTERVAL 9 HOUR), DATE_SUB(NOW(6), INTERVAL 9 HOUR));

-- 사용자1(비밀로그개발자) 작성 게시글 (7개) - 다양한 주제와 높은 조회수
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (31, 1, '비밀로그 2.0 업데이트 안내', '<h1 style="color: rgb(0, 102, 204);">비밀로그 2.0 업데이트 안내</h1><p>안녕하세요! <strong>비밀로그 개발자</strong>입니다.</p><p>이번 2.0 업데이트에서는 다음과 같은 기능이 추가되었습니다:</p><ul><li><strong style="color: rgb(0, 138, 0);">실시간 인기글 기능</strong></li><li>롤링페이퍼 개선</li><li>알림 기능 강화</li></ul><p>많은 이용 부탁드립니다!</p>', 187, 1, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (32, 1, 'React Query 최적화 팁', '<h2 style="color: rgb(102, 60, 187);">TanStack Query 최적화 팁</h2><p>TanStack Query를 사용하면서 배운 최적화 팁을 공유합니다.</p><blockquote><strong>staleTime</strong>과 <strong>cacheTime</strong>을 적절히 설정하면 불필요한 API 호출을 크게 줄일 수 있어요.</blockquote><p>특히 <code>optimistic update</code>를 활용하면 UX가 <em>크게 개선</em>됩니다.</p>', 142, 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (33, 1, 'Spring Boot 3.4 마이그레이션 후기', '<h2>Spring Boot 3.4 마이그레이션</h2><p>Spring Boot 3.4로 업그레이드하면서 겪은 이슈들과 해결 방법을 정리했습니다.</p><p><strong style="color: rgb(230, 0, 0);">주의!</strong> Spring Security 설정이 많이 바뀌어서 주의가 필요합니다.</p><p>주요 변경사항:</p><ol><li>Security 설정 방식</li><li>필터 체인 구조</li><li>권한 검증 로직</li></ol>', 98, 0, null, DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY)),
    (34, 1, '개발자의 일상', '<p><em>오늘도 버그와 싸우는 하루였습니다...</em></p><p>하지만 <strong style="color: rgb(0, 138, 0);">여러분의 피드백</strong> 덕분에 더 나은 서비스를 만들 수 있어서 행복합니다.</p><p>앞으로도 <u>열심히 개발</u>하겠습니다!</p>', 156, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (35, 1, 'QueryDSL vs JPQL 성능 비교', '<h2 style="color: rgb(0, 102, 204);">QueryDSL vs JPQL</h2><p>실제 프로젝트에서 <strong>QueryDSL</strong>과 <strong>JPQL</strong>의 성능을 비교해봤습니다.</p><p>복잡한 쿼리에서는 QueryDSL이 다음 면에서 <em>훨씬 우수</em>했습니다:</p><ul><li>가독성 향상</li><li>타입 안정성</li><li>컴파일 타임 체크</li></ul>', 123, 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (36, 1, '다음 업데이트 계획', '<h1 style="color: rgb(147, 101, 184);">다음 업데이트 계획</h1><p>커뮤니티에서 요청하신 <strong style="color: rgb(0, 138, 0);">다크모드 기능</strong>을 다음 업데이트에서 추가할 예정입니다.</p><p>또한 다음 기능들도 검토 중입니다:</p><ul><li>태그 기능</li><li>북마크 기능</li><li>검색 필터</li></ul><p><em>기대해주세요!</em></p>', 201, 1, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (37, 1, 'Redis 캐싱 전략 소개', '<h2 style="color: rgb(230, 0, 0);">Redis 캐싱 전략</h2><p>비밀로그에서 사용하는 <strong>Redis 캐싱 전략</strong>을 소개합니다.</p><blockquote>실시간 인기글 점수 계산에 <code>Sorted Set</code>을 활용하고, <strong>지수 감쇠 알고리즘</strong>을 적용했습니다.</blockquote><p>이를 통해 <em>실시간</em>으로 인기글을 추적할 수 있습니다.</p>', 89, 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY));

-- 주간 인기글 후보 (최근 7일 이내 작성, 조회수 높음, 추천 많이 받을 예정) (3개)
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (38, 2, '비밀로그 사용 꿀팁 모음', '<h1 style="color: rgb(250, 197, 28);">비밀로그 사용 꿀팁 모음</h1><p>비밀로그를 <strong>1년 넘게</strong> 사용하면서 발견한 숨겨진 기능들을 정리해봤어요!</p><h3>주요 팁:</h3><ul><li>롤링페이퍼 꾸미기</li><li>게시글 검색 활용법</li><li>알림 설정 최적화</li></ul><p><em>유용한 팁들이 가득합니다!</em></p>', 165, 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (39, 3, '감동적인 롤링페이퍼 이야기', '<h2 style="color: rgb(230, 126, 197);">롤링페이퍼로 친구 감동시킨 썰</h2><p>친구 생일에 <strong>롤링페이퍼</strong>를 만들어줬는데 정말 감동받았다고 하더라고요.</p><blockquote>익명으로 진심을 전할 수 있는 게 이렇게 좋은 건지 처음 알았어요.</blockquote><p>여러분도 <span style="color: rgb(0, 138, 0);">소중한 사람</span>에게 만들어주세요!</p>', 189, 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (40, 2, '커뮤니티 활성화 제안', '<h2 style="color: rgb(0, 102, 204);">커뮤니티 활성화 제안</h2><p>비밀로그 커뮤니티가 더 <strong>활발</strong>해지려면 어떤 기능이 필요할까요?</p><p>여러분의 <u>의견</u>을 듣고 싶습니다.</p><p>제안 사항:</p><ol><li>카테고리별 게시판</li><li>이벤트 기능</li><li>뱃지 시스템</li></ol>', 147, 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY));

-- 레전드 후보 (추천 20개 이상 받을 예정, 조회수 매우 높음) (3개)
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (41, 3, '인생 롤링페이퍼 만들기 가이드', '<h1 style="color: rgb(147, 101, 184);">인생 롤링페이퍼 만들기 완벽 가이드</h1><p>지금까지 <strong style="color: rgb(230, 0, 0);">50개 이상</strong>의 롤링페이퍼를 만들면서 터득한 모든 노하우를 공유합니다.</p><h2>목차:</h2><ol><li><strong>디자인 선택</strong> - 테마별 추천</li><li><strong>메시지 배치</strong> - 레이아웃 팁</li><li><strong>감동적인 문구</strong> - 작성법</li><li><strong>타이밍</strong> - 최적의 전달 시기</li></ol><blockquote>완벽한 롤링페이퍼를 위한 <em>모든 것</em>을 담았습니다!</blockquote>', 542, 0, null, DATE_SUB(NOW(6), INTERVAL 25 DAY), DATE_SUB(NOW(6), INTERVAL 25 DAY)),
    (42, 2, '비밀로그로 고백 성공한 썰', '<h1 style="color: rgb(230, 126, 197);">비밀로그로 고백 성공!</h1><p>익명으로 고백할 수 있는 <strong>롤링페이퍼 기능</strong> 덕분에 용기를 내서 고백했고...</p><p><span style="color: rgb(0, 138, 0); font-size: 18px;"><strong>결국 사귀게 되었습니다!</strong></span></p><blockquote>제 인생을 바꿔준 비밀로그에게 감사를 전하고 싶어요.</blockquote><p>여러분도 <em>용기내세요</em>!</p>', 687, 0, null, DATE_SUB(NOW(6), INTERVAL 30 DAY), DATE_SUB(NOW(6), INTERVAL 30 DAY)),
    (43, 1, '비밀로그 개발 비하인드 스토리', '<h1 style="color: rgb(0, 102, 204);">비밀로그 개발 비하인드 스토리</h1><p>비밀로그를 처음 기획하고 개발하기까지의 과정을 공유합니다.</p><h2>주요 내용:</h2><ul><li><strong style="color: rgb(102, 60, 187);">익명 소통의 가치</strong> - 왜 익명인가?</li><li><strong style="color: rgb(0, 138, 0);">기술 스택 선택</strong> - Spring Boot + Next.js</li><li><strong style="color: rgb(230, 0, 0);">겪었던 어려움</strong> - 실시간 알림, 보안</li><li><strong>보람</strong> - 여러분의 피드백</li></ul><blockquote>솔직하게 담았습니다. 긴 글 읽어주셔서 감사합니다!</blockquote>', 723, 1, null, DATE_SUB(NOW(6), INTERVAL 20 DAY), DATE_SUB(NOW(6), INTERVAL 20 DAY));

-- 인기댓글이 달릴 게시글 (댓글 추천 3개 이상 받을 예정) (2개)
INSERT INTO post (post_id, member_id, title, content, views, is_notice, password, created_at, modified_at)
VALUES
    (44, 2, '여러분의 개발 동기는 무엇인가요?', '<h2 style="color: rgb(102, 60, 187);">여러분의 개발 동기는?</h2><p>개발을 시작하게 된 <strong>계기</strong>가 궁금합니다.</p><p>저는 뭔가를 <span style="color: rgb(0, 138, 0);">만들어내는 과정</span>이 너무 재미있어서 시작했는데, 여러분은 어떤가요?</p><p><em>댓글로 공유해주세요!</em></p>', 134, 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (45, 3, '인생 최고의 조언', '<h2 style="color: rgb(250, 197, 28);">인생 최고의 조언</h2><p>지금까지 받았던 조언 중에서 <strong>가장 도움이 되었던 말</strong>을 공유해주세요.</p><p>저는 <blockquote>"실패는 성공의 어머니"</blockquote>라는 말이 가장 위로가 되었어요.</p><p>여러분의 <span style="color: rgb(0, 102, 204);">인생 명언</span>이 궁금합니다!</p>', 178, 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY));

-- 댓글 데이터 (각 게시글당 3-4개씩, 총 22개)
-- 게시글 1번의 댓글들
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (1, 1, 3, '<p>저도 같은 문제를 겪었어요. <strong>브라우저 캐시</strong>를 지우니까 해결됐어요!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (2, 1, 4, '<p>이 버그는 <span style="color: rgb(0, 102, 204);">개발팀</span>에서 확인했습니다. <em>곧 수정될 예정</em>이에요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (3, 1, null, '<p>익명 사용자도 <u>같은 문제</u>입니다.</p>', 0, '1234', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY));

-- 게시글 2번의 댓글들
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (4, 2, 4, '<p>댓글 작성 후 <strong>페이지 새로고침</strong> 대신 <code>F5</code>를 눌러보세요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (5, 2, 2, '<p>네, 시도해봤는데도 <span style="color: rgb(230, 0, 0);">계속 문제가 발생</span>해요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (6, 2, null, '<p><strong>모바일</strong>에서도 같은 증상이 있어요.</p>', 0, '5678', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (7, 2, 3, '<p>저는 <em>크롬에서만</em> 문제가 발생하고 <strong style="color: rgb(0, 138, 0);">파이어폭스에서는 정상</strong>이에요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY));

-- 계속해서 각 게시글마다 댓글 추가
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (8, 3, 3, '<p>이미지 <strong>용량</strong>이 너무 큰 건 아닐까요?</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (9, 3, 4, '<p><span style="color: rgb(0, 102, 204);">jpg, png</span> 파일만 지원됩니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (10, 3, null, '<p>저도 <em>같은 문제</em>예요.</p>', 0, '9999', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),

    (11, 4, 2, '<p>알림 설정을 변경한 후 <strong>로그아웃 후 재로그인</strong>해보세요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (12, 4, 3, '<p>저는 <span style="color: rgb(0, 138, 0);">정상 작동</span>해요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (13, 4, null, '<p><u>브라우저 문제</u>일 수도 있어요.</p>', 0, '1111', DATE_SUB(NOW(6), INTERVAL 12 HOUR), DATE_SUB(NOW(6), INTERVAL 12 HOUR)),

    (14, 5, 3, '<p>롤링페이퍼 메시지 위치는 <strong>자동으로 배치</strong>됩니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (15, 5, 4, '<p><em>새로고침</em>하면 정상 위치로 표시될 거예요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 18 HOUR), DATE_SUB(NOW(6), INTERVAL 18 HOUR)),
    (16, 5, null, '<p><span style="color: rgb(0, 138, 0);">감사합니다!</span> 해결됐어요.</p>', 0, '2222', DATE_SUB(NOW(6), INTERVAL 12 HOUR), DATE_SUB(NOW(6), INTERVAL 12 HOUR));

-- 더 많은 댓글들 (나머지 게시글들)
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (17, 11, 2, '<p>다크모드 <strong style="color: rgb(102, 60, 187);">정말 필요</strong>해요! 찬성합니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (18, 11, 4, '<p>다크모드 개발 <span style="color: rgb(0, 102, 204);">검토 중</span>입니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (19, 11, null, '<p><em>빨리 추가되길</em> 바라요!</p>', 0, '3333', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),

    (20, 12, 4, '<p>태그 기능은 <strong>좋은 아이디어</strong>네요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (21, 12, 2, '<p><u>카테고리 기능</u>도 함께 추가되면 좋을 것 같아요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (22, 12, null, '<p><span style="color: rgb(0, 138, 0);">#해시태그</span> 형태면 더 좋겠어요.</p>', 0, '4444', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY));

-- 사용자1 작성 댓글 (다양한 게시글에) (12개)
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (23, 11, 1, '<p>다크모드 정말 <strong style="color: rgb(102, 60, 187);">좋은 아이디어</strong>네요! 다음 업데이트에서 꼭 추가하겠습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (24, 15, 1, '<p>이모티콘 기능도 <span style="color: rgb(0, 102, 204);">검토하고 있습니다</span>. 조금만 기다려주세요!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (25, 2, 1, '<p>댓글 작성 오류는 <strong>다음 패치</strong>에서 수정될 예정입니다. <em>불편을 드려 죄송합니다.</em></p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (26, 5, 1, '<p>롤링페이퍼 위치 겹침 문제는 이미 <span style="color: rgb(0, 138, 0);">해결되었습니다</span>. 새로고침 해보세요!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (27, 38, 1, '<p>꿀팁 정리 <strong>감사합니다!</strong> 공식 가이드에 반영하겠습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (28, 39, 1, '<p>이런 <span style="color: rgb(230, 126, 197);">따뜻한 이야기</span>를 들으면 개발 보람을 느낍니다. <em>감사합니다!</em></p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (29, 40, 1, '<p>커뮤니티 활성화 아이디어 <strong>좋네요</strong>. 내부 회의에서 검토해보겠습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (30, 41, 1, '<p>정말 <strong style="color: rgb(147, 101, 184);">대단한 노하우</strong>네요! 많은 분들에게 도움이 될 것 같습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 24 DAY), DATE_SUB(NOW(6), INTERVAL 24 DAY)),
    (31, 42, 1, '<p><span style="color: rgb(0, 138, 0);">축하드립니다!</span> 비밀로그가 행복에 작은 도움이 되어 기쁩니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 29 DAY), DATE_SUB(NOW(6), INTERVAL 29 DAY)),
    (32, 44, 1, '<p>저는 <strong>누군가의 문제를 해결해주는 과정</strong>이 즐거워서 개발을 시작했어요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (33, 45, 1, '<p><em>"완벽을 추구하지 말고, <strong style="color: rgb(0, 102, 204);">완성을 추구하라</strong>"</em>는 말이 제게 큰 도움이 되었습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (34, 20, 1, '<p>테마 변경 기능은 <span style="color: rgb(147, 101, 184);">다크모드</span>와 함께 개발 예정입니다!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 6 HOUR), DATE_SUB(NOW(6), INTERVAL 6 HOUR));

-- 사용자1 게시글에 달린 댓글 (다른 사용자들이 작성) (10개)
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (35, 31, 2, '<p>업데이트 <strong>기대됩니다!</strong> 특히 <span style="color: rgb(0, 102, 204);">실시간 인기글 기능</span>이 유용할 것 같아요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (36, 31, 3, '<p>롤링페이퍼 개선 <em>감사합니다</em>. 더 사용하기 편해졌어요!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (37, 32, 2, '<p>React Query 팁 <strong style="color: rgb(102, 60, 187);">정말 도움됐어요</strong>. 제 프로젝트에도 적용해봐야겠습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (38, 32, 4, '<p><code>optimistic update</code> 예제 코드도 공유해주시면 좋을 것 같아요!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (39, 34, 3, '<p>개발자님 <strong style="color: rgb(0, 138, 0);">항상 감사합니다!</strong> 덕분에 좋은 서비스 사용하고 있어요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (40, 36, 2, '<p><span style="color: rgb(147, 101, 184);">다크모드</span> 기다리고 있었습니다! 빨리 나왔으면 좋겠어요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY)),
    (41, 36, 3, '<p>태그 기능도 추가되면 게시글 찾기가 <em>훨씬 편할</em> 것 같네요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 20 HOUR), DATE_SUB(NOW(6), INTERVAL 20 HOUR)),
    (42, 37, 4, '<p>Redis 캐싱 전략 <strong>정말 흥미롭네요</strong>. 지수 감쇠 알고리즘은 어떻게 구현하셨나요?</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (43, 43, 2, '<p>비밀로그 개발 스토리 <span style="color: rgb(230, 126, 197);">정말 감동적</span>이에요. 앞으로도 응원합니다!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 20 DAY), DATE_SUB(NOW(6), INTERVAL 20 DAY)),
    (44, 43, 3, '<p>기술 스택 선택 이유가 궁금했는데 <strong>자세히 알려주셔서 감사</strong>합니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 19 DAY), DATE_SUB(NOW(6), INTERVAL 19 DAY));

-- 인기댓글 (추천 3개 이상 받을 예정) (8개)
INSERT INTO comment (comment_id, post_id, member_id, content, deleted, password, created_at, modified_at)
VALUES
    (45, 44, 2, '<p>저는 어릴 때부터 <strong style="color: rgb(0, 102, 204);">컴퓨터 게임</strong>을 좋아했고, <em>"이걸 내가 만들 수 있다면 얼마나 멋질까?"</em>라는 생각에서 시작했어요.</p><p>지금은 게임보다 <span style="color: rgb(0, 138, 0);">웹 개발</span>이 더 재밌습니다!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (46, 44, 3, '<p><strong>문제 해결</strong>하는 걸 좋아해서 개발을 시작했습니다.</p><p><span style="color: rgb(230, 0, 0);">코드 한 줄</span>로 수백 명의 시간을 절약해줄 수 있다는 게 <em>정말 매력적</em>이에요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (47, 44, 4, '<p>처음에는 <u>돈 때문에</u> 시작했지만, 지금은 순수하게 <strong style="color: rgb(102, 60, 187);">개발 자체가 즐거워서</strong> 하고 있습니다.</p><p>무언가를 <em>창조하는 과정</em>이 정말 매력적이에요!</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (48, 45, 2, '<p><em>"실패는 그냥 실패일 뿐, <strong style="color: rgb(230, 0, 0);">성공의 어머니가 되려면 거기서 배워야 한다</strong>"</em>는 말을 들었을 때 큰 깨달음을 얻었어요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (49, 45, 3, '<p><em>"<strong style="color: rgb(0, 102, 204);">완벽을 추구하지 말고 완성을 추구하라</strong>"</em></p><p>이 말 덕분에 미루던 프로젝트를 <span style="color: rgb(0, 138, 0);">완성</span>할 수 있었습니다.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (50, 45, 4, '<p><em>"<strong style="color: rgb(147, 101, 184);">비교는 진보의 적이다</strong>"</em></p><p>남과 비교하지 않고 <u>어제의 나</u>와 비교하면서 성장하라는 조언이 가장 도움됐어요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (51, 41, 2, '<p>이 가이드 <strong style="color: rgb(250, 197, 28);">정말 최고예요!</strong></p><p>특히 <span style="color: rgb(0, 102, 204);">메시지 배치 팁</span>이 정말 유용했습니다. 덕분에 친구 롤링페이퍼를 <em>멋지게</em> 만들었어요.</p>', 0, null, DATE_SUB(NOW(6), INTERVAL 24 DAY), DATE_SUB(NOW(6), INTERVAL 24 DAY)),
    (52, 42, 3, '<p><span style="color: rgb(230, 126, 197);">진짜 감동적인 스토리</span>네요...</p><p>저도 <strong>용기내서 고백</strong>해봐야겠어요. <em>비밀로그 최고!</em></p>', 0, null, DATE_SUB(NOW(6), INTERVAL 29 DAY), DATE_SUB(NOW(6), INTERVAL 29 DAY));

-- 댓글 클로저 테이블 (계층형 댓글 구조)
INSERT INTO comment_closure (ancestor_id, descendant_id, depth)
VALUES
    -- 각 댓글의 자기 자신 참조 (depth 0) - 기존 22개
    (1, 1, 0), (2, 2, 0), (3, 3, 0), (4, 4, 0), (5, 5, 0),
    (6, 6, 0), (7, 7, 0), (8, 8, 0), (9, 9, 0), (10, 10, 0),
    (11, 11, 0), (12, 12, 0), (13, 13, 0), (14, 14, 0), (15, 15, 0),
    (16, 16, 0), (17, 17, 0), (18, 18, 0), (19, 19, 0), (20, 20, 0),
    (21, 21, 0), (22, 22, 0),

    -- 새로 추가된 댓글의 자기 자신 참조 (23~52번)
    (23, 23, 0), (24, 24, 0), (25, 25, 0), (26, 26, 0), (27, 27, 0),
    (28, 28, 0), (29, 29, 0), (30, 30, 0), (31, 31, 0), (32, 32, 0),
    (33, 33, 0), (34, 34, 0), (35, 35, 0), (36, 36, 0), (37, 37, 0),
    (38, 38, 0), (39, 39, 0), (40, 40, 0), (41, 41, 0), (42, 42, 0),
    (43, 43, 0), (44, 44, 0), (45, 45, 0), (46, 46, 0), (47, 47, 0),
    (48, 48, 0), (49, 49, 0), (50, 50, 0), (51, 51, 0), (52, 52, 0),

    -- 게시글 1번 내 댓글 계층 (댓글 1,2,3)
    -- 1번 댓글에 2번 댓글이 답글
    (1, 2, 1),

    -- 게시글 2번 내 댓글 계층 (댓글 4,5,6,7,25)
    -- 4번 댓글에 5번 댓글이 답글
    (4, 5, 1),
    -- 5번 댓글에 6번 댓글이 답글 (3단계 계층)
    (5, 6, 1),
    -- 조상-자손 관계 추가 (4번은 6번의 조상)
    (4, 6, 2),
    -- 사용자1(25번)이 답글 작성
    (4, 25, 1),

    -- 게시글 11번 내 댓글 계층 (댓글 17,18,19,23)
    -- 17번 댓글에 18번 댓글이 답글
    (17, 18, 1),
    -- 18번 댓글에 19번 댓글이 답글
    (18, 19, 1),
    -- 조상-자손 관계 추가 (17번은 19번의 조상)
    (17, 19, 2),
    -- 사용자1(23번)이 17번 댓글에 답글
    (17, 23, 1),

    -- 게시글 31번 내 댓글 계층 (댓글 35,36 - 사용자1 게시글)
    -- 35번 댓글에 36번 댓글이 답글
    (35, 36, 1),

    -- 게시글 32번 내 댓글 계층 (댓글 37,38 - 사용자1 게시글)
    -- 37번 댓글에 38번 댓글이 답글
    (37, 38, 1),

    -- 게시글 36번 내 댓글 계층 (댓글 40,41 - 사용자1 게시글)
    -- 40번 댓글에 41번 댓글이 답글
    (40, 41, 1),

    -- 게시글 43번 내 댓글 계층 (댓글 43,44 - 사용자1 게시글)
    -- 43번 댓글에 44번 댓글이 답글
    (43, 44, 1),

    -- 게시글 44번 내 댓글 계층 (댓글 32,45,46,47 - 인기댓글용)
    -- 45번 댓글에 32번 댓글(사용자1)이 답글
    (45, 32, 1),
    -- 45번 댓글에 46번 댓글이 답글
    (45, 46, 1),

    -- 게시글 45번 내 댓글 계층 (댓글 33,48,49,50 - 인기댓글용)
    -- 48번 댓글에 33번 댓글(사용자1)이 답글
    (48, 33, 1),
    -- 48번 댓글에 49번 댓글이 답글
    (48, 49, 1);

-- 게시글 추천 데이터
INSERT INTO post_like (post_like_id, member_id, post_id)
VALUES
    (1, 2, 11), (2, 2, 15), (3, 2, 17), (4, 2, 19),  -- 사용자2가 추천한 글들
    (5, 3, 1), (6, 3, 5), (7, 3, 21), (8, 3, 25),   -- 사용자3이 추천한 글들
    (9, 4, 2), (10, 4, 12), (11, 4, 16), (12, 4, 20),  -- 사용자4가 추천한 글들
    (13, 2, 22), (14, 3, 3), (15, 4, 13);           -- 추가 추천들

-- 레전드 게시글 추천 (20개 이상)
INSERT INTO post_like (post_like_id, member_id, post_id)
VALUES
    -- 게시글 41번 (레전드 - 25개)
    (16, 1, 41), (17, 2, 41), (18, 3, 41), (19, 4, 41), (20, 1, 41),
    (21, 2, 41), (22, 3, 41), (23, 4, 41), (24, 1, 41), (25, 2, 41),
    (26, 3, 41), (27, 4, 41), (28, 1, 41), (29, 2, 41), (30, 3, 41),
    (31, 4, 41), (32, 1, 41), (33, 2, 41), (34, 3, 41), (35, 4, 41),
    (36, 1, 41), (37, 2, 41), (38, 3, 41), (39, 4, 41), (40, 1, 41),

    -- 게시글 42번 (레전드 - 30개)
    (41, 1, 42), (42, 2, 42), (43, 3, 42), (44, 4, 42), (45, 1, 42),
    (46, 2, 42), (47, 3, 42), (48, 4, 42), (49, 1, 42), (50, 2, 42),
    (51, 3, 42), (52, 4, 42), (53, 1, 42), (54, 2, 42), (55, 3, 42),
    (56, 4, 42), (57, 1, 42), (58, 2, 42), (59, 3, 42), (60, 4, 42),
    (61, 1, 42), (62, 2, 42), (63, 3, 42), (64, 4, 42), (65, 1, 42),
    (66, 2, 42), (67, 3, 42), (68, 4, 42), (69, 1, 42), (70, 2, 42),

    -- 게시글 43번 (레전드 - 22개, 사용자1 게시글)
    (71, 2, 43), (72, 3, 43), (73, 4, 43), (74, 2, 43), (75, 3, 43),
    (76, 4, 43), (77, 2, 43), (78, 3, 43), (79, 4, 43), (80, 2, 43),
    (81, 3, 43), (82, 4, 43), (83, 2, 43), (84, 3, 43), (85, 4, 43),
    (86, 2, 43), (87, 3, 43), (88, 4, 43), (89, 2, 43), (90, 3, 43),
    (91, 4, 43), (92, 2, 43);

-- 주간 인기글 추천 (최근 7일, 1개 이상)
INSERT INTO post_like (post_like_id, member_id, post_id)
VALUES
    -- 게시글 38번 (주간 - 10개)
    (93, 1, 38), (94, 2, 38), (95, 3, 38), (96, 4, 38), (97, 1, 38),
    (98, 2, 38), (99, 3, 38), (100, 4, 38), (101, 1, 38), (102, 2, 38),

    -- 게시글 39번 (주간 - 12개)
    (103, 1, 39), (104, 2, 39), (105, 3, 39), (106, 4, 39), (107, 1, 39),
    (108, 2, 39), (109, 3, 39), (110, 4, 39), (111, 1, 39), (112, 2, 39),
    (113, 3, 39), (114, 4, 39),

    -- 게시글 40번 (주간 - 8개)
    (115, 1, 40), (116, 2, 40), (117, 3, 40), (118, 4, 40), (119, 1, 40),
    (120, 2, 40), (121, 3, 40), (122, 4, 40);

-- 사용자1 게시글 추천
INSERT INTO post_like (post_like_id, member_id, post_id)
VALUES
    -- 게시글 31번 (사용자1 - 15개, 주간 후보)
    (123, 2, 31), (124, 3, 31), (125, 4, 31), (126, 2, 31), (127, 3, 31),
    (128, 4, 31), (129, 2, 31), (130, 3, 31), (131, 4, 31), (132, 2, 31),
    (133, 3, 31), (134, 4, 31), (135, 2, 31), (136, 3, 31), (137, 4, 31),

    -- 게시글 32번 (사용자1 - 7개)
    (138, 2, 32), (139, 3, 32), (140, 4, 32), (141, 2, 32), (142, 3, 32),
    (143, 4, 32), (144, 2, 32),

    -- 게시글 34번 (사용자1 - 12개, 주간 후보)
    (145, 2, 34), (146, 3, 34), (147, 4, 34), (148, 2, 34), (149, 3, 34),
    (150, 4, 34), (151, 2, 34), (152, 3, 34), (153, 4, 34), (154, 2, 34),
    (155, 3, 34), (156, 4, 34),

    -- 게시글 36번 (사용자1 - 18개, 주간 후보)
    (157, 2, 36), (158, 3, 36), (159, 4, 36), (160, 2, 36), (161, 3, 36),
    (162, 4, 36), (163, 2, 36), (164, 3, 36), (165, 4, 36), (166, 2, 36),
    (167, 3, 36), (168, 4, 36), (169, 2, 36), (170, 3, 36), (171, 4, 36),
    (172, 2, 36), (173, 3, 36), (174, 4, 36),

    -- 게시글 37번 (사용자1 - 5개)
    (175, 2, 37), (176, 3, 37), (177, 4, 37), (178, 2, 37), (179, 3, 37);

-- 댓글 추천 데이터
INSERT INTO comment_like (comment_like_id, comment_id, member_id)
VALUES
    (1, 1, 3), (2, 1, 4),          -- 1번 댓글 추천
    (3, 2, 2), (4, 2, 3),          -- 2번 댓글 추천
    (5, 4, 2),                  -- 4번 댓글 추천
    (6, 8, 3), (7, 8, 4),          -- 8번 댓글 추천
    (8, 11, 3),                 -- 11번 댓글 추천
    (9, 17, 3), (10, 17, 4),        -- 17번 댓글 추천
    (11, 18, 2),                 -- 18번 댓글 추천
    (12, 20, 2), (13, 20, 3);        -- 20번 댓글 추천

-- 인기댓글 추천 (3개 이상)
INSERT INTO comment_like (comment_like_id, comment_id, member_id)
VALUES
    -- 댓글 45번 (인기댓글 - 5개)
    (14, 45, 1), (15, 45, 3), (16, 45, 4), (17, 45, 2), (18, 45, 3),

    -- 댓글 46번 (인기댓글 - 4개)
    (19, 46, 1), (20, 46, 2), (21, 46, 4), (22, 46, 2),

    -- 댓글 47번 (인기댓글 - 3개)
    (23, 47, 1), (24, 47, 3), (25, 47, 4),

    -- 댓글 48번 (인기댓글 - 4개)
    (26, 48, 1), (27, 48, 2), (28, 48, 3), (29, 48, 4),

    -- 댓글 49번 (인기댓글 - 3개)
    (30, 49, 1), (31, 49, 2), (32, 49, 4),

    -- 댓글 50번 (인기댓글 - 5개)
    (33, 50, 1), (34, 50, 2), (35, 50, 3), (36, 50, 4), (37, 50, 2),

    -- 댓글 51번 (인기댓글 - 6개)
    (38, 51, 1), (39, 51, 2), (40, 51, 3), (41, 51, 4), (42, 51, 1), (43, 51, 3),

    -- 댓글 52번 (인기댓글 - 5개)
    (44, 52, 1), (45, 52, 2), (46, 52, 4), (47, 52, 3), (48, 52, 2);

-- 사용자1 작성 댓글 추천
INSERT INTO comment_like (comment_like_id, comment_id, member_id)
VALUES
    (49, 23, 2), (50, 23, 3), (51, 23, 4),  -- 댓글 23번 (사용자1)
    (52, 24, 2), (53, 24, 3),               -- 댓글 24번 (사용자1)
    (54, 25, 2), (55, 25, 4),               -- 댓글 25번 (사용자1)
    (56, 26, 3), (57, 26, 4),               -- 댓글 26번 (사용자1)
    (58, 27, 2), (59, 27, 3), (60, 27, 4),  -- 댓글 27번 (사용자1)
    (61, 28, 2), (62, 28, 3), (63, 28, 4),  -- 댓글 28번 (사용자1)
    (64, 29, 2), (65, 29, 3),               -- 댓글 29번 (사용자1)
    (66, 30, 2), (67, 30, 3), (68, 30, 4),  -- 댓글 30번 (사용자1)
    (69, 31, 2), (70, 31, 3), (71, 31, 4),  -- 댓글 31번 (사용자1)
    (72, 32, 2), (73, 32, 3), (74, 32, 4),  -- 댓글 32번 (사용자1)
    (75, 33, 2), (76, 33, 3),               -- 댓글 33번 (사용자1)
    (77, 34, 2), (78, 34, 4);               -- 댓글 34번 (사용자1)

-- 사용자1 게시글에 달린 댓글 추천
INSERT INTO comment_like (comment_like_id, comment_id, member_id)
VALUES
    (79, 35, 1), (80, 35, 3), (81, 35, 4),  -- 댓글 35번
    (82, 36, 1), (83, 36, 2), (84, 36, 4),  -- 댓글 36번
    (85, 37, 1), (86, 37, 3), (87, 37, 4),  -- 댓글 37번
    (88, 38, 1), (89, 38, 2), (90, 38, 3),  -- 댓글 38번
    (91, 39, 1), (92, 39, 2), (93, 39, 4),  -- 댓글 39번
    (94, 40, 1), (95, 40, 3), (96, 40, 4),  -- 댓글 40번
    (97, 41, 1), (98, 41, 2),               -- 댓글 41번
    (99, 42, 1), (100, 42, 2), (101, 42, 3),  -- 댓글 42번
    (102, 43, 1), (103, 43, 3), (104, 43, 4),  -- 댓글 43번
    (105, 44, 1), (106, 44, 2);             -- 댓글 44번

-- 롤링페이퍼 메시지 데이터 (x, y 컬럼 사용) - 암호화된 content
INSERT INTO message (message_id, member_id, x, y, anonymity, content, deco_type, created_at, modified_at)
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

-- 사용자1(비밀로그개발자)의 롤링페이퍼 메시지 (다른 사용자들이 작성) (10개)
INSERT INTO message (message_id, member_id, x, y, anonymity, content, deco_type, created_at, modified_at)
VALUES
    (16, 2, 0, 5, '감사하는사람', 'oXEYvoqn1bXQDWiwZS1b8/P9chdaVGVoQa+OqGOwmyX8YWQy7KQKYbDGRYHfQi2+LLg6N9F+txgutn/bTc4Hh9tB6+pkilUx3B6QYQQ7yMjvoWhKN6cUKu2G5VlguEY0', 'STRAWBERRY', DATE_SUB(NOW(6), INTERVAL 10 DAY), DATE_SUB(NOW(6), INTERVAL 10 DAY)),
    (17, 3, 0, 6, '응원합니다', 'dMY54tYCdd1lRuE2dzKWebeFNIYAaDQNLLg6N9F+txhKKic4HXjKBb4FoI+0QI+PNpCQbpL7MYpmJoDGeUayzutn/bTc4Hh9tB6+pkilUxrQc24WmYq1PnHC5pothjGw==', 'CARROT', DATE_SUB(NOW(6), INTERVAL 9 DAY), DATE_SUB(NOW(6), INTERVAL 9 DAY)),
    (18, 4, 0, 7, '팬입니다', 'Wp/swJel1cUa0JZmYKCGxdY8q3/vI3uf+Lvj83nJVzaT1Dol/XG4MvCmdvwfF+LyCvXwMjl+vpbat9nDsk7AuWPdnf2Mb20fYf2dFI2dcMvFA702Hp3S8xaK7BOQT/h8=', 'COFFEE', DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY)),
    (19, 2, 1, 8, '최고개발자', '+UPgH81T8oH/JdM5yBAZ8J9d/XYaS5zFa9W+Qb+S0KxOQ0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJLjBNjDdWdVjaPzRq8n/QkR0ItXO11k+Z9yn2pt9smF10er2QXnNS', 'CAT', DATE_SUB(NOW(6), INTERVAL 7 DAY), DATE_SUB(NOW(6), INTERVAL 7 DAY)),
    (20, 3, 2, 9, '고마워요', 'vpk43LiTn33AZIahk/Bkdrfeikmj4HFNy5/PYX03rtCOQ0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJLjBNjDdWdQ==', 'STAR', DATE_SUB(NOW(6), INTERVAL 6 DAY), DATE_SUB(NOW(6), INTERVAL 6 DAY)),
    (21, 4, 3, 5, '존경합니다', 'vct16trR+lcKu6WZe3fKUQDhBrIvv3IaNj9arbyMw3yxaK7BOQT/h8T1Dol/XG4MvCmdvwfF+LyCvXwMjl+vpbat9nDsk7AuaJWtWsXVjczIGrwnr4txAvEhBOr7IDyH', 'APPLE', DATE_SUB(NOW(6), INTERVAL 5 DAY), DATE_SUB(NOW(6), INTERVAL 5 DAY)),
    (22, 2, 4, 6, '항상응원', 'C8MLY30ITER+6kNL62vAQ9Zcohv8RNjTnIDcK2Hl448Q0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJCmv1Rc/EYZc0giVee3vpZwui84PvPiBpWU+b/7P1AdZ+rCCgPIgsA', 'DOG', DATE_SUB(NOW(6), INTERVAL 4 DAY), DATE_SUB(NOW(6), INTERVAL 4 DAY)),
    (23, 3, 5, 7, '멋진개발자', 'DEi6y1BYrryfJVYYi2pLPE+dV8/pXm+ksR0dSmQl6m/FA702Hp3S8xaK7BOQT/h8+OQ0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJLjBNjDdWdbwwR42BDNZN7G4ri4amv2s=', 'MOON', DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 3 DAY)),
    (24, 4, 6, 8, '건강하세요', 'yc3c9B4g3dChnsK/NxW8sPOfyzG5QTuFMIpO45wiD8hT1Dol/XG4MvCmdvwfF+LyCvXwMjl+vpbat9nDhKKic4HXjKBb4FoI+0QI+PNpCQbpL7MYpmJoDGeUayzqS0Ua', 'BALLOON', DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 2 DAY)),
    (25, 2, 6, 9, '감사드려요', 'oogDmwzcTYhe9B+tEb/4wvYWmKf9WF2Nf8WV3wf1D1dY8q3/vI3uf+Lvj83nJVzcjIYSBxr6PZkJVgm/xjRTLukJuBod6DCKzh2V0mNOLMw==', 'SUNFLOWER', DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 1 DAY));

-- 비회원이 사용자1에게 남긴 롤링페이퍼 메시지 (5개)
INSERT INTO message (message_id, member_id, x, y, anonymity, content, deco_type, created_at, modified_at)
VALUES
    (26, null, 7, 5, '익명의친구', 'kPqh+eGx/xXCZebRvMQSmsXYcyog/aX0lHH54i6KmgQQ0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJR4EUv1nuHxKpZGRmY7uxaP0taL9I0lYyB4IJ7GESDN3dQ84+RHlQ2e', 'DRAGON', DATE_SUB(NOW(6), INTERVAL 12 DAY), DATE_SUB(NOW(6), INTERVAL 12 DAY)),
    (27, null, 8, 6, '지나가는사람', 'Swc6933Sj9qA1x4vlm5RZLp1FEpbeQlUfkL2DoGUWU6xaK7BOQT/h8q7YblWWC4RjSc8yFSMMua7nrN0brJeAN/YLBokRKrjXNTMoGwpG9vCNnssGurFaw==', 'ANGEL', DATE_SUB(NOW(6), INTERVAL 11 DAY), DATE_SUB(NOW(6), INTERVAL 11 DAY)),
    (28, null, 9, 7, '비밀사용자', 'aLWa04MllNYa9DIO2TvO34SDVUF5/q0dGLubE19h4CjT1Dol/XG4MvCmdvwfF+LyCvXwMjl+vpbat9nDVV3UqrNh8i5xSu5TWmCY5dIuuKzR7KbJ10/tbILx+ywwtHvO', 'PHOENIX', DATE_SUB(NOW(6), INTERVAL 10 DAY), DATE_SUB(NOW(6), INTERVAL 10 DAY)),
    (29, null, 10, 8, '익명응원단', 'LVoSznMzfeX7KTRfNOxYbR2hyAm4/Y1FDQwWy7AkTz+OQ0aMCr9FyOvFlo7k+y5xfLAgxie6KxkJ9lfCzayutPyO2OiSq98incGpUdGqfLmx94tjhhzIZml6S0Ua1akFr', 'STAR', DATE_SUB(NOW(6), INTERVAL 9 DAY), DATE_SUB(NOW(6), INTERVAL 9 DAY)),
    (30, null, 11, 9, '감사합니다', 'H98D1yYWvFAmnJBHJ8xP5DsoulerLLZErer4DxM1XBFQbLkRZaYsqMb08bZXZ+HJY8q3/vI3uf+Lvj83nJVzfjnzJ4fMa/9/N+FJrV52aQ==', 'RAINBOW', DATE_SUB(NOW(6), INTERVAL 8 DAY), DATE_SUB(NOW(6), INTERVAL 8 DAY));

-- 신고 데이터 (각 신고 유형별로)
INSERT INTO report (report_id, member_id, report_type, target_id, content, created_at, modified_at)
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
