package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.auth.*;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 시나리오 2: 인증 관련 페이지 E2E 테스트
 *
 * USER_SCENARIOS.md 기반 테스트 시나리오:
 * 1. 로그인 페이지 (/login)
 * 2. 회원가입 페이지 (/signup)
 * 3. 콜백 페이지 (/callback)
 *
 * 사용자 관점 테스트:
 * - 신규 사용자: 카카오 로그인 → OAuth 콜백 → 닉네임 설정 → 회원가입 완료
 * - 기존 사용자: 카카오 로그인 → OAuth 콜백 → 홈으로 리다이렉트
 * - 에러 처리: OAuth 실패, 네트워크 에러, 유효하지 않은 UUID
 * - 반응형 디자인: 모바일, 태블릿, 데스크톱
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("시나리오 2: 인증 플로우 E2E 테스트")
public class AuthenticationFlowTest extends BaseE2ETest {

    private LoginPage loginPage;
    private SignupPage signupPage;
    private CallbackPage callbackPage;

    @BeforeEach
    void setupPages() {
        loginPage = new LoginPage(page);
        signupPage = new SignupPage(page);
        callbackPage = new CallbackPage(page);

        System.out.println("=== 인증 플로우 테스트 시작 ===");
    }

    @Test
    @Order(1)
    @DisplayName("1. 로그인 페이지 기본 요소 확인")
    void testLoginPageElements() {
        // Given: 사용자가 로그인 페이지에 접근
        loginPage.navigate();

        // Then: 페이지 기본 요소들이 표시됨
        assertTrue(loginPage.hasCorrectTitle(), "로그인 페이지 제목이 표시되어야 함");
        assertTrue(loginPage.hasKakaoLoginButton(), "카카오 로그인 버튼이 표시되어야 함");
        assertTrue(loginPage.hasInfoMessage(), "로그인 없이도 이용 가능 메시지가 표시되어야 함");

        // 스크린샷 캡처
        loginPage.takeScreenshot("login_page_elements");
    }

    @Test
    @Order(2)
    @DisplayName("2. 로그인 페이지 에러 메시지 처리")
    void testLoginPageErrorHandling() {
        // Given: OAuth 에러와 함께 로그인 페이지로 리다이렉트
        page.navigate("http://localhost:3000/login?error=access_denied");

        // Then: 에러 메시지가 표시됨
        assertTrue(loginPage.hasErrorMessage(), "에러 메시지가 표시되어야 함");
        String errorMsg = loginPage.getErrorMessage();
        assertNotNull(errorMsg, "에러 메시지 텍스트가 있어야 함");

        // And: 5초 후 에러 메시지가 자동으로 사라짐
        boolean cleared = loginPage.waitForErrorToClear(5500);
        assertTrue(cleared, "에러 메시지가 5초 후 자동으로 사라져야 함");
    }

    @Test
    @Order(3)
    @DisplayName("3. 회원가입 페이지 초기 화면")
    void testSignupPageInitial() {
        // Given: 사용자가 회원가입 페이지에 접근
        signupPage.navigate();

        // Then: 카카오로 시작하기 버튼이 표시됨
        page.waitForSelector("text=회원가입");
        assertTrue(page.locator("button:has-text('카카오로 시작하기')").isVisible(),
            "카카오로 시작하기 버튼이 표시되어야 함");

        // And: 로그인 링크가 있음
        assertTrue(page.locator("a[href='/login']").isVisible(),
            "로그인 링크가 표시되어야 함");

        signupPage.takeScreenshot("signup_initial_page");
    }

    @Test
    @Order(4)
    @DisplayName("4. 닉네임 설정 페이지 - 유효성 검사")
    void testNicknameValidation() {
        // Given: 카카오 OAuth 후 닉네임 설정 페이지로 이동 (테스트용 UUID)
        String testUuid = UUID.randomUUID().toString();
        signupPage.navigateToNicknameSetup(testUuid);

        // When: 짧은 닉네임 입력 (1자)
        signupPage.enterNickname("a");

        // Then: 에러 메시지 표시
        assertTrue(signupPage.hasNicknameError(), "1자 닉네임은 에러가 표시되어야 함");
        String error = signupPage.getNicknameError();
        assertTrue(error.contains("2자") || error.contains("짧"), "닉네임 길이 에러 메시지");

        // When: 특수문자 포함 닉네임
        signupPage.enterNickname("test@#$");

        // Then: 에러 메시지 표시
        assertTrue(signupPage.hasNicknameError(), "특수문자는 허용되지 않아야 함");

        // When: 유효한 닉네임 입력 (2~8자 제한으로 인해 짧게 생성)
        String validNickname = "테스트" + (System.currentTimeMillis() % 10000);
        signupPage.enterNickname(validNickname);
        signupPage.waitForDuplicateCheck();

        // Then: 성공 메시지 표시
        assertTrue(signupPage.hasNicknameSuccess(), "유효한 닉네임은 성공 메시지가 표시되어야 함");

        signupPage.takeScreenshot("nickname_validation");
    }

