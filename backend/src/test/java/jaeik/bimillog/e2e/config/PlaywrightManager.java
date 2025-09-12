package jaeik.bimillog.e2e.config;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RecordVideoSize;
import com.microsoft.playwright.options.ViewportSize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * <h2>Playwright 브라우저 관리자</h2>
 * <p>E2E 테스트를 위한 브라우저 인스턴스를 관리합니다.</p>
 * <p>브라우저 생성, 컨텍스트 설정, 스크린샷 및 비디오 녹화 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class PlaywrightManager {
    private static Playwright playwright;
    private static Browser browser;
    private static final String SCREENSHOTS_DIR = "test-results/screenshots";
    private static final String VIDEOS_DIR = "test-results/videos";
    private static final String TRACES_DIR = "test-results/traces";

    /**
     * <h3>Playwright 초기화</h3>
     * <p>Playwright 인스턴스를 생성하고 브라우저를 시작합니다.</p>
     * <p>시스템 프로퍼티 playwright.headless로 헤드리스 모드를 제어합니다.</p>
     *
     * @return 초기화된 Browser 인스턴스
     * @author Jaeik
     * @since 2.0.0
     */
    public static Browser initialize() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
        
        if (browser == null || !browser.isConnected()) {
            boolean headless = !System.getProperty("playwright.headless", "true").equals("false");
            int slowMo = headless ? 0 : 500;
            
            browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setSlowMo(slowMo)
                    .setArgs(Arrays.asList(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-web-security",
                        "--disable-features=IsolateOrigins,site-per-process"
                    ))
            );
            
            System.out.println("Browser launched in " + (headless ? "headless" : "headful") + " mode");
        }
        
        return browser;
    }

    /**
     * <h3>새 브라우저 컨텍스트 생성</h3>
     * <p>테스트용 브라우저 컨텍스트를 생성합니다.</p>
     * <p>비디오 녹화, 뷰포트 크기, 로케일 등을 설정합니다.</p>
     *
     * @param testName 테스트 이름 (비디오 파일명에 사용)
     * @return 설정된 BrowserContext
     * @author Jaeik
     * @since 2.0.0
     */
    public static BrowserContext createContext(String testName) {
        Browser browser = initialize();
        
        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(new ViewportSize(1920, 1080))
            .setLocale("ko-KR")
            .setTimezoneId("Asia/Seoul")
            .setIgnoreHTTPSErrors(true)
            .setAcceptDownloads(true);
        
        boolean recordVideo = Boolean.parseBoolean(System.getProperty("playwright.video", "true"));
        if (recordVideo) {
            Path videoPath = Paths.get(VIDEOS_DIR);
            options.setRecordVideoDir(videoPath)
                   .setRecordVideoSize(new RecordVideoSize(1920, 1080));
        }
        
        BrowserContext context = browser.newContext(options);
        
        boolean enableTracing = Boolean.parseBoolean(System.getProperty("playwright.trace", "false"));
        if (enableTracing) {
            context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        }
        
        return context;
    }

    /**
     * <h3>새 페이지 생성</h3>
     * <p>브라우저 컨텍스트에서 새 페이지를 생성합니다.</p>
     * <p>콘솔 메시지와 에러를 자동으로 로깅합니다.</p>
     *
     * @param context 브라우저 컨텍스트
     * @return 설정된 Page 인스턴스
     * @author Jaeik
     * @since 2.0.0
     */
    public static Page createPage(BrowserContext context) {
        Page page = context.newPage();
        
        page.onConsoleMessage(msg -> {
            String type = msg.type();
            if ("error".equals(type)) {
                System.err.println("[Browser Console ERROR] " + msg.text());
            } else if ("warning".equals(type)) {
                System.out.println("[Browser Console WARN] " + msg.text());
            } else if (Boolean.parseBoolean(System.getProperty("playwright.debug", "false"))) {
                System.out.println("[Browser Console " + type.toUpperCase() + "] " + msg.text());
            }
        });
        
        page.onPageError(error -> {
            System.err.println("[Page Error] " + error);
        });
        
        page.onRequestFailed(request -> {
            System.err.println("[Request Failed] " + request.url() + " - " + request.failure());
        });
        
        return page;
    }

    /**
     * <h3>스크린샷 캡처</h3>
     * <p>현재 페이지의 스크린샷을 저장합니다.</p>
     * <p>테스트 이름과 타임스탬프를 파일명에 포함합니다.</p>
     *
     * @param page 캡처할 페이지
     * @param testName 테스트 이름
     * @param suffix 파일명 접미사 (예: "before", "after", "error")
     * @return 저장된 스크린샷 파일 경로
     * @author Jaeik
     * @since 2.0.0
     */
    public static Path captureScreenshot(Page page, String testName, String suffix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s_%s.png", testName, suffix, timestamp);
        Path screenshotPath = Paths.get(SCREENSHOTS_DIR, fileName);
        
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(screenshotPath)
            .setFullPage(true));
        
        System.out.println("Screenshot saved: " + screenshotPath);
        return screenshotPath;
    }

    /**
     * <h3>트레이스 저장</h3>
     * <p>디버깅을 위한 트레이스 파일을 저장합니다.</p>
     * <p>트레이스 파일은 Playwright Trace Viewer로 확인할 수 있습니다.</p>
     *
     * @param context 트레이싱이 활성화된 컨텍스트
     * @param testName 테스트 이름
     * @author Jaeik
     * @since 2.0.0
     */
    public static void saveTrace(BrowserContext context, String testName) {
        if (Boolean.parseBoolean(System.getProperty("playwright.trace", "false"))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("%s_%s.zip", testName, timestamp);
            Path tracePath = Paths.get(TRACES_DIR, fileName);
            
            context.tracing().stop(new Tracing.StopOptions()
                .setPath(tracePath));
            
            System.out.println("Trace saved: " + tracePath);
            System.out.println("View with: npx playwright show-trace " + tracePath);
        }
    }

    /**
     * <h3>브라우저 종료</h3>
     * <p>브라우저와 Playwright 인스턴스를 종료합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public static void cleanup() {
        if (browser != null && browser.isConnected()) {
            browser.close();
            browser = null;
        }
        
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }

    /**
     * <h3>모바일 뷰포트 설정</h3>
     * <p>모바일 디바이스 에뮬레이션을 위한 컨텍스트를 생성합니다.</p>
     *
     * @param deviceName 디바이스 이름 ("iPhone 12", "Pixel 5" 등)
     * @return 모바일 설정이 적용된 BrowserContext
     * @author Jaeik
     * @since 2.0.0
     */
    public static BrowserContext createMobileContext(String deviceName) {
        Browser browser = initialize();
        
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
        
        Browser.NewContextOptions contextOptions = switch (deviceName.toLowerCase()) {
            case "iphone 12" -> new Browser.NewContextOptions()
                    .setViewportSize(new ViewportSize(390, 844))
                    .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15")
                    .setDeviceScaleFactor(3)
                    .setIsMobile(true)
                    .setHasTouch(true);
            case "pixel 5" -> new Browser.NewContextOptions()
                    .setViewportSize(new ViewportSize(393, 851))
                    .setUserAgent("Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36")
                    .setDeviceScaleFactor(2.625)
                    .setIsMobile(true)
                    .setHasTouch(true);
            default -> new Browser.NewContextOptions()
                    .setViewportSize(new ViewportSize(375, 667))
                    .setIsMobile(true)
                    .setHasTouch(true);
        };

        return browser.newContext(contextOptions);
    }

    /**
     * <h3>네트워크 속도 제한</h3>
     * <p>느린 네트워크 환경을 시뮬레이션합니다.</p>
     *
     * @param page 설정할 페이지
     * @param profile 네트워크 프로파일 ("3G", "4G", "WiFi")
     * @author Jaeik
     * @since 2.0.0
     */
    public static void setNetworkCondition(Page page, String profile) {
        switch (profile.toUpperCase()) {
            case "OFFLINE":
                page.context().setOffline(true);
                break;
            case "4G", "3G":
            default:
                page.context().setOffline(false);
        }
    }
}