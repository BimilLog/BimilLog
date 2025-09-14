package jaeik.bimillog.e2e.utils;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

/**
 * 인증 헬퍼 유틸리티
 * 
 * E2E 테스트에서 사용자 인증을 시뮬레이션하는 유틸리티 클래스입니다.
 * - 카카오 OAuth 로그인 시뮬레이션
 * - JWT 토큰 설정
 * - 로그인/로그아웃 처리
 * - 사용자 상태 확인
 */
public class AuthHelper {

    private final Page page;
    private final String frontendUrl;
    
    // 테스트용 사용자 정보
    public static final String TEST_USER_NAME = "테스트사용자";
    public static final String TEST_USER_ID = "1";
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String TEST_USER_THUMBNAIL = "https://via.placeholder.com/150";
    public static final String TEST_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwibmFtZSI6InRlc3R1c2VyIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE1MTYyMzkwMjJ9.test_signature";
    public static final String TEST_ADMIN_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwibmFtZSI6ImFkbWluIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNTE2MjM5MDIyfQ.admin_signature";
    
    // localStorage 키
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String USER_KEY = "user";
    private static final String FCM_TOKEN_KEY = "fcm_token";
    
    public AuthHelper(Page page, String frontendUrl) {
        this.page = page;
        this.frontendUrl = frontendUrl;
    }
    
    /**
     * 일반 사용자로 로그인 시뮬레이션
     */
    public void loginAsUser() {
        loginAsUser(TEST_USER_NAME, TEST_USER_ID, "USER");
    }
    
    /**
     * 커스텀 사용자 정보로 로그인 시뮬레이션
     */
    public void loginAsUser(String userName, String userId, String role) {
        // localStorage에 토큰과 사용자 정보 설정
        // 주의: localStorage 설정 후 페이지를 반드시 새로고침해야 React 컴포넌트가 인식함
        String token = "USER".equals(role) ? TEST_JWT_TOKEN : TEST_ADMIN_TOKEN;

        String script = String.format(
            "localStorage.setItem('%s', '%s');" +
            "localStorage.setItem('%s', JSON.stringify({" +
            "  id: '%s'," +
            "  userName: '%s'," +
            "  email: 'test@example.com'," +
            "  role: '%s'," +
            "  thumbnailImage: '%s'," +
            "  createdAt: new Date().toISOString()" +
            "}));" +
            "localStorage.setItem('%s', 'test_fcm_token');",
            ACCESS_TOKEN_KEY, token,
            USER_KEY,
            userId,
            userName,
            role,
            TEST_USER_THUMBNAIL,
            FCM_TOKEN_KEY
        );

        page.evaluate(script);

        // 페이지 새로고침하여 로그인 상태 반영
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 로그인 상태가 반영될 때까지 잠시 대기
        page.waitForTimeout(500);
    }
    
    /**
     * 관리자로 로그인 시뮬레이션
     */
    public void loginAsAdmin() {
        loginAsUser("관리자", "2", "ADMIN");
    }
    
    /**
     * 카카오 OAuth 로그인 플로우 시뮬레이션
     */
    public void simulateKakaoOAuthLogin() {
        // 로그인 페이지로 이동
        page.navigate(frontendUrl + "/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // 카카오 로그인 버튼 클릭
        page.locator("button:has-text('카카오 로그인'), button:has-text('카카오로 시작하기')").click();
        
        // OAuth 콜백 시뮬레이션 (실제 환경에서는 카카오 서버로 리다이렉트됨)
        // 테스트 환경에서는 직접 콜백 URL로 이동
        String callbackUrl = frontendUrl + "/callback?code=test_auth_code&state=test_state";
        page.navigate(callbackUrl);
        
        // 콜백 처리 대기
        page.waitForURL(frontendUrl + "/**", new Page.WaitForURLOptions()
            .setTimeout(10000));
        
        // 로그인 성공 후 localStorage에 토큰 설정
        loginAsUser();
    }
    
    /**
     * 로그아웃
     */
    public void logout() {
        // localStorage 초기화
        page.evaluate("() => {" +
            "localStorage.removeItem('" + ACCESS_TOKEN_KEY + "');" +
            "localStorage.removeItem('" + USER_KEY + "');" +
            "localStorage.removeItem('" + FCM_TOKEN_KEY + "');" +
            "sessionStorage.clear();" +
        "}");
        
        // 페이지 새로고침하여 로그아웃 상태 반영
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
    
    /**
     * 현재 로그인 상태 확인
     */
    public boolean isLoggedIn() {
        Object token = page.evaluate("() => localStorage.getItem('" + ACCESS_TOKEN_KEY + "')");
        return token != null && !token.toString().isEmpty();
    }
    
    /**
     * 현재 사용자 정보 가져오기
     */
    public UserInfo getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }
        
        Object userJson = page.evaluate("() => localStorage.getItem('" + USER_KEY + "')");
        if (userJson == null) {
            return null;
        }
        
        // JSON 파싱하여 UserInfo 객체 생성
        String json = userJson.toString();
        return parseUserInfo(json);
    }
    