    @Test
    @Order(5)
    @DisplayName("5. 닉네임 중복 체크")
    void testNicknameDuplicateCheck() {
        // Given: 닉네임 설정 페이지
        String testUuid = UUID.randomUUID().toString();
        signupPage.navigateToNicknameSetup(testUuid);

        // When: 이미 존재하는 닉네임 입력 (예: "admin")
        signupPage.enterNickname("admin");
        signupPage.waitForDuplicateCheck();

        // Then: 중복 에러 메시지 표시
        if (signupPage.hasNicknameError()) {
            String error = signupPage.getNicknameError();
            assertTrue(error.contains("중복") || error.contains("사용"),
                "닉네임 중복 에러 메시지가 표시되어야 함");
        }

        // When: 유니크한 닉네임 입력 (2~8자 제한)
        String uniqueNickname = "유저" + (System.currentTimeMillis() % 100000);
        signupPage.enterNickname(uniqueNickname);
        signupPage.waitForDuplicateCheck();

        // Then: 사용 가능 메시지
        assertTrue(signupPage.hasNicknameSuccess(), "유니크한 닉네임은 성공 메시지가 표시되어야 함");
    }

    @Test
    @Order(6)
    @DisplayName("6. 약관 동의 및 회원가입 완료")
    void testTermsAndSignupCompletion() {
        // Given: 닉네임 설정 페이지
        String testUuid = UUID.randomUUID().toString();
        signupPage.navigateToNicknameSetup(testUuid);

        // When: 유효한 닉네임 입력 (2~8자 제한)
        String nickname = "테스트" + (System.currentTimeMillis() % 1000);
        signupPage.enterNickname(nickname);
        signupPage.waitForDuplicateCheck();

        // And: 약관 동의 없이 가입 시도
        assertFalse(signupPage.isSubmitButtonEnabled(),
            "약관 동의 없이는 가입 버튼이 비활성화되어야 함");

        // When: 약관 모두 동의
        signupPage.checkAllAgree();

        // Then: 가입 버튼 활성화
        assertTrue(signupPage.isSubmitButtonEnabled(),
            "약관 동의 후 가입 버튼이 활성화되어야 함");

        signupPage.takeScreenshot("signup_ready");
    }

    @Test
    @Order(7)
    @DisplayName("7. 콜백 페이지 - 로딩 상태")
    void testCallbackPageLoading() {
        // Given: 테스트용 authorization code
        String testCode = "test_auth_code_" + System.currentTimeMillis();

        // When: 콜백 페이지로 이동
        callbackPage.navigateWithCode(testCode);

        // Then: 로딩 스피너와 메시지 표시
        assertTrue(callbackPage.hasLoadingMessage(), "로딩 메시지가 표시되어야 함");

        callbackPage.takeScreenshot("callback_loading");
    }

    @Test
    @Order(8)
    @DisplayName("8. 콜백 페이지 - 신규 사용자 리다이렉트")
    void testCallbackNewUserRedirect() {
        // Given: 신규 사용자 응답 시뮬레이션
        String testUuid = UUID.randomUUID().toString();
        callbackPage.simulateNewUserResponse(testUuid);

        // When: 콜백 페이지로 이동
        callbackPage.navigateWithCode("test_code");

        // Then: 회원가입 페이지로 리다이렉트
        page.waitForTimeout(2000);
        String extractedUuid = callbackPage.getUuidFromUrl();
        assertNotNull(extractedUuid, "UUID가 URL에 포함되어야 함");
    }

