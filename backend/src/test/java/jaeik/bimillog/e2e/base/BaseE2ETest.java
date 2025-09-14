package jaeik.bimillog.e2e.base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * E2E 테스트 베이스 클래스
 * 모든 E2E 테스트가 상속받는 기본 클래스
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseE2ETest {

    @LocalServerPort
    protected int port;

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    protected String BASE_URL;
    protected static final String FRONTEND_URL = "http://localhost:3000";
    protected static final int DEFAULT_TIMEOUT = 30000;
    protected static final int NAVIGATION_TIMEOUT = 60000;

    // 뷰포트 사이즈 정의
    protected static final ViewportSize MOBILE = new ViewportSize(375, 812);
    protected static final ViewportSize TABLET = new ViewportSize(768, 1024);
    protected static final ViewportSize DESKTOP = new ViewportSize(1920, 1080);

    @BeforeAll
    static void launchBrowser() {
        System.out.println("=== E2E Test Suite Starting ===");

        playwright = Playwright.create();

        String browserType = System.getProperty("playwright.browser", "chromium");
        boolean headless = Boolean.parseBoolean(System.getProperty("playwright.headless", "true"));

        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(DESKTOP)
            .setIgnoreHTTPSErrors(true)
            .setLocale("ko-KR");

        switch (browserType.toLowerCase()) {
            case "firefox":
                browser = playwright.firefox().launch(
                    new BrowserType.LaunchOptions().setHeadless(headless)
                );
                break;
            case "webkit":
                browser = playwright.webkit().launch(
                    new BrowserType.LaunchOptions().setHeadless(headless)
                );
                break;
            default:
                browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(List.of(
                            "--disable-dev-shm-usage",
                            "--no-sandbox",
                            "--disable-setuid-sandbox"
                        ))
                );
        }
    }

    @BeforeEach
    void createContextAndPage(TestInfo testInfo) {
        BASE_URL = "http://localhost:" + port;

        String testName = testInfo.getDisplayName();
        System.out.println("\n>>> Starting test: " + testName);

        // 브라우저 컨텍스트 생성
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
            .setViewportSize(DESKTOP)
            .setIgnoreHTTPSErrors(true)
            .setLocale("ko-KR");

        // 비디오 녹화 설정 (디버깅용)
        if (Boolean.parseBoolean(System.getProperty("playwright.video", "false"))) {
            contextOptions.setRecordVideoDir(Paths.get("target/videos"));
        }

        context = browser.newContext(contextOptions);

        // 추적 시작 (디버깅용)
        if (Boolean.parseBoolean(System.getProperty("playwright.trace", "false"))) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        }

        page = context.newPage();
        page.setDefaultTimeout(DEFAULT_TIMEOUT);
        page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT);

        // 콘솔 메시지 로깅
        page.onConsoleMessage(msg -> {
            if (msg.type().equals("error")) {
                System.err.println("[Browser Console Error] " + msg.text());
            }
        });

        // 네트워크 요청 로깅
        setupNetworkLogging();
    }

    @AfterEach
    void cleanup(TestInfo testInfo) {
        String testName = testInfo.getDisplayName();

        // 실패 시 스크린샷 캡처
        if (testInfo.getTestMethod().isPresent()) {
            takeScreenshot("test_" + testName.replaceAll("[^a-zA-Z0-9]", "_"));
        }

        // 추적 저장
        if (Boolean.parseBoolean(System.getProperty("playwright.trace", "false"))) {
            context.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("target/traces/" + testName + ".zip")));
        }

        if (page != null) {
            page.close();
        }
        if (context != null) {
            context.close();
        }

        System.out.println("<<< Finished test: " + testName);
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        System.out.println("=== E2E Test Suite Completed ===");
    }

    private void setupNetworkLogging() {
        page.onRequest(request -> {
            if (request.url().contains("/api/")) {
                System.out.println(String.format("[API Request] %s %s",
                    request.method(), request.url()));
            }
        });

        page.onResponse(response -> {
            if (response.url().contains("/api/") && response.status() >= 400) {
                System.err.println(String.format("[API Error] %d %s",
                    response.status(), response.url()));
            }
        });
    }

    // ===== 헬퍼 메서드 =====

    protected void navigateToFrontend(String path) {
        page.navigate(FRONTEND_URL + path);
        page.waitForLoadState();
    }

    protected void navigateToBackend(String path) {
        page.navigate(BASE_URL + path);
        page.waitForLoadState();
    }

    protected void login(String username, String password) {
        navigateToFrontend("/login");

        // 카카오 로그인 버튼 클릭
        page.click("button:has-text('카카오 로그인')");

        // OAuth 콜백 대기
        page.waitForURL("**/callback**", new Page.WaitForURLOptions()
            .setTimeout(NAVIGATION_TIMEOUT));

        // 로그인 완료 확인
        waitForElement("[data-testid='user-menu']", 10000);
    }

    protected void logout() {
        safeClick("[data-testid='user-menu']");
        safeClick("text=로그아웃");
        page.waitForURL("**/login**");
    }

    protected Locator waitForElement(String selector, int timeout) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
            .setTimeout(timeout)
            .setState(WaitForSelectorState.VISIBLE));
        return page.locator(selector);
    }

    protected Locator waitForElement(String selector) {
        return waitForElement(selector, DEFAULT_TIMEOUT);
    }

    protected void safeClick(String selector) {
        Locator element = page.locator(selector);
        element.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));
        element.scrollIntoViewIfNeeded();
        element.click();
    }

    protected void safeClick(Locator element) {
        element.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));
        element.scrollIntoViewIfNeeded();
        element.click();
    }

    protected void safeFill(String selector, String text) {
        Locator element = page.locator(selector);
        element.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));
        element.clear();
        element.fill(text);
    }

    protected void safeType(String selector, String text) {
        Locator element = page.locator(selector);
        element.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));
        element.type(text);
    }

    protected void waitForText(String text) {
        page.waitForSelector("text=" + text, new Page.WaitForSelectorOptions()
            .setTimeout(DEFAULT_TIMEOUT));
    }

    protected boolean isElementVisible(String selector) {
        try {
            return page.locator(selector).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    protected void takeScreenshot(String name) {
        try {
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/" + name + ".png"))
                .setFullPage(true));
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }

    protected void setMobileViewport() {
        page.setViewportSize(MOBILE.width, MOBILE.height);
    }

    protected void setTabletViewport() {
        page.setViewportSize(TABLET.width, TABLET.height);
    }

    protected void setDesktopViewport() {
        page.setViewportSize(DESKTOP.width, DESKTOP.height);
    }

    protected String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    protected String generateTestData(String prefix) {
        return prefix + "_" + generateTimestamp();
    }

    protected void waitForApiResponse(String urlPattern) {
        page.waitForResponse(response ->
            response.url().contains(urlPattern) && response.status() == 200,
            () -> {}
        );
    }

    protected void scrollToBottom() {
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
    }

    protected void scrollToTop() {
        page.evaluate("window.scrollTo(0, 0)");
    }

    protected void waitForNetworkIdle() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    protected void acceptDialog() {
        page.onceDialog(dialog -> dialog.accept());
    }

    protected void dismissDialog() {
        page.onceDialog(dialog -> dialog.dismiss());
    }
}