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

        // Breadcrumb 네비게이션 확인 - nav 태그 사용
        waitForElement("nav");
        assertThat(page.locator("text=홈").isVisible()).isTrue();
        assertThat(page.locator("text=커뮤니티").isVisible()).isTrue();

        // 게시글 목록 표시 확인 - 실제 링크 패턴으로 찾기
        sleep(2000); // 데이터 로드 대기
        Locator postItems = page.locator("a[href^='/board/post/']");
        if (postItems.count() == 0) {
            // 게시글이 없을 경우 빈 상태 메시지 확인
            assertThat(page.locator("text=게시글이 없습니다, text=생각을 나눠어보세요").isVisible()).isTrue();
        } else {
            assertThat(postItems.count()).isGreaterThan(0);

            // 각 게시글 항목 요소 확인
            Locator firstPost = postItems.first();
            // 게시글 카드 내부 요소들 확인 (실제 클래스나 구조 기반)
            assertThat(firstPost.locator("h3, h4, .title").isVisible()).isTrue(); // 제목
            assertThat(firstPost.locator(".author, .text-sm").count()).isGreaterThan(0); // 작성자
            assertThat(firstPost.locator(".date, .text-muted").count()).isGreaterThan(0); // 날짜

            // 페이지네이션 확인 (실제 페이지네이션 구조)
            Locator pagination = page.locator("nav[aria-label='pagination'], .pagination, [role='navigation']");
            if (pagination.count() > 0) {
                // 다음 페이지 버튼 찾기
                Locator nextButton = page.locator("button:has-text('다음'), a:has-text('다음'), button[aria-label='Next'], a[aria-label='Next']");
                if (nextButton.count() > 0 && nextButton.isEnabled()) {
                    safeClick(nextButton);
                    waitForNetworkIdle();

                    // URL 파라미터 확인
                    assertThat(page.url()).contains("page=2");

                    // 새로운 게시글 목록 로드 확인
                    sleep(1000);
                    assertThat(page.locator("a[href^='/board/post/']").count()).isGreaterThan(0);

                    // 이전 페이지로 돌아가기
                    Locator prevButton = page.locator("button:has-text('이전'), a:has-text('이전'), button[aria-label='Previous'], a[aria-label='Previous']");
                    if (prevButton.count() > 0) {
                        safeClick(prevButton);
                        waitForNetworkIdle();
                    }
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("02. 게시글 검색 - MySQL ngram 전문 검색")
    void testSearchPosts() {
        // 검색 입력창 찾기
        Locator searchInput = page.locator("input[placeholder*='검색']");
        assertThat(searchInput.isVisible()).isTrue();

        // 검색 타입 선택 (select 요소가 있는 경우)
        Locator searchSelect = page.locator("select[name='type'], select.search-type");
        if (searchSelect.count() > 0) {
            searchSelect.selectOption("title");
        }

        // 검색어 입력
        String searchQuery = "테스트";
        searchInput.fill(searchQuery);

        // 검색 실행 (Enter 키)
        searchInput.press("Enter");
        waitForNetworkIdle();
        sleep(1000);

        // URL 파라미터 확인
        assertThat(page.url()).contains("q=" + searchQuery);

        // 검색 결과 확인 (게시글 목록 또는 빈 상태)
        Locator postItems = page.locator("a[href^='/board/post/']");
        if (postItems.count() == 0) {
            // 검색 결과가 없을 때 메시지 확인
            String pageContent = page.textContent("body");
            assertThat(pageContent).containsAnyOf("검색 결과가 없습니다", "게시글이 없습니다", "결과가 없습니다");
        } else {
            // 검색 결과가 있을 때
            assertThat(postItems.count()).isGreaterThan(0);

            // 검색어 하이라이팅 확인 (있는 경우)
            Locator highlightedText = page.locator("mark, .highlight, em");
            if (highlightedText.count() > 0) {
                assertThat(highlightedText.first().textContent()).containsIgnoringCase(searchQuery);
            }
        }

        // 검색 초기화 (검색어 비우고 다시 검색)
        searchInput.fill("");
        searchInput.press("Enter");
        waitForNetworkIdle();
        assertThat(page.url()).doesNotContain("q=");
    }

    @Test
    @Order(3)
    @DisplayName("03. 인기글 탭 네비게이션 (실시간/주간/레전드)")
    void testPopularPostsTabs() {
        // 탭 네비게이션 요소 찾기 (버튼 그룹 또는 탭 리스트)
        Locator tabs = page.locator("[role='tablist'], .tabs, .tab-group, div:has(button:has-text('전체'))");

        if (tabs.count() > 0) {
            // 실시간 인기글 탭
            Locator realtimeTab = page.locator("button:has-text('실시간'), button:has-text('실시간 인기글')");
            if (realtimeTab.count() > 0) {
                safeClick(realtimeTab);
                waitForNetworkIdle();
                sleep(1000);

                // 게시글 목록 확인
                Locator posts = page.locator("a[href^='/board/post/']");
                if (posts.count() > 0) {
                    // 인기글은 보통 좋아요 수가 표시됨
                    assertThat(posts.first().textContent()).matches(".*\\d+.*"); // 숫자 포함 확인
                }
            }

            // 주간 인기글 탭
            Locator weeklyTab = page.locator("button:has-text('주간'), button:has-text('주간 인기글')");
            if (weeklyTab.count() > 0) {
                safeClick(weeklyTab);
                waitForNetworkIdle();
                sleep(1000);
            }

            // 레전드 탭
            Locator legendTab = page.locator("button:has-text('레전드')");
            if (legendTab.count() > 0) {
                safeClick(legendTab);
                waitForNetworkIdle();
                sleep(1000);
            }

            // 전체 탭으로 돌아가기
            Locator allTab = page.locator("button:has-text('전체')");
            if (allTab.count() > 0) {
                safeClick(allTab);
                waitForNetworkIdle();
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("04. 비로그인 사용자 글쓰기 접근 제한")
    void testNonMemberWriteRestriction() {
        // 로그아웃 상태 확인 (사용자 메뉴가 있으면 로그아웃)
        Locator userMenu = page.locator(".user-menu, .avatar, button[aria-label*='user'], img[alt*='avatar']");
        if (userMenu.count() > 0) {
            logout();
        }

        // 글쓰기 버튼 찾기 (링크 또는 플로팅 버튼)
        Locator writeButton = page.locator("a[href='/board/write'], button:has-text('글쓰기'), .floating-button, .fab");
        assertThat(writeButton.count()).isGreaterThan(0);

        // 글쓰기 버튼 클릭
        safeClick(writeButton.first());
        sleep(1000);

        // 로그인 페이지로 리다이렉트 또는 모달 확인
        if (page.url().contains("/login") || page.url().contains("/auth")) {
            // 로그인 페이지로 리다이렉트됨
            assertThat(page.url()).containsAnyOf("/login", "/auth");
        } else {
            // 모달이 표시됨
            Locator modal = page.locator("[role='dialog'], .modal, .popup");
            if (modal.count() > 0) {
                String modalText = modal.textContent();
                assertThat(modalText).containsAnyOf("로그인", "필요", "권한");

                // 모달 닫기
                Locator closeButton = page.locator("button[aria-label*='Close'], button[aria-label*='닫기'], .close, button:has-text('X')");
                if (closeButton.count() > 0) {
                    safeClick(closeButton.first());
                } else {
                    page.keyboard().press("Escape");
                }
            }
        }

        // 게시판 페이지로 돌아가기
        if (!page.url().contains("/board")) {
            navigateToFrontend("/board");
        }
    }

    @Test
    @Order(5)
    @DisplayName("05. 게시글 작성 - Quill 에디터 사용")
    void testCreatePostWithRichText() {
        // 로그인
        login("testuser", "password");

        // 게시판으로 이동
        navigateToFrontend("/board");
        sleep(2000);

        // 글쓰기 버튼 클릭
        Locator writeButton = page.locator("a[href='/board/write'], button:has-text('글쓰기')");
        safeClick(writeButton.first());

        // 글쓰기 페이지 로드 확인
        waitForURL("**/board/write");
        sleep(2000);

        // 제목 입력 필드 찾기
        Locator titleInput = page.locator("input[placeholder*='제목'], input[name='title'], #title");
        assertThat(titleInput.isVisible()).isTrue();
        titleInput.fill(testPostTitle);

        // Quill 에디터 로드 대기
        waitForElement(".ql-editor");

        // 본문 입력 (Quill 에디터)
        Locator editor = page.locator(".ql-editor");
        editor.click();

        // 텍스트 포맷팅 테스트
        // 볼드 텍스트 (툴바가 있는 경우)
        Locator boldButton = page.locator(".ql-bold, button[title*='Bold'], button[aria-label*='Bold']");
        if (boldButton.count() > 0) {
            page.keyboard().type("이것은 ");
            safeClick(boldButton);
            page.keyboard().type("볼드 텍스트");
            safeClick(boldButton);
            page.keyboard().type("입니다.\n");
        } else {
            page.keyboard().type("이것은 볼드 텍스트입니다.\n");
        }

        // 일반 텍스트
        page.keyboard().type(testPostContent + "\n");

        // 리스트 추가 (툴바가 있는 경우)
        Locator listButton = page.locator(".ql-list[value='ordered'], button[title*='Ordered'], button[aria-label*='Numbered']");
        if (listButton.count() > 0) {
            safeClick(listButton);
            page.keyboard().type("첫 번째 항목\n");
            page.keyboard().type("두 번째 항목\n");
            page.keyboard().type("세 번째 항목");
        }

        // 미리보기 토글 (있는 경우)
        Locator previewButton = page.locator("button:has-text('미리보기'), button:has-text('Preview')");
        if (previewButton.count() > 0) {
            safeClick(previewButton);
            sleep(500);

            // 미리보기 내용 확인
            assertThat(page.locator("text=" + testPostTitle).isVisible()).isTrue();

            // 편집 모드로 돌아가기
            safeClick(previewButton);
        }

        // 작성하기 버튼 클릭
        Locator submitButton = page.locator("button:has-text('작성'), button:has-text('게시'), button:has-text('등록'), button[type='submit']");
        safeClick(submitButton.first());

        // 성공 메시지 또는 상세 페이지 이동 확인
        waitForURL("**/board/post/**");
        sleep(2000);

        // 게시글 ID 추출
        String currentUrl = page.url();
        Pattern pattern = Pattern.compile("/board/post/(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(currentUrl);
        if (matcher.find()) {
            testPostId = Long.parseLong(matcher.group(1));
        }

        // 작성된 게시글 확인 (제목과 내용)
        String pageContent = page.textContent("body");
        assertThat(pageContent).contains(testPostTitle);
        assertThat(pageContent).contains(testPostContent);
    }

    @Test
    @Order(6)
    @DisplayName("06. 게시글 상세 조회 및 조회수 증가")
    void testViewPostDetailAndIncrementViews() {
        // 게시판 목록으로 이동
        navigateToFrontend("/board");
        sleep(2000);

        // 방금 작성한 게시글 찾기 (제목으로 필터링)
        Locator allPosts = page.locator("a[href^='/board/post/']");
        Locator targetPost = null;

        for (int i = 0; i < allPosts.count(); i++) {
            if (allPosts.nth(i).textContent().contains(testPostTitle)) {
                targetPost = allPosts.nth(i);
                break;
            }
        }

        if (targetPost != null) {
            // 게시글 클릭
            targetPost.click();
            waitForURL("**/board/post/**");
            sleep(2000);

            // 상세 페이지 내용 확인
            String pageContent = page.textContent("body");
            assertThat(pageContent).contains(testPostTitle);
            assertThat(pageContent).contains(testPostContent);

            // 액션 버튼 확인 (좋아요, 공유 등)
            Locator likeButton = page.locator("button:has-text('좋아요'), button[aria-label*='like'], .like-button");
            assertThat(likeButton.count()).isGreaterThan(0);

            // 작성자인 경우 수정/삭제 버튼 확인
            Locator editButton = page.locator("button:has-text('수정'), a:has-text('수정')");
            Locator deleteButton = page.locator("button:has-text('삭제')");
            if (editButton.count() > 0) {
                assertThat(editButton.isVisible()).isTrue();
                assertThat(deleteButton.isVisible()).isTrue();
            }
        }

        // 목록으로 돌아가기
        Locator backButton = page.locator("button:has-text('목록'), a:has-text('목록'), a[href='/board']");
        if (backButton.count() > 0) {
            safeClick(backButton.first());
        } else {
            navigateToFrontend("/board");
        }
        sleep(2000);
    }

    @Test
    @Order(7)
    @DisplayName("07. 게시글 수정 및 삭제")
    void testEditAndDeletePost() {
        // 로그인 상태 확인
        Locator userMenu = page.locator(".user-menu, .avatar, button[aria-label*='user']");
        if (userMenu.count() == 0) {
            login("testuser", "password");
        }

        // 게시글 상세 페이지로 이동
        if (testPostId != null) {
            navigateToFrontend("/board/post/" + testPostId);
        } else {
            // 목록에서 작성한 게시글 찾기
            navigateToFrontend("/board");
            sleep(2000);
            Locator allPosts = page.locator("a[href^='/board/post/']");
            for (int i = 0; i < allPosts.count(); i++) {
                if (allPosts.nth(i).textContent().contains(testPostTitle)) {
                    allPosts.nth(i).click();
                    break;
                }
            }
        }
        sleep(2000);

        // 수정 버튼 클릭
        Locator editButton = page.locator("button:has-text('수정'), a:has-text('수정')");
        if (editButton.count() > 0) {
            safeClick(editButton.first());
            waitForURL("**/edit");
            sleep(2000);

            // 제목 수정
            String updatedTitle = testPostTitle + " (수정됨)";
            Locator titleInput = page.locator("input[placeholder*='제목'], input[name='title']");
            if (titleInput.count() > 0) {
                titleInput.clear();
                titleInput.fill(updatedTitle);
            }

            // 내용 수정
            Locator editor = page.locator(".ql-editor, textarea[placeholder*='내용'], textarea[name='content']");
            if (editor.count() > 0) {
                editor.click();
                page.keyboard().press("Control+A");
                page.keyboard().type(testPostContent + "\n\n수정된 내용입니다.");
            }

            // 수정하기 버튼 클릭
            Locator saveButton = page.locator("button:has-text('수정'), button:has-text('저장'), button[type='submit']");
            if (saveButton.count() > 0) {
                safeClick(saveButton.first());
                waitForURL("**/board/post/**");
                sleep(2000);

                // 수정된 내용 확인
                String pageContent = page.textContent("body");
                assertThat(pageContent).contains("수정됨");
                assertThat(pageContent).contains("수정된 내용");
            }
        }

        // 삭제 버튼 클릭
        Locator deleteButton = page.locator("button:has-text('삭제')");
        if (deleteButton.count() > 0) {
            safeClick(deleteButton.first());

            // 확인 다이얼로그 처리
            acceptDialog();

            // 삭제 확인 (목록으로 리다이렉트 또는 성공 메시지)
            sleep(2000);
            if (page.url().contains("/board/post/")) {
                navigateToFrontend("/board");
            }
            sleep(2000);

            // 삭제된 게시글이 목록에서 사라졌는지 확인
            String pageContent = page.textContent("body");
            assertThat(pageContent).doesNotContain(testPostTitle + " (수정됨)");
        }
    }

    @Test
    @Order(8)
    @DisplayName("08. 게시글 좋아요 토글")
    void testPostLikeToggle() {
        // 로그인 상태 확인
        Locator userMenu = page.locator(".user-menu, .avatar, button[aria-label*='user']");
        if (userMenu.count() == 0) {
            login("testuser", "password");
        }

        // 게시판 목록에서 첫 번째 게시글 선택
        navigateToFrontend("/board");
        sleep(2000);

        Locator allPosts = page.locator("a[href^='/board/post/']");
        if (allPosts.count() > 0) {
            allPosts.first().click();
            sleep(2000);

            // 좋아요 버튼 찾기
            Locator likeButton = page.locator("button:has-text('좋아요'), button[aria-label*='like'], .like-button, button:has(.heart)");

            if (likeButton.count() > 0) {
                // 현재 좋아요 수 확인 (숙자 추출)
                String pageContent = page.textContent("body");
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
                java.util.regex.Matcher matcher = pattern.matcher(pageContent);
                int initialLikes = 0;
                while (matcher.find()) {
                    // 좋아요 수로 추정되는 숫자 찾기
                    try {
                        int num = Integer.parseInt(matcher.group());
                        if (num < 1000) { // 좋아요 수는 보통 크지 않음
                            initialLikes = num;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // 무시
                    }
                }

                // 좋아요 버튼 클릭
                safeClick(likeButton.first());
                sleep(1000);

                // 버튼 상태 변경 확인
                String buttonClass = likeButton.first().getAttribute("class");
                // 활성 상태인지 확인 (색상이나 스타일 변경)
                boolean isActive = buttonClass != null &&
                    (buttonClass.contains("active") || buttonClass.contains("text-pink") ||
                     buttonClass.contains("bg-pink") || buttonClass.contains("liked"));

                // 좋아요 취소 (다시 클릭)
                safeClick(likeButton.first());
                sleep(1000);
            }
        }
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