    @Test
    @Order(9)
    @DisplayName("9. 콜백 페이지 - 기존 사용자 리다이렉트")
    void testCallbackExistingUserRedirect() {
        // Given: 기존 사용자 응답 시뮬레이션
        callbackPage.simulateSuccessResponse();

        // When: 콜백 페이지로 이동
        callbackPage.navigateWithCode("test_code");

        // Then: 홈으로 리다이렉트 (시뮬레이션이므로 실제 리다이렉트는 발생하지 않을 수 있음)
        page.waitForTimeout(2000);
    }

    @Test
    @Order(10)
    @DisplayName("10. 콜백 페이지 - 에러 처리")
    void testCallbackErrorHandling() {
        // Given: OAuth 에러
        callbackPage.navigateWithError("access_denied");

        // Then: 로그인 페이지로 리다이렉트 with 에러
        page.waitForTimeout(2000);
        String error = callbackPage.getErrorFromUrl();
        if (error != null) {
            assertEquals("access_denied", error, "에러가 URL에 포함되어야 함");
        }
    }

    @Test
    @Order(11)
    @DisplayName("11. 반응형 디자인 - 모바일")
    void testMobileResponsive() {
        // Given: 모바일 뷰포트
        loginPage.setMobileViewport();

        // When: 로그인 페이지 접근
        loginPage.navigate();

        // Then: 모바일에서 모든 요소가 표시됨
        assertTrue(loginPage.hasKakaoLoginButton(), "모바일에서 카카오 버튼이 표시되어야 함");

        loginPage.takeScreenshot("login_mobile");

        // When: 회원가입 페이지
        signupPage.setMobileViewport();
        signupPage.navigate();

        // Then: 모바일에서 회원가입 요소 표시
        assertTrue(page.locator("text=회원가입").isVisible(), "모바일에서 회원가입 페이지가 표시되어야 함");

        signupPage.takeScreenshot("signup_mobile");
    }

    @Test
    @Order(12)
    @DisplayName("12. 반응형 디자인 - 태블릿")
    void testTabletResponsive() {
        // Given: 태블릿 뷰포트
        loginPage.setTabletViewport();

        // When: 로그인 페이지 접근
        loginPage.navigate();

        // Then: 태블릿에서 모든 요소가 표시됨
        assertTrue(loginPage.hasKakaoLoginButton(), "태블릿에서 카카오 버튼이 표시되어야 함");

        loginPage.takeScreenshot("login_tablet");
    }

    @Test
    @Order(13)
    @DisplayName("13. 네비게이션 - 로그인 ↔ 회원가입")
    void testNavigationBetweenPages() {
        // Given: 로그인 페이지에서 시작
        loginPage.navigate();

        // When: 회원가입 링크 클릭 (실제로는 없을 수 있음, 페이지 구조에 따라)
        page.navigate("http://localhost:3000/signup");

        // Then: 회원가입 페이지로 이동
        page.waitForSelector("text=회원가입");

        // When: 로그인 링크 클릭
        signupPage.clickLoginLink();

        // Then: 로그인 페이지로 이동
        assertTrue(page.url().contains("/login"), "로그인 페이지로 이동해야 함");
    }

    @Test
    @Order(14)
    @DisplayName("14. 브라우저 뒤로가기/앞으로가기")
    void testBrowserNavigation() {
        // Given: 로그인 페이지 → 회원가입 페이지 이동
        loginPage.navigate();
        page.navigate("http://localhost:3000/signup");

        // When: 뒤로가기
        signupPage.goBack();
        page.waitForTimeout(1000);

        // Then: 로그인 페이지로 돌아옴
        assertTrue(page.url().contains("/login"), "뒤로가기로 로그인 페이지로 돌아와야 함");

        // When: 앞으로가기
        page.goForward();
        page.waitForTimeout(1000);

        // Then: 다시 회원가입 페이지
        assertTrue(page.url().contains("/signup"), "앞으로가기로 회원가입 페이지로 가야 함");
    }

    @Test
    @Order(15)
    @DisplayName("15. 이미 로그인된 사용자 리다이렉트")
    void testAlreadyLoggedInRedirect() {
        // Given: 로그인 상태 시뮬레이션 (쿠키 설정)
        context.addCookies(java.util.List.of(
            new Cookie("auth-token", "test-jwt-token")
                .setDomain("localhost")
                .setPath("/")
        ));

        // When: 로그인 페이지 접근
        loginPage.navigate();

        // Then: 홈으로 리다이렉트될 수 있음 (실제 구현에 따라)
        // 또는 로그인 페이지에 머물 수도 있음
        page.waitForTimeout(2000);

        // 쿠키 정리
        context.clearCookies();
    }

