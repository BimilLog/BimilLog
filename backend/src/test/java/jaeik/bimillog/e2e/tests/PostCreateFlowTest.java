package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 게시글 작성 전체 플로우 E2E 테스트
 *
 * 테스트 시나리오:
 * 1. 카카오 로그인
 * 2. 게시판 접근
 * 3. 게시글 작성
 * 4. 게시글 확인
 * 5. 댓글 작성
 * 6. 좋아요 클릭
 *
 * 모든 테스트는 비디오로 녹화됩니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostCreateFlowTest extends BaseE2ETest {

    private static final String TEST_TITLE = "E2E 테스트 게시글 - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private static final String TEST_CONTENT = "이것은 자동화된 E2E 테스트로 작성된 게시글입니다.\n\n테스트 내용:\n- 게시글 작성\n- 댓글 작성\n- 좋아요 기능";
    private static final String TEST_COMMENT = "자동화 테스트 댓글입니다.";

    private String createdPostUrl;

    @BeforeEach
    void setupVideoRecording(TestInfo testInfo) {
        // 각 테스트마다 비디오 녹화 설정
        String testName = testInfo.getTestMethod().get().getName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String videoFileName = String.format("%s_%s", testName, timestamp);

        // 브라우저 컨텍스트 재생성 with 비디오 녹화
        if (context != null) {
            context.close();
        }

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
            .setViewportSize(DESKTOP)
            .setIgnoreHTTPSErrors(true)
            .setLocale("ko-KR")
            .setRecordVideoDir(Paths.get("target/e2e-videos"))
            .setRecordVideoSize(new ViewportSize(1280, 720));

        context = browser.newContext(contextOptions);

        // 콘솔 로그 수집
        context.onConsoleMessage(msg -> {
            if (msg.type().equals("error")) {
                System.err.println("[Browser Console Error] " + msg.text());
            }
        });

        page = context.newPage();
        page.setDefaultTimeout(DEFAULT_TIMEOUT);

        // 네트워크 에러 감지
        page.onResponse(response -> {
            if (response.status() >= 400) {
                System.err.println(String.format("[Network Error] %d - %s",
                    response.status(), response.url()));
            }
        });

        System.out.println("🎥 Recording video: " + videoFileName);
    }

    @Test
    @Order(1)
    @DisplayName("1. 카카오 로그인 시뮬레이션")
    void testKakaoLogin() {
        System.out.println("📝 Starting Kakao login simulation...");

        // 프론트엔드 홈페이지 접속
        page.navigate(FRONTEND_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 스크린샷 캡처
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/01_homepage.png"))
            .setFullPage(true));

        // 로그인 버튼 클릭
        page.click("text=로그인");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 카카오 로그인 버튼 확인 및 클릭
        assertTrue(page.isVisible("text=카카오 로그인"), "카카오 로그인 버튼이 표시되어야 함");

        // 실제 카카오 OAuth는 테스트 환경에서 시뮬레이션
        // 테스트용 토큰 직접 설정 (실제 환경에서는 Mock 서버 사용)
        mockKakaoLogin();

        // 로그인 후 홈으로 리다이렉트 확인
        page.waitForURL(FRONTEND_URL, new Page.WaitForURLOptions().setTimeout(10000));

        // 로그인 성공 확인 (사용자 프로필 표시)
        assertTrue(page.isVisible("[data-testid='user-profile']"), "사용자 프로필이 표시되어야 함");

        System.out.println("✅ Login successful");
    }

    @Test
    @Order(2)
    @DisplayName("2. 게시판 페이지 접근 및 글쓰기 버튼 클릭")
    void testNavigateToBoard() {
        // 로그인 상태 유지
        mockKakaoLogin();

        System.out.println("📝 Navigating to board...");

        // 게시판 메뉴 클릭
        page.click("text=게시판");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // URL 확인
        assertTrue(page.url().contains("/board"), "게시판 페이지로 이동해야 함");

        // 게시글 목록 로딩 확인
        page.waitForSelector("[data-testid='post-list']",
            new Page.WaitForSelectorOptions().setTimeout(10000));

        // 스크린샷 캡처
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/02_board_list.png"))
            .setFullPage(true));

        // 글쓰기 버튼 확인 및 클릭
        Locator writeButton = page.locator("[data-testid='write-button'], button:has-text('글쓰기')");
        assertTrue(writeButton.isVisible(), "글쓰기 버튼이 표시되어야 함");

        writeButton.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 글쓰기 페이지 이동 확인
        assertTrue(page.url().contains("/write"), "글쓰기 페이지로 이동해야 함");

        System.out.println("✅ Navigated to write page");
    }

    @Test
    @Order(3)
    @DisplayName("3. 게시글 작성 및 발행")
    void testCreatePost() {
        // 로그인 상태 유지 및 글쓰기 페이지로 이동
        mockKakaoLogin();
        page.navigate(FRONTEND_URL + "/board/write");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        System.out.println("📝 Writing post...");

        // 제목 입력
        Locator titleInput = page.locator("input[placeholder*='제목']");
        titleInput.fill(TEST_TITLE);

        // Quill 에디터 로딩 대기
        page.waitForSelector(".ql-editor", new Page.WaitForSelectorOptions().setTimeout(10000));

        // 내용 입력 (Quill 에디터)
        Locator contentEditor = page.locator(".ql-editor");
        contentEditor.click();
        contentEditor.fill(TEST_CONTENT);

        // 미리보기 토글 테스트
        Locator previewToggle = page.locator("button:has-text('미리보기')");
        if (previewToggle.isVisible()) {
            previewToggle.click();
            page.waitForTimeout(1000);

            // 미리보기 스크린샷
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/03_post_preview.png")));

            previewToggle.click(); // 다시 편집 모드로
        }

        // 작성 전 스크린샷
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/04_post_writing.png")));

        // 게시하기 버튼 클릭
        Locator submitButton = page.locator("button:has-text('작성하기'), button:has-text('게시하기')");
        assertTrue(submitButton.isEnabled(), "게시하기 버튼이 활성화되어야 함");
        submitButton.click();

        // 게시글 상세 페이지로 리다이렉트 대기
        page.waitForURL("**/board/post/**", new Page.WaitForURLOptions().setTimeout(10000));

        // 작성된 게시글 URL 저장
        createdPostUrl = page.url();
        System.out.println("Created post URL: " + createdPostUrl);

        // 게시글 내용 확인
        assertTrue(page.textContent("h1, h2").contains(TEST_TITLE), "게시글 제목이 표시되어야 함");
        assertTrue(page.textContent(".post-content").contains(TEST_CONTENT.split("\n")[0]), "게시글 내용이 표시되어야 함");

        // 게시글 상세 스크린샷
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/05_post_detail.png"))
            .setFullPage(true));

        System.out.println("✅ Post created successfully");
    }

    @Test
    @Order(4)
    @DisplayName("4. 댓글 작성")
    void testCreateComment() {
        // 로그인 상태 유지
        mockKakaoLogin();

        // 작성된 게시글로 이동
        assertNotNull(createdPostUrl, "게시글 URL이 저장되어 있어야 함");
        page.navigate(createdPostUrl);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        System.out.println("📝 Writing comment...");

        // 댓글 섹션으로 스크롤
        page.evaluate("document.querySelector('[data-testid=\"comment-section\"]')?.scrollIntoView()");

        // 댓글 입력 필드 찾기
        Locator commentInput = page.locator("textarea[placeholder*='댓글']");
        assertTrue(commentInput.isVisible(), "댓글 입력 필드가 표시되어야 함");

        // 댓글 작성
        commentInput.fill(TEST_COMMENT);

        // 댓글 등록 버튼 클릭
        Locator commentSubmit = page.locator("button:has-text('등록'), button:has-text('작성')").last();
        commentSubmit.click();

        // 댓글 등록 완료 대기
        page.waitForTimeout(2000);

        // 댓글 표시 확인
        Locator commentList = page.locator("[data-testid='comment-list']");
        assertTrue(commentList.textContent().contains(TEST_COMMENT), "작성한 댓글이 표시되어야 함");

        // 댓글 스크린샷
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/06_comment_added.png")));

        System.out.println("✅ Comment created successfully");
    }

    @Test
    @Order(5)
    @DisplayName("5. 좋아요 기능 테스트")
    void testLikePost() {
        // 로그인 상태 유지
        mockKakaoLogin();

        // 작성된 게시글로 이동
        assertNotNull(createdPostUrl, "게시글 URL이 저장되어 있어야 함");
        page.navigate(createdPostUrl);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        System.out.println("📝 Testing like feature...");

        // 좋아요 버튼 찾기
        Locator likeButton = page.locator("[data-testid='like-button'], button:has-text('좋아요')").first();

        // 현재 좋아요 수 확인
        String likeCountText = page.locator("[data-testid='like-count']").textContent();
        int initialLikeCount = extractNumber(likeCountText);

        // 좋아요 클릭
        likeButton.click();
        page.waitForTimeout(1000);

        // 좋아요 수 증가 확인
        String newLikeCountText = page.locator("[data-testid='like-count']").textContent();
        int newLikeCount = extractNumber(newLikeCountText);

        assertTrue(newLikeCount > initialLikeCount, "좋아요 수가 증가해야 함");

        // 좋아요 취소 테스트
        likeButton.click();
        page.waitForTimeout(1000);

        String finalLikeCountText = page.locator("[data-testid='like-count']").textContent();
        int finalLikeCount = extractNumber(finalLikeCountText);

        assertEquals(initialLikeCount, finalLikeCount, "좋아요 취소 후 원래 수로 돌아와야 함");

        // 최종 스크린샷
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/07_like_tested.png")));

        System.out.println("✅ Like feature tested successfully");
    }

    @AfterEach
    void saveVideoAndCleanup(TestInfo testInfo) {
        // 비디오 저장
        if (page != null && page.video() != null) {
            String testName = testInfo.getTestMethod().get().getName();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String videoPath = String.format("target/e2e-videos/%s_%s.webm", testName, timestamp);

            page.video().saveAs(Paths.get(videoPath));
            page.video().delete();

            System.out.println("📹 Video saved: " + videoPath);
        }

        // 페이지 닫기
        if (page != null) {
            page.close();
        }
    }

    @AfterAll
    static void generateTestReport() {
        System.out.println("\n=== E2E Test Report ===");
        System.out.println("📹 Videos saved in: target/e2e-videos/");
        System.out.println("📸 Screenshots saved in: target/screenshots/");
        System.out.println("========================\n");
    }

    /**
     * 카카오 로그인 시뮬레이션 (테스트 환경용)
     */
    private void mockKakaoLogin() {
        // 실제 환경에서는 테스트용 OAuth 토큰을 사용하거나
        // Mock 서버를 통해 로그인을 시뮬레이션합니다.

        // localStorage에 테스트 토큰 설정
        page.evaluate("() => {" +
            "localStorage.setItem('access_token', 'test_jwt_token');" +
            "localStorage.setItem('user', JSON.stringify({" +
            "  id: 1," +
            "  userName: 'test_user'," +
            "  role: 'USER'" +
            "}));" +
        "}");

        // 홈페이지로 이동하여 로그인 상태 반영
        page.navigate(FRONTEND_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 문자열에서 숫자 추출
     */
    private int extractNumber(String text) {
        String numbers = text.replaceAll("[^0-9]", "");
        return numbers.isEmpty() ? 0 : Integer.parseInt(numbers);
    }
}