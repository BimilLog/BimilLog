package jaeik.bimillog.e2e.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * E2E 테스트 설정 관리 클래스
 */
public class E2ETestConfig {

    // 서버 설정
    public static final String BACKEND_HOST = System.getProperty("e2e.backend.host", "localhost");
    public static final String FRONTEND_HOST = System.getProperty("e2e.frontend.host", "localhost");
    public static final int FRONTEND_PORT = Integer.parseInt(System.getProperty("e2e.frontend.port", "3000"));

    // 타임아웃 설정
    public static final int DEFAULT_TIMEOUT = 30000;
    public static final int LONG_TIMEOUT = 60000;
    public static final int SHORT_TIMEOUT = 5000;

    // 브라우저 설정
    public static final String BROWSER_TYPE = System.getProperty("e2e.browser", "chromium");
    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("e2e.headless", "true"));
    public static final boolean SLOW_MO = Boolean.parseBoolean(System.getProperty("e2e.slowmo", "false"));
    public static final int SLOW_MO_DELAY = Integer.parseInt(System.getProperty("e2e.slowmo.delay", "100"));

    // 디버깅 설정
    public static final boolean ENABLE_VIDEO = Boolean.parseBoolean(System.getProperty("e2e.video", "false"));
    public static final boolean ENABLE_TRACE = Boolean.parseBoolean(System.getProperty("e2e.trace", "false"));
    public static final boolean ENABLE_SCREENSHOT = Boolean.parseBoolean(System.getProperty("e2e.screenshot", "true"));
    public static final boolean VERBOSE_LOGGING = Boolean.parseBoolean(System.getProperty("e2e.verbose", "false"));

    // 테스트 데이터
    public static final String TEST_USER_PREFIX = "e2e_test_";
    public static final String TEST_POST_PREFIX = "E2E테스트_게시글_";
    public static final String TEST_COMMENT_PREFIX = "E2E테스트_댓글_";
    public static final String TEST_PAPER_PREFIX = "E2E테스트_롤링페이퍼_";

    // 환경별 URL 생성
    public static String getBackendUrl(int port) {
        return String.format("http://%s:%d", BACKEND_HOST, port);
    }

    public static String getFrontendUrl() {
        return String.format("http://%s:%d", FRONTEND_HOST, FRONTEND_PORT);
    }

    // 브라우저 옵션 생성
    public static BrowserType.LaunchOptions getLaunchOptions() {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
            .setHeadless(HEADLESS);

        if (SLOW_MO) {
            options.setSlowMo(SLOW_MO_DELAY);
        }

        // Chromium 전용 옵션
        if (BROWSER_TYPE.equals("chromium")) {
            options.setArgs(Arrays.asList(
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-web-security",
                "--disable-features=IsolateOrigins,site-per-process"
            ));
        }

        return options;
    }

    // 컨텍스트 옵션 생성
    public static Browser.NewContextOptions getContextOptions() {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setLocale("ko-KR")
            .setTimezoneId("Asia/Seoul")
            .setPermissions(Arrays.asList("geolocation", "notifications"));

        // User Agent 설정 (봇 감지 우회)
        options.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // 로컬 스토리지 초기화
        options.setStorageState("{\"cookies\":[],\"origins\":[]}");

        return options;
    }

    // 서버 상태 확인
    public static boolean waitForServer(String url, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200 || responseCode == 404) {
                    System.out.println("Server is ready: " + url);
                    return true;
                }
            } catch (Exception e) {
                System.out.println("Waiting for server: " + url + " (attempt " + (i + 1) + "/" + maxRetries + ")");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return false;
    }

    // 프론트엔드 서버 시작 확인
    public static void ensureFrontendReady() {
        String frontendUrl = getFrontendUrl();
        if (!waitForServer(frontendUrl, 30)) {
            throw new RuntimeException("Frontend server is not responding at " + frontendUrl);
        }
    }

    // 백엔드 서버 시작 확인
    public static void ensureBackendReady(int port) {
        String backendUrl = getBackendUrl(port) + "/actuator/health";
        if (!waitForServer(backendUrl, 30)) {
            throw new RuntimeException("Backend server is not responding at " + backendUrl);
        }
    }

    // 테스트 환경 정보 출력
    public static void printTestEnvironment(int backendPort) {
        System.out.println("========================================");
        System.out.println("E2E Test Environment Configuration:");
        System.out.println("----------------------------------------");
        System.out.println("Backend URL: " + getBackendUrl(backendPort));
        System.out.println("Frontend URL: " + getFrontendUrl());
        System.out.println("Browser: " + BROWSER_TYPE);
        System.out.println("Headless: " + HEADLESS);
        System.out.println("Video Recording: " + ENABLE_VIDEO);
        System.out.println("Trace Recording: " + ENABLE_TRACE);
        System.out.println("Screenshot on Failure: " + ENABLE_SCREENSHOT);
        System.out.println("========================================");
    }

    // 모바일 디바이스 에뮬레이션 설정
    public static class DeviceEmulation {
        public static final Device IPHONE_12 = new Device("iPhone 12", 390, 844, 3, true);
        public static final Device GALAXY_S20 = new Device("Galaxy S20", 412, 915, 3.5, true);
        public static final Device IPAD_MINI = new Device("iPad Mini", 768, 1024, 2, true);
        public static final Device DESKTOP_1080P = new Device("Desktop 1080p", 1920, 1080, 1, false);

        public static class Device {
            public final String name;
            public final int width;
            public final int height;
            public final double deviceScaleFactor;
            public final boolean isMobile;

            Device(String name, int width, int height, double deviceScaleFactor, boolean isMobile) {
                this.name = name;
                this.width = width;
                this.height = height;
                this.deviceScaleFactor = deviceScaleFactor;
                this.isMobile = isMobile;
            }

            public Browser.NewContextOptions applyTo(Browser.NewContextOptions options) {
                options.setViewportSize(width, height);
                options.setDeviceScaleFactor(deviceScaleFactor);
                options.setIsMobile(isMobile);
                options.setHasTouch(isMobile);
                return options;
            }
        }
    }

    // 네트워크 조건 시뮬레이션
    public static class NetworkCondition {
        public static final NetworkProfile FAST_3G = new NetworkProfile("Fast 3G", (int)(1.6 * 1024 * 1024 / 8), 750 * 1024 / 8, 40);
        public static final NetworkProfile SLOW_3G = new NetworkProfile("Slow 3G", 500 * 1024 / 8, 500 * 1024 / 8, 100);
        public static final NetworkProfile OFFLINE = new NetworkProfile("Offline", 0, 0, 0);

        public static class NetworkProfile {
            public final String name;
            public final int downloadThroughput;
            public final int uploadThroughput;
            public final int latency;

            NetworkProfile(String name, int downloadThroughput, int uploadThroughput, int latency) {
                this.name = name;
                this.downloadThroughput = downloadThroughput;
                this.uploadThroughput = uploadThroughput;
                this.latency = latency;
            }
        }
    }
}