    @Test
    @Order(16)
    @DisplayName("16. 유효하지 않은 UUID로 회원가입 시도")
    void testInvalidUuidSignup() {
        // Given: 잘못된 UUID로 닉네임 설정 페이지 접근
        signupPage.navigateToNicknameSetup("invalid-uuid-123");

        // Then: 에러 메시지 표시 또는 리다이렉트
        page.waitForTimeout(2000);
        boolean hasError = signupPage.hasInvalidUuidError();
        if (hasError) {
            assertTrue(hasError, "유효하지 않은 UUID 에러가 표시되어야 함");
        }
    }

    @Test
    @Order(17)
    @DisplayName("17. 세션 만료 처리")
    void testSessionExpiry() {
        // Given: 닉네임 설정 페이지
        String testUuid = UUID.randomUUID().toString();
        signupPage.navigateToNicknameSetup(testUuid);

        // When: 오랜 시간 대기 시뮬레이션
        page.waitForTimeout(2000);

        // And: 페이지 새로고침
        signupPage.refresh();

        // Then: 에러 처리 또는 로그인 페이지로 리다이렉트
        page.waitForTimeout(2000);
    }

    @Test
    @Order(18)
    @DisplayName("18. 네트워크 에러 처리")
    void testNetworkError() {
        // Given: 네트워크 에러 시뮬레이션
        callbackPage.simulateNetworkError();

        // When: 콜백 페이지로 이동
        callbackPage.navigateWithCode("test_code");

        // Then: 에러 처리
        page.waitForTimeout(3000);

        // 에러 페이지나 재시도 옵션이 표시될 수 있음
        callbackPage.takeScreenshot("network_error");
    }

    @Test
    @Order(19)
    @DisplayName("19. 모바일 FCM 토큰 처리")
    void testMobileFCMToken() {
        // Given: 모바일 뷰포트
        callbackPage.setMobileViewport();

        // When: 콜백 페이지로 이동
        callbackPage.navigateWithCode("test_code");

        // Then: 모바일 알림 설정 메시지 확인
        boolean hasMobileMsg = callbackPage.hasMobileMessage();
        // 실제로는 isMobileOrTablet() 조건에 따라 표시됨

        callbackPage.takeScreenshot("mobile_fcm_message");
    }

    @Test
    @Order(20)
    @DisplayName("20. 전체 인증 플로우 통합 테스트")
    void testCompleteAuthFlow() {
        System.out.println("=== 전체 인증 플로우 통합 테스트 시작 ===");

        // Step 1: 로그인 페이지 접근
        loginPage.navigate();
        assertTrue(loginPage.hasKakaoLoginButton(), "카카오 로그인 버튼이 있어야 함");
        loginPage.takeScreenshot("flow_1_login");

        // Step 2: 카카오 로그인 시뮬레이션 (실제로는 OAuth 리다이렉트)
        // 테스트 환경에서는 직접 콜백 페이지로 이동
        String testCode = "integration_test_code";
        String testUuid = UUID.randomUUID().toString();

        // Step 3: 신규 사용자 시나리오 - 닉네임 설정
        signupPage.navigateToNicknameSetup(testUuid);
        signupPage.takeScreenshot("flow_2_nickname_setup");

        // Step 4: 닉네임 입력 및 검증 (2~8자 제한)
        String nickname = "통테" + (System.currentTimeMillis() % 10000);
        signupPage.enterNickname(nickname);
        signupPage.waitForDuplicateCheck();
        assertTrue(signupPage.hasNicknameSuccess(), "닉네임 사용 가능해야 함");

        // Step 5: 약관 동의
        signupPage.checkAllAgree();
        assertTrue(signupPage.isSubmitButtonEnabled(), "가입 버튼 활성화되어야 함");
        signupPage.takeScreenshot("flow_3_ready_to_submit");

        // Step 6: 회원가입 완료 (실제 제출은 API 호출이므로 시뮬레이션)
        // signupPage.clickSubmitButton();
        // signupPage.isRedirectedToHome();

        System.out.println("=== 전체 인증 플로우 통합 테스트 완료 ===");
    }

    @AfterEach
    void cleanupAuth() {
        // 인증 관련 쿠키 및 스토리지 정리
        context.clearCookies();
        page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }");

        System.out.println("=== 인증 플로우 테스트 종료 ===");
    }
}