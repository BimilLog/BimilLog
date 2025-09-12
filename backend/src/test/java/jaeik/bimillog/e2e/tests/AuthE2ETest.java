package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>인증 E2E 테스트</h2>
 * <p>카카오 로그인, 로그아웃, 세션 관리 테스트를 수행합니다.</p>
 * <p>실제 카카오 계정 연동 상태에서 자동 로그인 테스트</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("인증 플로우 E2E 테스트")
public class AuthE2ETest extends BaseE2ETest {
    
    private LoginPage loginPage;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        loginPage = new LoginPage(page, BASE_URL);
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 카카오 로그인 테스트")
    void 카카오_로그인_성공() {
        System.out.println(">>> 카카오 로그인 테스트 시작");
        
        loginPage.navigate("/");
        
        assertThat(loginPage.isLoggedIn()).isFalse();
        
        boolean loginSuccess = loginPage.performKakaoLogin();
        assertThat(loginSuccess).isTrue();
        
        assertThat(loginPage.isLoggedIn()).isTrue();
        
        String username = loginPage.getUsername();
        assertThat(username).isNotNull();
        System.out.println("로그인된 사용자: " + username);
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 세션 유지 테스트")
    void 페이지_새로고침_후_세션_유지() {
        System.out.println(">>> 세션 유지 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String usernameBefore = loginPage.getUsername();
        assertThat(usernameBefore).isNotNull();
        
        page.reload();
        loginPage.waitForPageLoad();
        
        assertThat(loginPage.isLoggedIn()).isTrue();
        
        String usernameAfter = loginPage.getUsername();
        assertThat(usernameAfter).isEqualTo(usernameBefore);
        
        System.out.println("세션 유지 확인: " + usernameAfter);
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 보호된 페이지 접근 테스트")
    void 로그인_없이_보호된_페이지_접근시_리다이렉트() {
        System.out.println(">>> 보호된 페이지 접근 테스트 시작");
        
        if (loginPage.isLoggedIn()) {
            loginPage.performLogout();
        }
        
        loginPage.navigate("/paper/create");
        loginPage.wait(2000);
        
        String currentUrl = loginPage.getCurrentUrl();
        assertThat(currentUrl).doesNotContain("/paper/create");
        
        System.out.println("리다이렉트 확인: " + currentUrl);
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 로그아웃 테스트")
    void 로그아웃_성공() {
        System.out.println(">>> 로그아웃 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        assertThat(loginPage.isLoggedIn()).isTrue();
        
        boolean logoutSuccess = loginPage.performLogout();
        assertThat(logoutSuccess).isTrue();
        
        assertThat(loginPage.isLoggedIn()).isFalse();
        
        System.out.println("로그아웃 완료");
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 로그아웃 후 세션 무효화 확인")
    void 로그아웃_후_세션_무효화() {
        System.out.println(">>> 세션 무효화 테스트 시작");
        
        if (loginPage.isLoggedIn()) {
            loginPage.performLogout();
        }
        
        page.reload();
        loginPage.waitForPageLoad();
        
        assertThat(loginPage.isLoggedIn()).isFalse();
        
        loginPage.navigate("/paper/create");
        loginPage.wait(2000);
        
        String currentUrl = loginPage.getCurrentUrl();
        assertThat(currentUrl).doesNotContain("/paper/create");
        
        System.out.println("세션 무효화 확인");
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 연속 로그인/로그아웃 테스트")
    void 연속_로그인_로그아웃() {
        System.out.println(">>> 연속 로그인/로그아웃 테스트 시작");
        
        for (int i = 1; i <= 3; i++) {
            System.out.println("시도 " + i + "/3");
            
            assertThat(loginPage.isLoggedIn()).isFalse();
            
            loginPage.performKakaoLogin();
            assertThat(loginPage.isLoggedIn()).isTrue();
            
            loginPage.performLogout();
            assertThat(loginPage.isLoggedIn()).isFalse();
        }
        
        System.out.println("연속 로그인/로그아웃 테스트 완료");
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        try {
            if (loginPage != null && loginPage.isLoggedIn()) {
                loginPage.performLogout();
            }
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
        
        super.afterEach(testInfo);
    }
}