    /**
     * 현재 사용자가 관리자인지 확인
     */
    public boolean isAdmin() {
        UserInfo user = getCurrentUser();
        return user != null && "ADMIN".equals(user.role);
    }
    
    /**
     * JWT 토큰 직접 설정
     */
    public void setAccessToken(String token) {
        page.evaluate("token => localStorage.setItem('" + ACCESS_TOKEN_KEY + "', token)", token);
    }
    
    /**
     * JWT 토큰 가져오기
     */
    public String getAccessToken() {
        Object token = page.evaluate("() => localStorage.getItem('" + ACCESS_TOKEN_KEY + "')");
        return token != null ? token.toString() : null;
    }
    
    /**
     * FCM 토큰 설정 (푸시 알림용)
     */
    public void setFCMToken(String token) {
        page.evaluate("token => localStorage.setItem('" + FCM_TOKEN_KEY + "', token)", token);
    }
    
    /**
     * 세션 스토리지 초기화
     */
    public void clearSession() {
        page.evaluate("() => {" +
            "localStorage.clear();" +
            "sessionStorage.clear();" +
        "}");
    }
    
    /**
     * 쿠키 설정
     */
    public void setCookie(String name, String value) {
        page.context().addCookies(java.util.Arrays.asList(
            new com.microsoft.playwright.options.Cookie(name, value)
                .setDomain("localhost")
                .setPath("/")
        ));
    }
    
    /**
     * 모든 쿠키 삭제
     */
    public void clearCookies() {
        page.context().clearCookies();
    }
    
    /**
     * 로그인 후 메인 페이지로 이동
     */
    public void loginAndNavigateToHome() {
        loginAsUser();
        page.navigate(frontendUrl);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
    
    /**
     * 로그인 후 특정 페이지로 이동
     */
    public void loginAndNavigateTo(String path) {
        loginAsUser();
        page.navigate(frontendUrl + path);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
    
    // Helper Methods
    
    private Object createLoginData(String userName, String userId, String role) {
        String token = "USER".equals(role) ? TEST_JWT_TOKEN : TEST_ADMIN_TOKEN;
        
        return page.evaluate("({userName, userId, role, token}) => ({" +
            "token: token," +
            "user: {" +
            "  id: userId," +
            "  userName: userName," +
            "  email: 'test@example.com'," +
            "  role: role," +
            "  thumbnailImage: 'https://via.placeholder.com/150'," +
            "  createdAt: new Date().toISOString()" +
            "}" +
        "})", java.util.Map.of(
            "userName", userName,
            "userId", userId,
            "role", role,
            "token", token
        ));
    }
    
    private UserInfo parseUserInfo(String json) {
        try {
            // 간단한 JSON 파싱 (실제로는 Jackson 등 사용 권장)
            UserInfo user = new UserInfo();
            
            if (json.contains("\"id\":")) {
                user.id = extractJsonValue(json, "id");
            }
            if (json.contains("\"userName\":")) {
                user.userName = extractJsonValue(json, "userName");
            }
            if (json.contains("\"email\":")) {
                user.email = extractJsonValue(json, "email");
            }
            if (json.contains("\"role\":")) {
                user.role = extractJsonValue(json, "role");
            }
            if (json.contains("\"thumbnailImage\":")) {
                user.thumbnailImage = extractJsonValue(json, "thumbnailImage");
            }
            
            return user;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            // 숫자 값인 경우
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start == -1) return null;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    
    /**
     * 사용자 정보 클래스
     */
    public static class UserInfo {
        public String id;
        public String userName;
        public String email;
        public String role;
        public String thumbnailImage;
        
        @Override
        public String toString() {
            return String.format("UserInfo{id='%s', userName='%s', role='%s'}", 
                id, userName, role);
        }
    }
    
    /**
     * 로그인 상태 확인 후 대기
     */
    public void waitForLoginState(boolean shouldBeLoggedIn) {
        int maxAttempts = 10;
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            if (isLoggedIn() == shouldBeLoggedIn) {
                break;
            }
            page.waitForTimeout(500);
            attempts++;
        }
        
        if (isLoggedIn() != shouldBeLoggedIn) {
            throw new RuntimeException("로그인 상태가 예상과 다릅니다. 예상: " + shouldBeLoggedIn + ", 실제: " + isLoggedIn());
        }
    }
    
    /**
     * SSE 알림 구독 시뮬레이션
     */
    public void simulateSSEConnection() {
        if (!isLoggedIn()) {
            throw new RuntimeException("SSE 연결을 위해서는 로그인이 필요합니다.");
        }
        
        // SSE 연결 시뮬레이션 (실제로는 백엔드 API 호출)
        page.evaluate("() => {" +
            "window.__sse_connected = true;" +
            "window.dispatchEvent(new CustomEvent('sse:connected'));" +
        "}");
    }
    
    /**
     * 알림 권한 설정
     */
    public void grantNotificationPermission() {
        page.context().grantPermissions(java.util.Arrays.asList("notifications"));
    }
}