package jaeik.bimillog.e2e.pages.auth;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 카카오 OAuth 콜백 페이지 Page Object
 *
 * 시나리오 2: 콜백 페이지 테스트
 * - 카카오 OAuth 리다이렉트 처리
 * - 로딩 스피너 표시
 * - 인증 성공/실패에 따른 리다이렉트
 * - 모바일/태블릿 FCM 토큰 처리
 */
public class CallbackPage {
    private final Page page;
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final int OAUTH_TIMEOUT = 60000; // OAuth 처리는 더 긴 타임아웃

    // 페이지 요소 선택자
    private static final String LOADING_SPINNER = "div[class*='animate-spin']";
    private static final String LOADING_MESSAGE = "text=로그인 처리 중...";
    private static final String MOBILE_MESSAGE = "text=모바일 알림 설정 중...";
    private static final String ERROR_MESSAGE = "text=로그인 오류";
    private static final String AUTH_LOADING_SCREEN = "div[class*='AuthLoadingScreen']";

    public CallbackPage(Page page) {
        this.page = page;
    }

    /**
     * 콜백 페이지로 직접 이동 (테스트용)
     */
    public void navigateWithCode(String code) {
        page.navigate("http://localhost:3000/callback?code=" + code);
        waitForCallbackProcessing();
    }

    /**
     * 에러와 함께 콜백 페이지로 이동
     */
    public void navigateWithError(String error) {
        page.navigate("http://localhost:3000/callback?error=" + error);
    }

    /**
     * 콜백 처리 대기
     */
    public void waitForCallbackProcessing() {
        try {
            // 로딩 스피너가 나타나기를 기다림
            page.waitForSelector(LOADING_SPINNER, new Page.WaitForSelectorOptions()
                .setTimeout(5000)
                .setState(WaitForSelectorState.VISIBLE));
        } catch (Exception e) {
            // 로딩 스피너가 너무 빨리 사라질 수 있음
        }
    }

    /**
     * 로딩 스피너 표시 확인
     */
    public boolean hasLoadingSpinner() {
        return page.locator(LOADING_SPINNER).isVisible();
    }

    /**
     * 로딩 메시지 확인
     */
    public boolean hasLoadingMessage() {
        return page.locator(LOADING_MESSAGE).isVisible();
    }

    /**
     * 모바일 알림 설정 메시지 확인 (모바일/태블릿)
     */
    public boolean hasMobileMessage() {
        return page.locator(MOBILE_MESSAGE).isVisible();
    }

    /**
     * 로그인 성공 후 홈으로 리다이렉트 확인
     */
    public boolean isRedirectedToHome() {
        try {
            page.waitForURL("http://localhost:3000/", new Page.WaitForURLOptions()
                .setTimeout(OAUTH_TIMEOUT));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 신규 사용자 - 회원가입 페이지로 리다이렉트 확인
     */
    public boolean isRedirectedToSignup() {
        try {
            page.waitForURL("**/signup?required=true**", new Page.WaitForURLOptions()
                .setTimeout(OAUTH_TIMEOUT));
            return page.url().contains("signup") && page.url().contains("required=true");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 에러 발생 시 로그인 페이지로 리다이렉트 확인
     */
    public boolean isRedirectedToLoginWithError() {
        try {
            page.waitForURL("**/login?error=**", new Page.WaitForURLOptions()
                .setTimeout(OAUTH_TIMEOUT));
            return page.url().contains("login") && page.url().contains("error=");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * URL에서 UUID 추출 (신규 사용자용)
     */
    public String getUuidFromUrl() {
        String url = page.url();
        if (url.contains("uuid=")) {
            String[] parts = url.split("uuid=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        }
        return null;
    }

    /**
     * URL에서 에러 메시지 추출
     */
    public String getErrorFromUrl() {
        String url = page.url();
        if (url.contains("error=")) {
            String[] parts = url.split("error=");
            if (parts.length > 1) {
                String error = parts[1].split("&")[0];
                try {
                    return java.net.URLDecoder.decode(error, "UTF-8");
                } catch (Exception e) {
                    return error;
                }
            }
        }
        return null;
    }

    /**
     * 네트워크 에러 시뮬레이션
     */
    public void simulateNetworkError() {
        page.route("**/api/auth/kakao/callback**", route -> {
            route.abort();
        });
    }

    /**
     * OAuth 성공 응답 시뮬레이션
     */
    public void simulateSuccessResponse() {
        page.route("**/api/auth/kakao/callback**", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("{\"success\":true,\"data\":{\"status\":\"EXISTING_USER\"}}"));
        });
    }

    /**
     * OAuth 신규 사용자 응답 시뮬레이션
     */
    public void simulateNewUserResponse(String uuid) {
        page.route("**/api/auth/kakao/callback**", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(200)
                .setContentType("application/json")
                .setBody("{\"success\":true,\"data\":{\"status\":\"NEW_USER\",\"uuid\":\"" + uuid + "\"}}"));
        });
    }

    /**
     * OAuth 실패 응답 시뮬레이션
     */
    public void simulateFailureResponse(String errorMessage) {
        page.route("**/api/auth/kakao/callback**", route -> {
            route.fulfill(new Route.FulfillOptions()
                .setStatus(400)
                .setContentType("application/json")
                .setBody("{\"success\":false,\"error\":\"" + errorMessage + "\"}"));
        });
    }

    /**
     * 페이지 로드 상태 확인
     */
    public boolean isLoading() {
        return page.locator(LOADING_SPINNER).isVisible() ||
               page.locator(LOADING_MESSAGE).isVisible();
    }

    /**
     * 모바일 뷰포트 설정
     */
    public void setMobileViewport() {
        page.setViewportSize(375, 812);
    }

    /**
     * 태블릿 뷰포트 설정
     */
    public void setTabletViewport() {
        page.setViewportSize(768, 1024);
    }

    /**
     * 데스크톱 뷰포트 설정
     */
    public void setDesktopViewport() {
        page.setViewportSize(1920, 1080);
    }

    /**
     * 콘솔 에러 확인
     */
    public void checkConsoleErrors() {
        page.onConsoleMessage(msg -> {
            if (msg.type().equals("error")) {
                System.err.println("[Console Error] " + msg.text());
            }
        });
    }

    /**
     * 네트워크 요청 로깅
     */
    public void logNetworkRequests() {
        page.onRequest(request -> {
            if (request.url().contains("/api/auth")) {
                System.out.println("[Auth Request] " + request.method() + " " + request.url());
            }
        });

        page.onResponse(response -> {
            if (response.url().contains("/api/auth")) {
                System.out.println("[Auth Response] " + response.status() + " " + response.url());
            }
        });
    }

    /**
     * 페이지 스크린샷 캡처
     */
    public void takeScreenshot(String filename) {
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get("target/screenshots/" + filename + ".png"))
            .setFullPage(true));
    }

    /**
     * 타임아웃까지 대기
     */
    public void waitForTimeout(int milliseconds) {
        page.waitForTimeout(milliseconds);
    }
}