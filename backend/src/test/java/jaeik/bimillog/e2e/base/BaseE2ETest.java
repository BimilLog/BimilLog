package jaeik.bimillog.e2e.base;

import com.microsoft.playwright.*;
import jaeik.bimillog.e2e.config.PlaywrightManager;
import jaeik.bimillog.e2e.config.ServerManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <h2>E2E 테스트 베이스 클래스</h2>
 * <p>모든 E2E 테스트가 상속받는 기본 클래스입니다.</p>
 * <p>서버 시작/종료, 브라우저 관리, 테스트 데이터 정리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(BaseE2ETest.E2ETestWatcher.class)
public abstract class BaseE2ETest {
    
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;
    
    protected static final String BASE_URL = "http://localhost:3000";
    protected static final String API_URL = "http://localhost:8080";
    protected static final int DEFAULT_TIMEOUT = 30000;
    
    /**
     * <h3>테스트 스위트 시작 전 초기화</h3>
     * <p>서버 시작 및 브라우저 초기화를 수행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @BeforeAll
    static void beforeAll() {
        System.out.println("=== E2E Test Suite Starting ===");
        
        if (!ServerManager.areServersRunning()) {
            ServerManager.startServers();
        }
        
        browser = PlaywrightManager.initialize();
    }
    
    /**
     * <h3>각 테스트 메소드 실행 전 설정</h3>
     * <p>새로운 브라우저 컨텍스트와 페이지를 생성합니다.</p>
     *
     * @param testInfo 테스트 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        String testName = testInfo.getDisplayName();
        System.out.println("\n>>> Starting test: " + testName);
        
        context = PlaywrightManager.createContext(testName);
        page = PlaywrightManager.createPage(context);
        
        page.setDefaultTimeout(DEFAULT_TIMEOUT);
        page.setDefaultNavigationTimeout(DEFAULT_TIMEOUT);
        
        setupNetworkInterceptors();
    }
    
    /**
     * <h3>각 테스트 메소드 실행 후 정리</h3>
     * <p>테스트 데이터 정리 및 컨텍스트 종료를 수행합니다.</p>
     *
     * @param testInfo 테스트 정보
     * @author Jaeik
     * @since 2.0.0
     */
    @AfterEach
    void afterEach(TestInfo testInfo) {
        String testName = testInfo.getDisplayName();
        
        try {
            cleanupTestData();
            
            if (testInfo.getExecutionException().isPresent()) {
                PlaywrightManager.captureScreenshot(page, testName, "failure");
            }
            
            PlaywrightManager.saveTrace(context, testName);
        } finally {
            if (page != null && !page.isClosed()) {
                page.close();
            }
            if (context != null) {
                context.close();
            }
            
            System.out.println("<<< Finished test: " + testName);
        }
    }
    
