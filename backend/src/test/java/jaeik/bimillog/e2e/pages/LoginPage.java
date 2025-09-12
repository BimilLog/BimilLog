package jaeik.bimillog.e2e.pages;

import com.microsoft.playwright.Page;

/**
 * <h2>로그인 페이지 객체</h2>
 * <p>로그인 관련 페이지 요소와 동작을 관리합니다.</p>
 * <p>카카오 로그인, 로그아웃, 로그인 상태 확인</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class LoginPage extends BasePage {
    
    private static final String LOGIN_BUTTON = "text=로그인";
    private static final String LOGOUT_BUTTON = "text=로그아웃";
    private static final String KAKAO_LOGIN_BUTTON = "button:has-text('카카오 로그인')";
    private static final String USER_PROFILE = "[data-testid='user-profile']";
    
    public LoginPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }
    
    /**
     * <h3>로그인 페이지로 이동</h3>
     * <p>홈페이지에서 로그인 버튼을 클릭합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void navigateToLogin() {
        navigate("/");
        clickByText("로그인");
    }
    
    /**
     * <h3>카카오 로그인 수행</h3>
     * <p>카카오 OAuth 로그인을 수행합니다.</p>
     * <p>이미 카카오 계정이 연결되어 있어 자동 로그인됩니다.</p>
     *
     * @return 로그인 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean performKakaoLogin() {
        try {
            navigate("/");
            
            if (isLoggedIn()) {
                System.out.println("Already logged in");
                return true;
            }
            
            clickByText("로그인");
            
            waitForElement(KAKAO_LOGIN_BUTTON);
            click(KAKAO_LOGIN_BUTTON);
            
            page.waitForURL("**/auth/callback**", 
                new Page.WaitForURLOptions().setTimeout(10000));
            
            waitForElement(LOGOUT_BUTTON, 5000);
            
            System.out.println("Kakao login successful");
            return true;
            
        } catch (Exception e) {
            System.err.println("Kakao login failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>로그아웃 수행</h3>
     * <p>현재 세션에서 로그아웃합니다.</p>
     *
     * @return 로그아웃 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean performLogout() {
        try {
            if (!isLoggedIn()) {
                System.out.println("Already logged out");
                return true;
            }
            
            clickByText("로그아웃");
            
            waitForElement(LOGIN_BUTTON, 5000);
            
            System.out.println("Logout successful");
            return true;
            
        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>로그인 상태 확인</h3>
     * <p>현재 로그인 상태를 확인합니다.</p>
     *
     * @return 로그인되어 있으면 true
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isLoggedIn() {
        return isTextVisible("로그아웃");
    }
    
    /**
     * <h3>사용자 이름 가져오기</h3>
     * <p>로그인된 사용자의 이름을 가져옵니다.</p>
     *
     * @return 사용자 이름, 로그인되지 않은 경우 null
     * @author Jaeik
     * @since 2.0.0
     */
    public String getUsername() {
        if (!isLoggedIn()) {
            return null;
        }
        
        try {
            return getText(USER_PROFILE);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * <h3>로그인 필수 페이지 접근 확인</h3>
     * <p>로그인이 필요한 페이지에 접근 시 리다이렉트되는지 확인합니다.</p>
     *
     * @param protectedUrl 보호된 페이지 URL
     * @return 리다이렉트되면 true
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean verifyLoginRequired(String protectedUrl) {
        performLogout();
        
        navigate(protectedUrl);
        wait(2000);
        
        String currentUrl = getCurrentUrl();
        return currentUrl.contains("/login") || currentUrl.contains("/auth");
    }
    
    /**
     * <h3>세션 유지 확인</h3>
     * <p>페이지 새로고침 후에도 로그인 상태가 유지되는지 확인합니다.</p>
     *
     * @return 세션이 유지되면 true
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean verifySessionPersistence() {
        if (!isLoggedIn()) {
            performKakaoLogin();
        }
        
        String usernameBefore = getUsername();
        
        page.reload();
        waitForPageLoad();
        
        String usernameAfter = getUsername();
        
        return usernameBefore != null && 
               usernameBefore.equals(usernameAfter) && 
               isLoggedIn();
    }
}