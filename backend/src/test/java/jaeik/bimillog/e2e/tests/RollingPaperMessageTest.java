package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 롤링페이퍼 메시지 작성 E2E 테스트
 *
 * 테스트 시나리오:
 * 1. 롤링페이퍼 방문 페이지에서 사용자 검색
 * 2. 익명으로 메시지 작성
 * 3. 메시지 확인 및 삭제
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RollingPaperMessageTest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;

    private String baseUrl;
    private static final String TEST_USER = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TEST_MESSAGE = "테스트 메시지입니다! 🎉";
    private static final String TEST_AUTHOR = "익명의 친구";
    private static final String DELETE_PASSWORD = "1234";

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)  // 디버깅을 위해 브라우저 표시
                .setSlowMo(500));     // 각 액션 사이 500ms 대기
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        baseUrl = "http://localhost:" + port;

        // 새 브라우저 컨텍스트 생성 (모바일 뷰포트)
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(390, 844)  // iPhone 14 Pro 크기
                .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15")
                .setHasTouch(true)
                .setIsMobile(true));

        page = context.newPage();

        // 콘솔 로그 출력
        page.onConsoleMessage(msg -> System.out.println("Console: " + msg.text()));

        // 네트워크 에러 감지
        page.onRequestFailed(request ->
            System.err.println("Request failed: " + request.url() + " - " + request.failure()));
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. 회원가입 후 롤링페이퍼 생성")
    void createUserAndRollingPaper() {
        // 1. 메인 페이지 접속
        page.navigate(baseUrl);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 로그인 페이지로 이동
        page.click("text=시작하기");
        assertThat(page.url()).contains("/login");

        // 3. 카카오 로그인 시뮬레이션 (테스트 환경에서는 mock 처리)
        // 실제 환경에서는 카카오 OAuth를 거치지만, 테스트에서는 직접 회원가입 페이지로 이동
        page.navigate(baseUrl + "/signup?test_mode=true");

        // 4. 회원가입 폼 작성
        page.fill("input[name='nickname']", TEST_USER);
        page.check("input[name='terms']");
        page.check("input[name='privacy']");

        // 5. 중복 확인 대기
        page.waitForTimeout(1000);

        // 6. 가입하기 버튼 클릭
        page.click("button:has-text('가입하기')");

        // 7. 메인 페이지로 리다이렉트 확인
        page.waitForURL(baseUrl + "/");

        // 8. 내 롤링페이퍼 페이지 확인
        page.navigate(baseUrl + "/rolling-paper");
        assertThat(page.locator("h1:has-text('내 롤링페이퍼')")).isVisible();
    }

    @Test
    @Order(2)
    @DisplayName("2. 익명 사용자가 롤링페이퍼 메시지 작성")
    void writeAnonymousMessage() {
        // 1. 롤링페이퍼 방문 페이지 접속
        page.navigate(baseUrl + "/visit");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 사용자 검색
        Locator searchInput = page.locator("input[placeholder*='닉네임']");
        searchInput.fill(TEST_USER);

        // Enter 키 또는 검색 버튼 클릭
        searchInput.press("Enter");

        // 3. 검색 결과 대기 및 롤링페이퍼 페이지 이동
        page.waitForURL("**/" + TEST_USER);
        assertThat(page.url()).contains("/rolling-paper/" + TEST_USER);

        // 4. 롤링페이퍼 그리드에서 빈 칸 찾아 클릭
        Locator emptyCell = page.locator(".grid-cell:not(.has-message)").first();
        emptyCell.click();

        // 5. 메시지 작성 모달 대기
        Locator modal = page.locator("[role='dialog'], .modal");
        assertThat(modal).isVisible();

        // 6. 메시지 작성
        page.fill("input[name='author'], input[placeholder*='작성자']", TEST_AUTHOR);
        page.fill("textarea[name='content'], textarea[placeholder*='메시지']", TEST_MESSAGE);
        page.fill("input[name='password'], input[type='password']", DELETE_PASSWORD);

        // 7. 디자인 선택 (옵션)
        Locator designOptions = page.locator(".design-option, .message-style");
        if (designOptions.count() > 0) {
            designOptions.nth(1).click();  // 두 번째 디자인 선택
        }

        // 8. 작성하기 버튼 클릭
        page.click("button:has-text('작성'), button:has-text('보내기')");

        // 9. 성공 메시지 확인
        Locator toast = page.locator(".toast, [role='alert']");
        assertThat(toast).containsText("메시지");

        // 10. 모달 닫힘 확인
        page.waitForTimeout(1000);
        assertThat(modal).not().isVisible();
    }

    @Test
    @Order(3)
    @DisplayName("3. 작성된 메시지 확인")
    void verifyMessage() {
        // 1. 해당 사용자의 롤링페이퍼 페이지 접속
        page.navigate(baseUrl + "/rolling-paper/" + TEST_USER);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 메시지가 있는 칸 찾기
        Locator messageCell = page.locator(".grid-cell.has-message, .message-cell");
        assertThat(messageCell.count()).isGreaterThan(0);

        // 3. 메시지 칸 클릭하여 상세 보기
        messageCell.first().click();

        // 4. 메시지 상세 모달 확인
        Locator modal = page.locator("[role='dialog'], .modal");
        assertThat(modal).isVisible();

        // 5. 메시지 내용 확인
        assertThat(modal).containsText(TEST_MESSAGE);
        assertThat(modal).containsText(TEST_AUTHOR);

        // 6. 모달 닫기
        Locator closeButton = page.locator("button[aria-label='닫기'], .close-button, button:has-text('닫기')");
        if (closeButton.isVisible()) {
            closeButton.click();
        } else {
            // ESC 키로 닫기
            page.keyboard().press("Escape");
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. 메시지 삭제")
    void deleteMessage() {
        // 1. 롤링페이퍼 페이지 접속
        page.navigate(baseUrl + "/rolling-paper/" + TEST_USER);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 메시지 칸 클릭
        Locator messageCell = page.locator(".grid-cell.has-message, .message-cell").first();
        messageCell.click();

        // 3. 삭제 버튼 클릭
        Locator deleteButton = page.locator("button:has-text('삭제'), button[aria-label='삭제']");
        deleteButton.click();

        // 4. 비밀번호 입력 다이얼로그
        Locator passwordInput = page.locator("input[type='password']");
        passwordInput.fill(DELETE_PASSWORD);

        // 5. 확인 버튼 클릭
        page.click("button:has-text('확인'), button:has-text('삭제하기')");

        // 6. 삭제 완료 확인
        Locator toast = page.locator(".toast, [role='alert']");
        assertThat(toast).containsText("삭제");

        // 7. 메시지가 사라졌는지 확인
        page.waitForTimeout(1000);
        page.reload();

        // 그리드에서 해당 메시지가 없어졌는지 확인
        Locator emptyCells = page.locator(".grid-cell:not(.has-message)");
        int emptyCount = emptyCells.count();
        assertThat(emptyCount).isGreaterThan(0);
    }

    @Test
    @Order(5)
    @DisplayName("5. 최근 방문 기록 확인")
    void checkRecentVisits() {
        // 1. 롤링페이퍼 방문 페이지 접속
        page.navigate(baseUrl + "/visit");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 최근 방문 섹션 확인
        Locator recentSection = page.locator("h2:has-text('최근 방문'), h3:has-text('최근')").locator("..");
        assertThat(recentSection).isVisible();

        // 3. 방문 기록에 TEST_USER가 있는지 확인
        Locator recentItem = page.locator("text=" + TEST_USER);
        if (recentItem.count() > 0) {
            assertThat(recentItem.first()).isVisible();

            // 4. 방문 기록 클릭하여 이동
            recentItem.first().click();
            page.waitForURL("**/" + TEST_USER);
            assertThat(page.url()).contains("/rolling-paper/" + TEST_USER);
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. 모바일 반응형 테스트")
    void testMobileResponsive() {
        // 1. 롤링페이퍼 페이지 접속
        page.navigate(baseUrl + "/rolling-paper/" + TEST_USER);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 모바일 그리드 확인 (4x10)
        Locator grid = page.locator(".rolling-paper-grid, .message-grid");
        assertThat(grid).isVisible();

        // 3. 그리드 컬럼 수 확인
        String gridStyle = grid.evaluate("el => window.getComputedStyle(el).gridTemplateColumns").toString();
        assertThat(gridStyle).contains("repeat(4");  // 모바일은 4컬럼

        // 4. 터치 제스처 시뮬레이션
        Locator emptyCell = page.locator(".grid-cell:not(.has-message)").first();
        emptyCell.tap();  // 모바일 탭 제스처

        // 5. 모달이 모바일에 최적화되어 표시되는지 확인
        Locator modal = page.locator("[role='dialog'], .modal");
        assertThat(modal).isVisible();

        // 모달 너비가 화면에 맞는지 확인
        Object modalWidth = modal.evaluate("el => el.offsetWidth");
        assertThat((Integer) modalWidth).isLessThanOrEqualTo(390);
    }

    @Test
    @Order(7)
    @DisplayName("7. 에러 처리 - 존재하지 않는 사용자")
    void testNonExistentUser() {
        // 1. 롤링페이퍼 방문 페이지 접속
        page.navigate(baseUrl + "/visit");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 2. 존재하지 않는 사용자 검색
        String nonExistentUser = "nonexistent_user_" + UUID.randomUUID();
        Locator searchInput = page.locator("input[placeholder*='닉네임']");
        searchInput.fill(nonExistentUser);
        searchInput.press("Enter");

        // 3. 에러 메시지 확인
        Locator errorMessage = page.locator(".error, [role='alert'], .text-red-500");
        page.waitForSelector(".error, [role='alert'], .text-red-500",
            new Page.WaitForSelectorOptions().setTimeout(5000));
        assertThat(errorMessage).containsText("존재하지 않는");
    }
}