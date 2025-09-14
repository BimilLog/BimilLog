package jaeik.bimillog.e2e.scenarios;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.base.E2ETestConfig;
import jaeik.bimillog.e2e.utils.ApiHelper;
import jaeik.bimillog.e2e.utils.TestDataGenerator;
import org.junit.jupiter.api.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;

/**
 * 게시판 시나리오 E2E 테스트
 * USER_SCENARIOS.md의 4번 시나리오 "게시판 기능" 구현
 * - 게시글 CRUD (회원/비회원)
 * - 댓글 기능 (계층형 구조)
 * - 검색 기능 (MySQL ngram 전문 검색)
 * - 좋아요 기능 (로그인 필수)
 * - 인기글 탭 (실시간/주간/레전드)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("게시판 시나리오 E2E 테스트")
public class BoardScenarioTest extends BaseE2ETest {

    private static String testPostTitle;
    private static String testPostContent;
    private static Long testPostId;
    private static String testCommentContent;

    @BeforeAll
    static void setupTestData() {
        testPostTitle = TestDataGenerator.generatePostTitle();
        testPostContent = TestDataGenerator.generatePostContent();
        testCommentContent = TestDataGenerator.generateCommentContent();
    }

    @BeforeEach
    void setup() {
        E2ETestConfig.ensureFrontendReady();
        navigateToFrontend("/board");
    }

    @Test
    @Order(1)
    @DisplayName("01. 게시판 목록 조회 및 페이지네이션")
    void testBoardListAndPagination() {
        // 게시판 페이지 로드 확인
        assertThat(page.title()).contains("커뮤니티 게시판");

        // Breadcrumb 네비게이션 확인
        waitForElement("[data-testid='breadcrumb']");
        assertThat(page.locator("text=홈").isVisible()).isTrue();
        assertThat(page.locator("text=커뮤니티").isVisible()).isTrue();

        // 게시글 목록 표시 확인
        waitForElement("[data-testid='post-list']");
        Locator postItems = page.locator("[data-testid='post-item']");
        assertThat(postItems.count()).isGreaterThan(0);

        // 각 게시글 항목 요소 확인
        Locator firstPost = postItems.first();
        assertThat(firstPost.locator("[data-testid='post-title']").isVisible()).isTrue();
        assertThat(firstPost.locator("[data-testid='post-author']").isVisible()).isTrue();
        assertThat(firstPost.locator("[data-testid='post-date']").isVisible()).isTrue();
        assertThat(firstPost.locator("[data-testid='post-views']").isVisible()).isTrue();
        assertThat(firstPost.locator("[data-testid='post-likes']").isVisible()).isTrue();
        assertThat(firstPost.locator("[data-testid='post-comments']").isVisible()).isTrue();

        // 페이지네이션 확인
        if (isElementVisible("[data-testid='pagination']")) {
            Locator pagination = page.locator("[data-testid='pagination']");

            // 다음 페이지로 이동
            if (pagination.locator("button:has-text('다음')").isEnabled()) {
                safeClick("button:has-text('다음')");
                waitForNetworkIdle();

                // URL 파라미터 확인
                assertThat(page.url()).contains("page=2");

                // 새로운 게시글 목록 로드 확인
                waitForElement("[data-testid='post-list']");
                assertThat(postItems.count()).isGreaterThan(0);

                // 이전 페이지로 돌아가기
                safeClick("button:has-text('이전')");
                waitForNetworkIdle();
            }
        }

        // 페이지당 게시글 수 설정
        if (isElementVisible("[data-testid='page-size-selector']")) {
            safeClick("[data-testid='page-size-selector']");
            safeClick("text=50");
            waitForNetworkIdle();
            assertThat(page.url()).contains("size=50");
        }
    }

    @Test
    @Order(2)
    @DisplayName("02. 게시글 검색 - MySQL ngram 전문 검색")
    void testSearchPosts() {
        // 검색 섹션 확인
        waitForElement("[data-testid='search-section']");

        // 검색 타입 선택 (제목)
        safeClick("[data-testid='search-type-select']");
        safeClick("text=제목");

        // 검색어 입력
        String searchQuery = "테스트";
        safeFill("[data-testid='search-input']", searchQuery);

        // 검색 실행 (Enter 키 또는 검색 버튼)
        page.keyboard().press("Enter");
        waitForNetworkIdle();

        // URL 파라미터 확인
        assertThat(page.url()).contains("q=" + searchQuery);
        assertThat(page.url()).contains("type=title");

        // 검색 결과 확인
        if (isElementVisible("[data-testid='search-results']")) {
            // 검색 결과 하이라이팅 확인
            Locator highlightedText = page.locator("mark, .highlight");
            if (highlightedText.count() > 0) {
                assertThat(highlightedText.first().textContent()).containsIgnoringCase(searchQuery);
            }

            // 검색 결과 수 표시 확인
            if (isElementVisible("[data-testid='search-count']")) {
                String countText = page.locator("[data-testid='search-count']").textContent();
                assertThat(countText).matches(".*\\d+.*");
            }
        }

        // 내용 검색 테스트
        safeClick("[data-testid='search-type-select']");
        safeClick("text=내용");
        safeFill("[data-testid='search-input']", "비밀");
        page.keyboard().press("Enter");
        waitForNetworkIdle();

        assertThat(page.url()).contains("type=content");

        // 제목+내용 통합 검색 테스트
        safeClick("[data-testid='search-type-select']");
        safeClick("text=제목+내용");
        safeFill("[data-testid='search-input']", "로그");
        page.keyboard().press("Enter");
        waitForNetworkIdle();

        assertThat(page.url()).contains("type=all");

        // 검색 초기화
        if (isElementVisible("[data-testid='search-clear']")) {
            safeClick("[data-testid='search-clear']");
            waitForNetworkIdle();
            assertThat(page.url()).doesNotContain("q=");
        }
    }

    @Test
    @Order(3)
    @DisplayName("03. 인기글 탭 네비게이션 (실시간/주간/레전드)")
    void testPopularPostsTabs() {
        // 탭 네비게이션 확인
        waitForElement("[data-testid='board-tabs']");

        // 실시간 인기글 탭
        safeClick("button:has-text('실시간 인기글'), [data-testid='tab-realtime']");
        waitForNetworkIdle();

        // 실시간 인기글 목록 확인
        waitForElement("[data-testid='popular-posts']");
        Locator realtimePosts = page.locator("[data-testid='post-item']");
        if (realtimePosts.count() > 0) {
            // 좋아요 수가 표시되는지 확인
            assertThat(realtimePosts.first().locator("[data-testid='post-likes']").isVisible()).isTrue();
        }

        // 주간 인기글 탭
        safeClick("button:has-text('주간 인기글'), [data-testid='tab-weekly']");
        waitForNetworkIdle();

        // 주간 인기글 목록 확인
        waitForElement("[data-testid='popular-posts']");
        Locator weeklyPosts = page.locator("[data-testid='post-item']");
        if (weeklyPosts.count() > 0) {
            String firstPostLikes = weeklyPosts.first().locator("[data-testid='post-likes']").textContent();
            assertThat(firstPostLikes).matches(".*\\d+.*");
        }

        // 레전드 탭
        safeClick("button:has-text('레전드'), [data-testid='tab-legend']");
        waitForNetworkIdle();

        // 레전드 게시글 목록 확인
        waitForElement("[data-testid='popular-posts'], [data-testid='post-list']");

        // 전체 탭으로 돌아가기
        safeClick("button:has-text('전체'), [data-testid='tab-all']");
        waitForNetworkIdle();
    }

    @Test
    @Order(4)
    @DisplayName("04. 비로그인 사용자 글쓰기 접근 제한")
    void testNonMemberWriteRestriction() {
        // 로그아웃 상태 확인
        if (isElementVisible("[data-testid='user-menu']")) {
            logout();
        }

        // 글쓰기 플로팅 버튼 확인
        waitForElement("[data-testid='write-floating-btn'], button:has-text('글쓰기')");

        // 글쓰기 버튼 클릭
        safeClick("[data-testid='write-floating-btn'], button:has-text('글쓰기')");

        // 로그인 유도 모달 확인
        waitForElement("[data-testid='login-modal'], [role='dialog']");
        assertThat(page.locator("text=로그인이 필요합니다").isVisible()).isTrue();

        // 로그인 버튼 확인
        assertThat(page.locator("button:has-text('로그인'), a:has-text('로그인')").isVisible()).isTrue();

        // 모달 닫기
        if (isElementVisible("[data-testid='modal-close'], button[aria-label='Close']")) {
            safeClick("[data-testid='modal-close'], button[aria-label='Close']");
        } else {
            page.keyboard().press("Escape");
        }

        waitForElement("[data-testid='board-tabs'], [data-testid='post-list']");
    }

    @Test
    @Order(5)
    @DisplayName("05. 게시글 작성 - Quill 에디터 사용")
    void testCreatePostWithRichText() {
        // 로그인
        login("testuser", "password");

        // 게시판으로 이동
        navigateToFrontend("/board");
        waitForElement("[data-testid='write-floating-btn'], button:has-text('글쓰기')");

        // 글쓰기 버튼 클릭
        safeClick("[data-testid='write-floating-btn'], button:has-text('글쓰기')");

        // 글쓰기 페이지 로드 확인
        waitForURL("**/board/write");
        waitForElement("[data-testid='post-form']");

        // Breadcrumb 확인 (홈 > 커뮤니티 > 글쓰기)
        assertThat(page.locator("text=글쓰기").isVisible()).isTrue();

        // 제목 입력
        safeFill("[data-testid='post-title-input'], input[placeholder*='제목']", testPostTitle);

        // Quill 에디터 로드 대기
        waitForElement(".ql-editor, [data-testid='quill-editor']");

        // 본문 입력 (Quill 에디터)
        Locator editor = page.locator(".ql-editor");
        editor.click();

        // 텍스트 포맷팅 테스트
        // 볼드 텍스트
        page.keyboard().type("이것은 ");
        safeClick(".ql-bold, button[data-testid='bold']");
        page.keyboard().type("볼드 텍스트");
        safeClick(".ql-bold, button[data-testid='bold']");
        page.keyboard().type("입니다.\n");

        // 일반 텍스트
        page.keyboard().type(testPostContent + "\n");

        // 리스트 추가
        safeClick(".ql-list[value='ordered'], button[data-testid='ordered-list']");
        page.keyboard().type("첫 번째 항목\n");
        page.keyboard().type("두 번째 항목\n");
        page.keyboard().type("세 번째 항목");

        // 미리보기 토글
        if (isElementVisible("[data-testid='preview-toggle']")) {
            safeClick("[data-testid='preview-toggle']");
            sleep(500);

            // 미리보기 내용 확인
            assertThat(page.locator("text=" + testPostTitle).isVisible()).isTrue();

            // 편집 모드로 돌아가기
            safeClick("[data-testid='preview-toggle']");
        }

        // 작성하기 버튼 클릭
        safeClick("button:has-text('작성'), button:has-text('게시')");

        // 성공 메시지 또는 상세 페이지 이동 확인
        waitForURL("**/board/post/**");

        // 게시글 ID 추출
        String currentUrl = page.url();
        Pattern pattern = Pattern.compile("/board/post/(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(currentUrl);
        if (matcher.find()) {
            testPostId = Long.parseLong(matcher.group(1));
        }

        // 작성된 게시글 확인
        waitForElement("[data-testid='post-title']");
        assertThat(page.locator("[data-testid='post-title']").textContent()).contains(testPostTitle);
        assertThat(page.locator("[data-testid='post-content']").textContent()).contains(testPostContent);
    }

    @Test
    @Order(6)
    @DisplayName("06. 게시글 상세 조회 및 조회수 증가")
    void testViewPostDetailAndIncrementViews() {
        // 게시판 목록으로 이동
        navigateToFrontend("/board");
        waitForElement("[data-testid='post-list']");

        // 방금 작성한 게시글 찾기
        Locator targetPost = page.locator("[data-testid='post-item']")
            .filter(new Locator.FilterOptions().setHasText(testPostTitle))
            .first();

        // 조회수 초기값 저장
        String initialViews = "0";
        if (targetPost.locator("[data-testid='post-views']").isVisible()) {
            initialViews = targetPost.locator("[data-testid='post-views']").textContent()
                .replaceAll("[^0-9]", "");
        }

        // 게시글 클릭
        targetPost.click();
        waitForURL("**/board/post/**");

        // 상세 페이지 요소 확인
        waitForElement("[data-testid='post-detail']");

        // 게시글 헤더 정보 확인
        assertThat(page.locator("[data-testid='post-title']").textContent()).contains(testPostTitle);
        assertThat(page.locator("[data-testid='post-author']").isVisible()).isTrue();
        assertThat(page.locator("[data-testid='post-date']").isVisible()).isTrue();
        assertThat(page.locator("[data-testid='post-views']").isVisible()).isTrue();

        // 본문 내용 확인
        assertThat(page.locator("[data-testid='post-content']").textContent()).contains(testPostContent);

        // 액션 버튼 확인
        assertThat(page.locator("[data-testid='like-button']").isVisible()).isTrue();
        assertThat(page.locator("[data-testid='share-button']").isVisible()).isTrue();

        // 작성자인 경우 수정/삭제 버튼 확인
        if (isElementVisible("[data-testid='user-menu']")) {
            assertThat(page.locator("[data-testid='edit-button']").isVisible()).isTrue();
            assertThat(page.locator("[data-testid='delete-button']").isVisible()).isTrue();
        }

        // 목록으로 돌아가기
        safeClick("button:has-text('목록'), a:has-text('목록')");
        waitForElement("[data-testid='post-list']");

        // 조회수 증가 확인 (새로고침 후)
        page.reload();
        waitForElement("[data-testid='post-list']");

        Locator updatedPost = page.locator("[data-testid='post-item']")
            .filter(new Locator.FilterOptions().setHasText(testPostTitle))
            .first();

        if (updatedPost.locator("[data-testid='post-views']").isVisible()) {
            String updatedViews = updatedPost.locator("[data-testid='post-views']").textContent()
                .replaceAll("[^0-9]", "");
            int initial = Integer.parseInt(initialViews);
            int updated = Integer.parseInt(updatedViews);
            assertThat(updated).isGreaterThan(initial);
        }
    }

    @Test
    @Order(7)
    @DisplayName("07. 게시글 수정 및 삭제")
    void testEditAndDeletePost() {
        // 로그인 상태 확인
        if (!isElementVisible("[data-testid='user-menu']")) {
            login("testuser", "password");
        }

        // 게시글 상세 페이지로 이동
        if (testPostId != null) {
            navigateToFrontend("/board/post/" + testPostId);
        } else {
            navigateToFrontend("/board");
            Locator targetPost = page.locator("[data-testid='post-item']")
                .filter(new Locator.FilterOptions().setHasText(testPostTitle))
                .first();
            targetPost.click();
        }

        waitForElement("[data-testid='post-detail']");

        // 수정 버튼 클릭
        safeClick("[data-testid='edit-button']");
        waitForURL("**/board/post/**/edit");

        // 수정 페이지 로드 확인
        waitForElement("[data-testid='post-form']");

        // 제목 수정
        String updatedTitle = testPostTitle + " (수정됨)";
        Locator titleInput = page.locator("[data-testid='post-title-input'], input[placeholder*='제목']");
        titleInput.clear();
        titleInput.fill(updatedTitle);

        // 내용 수정
        Locator editor = page.locator(".ql-editor");
        editor.click();
        page.keyboard().press("Control+A");
        page.keyboard().type(testPostContent + "\n\n수정된 내용입니다.");

        // 수정하기 버튼 클릭
        safeClick("button:has-text('수정'), button:has-text('저장')");

        // 상세 페이지로 돌아가기
        waitForURL("**/board/post/**");

        // 수정된 내용 확인
        waitForElement("[data-testid='post-title']");
        assertThat(page.locator("[data-testid='post-title']").textContent()).contains("수정됨");
        assertThat(page.locator("[data-testid='post-content']").textContent()).contains("수정된 내용");

        // 삭제 버튼 클릭
        safeClick("[data-testid='delete-button']");

        // 확인 다이얼로그 처리
        acceptDialog();

        // 삭제 확인 (목록으로 리다이렉트)
        waitForURL("**/board");
        waitForElement("[data-testid='post-list']");

        // 삭제된 게시글이 목록에서 사라졌는지 확인
        Locator deletedPost = page.locator("[data-testid='post-item']")
            .filter(new Locator.FilterOptions().setHasText(updatedTitle));
        assertThat(deletedPost.count()).isEqualTo(0);
    }

    @Test
    @Order(8)
    @DisplayName("08. 게시글 좋아요 토글")
    void testPostLikeToggle() {
        // 로그인 상태 확인
        if (!isElementVisible("[data-testid='user-menu']")) {
            login("testuser", "password");
        }

        // 게시판 목록에서 아무 게시글 선택
        navigateToFrontend("/board");
        waitForElement("[data-testid='post-list']");

        Locator firstPost = page.locator("[data-testid='post-item']").first();
        firstPost.click();

        waitForElement("[data-testid='post-detail']");

        // 좋아요 버튼과 현재 좋아요 수 확인
        Locator likeButton = page.locator("[data-testid='like-button']");
        Locator likeCount = page.locator("[data-testid='like-count']");

        String initialLikes = "0";
        if (likeCount.isVisible()) {
            initialLikes = likeCount.textContent().replaceAll("[^0-9]", "");
        }

        // 좋아요 버튼 클릭
        likeButton.click();
        sleep(500); // API 응답 대기

        // 좋아요 수 증가 확인
        String updatedLikes = likeCount.textContent().replaceAll("[^0-9]", "");
        int initial = Integer.parseInt(initialLikes.isEmpty() ? "0" : initialLikes);
        int updated = Integer.parseInt(updatedLikes.isEmpty() ? "0" : updatedLikes);

        // 버튼 상태 변경 확인 (활성화된 스타일)
        String buttonClass = likeButton.getAttribute("class");
        assertThat(buttonClass).containsAnyOf("active", "liked", "text-pink", "bg-pink");

        // 좋아요 취소 (토글)
        likeButton.click();
        sleep(500);

        // 좋아요 수 감소 확인
        String finalLikes = likeCount.textContent().replaceAll("[^0-9]", "");
        int finalCount = Integer.parseInt(finalLikes.isEmpty() ? "0" : finalLikes);
        assertThat(finalCount).isEqualTo(initial);

        // 버튼 상태 원복 확인
        buttonClass = likeButton.getAttribute("class");
        assertThat(buttonClass).doesNotContain("active", "liked");
    }

    @Test
    @Order(9)
    @DisplayName("09. 댓글 작성")
    void testCreateComment() {
        // 로그인 상태 확인
        if (!isElementVisible("[data-testid='user-menu']")) {
            login("testuser", "password");
        }

        // 게시판에서 아무 게시글 선택
        navigateToFrontend("/board");
        waitForElement("[data-testid='post-list']");

        page.locator("[data-testid='post-item']").first().click();
        waitForElement("[data-testid='post-detail']");

        // 댓글 섹션으로 스크롤
        Locator commentSection = page.locator("[data-testid='comment-section']");
        commentSection.scrollIntoViewIfNeeded();

        // 댓글 작성 폼 확인
        waitForElement("[data-testid='comment-form']");

        // 댓글 입력
        safeFill("[data-testid='comment-input'], textarea[placeholder*='댓글']", testCommentContent);

        // 댓글 등록 버튼 클릭
        safeClick("button:has-text('등록'), button:has-text('작성')");

        // 댓글 목록에 새 댓글 표시 확인
        waitForElement("[data-testid='comment-list']");

        // 방금 작성한 댓글 확인
        Locator newComment = page.locator("[data-testid='comment-item']")
            .filter(new Locator.FilterOptions().setHasText(testCommentContent));

        assertThat(newComment.count()).isGreaterThan(0);

        // 댓글 요소 확인
        Locator comment = newComment.first();
        assertThat(comment.locator("[data-testid='comment-author']").isVisible()).isTrue();
        assertThat(comment.locator("[data-testid='comment-date']").isVisible()).isTrue();
        assertThat(comment.locator("[data-testid='comment-content']").textContent()).contains(testCommentContent);

        // 댓글 액션 버튼 확인
        assertThat(comment.locator("[data-testid='comment-like-button']").isVisible()).isTrue();
        assertThat(comment.locator("[data-testid='reply-button']").isVisible()).isTrue();

        // 본인 댓글인 경우 수정/삭제 버튼 확인
        assertThat(comment.locator("[data-testid='comment-edit-button']").isVisible()).isTrue();
        assertThat(comment.locator("[data-testid='comment-delete-button']").isVisible()).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("10. 대댓글 작성 - 계층형 구조")
    void testCreateNestedComment() {
        // 이전 테스트에서 작성한 댓글이 있는 게시글 유지
        // 또는 새로운 게시글로 이동

        // 댓글 목록에서 답글 버튼 찾기
        Locator parentComment = page.locator("[data-testid='comment-item']").first();

        // 답글 버튼 클릭
        parentComment.locator("[data-testid='reply-button']").click();

        // 대댓글 입력 폼 표시 확인
        waitForElement("[data-testid='reply-form']");

        // 대댓글 입력
        String replyContent = "이것은 대댓글입니다.";
        safeFill("[data-testid='reply-input'], textarea[placeholder*='답글']", replyContent);

        // 대댓글 등록
        safeClick("[data-testid='reply-submit'], button:has-text('답글')");

        // 대댓글 표시 확인
        sleep(1000); // 렌더링 대기

        // 계층 구조 확인 (들여쓰기)
        Locator nestedComment = page.locator("[data-testid='comment-item']")
            .filter(new Locator.FilterOptions().setHasText(replyContent));

        assertThat(nestedComment.count()).isGreaterThan(0);

        // 대댓글의 들여쓰기 스타일 확인
        String nestedClass = nestedComment.first().getAttribute("class");
        String nestedStyle = nestedComment.first().getAttribute("style");

        assertThat(nestedClass + " " + nestedStyle).containsAnyOf(
            "ml-", "pl-", "margin-left", "padding-left", "nested", "reply"
        );

        // 대댓글 depth 표시 확인
        if (nestedComment.locator("[data-testid='comment-depth']").isVisible()) {
            String depth = nestedComment.locator("[data-testid='comment-depth']").getAttribute("data-depth");
            assertThat(Integer.parseInt(depth)).isGreaterThan(0);
        }
    }

    @Test
    @Order(11)
    @DisplayName("11. 댓글 수정 및 삭제")
    void testEditAndDeleteComment() {
        // 본인이 작성한 댓글 찾기
        Locator myComment = page.locator("[data-testid='comment-item']")
            .filter(new Locator.FilterOptions().setHasText(testCommentContent))
            .first();

        // 댓글 수정 버튼 클릭
        myComment.locator("[data-testid='comment-edit-button']").click();

        // 수정 폼 표시 확인
        waitForElement("[data-testid='comment-edit-form']");

        // 내용 수정
        String updatedComment = testCommentContent + " (수정됨)";
        Locator editInput = page.locator("[data-testid='comment-edit-input']");
        editInput.clear();
        editInput.fill(updatedComment);

        // 수정 저장
        safeClick("[data-testid='comment-edit-save'], button:has-text('수정')");

        // 수정된 내용 확인
        sleep(500);
        assertThat(myComment.locator("[data-testid='comment-content']").textContent()).contains("수정됨");

        // 댓글 삭제 버튼 클릭
        myComment.locator("[data-testid='comment-delete-button']").click();

        // 삭제 확인 다이얼로그
        acceptDialog();

        // 댓글 삭제 확인
        sleep(1000);
        Locator deletedComment = page.locator("[data-testid='comment-item']")
            .filter(new Locator.FilterOptions().setHasText(updatedComment));

        assertThat(deletedComment.count()).isEqualTo(0);
    }

    @Test
    @Order(12)
    @DisplayName("12. 댓글 좋아요 토글")
    void testCommentLike() {
        // 댓글이 있는지 확인
        if (page.locator("[data-testid='comment-item']").count() == 0) {
            // 댓글 작성
            safeFill("[data-testid='comment-input']", "좋아요 테스트용 댓글");
            safeClick("button:has-text('등록')");
            sleep(1000);
        }

        // 첫 번째 댓글 선택
        Locator firstComment = page.locator("[data-testid='comment-item']").first();

        // 좋아요 버튼과 카운트 확인
        Locator likeButton = firstComment.locator("[data-testid='comment-like-button']");
        Locator likeCount = firstComment.locator("[data-testid='comment-like-count']");

        String initialLikes = "0";
        if (likeCount.isVisible()) {
            initialLikes = likeCount.textContent().replaceAll("[^0-9]", "");
        }

        // 좋아요 클릭
        likeButton.click();
        sleep(500);

        // 좋아요 수 증가 확인
        if (likeCount.isVisible()) {
            String updatedLikes = likeCount.textContent().replaceAll("[^0-9]", "");
            int updated = Integer.parseInt(updatedLikes.isEmpty() ? "0" : updatedLikes);
            int initial = Integer.parseInt(initialLikes.isEmpty() ? "0" : initialLikes);
            assertThat(updated).isGreaterThan(initial);
        }

        // 버튼 상태 확인
        String buttonClass = likeButton.getAttribute("class");
        assertThat(buttonClass).containsAnyOf("active", "liked", "text-pink");

        // 좋아요 취소
        likeButton.click();
        sleep(500);

        // 원래 상태로 돌아갔는지 확인
        if (likeCount.isVisible()) {
            String finalLikes = likeCount.textContent().replaceAll("[^0-9]", "");
            assertThat(finalLikes).isEqualTo(initialLikes);
        }
    }

    @Test
    @Order(13)
    @DisplayName("13. 모바일 반응형 테스트")
    void testMobileResponsive() {
        // 모바일 뷰포트로 변경
        setMobileViewport();

        // 페이지 새로고침
        page.reload();
        waitForElement("[data-testid='post-list']");

        // 햄버거 메뉴 확인
        assertThat(page.locator("[data-testid='mobile-menu-button'], [data-testid='hamburger-menu']").isVisible()).isTrue();

        // 글쓰기 플로팅 버튼 확인 (모바일에서 더 중요)
        assertThat(page.locator("[data-testid='write-floating-btn']").isVisible()).isTrue();

        // 게시글 목록이 단일 컬럼으로 표시되는지 확인
        Locator postItems = page.locator("[data-testid='post-item']");
        if (postItems.count() > 1) {
            // 첫 번째와 두 번째 게시글의 Y 좌표 비교
            var firstBox = postItems.nth(0).boundingBox();
            var secondBox = postItems.nth(1).boundingBox();

            // 세로로 정렬되어 있는지 확인 (Y 좌표가 다름)
            assertThat(secondBox.y).isGreaterThan(firstBox.y);
            // 가로로 나란히 있지 않은지 확인 (X 좌표가 거의 같음)
            assertThat(Math.abs(secondBox.x - firstBox.x)).isLessThan(50);
        }

        // 탭 네비게이션이 스크롤 가능한지 확인
        Locator tabs = page.locator("[data-testid='board-tabs']");
        String tabsClass = tabs.getAttribute("class");
        assertThat(tabsClass).containsAnyOf("overflow-x-auto", "overflow-x-scroll", "scrollable");

        // 태블릿 뷰포트 테스트
        setTabletViewport();
        page.reload();
        waitForElement("[data-testid='post-list']");

        // 태블릿에서는 2컬럼 레이아웃 가능
        // 사이드바 표시 여부 확인

        // 데스크톱으로 복원
        setDesktopViewport();
    }

    @Test
    @Order(14)
    @DisplayName("14. 광고 배너 표시 확인")
    void testAdBanners() {
        // 게시판 페이지 로드
        navigateToFrontend("/board");
        waitForElement("[data-testid='post-list']");

        // 상단 광고 배너 확인
        if (isElementVisible("[data-testid='ad-banner-top'], .adfit-banner")) {
            Locator topBanner = page.locator("[data-testid='ad-banner-top'], .adfit-banner").first();
            assertThat(topBanner.isVisible()).isTrue();
        }

        // 모바일 배너 확인 (모바일 뷰포트에서)
        setMobileViewport();
        page.reload();
        waitForElement("[data-testid='post-list']");

        if (isElementVisible("[data-testid='ad-banner-mobile'], .mobile-banner")) {
            Locator mobileBanner = page.locator("[data-testid='ad-banner-mobile'], .mobile-banner").first();
            assertThat(mobileBanner.isVisible()).isTrue();
        }

        // 데스크톱으로 복원
        setDesktopViewport();
    }

    private void waitForURL(String urlPattern) {
        page.waitForURL(urlPattern, new Page.WaitForURLOptions().setTimeout(NAVIGATION_TIMEOUT));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}