    /**
     * <h3>테스트 스위트 종료 후 정리</h3>
     * <p>브라우저와 서버를 종료합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    @AfterAll
    static void afterAll() {
        PlaywrightManager.cleanup();
        
        if (Boolean.parseBoolean(System.getProperty("e2e.stopServers", "true"))) {
            ServerManager.stopServers();
        }
        
        System.out.println("=== E2E Test Suite Completed ===");
    }
    
    /**
     * <h3>네트워크 인터셉터 설정</h3>
     * <p>API 요청 로깅 및 에러 감지를 설정합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    private void setupNetworkInterceptors() {
        page.onRequest(request -> {
            if (request.url().startsWith(API_URL)) {
                System.out.println("[API Request] " + request.method() + " " + request.url());
            }
        });
        
        page.onResponse(response -> {
            if (response.url().startsWith(API_URL) && response.status() >= 400) {
                System.err.println("[API Error] " + response.status() + " " + response.url());
            }
        });
    }
    
    /**
     * <h3>테스트 데이터 정리</h3>
     * <p>테스트로 생성된 데이터를 삭제합니다.</p>
     * <p>하위 클래스에서 오버라이드하여 구현합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    protected void cleanupTestData() {
        // 하위 클래스에서 구현
    }
    
    /**
     * <h3>로그인 헬퍼 메소드</h3>
     * <p>카카오 로그인을 수행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    protected void login() {
        page.navigate(BASE_URL);
        page.click("text=로그인");
        page.click("button:has-text('카카오 로그인')");
        page.waitForURL("**/auth/callback**", new Page.WaitForURLOptions().setTimeout(10000));
        page.waitForSelector("text=로그아웃", new Page.WaitForSelectorOptions().setTimeout(5000));
    }
    
    /**
     * <h3>로그아웃 헬퍼 메소드</h3>
     * <p>로그아웃을 수행합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    protected void logout() {
        page.click("text=로그아웃");
        page.waitForSelector("text=로그인", new Page.WaitForSelectorOptions().setTimeout(5000));
    }
    
    /**
     * <h3>요소 대기 헬퍼 메소드</h3>
     * <p>특정 요소가 나타날 때까지 대기합니다.</p>
     *
     * @param selector CSS 선택자
     * @param timeout 타임아웃 (밀리초)
     * @return 찾은 요소
     * @author Jaeik
     * @since 2.0.0
     */
    protected Locator waitForElement(String selector, int timeout) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeout));
        return page.locator(selector);
    }
    
    /**
     * <h3>텍스트 대기 헬퍼 메소드</h3>
     * <p>특정 텍스트가 나타날 때까지 대기합니다.</p>
     *
     * @param text 대기할 텍스트
     * @return 찾은 요소
     * @author Jaeik
     * @since 2.0.0
     */
    protected Locator waitForText(String text) {
        return waitForElement("text=" + text, DEFAULT_TIMEOUT);
    }
    
    /**
     * <h3>안전한 클릭 헬퍼 메소드</h3>
     * <p>요소가 클릭 가능한 상태가 될 때까지 대기 후 클릭합니다.</p>
     *
     * @param selector CSS 선택자
     * @author Jaeik
     * @since 2.0.0
     */
    protected void safeClick(String selector) {
        Locator element = page.locator(selector);
        element.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        element.click();
    }
    
    /**
     * <h3>안전한 입력 헬퍼 메소드</h3>
     * <p>입력 필드를 클리어하고 새 값을 입력합니다.</p>
     *
     * @param selector CSS 선택자
     * @param text 입력할 텍스트
     * @author Jaeik
     * @since 2.0.0
     */
    protected void safeFill(String selector, String text) {
        Locator element = page.locator(selector);
        element.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        element.clear();
        element.fill(text);
    }
    
    /**
     * <h3>타임스탬프 생성 헬퍼</h3>
     * <p>테스트 데이터에 사용할 고유한 타임스탬프를 생성합니다.</p>
     *
     * @return 타임스탬프 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    protected String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
    
    /**
     * <h3>테스트 데이터 생성 헬퍼</h3>
     * <p>테스트용 고유 데이터를 생성합니다.</p>
     *
     * @param prefix 데이터 접두사
     * @return 고유한 테스트 데이터
     * @author Jaeik
     * @since 2.0.0
     */
    protected String generateTestData(String prefix) {
        return prefix + "_" + generateTimestamp();
    }
    
    /**
     * <h3>API 호출 헬퍼 메소드</h3>
     * <p>Backend API를 직접 호출합니다.</p>
     *
     * @param endpoint API 엔드포인트
     * @param method HTTP 메소드
     * @param data 요청 데이터
     * @return API 응답
     * @author Jaeik
     * @since 2.0.0
     */
    protected APIResponse callAPI(String endpoint, String method, Object data) {
        APIRequestContext.NewRequestOptions options = new APIRequestContext.NewRequestOptions();
        
        if (data != null) {
            options.setData(data);
        }
        
        APIRequestContext apiContext = page.request();
        APIResponse response = null;
        
        switch (method.toUpperCase()) {
            case "GET":
                response = apiContext.get(API_URL + endpoint, options);
                break;
            case "POST":
                response = apiContext.post(API_URL + endpoint, options);
                break;
            case "PUT":
                response = apiContext.put(API_URL + endpoint, options);
                break;
            case "DELETE":
                response = apiContext.delete(API_URL + endpoint, options);
                break;
        }
        
        return response;
    }
    
    /**
     * <h2>E2E 테스트 감시자</h2>
     * <p>테스트 실행 결과를 모니터링하고 추가 작업을 수행합니다.</p>
     */
    static class E2ETestWatcher implements TestWatcher {
        
        @Override
        public void testSuccessful(ExtensionContext context) {
            System.out.println("✓ Test passed: " + context.getDisplayName());
        }
        
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            System.err.println("✗ Test failed: " + context.getDisplayName());
            System.err.println("Failure reason: " + cause.getMessage());
        }
        
        @Override
        public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
            System.out.println("⊘ Test disabled: " + context.getDisplayName());
        }
    }
}