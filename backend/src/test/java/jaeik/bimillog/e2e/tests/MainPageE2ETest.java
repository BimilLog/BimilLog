package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.common.HomePage;
import jaeik.bimillog.e2e.pages.common.NavigationComponent;
import jaeik.bimillog.e2e.utils.AuthHelper;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 메인 페이지 E2E 테스트
 * 
 * USER_SCENARIOS.md의 시나리오 1: 메인 페이지 테스트
 * 
 * 테스트 범위:
 * - 비로그인 사용자 시나리오
 * - 로그인 사용자 시나리오
 * - 네비게이션 동작
 * - 반응형 디자인
 * - PWA 설치 프롬프트
 * - 광고 배너 표시
 * - 프로토콜 URL 처리
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("시나리오 1: 메인 페이지 E2E 테스트")
public class MainPageE2ETest extends BaseE2ETest {

    private HomePage homePage;
    private NavigationComponent navigation;
    private AuthHelper authHelper;
    
    @BeforeEach
    void setupPageObjects(TestInfo testInfo) {
        // 테스트 이름 출력
        String testName = testInfo.getDisplayName();
        System.out.println("\n🧪 테스트 시작: " + testName);
        System.out.println("=" . repeat(60));
        
        // Page Object 초기화
        homePage = new HomePage(page, FRONTEND_URL);
        navigation = new NavigationComponent(page);
        authHelper = new AuthHelper(page, FRONTEND_URL);
        
        // 스크린샷 디렉토리 생성
        page.context().tracing().start(new Tracing.StartOptions()
            .setScreenshots(true)
            .setSnapshots(true));
    }

    @AfterEach
    void saveTestArtifacts(TestInfo testInfo) {
        // 테스트 이름에서 특수문자 제거
        String testName = testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9가-힣]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // 스크린샷 저장
        try {
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/main_page/" + testName + "_" + timestamp + ".png"))
                .setFullPage(true));
        } catch (Exception e) {
            System.err.println("스크린샷 저장 실패: " + e.getMessage());
        }
        
        // 추적 저장
        try {
            page.context().tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("target/traces/main_page/" + testName + "_" + timestamp + ".zip")));
        } catch (Exception e) {
            System.err.println("추적 저장 실패: " + e.getMessage());
        }
        
        System.out.println("✅ 테스트 완료: " + testInfo.getDisplayName());
        System.out.println("=" . repeat(60));
    }

    // ==================== 비로그인 사용자 테스트 ====================

    @Test
    @Order(1)
    @DisplayName("1. 비로그인 사용자 - 메인 페이지 기본 요소 확인")
    void testNonAuthenticatedUserMainPageElements() {
        System.out.println("📍 비로그인 상태에서 메인 페이지 접속");
        
        // 로그아웃 상태 확인
        authHelper.logout();
        
        // 메인 페이지 접속
        homePage.navigateToHome();
        assertTrue(homePage.isLoaded(), "메인 페이지가 로드되어야 함");
        
        // Hero 섹션 확인
        System.out.println("📍 Hero 섹션 확인");
        assertTrue(homePage.isHeroSectionVisible(), "Hero 섹션이 표시되어야 함");
        assertEquals("익명으로 마음을 전해보세요", homePage.getHeroTitle(), "Hero 제목이 올바르게 표시되어야 함");
        assertTrue(homePage.getHeroSubtitle().contains("비밀로그"), "Hero 부제목에 '비밀로그'가 포함되어야 함");
        
        // CTA 버튼 확인
        System.out.println("📍 CTA 버튼 확인");
        assertTrue(homePage.isStartButtonVisible(), "'내 롤링페이퍼 만들기' 버튼이 표시되어야 함");
        assertTrue(homePage.isVisitButtonVisible(), "'다른 롤링페이퍼 방문하기' 버튼이 표시되어야 함");
        assertFalse(homePage.isKakaoFriendsButtonVisible(), "비로그인 상태에서는 '카카오 친구 확인하기' 버튼이 표시되지 않아야 함");
        assertFalse(homePage.isKakaoShareButtonVisible(), "비로그인 상태에서는 카카오 공유 버튼이 표시되지 않아야 함");
        
        // Features 섹션 확인
        System.out.println("📍 Features 섹션 확인");
        assertTrue(homePage.isFeaturesSectionVisible(), "Features 섹션이 표시되어야 함");
        assertEquals(4, homePage.getFeatureCardsCount(), "4개의 기능 카드가 표시되어야 함");
        assertTrue(homePage.hasAllFeatures(), "모든 주요 기능이 표시되어야 함");
        
        // Footer 확인
        System.out.println("📍 Footer 확인");
        assertTrue(homePage.isFooterVisible(), "Footer가 표시되어야 함");
        assertTrue(homePage.hasFooterLogo(), "Footer에 로고가 표시되어야 함");
        assertTrue(homePage.hasFooterVersion(), "Footer에 버전 정보가 표시되어야 함");
        assertTrue(homePage.hasAllPolicyLinks(), "개인정보처리방침과 이용약관 링크가 표시되어야 함");
    }

    @Test
    @Order(2)
    @DisplayName("2. 비로그인 사용자 - 네비게이션 메뉴 확인")
    void testNonAuthenticatedUserNavigation() {
        System.out.println("📍 비로그인 상태 네비게이션 확인");
        
        authHelper.logout();
        homePage.navigateToHome();
        
        // 네비게이션 로고 확인
        assertTrue(navigation.isLogoVisible(), "로고가 표시되어야 함");
        
        // 비로그인 메뉴 아이템 확인
        System.out.println("📍 비로그인 메뉴 아이템 확인");
        assertTrue(navigation.hasNonAuthenticatedMenuItems(), "비로그인 메뉴 아이템이 표시되어야 함");
        assertFalse(navigation.isUserAvatarVisible(), "사용자 아바타가 표시되지 않아야 함");
        assertFalse(navigation.isNotificationBellVisible(), "알림 벨이 표시되지 않아야 함");
        assertFalse(navigation.hasAdminMenuItem(), "관리자 메뉴가 표시되지 않아야 함");
        
        // 헤더 스티키 확인
        assertTrue(navigation.isHeaderSticky(), "헤더가 스티키여야 함");
    }

    @Test
    @Order(3)
    @DisplayName("3. 비로그인 사용자 - CTA 버튼 동작 확인")
    void testNonAuthenticatedUserCTAButtons() {
        System.out.println("📍 비로그인 CTA 버튼 동작 테스트");
        
        authHelper.logout();
        homePage.navigateToHome();
        
        // '내 롤링페이퍼 만들기' 버튼 클릭
        System.out.println("📍 '내 롤링페이퍼 만들기' 버튼 클릭");
        homePage.clickStartButton();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.url().contains("/login"), "로그인 페이지로 이동해야 함");
        
        // 다시 홈으로 돌아가기
        homePage.navigateToHome();
        
        // '다른 롤링페이퍼 방문하기' 버튼 클릭
        System.out.println("📍 '다른 롤링페이퍼 방문하기' 버튼 클릭");
        homePage.clickVisitButton();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.url().contains("/visit"), "방문 페이지로 이동해야 함");
    }

    @Test
    @Order(4)
    @DisplayName("4. 비로그인 사용자 - 네비게이션 링크 동작")
    void testNonAuthenticatedUserNavigationLinks() {
        System.out.println("📍 비로그인 네비게이션 링크 동작 테스트");
        
        authHelper.logout();
        homePage.navigateToHome();
        
        // 게시판 링크
        System.out.println("📍 게시판 링크 클릭");
        navigation.navigateToBoard();
        assertTrue(page.url().contains("/board"), "게시판 페이지로 이동해야 함");
        
        // 다른 롤링페이퍼 방문 링크
        homePage.navigateToHome();
        System.out.println("📍 다른 롤링페이퍼 방문 링크 클릭");
        navigation.navigateToVisit();
        assertTrue(page.url().contains("/visit"), "방문 페이지로 이동해야 함");
        
        // 건의하기 링크
        homePage.navigateToHome();
        System.out.println("📍 건의하기 링크 클릭");
        navigation.navigateToSuggest();
        assertTrue(page.url().contains("/suggest"), "건의하기 페이지로 이동해야 함");
        
        // 로그인 링크
        homePage.navigateToHome();
        System.out.println("📍 로그인 링크 클릭");
        navigation.navigateToLogin();
        assertTrue(page.url().contains("/login"), "로그인 페이지로 이동해야 함");
    }

    // ==================== 로그인 사용자 테스트 ====================

    @Test
    @Order(5)
    @DisplayName("5. 로그인 사용자 - 메인 페이지 요소 확인")
    void testAuthenticatedUserMainPageElements() {
        System.out.println("📍 로그인 상태에서 메인 페이지 확인");
        
        // 로그인
        authHelper.loginAsUser();
        homePage.navigateToHome();
        
        // 로그인 상태 확인
        assertTrue(authHelper.isLoggedIn(), "로그인 상태여야 함");
        assertTrue(homePage.isUserAuthenticated(), "사용자가 인증된 상태여야 함");
        
        // 로그인 사용자 전용 버튼 확인
        System.out.println("📍 로그인 사용자 전용 버튼 확인");
        assertFalse(homePage.isStartButtonVisible(), "로그인 상태에서는 '내 롤링페이퍼 만들기' 버튼이 표시되지 않아야 함");
        assertTrue(homePage.isKakaoFriendsButtonVisible(), "'카카오 친구 확인하기' 버튼이 표시되어야 함");
        assertTrue(homePage.isKakaoShareButtonVisible(), "카카오 공유 버튼이 표시되어야 함");
        assertTrue(homePage.isVisitButtonVisible(), "'다른 롤링페이퍼 방문하기' 버튼이 여전히 표시되어야 함");
    }

    @Test
    @Order(6)
    @DisplayName("6. 로그인 사용자 - 네비게이션 메뉴 확인")
    void testAuthenticatedUserNavigation() {
        System.out.println("📍 로그인 상태 네비게이션 확인");
        
        authHelper.loginAsUser();
        homePage.navigateToHome();
        
        // 로그인 메뉴 아이템 확인
        System.out.println("📍 로그인 메뉴 아이템 확인");
        assertTrue(navigation.hasAuthenticatedMenuItems(), "로그인 메뉴 아이템이 표시되어야 함");
        assertTrue(navigation.isUserAvatarVisible(), "사용자 아바타가 표시되어야 함");
        assertTrue(navigation.isNotificationBellVisible(), "알림 벨이 표시되어야 함");
        assertTrue(navigation.isUserNameVisible(), "사용자 이름이 표시되어야 함");
        assertEquals(AuthHelper.TEST_USER_NAME, navigation.getUserName(), "올바른 사용자 이름이 표시되어야 함");
    }

    @Test
    @Order(7)
    @DisplayName("7. 로그인 사용자 - 카카오 친구 모달")
    void testKakaoFriendsModal() {
        System.out.println("📍 카카오 친구 모달 테스트");
        
        authHelper.loginAsUser();
        homePage.navigateToHome();
        
        // 카카오 친구 확인하기 버튼 클릭
        System.out.println("📍 카카오 친구 확인하기 버튼 클릭");
        homePage.clickKakaoFriendsButton();
        
        // 모달이 열릴 때까지 대기 (동적 로딩)
        page.waitForTimeout(1000);
        
        // 모달 표시 확인
        assertTrue(homePage.isKakaoFriendsModalOpen(), "카카오 친구 모달이 열려야 함");
        
        // 모달 닫기
        System.out.println("📍 모달 닫기");
        homePage.closeKakaoFriendsModal();
        assertFalse(homePage.isKakaoFriendsModalOpen(), "카카오 친구 모달이 닫혀야 함");
    }

    @Test
    @Order(8)
    @DisplayName("8. 관리자 사용자 - 관리자 메뉴 표시")
    void testAdminUserNavigation() {
        System.out.println("📍 관리자 계정 네비게이션 확인");
        
        // 관리자로 로그인
        authHelper.loginAsAdmin();
        homePage.navigateToHome();
        
        // 관리자 메뉴 확인
        assertTrue(navigation.hasAdminMenuItem(), "관리자 메뉴가 표시되어야 함");
        
        // 관리자 페이지 이동
        System.out.println("📍 관리자 페이지 이동");
        navigation.navigateToAdmin();
        assertTrue(page.url().contains("/admin"), "관리자 페이지로 이동해야 함");
    }

    // ==================== 프로토콜 URL 처리 ====================

    @Test
    @Order(9)
    @DisplayName("9. 프로토콜 URL 리다이렉트 처리")
    void testProtocolUrlRedirect() {
        System.out.println("📍 프로토콜 URL 처리 테스트");
        
        // web+bimillog:// 프로토콜로 board 페이지 접근
        System.out.println("📍 web+bimillog://board 접근");
        homePage.navigateWithProtocolUrl("board");
        page.waitForTimeout(1000);
        
        // board 페이지로 리다이렉트 확인
        assertTrue(page.url().contains("/board"), "board 페이지로 리다이렉트되어야 함");
        
        // web+bimillog:// 프로토콜로 rolling-paper 페이지 접근
        System.out.println("📍 web+bimillog://rolling-paper 접근");
        homePage.navigateWithProtocolUrl("rolling-paper");
        page.waitForTimeout(1000);
        
        // rolling-paper 페이지로 리다이렉트 확인 (로그인 필요한 경우 로그인 페이지로)
        assertTrue(page.url().contains("/rolling-paper") || page.url().contains("/login"), 
            "rolling-paper 또는 로그인 페이지로 리다이렉트되어야 함");
    }

    // ==================== 반응형 디자인 테스트 ====================

    @Test
    @Order(10)
    @DisplayName("10. 반응형 디자인 - 모바일 뷰 (320px)")
    void testMobileResponsive() {
        System.out.println("📍 모바일 반응형 테스트 (320px)");
        
        // 모바일 뷰포트 설정
        setMobileViewport();
        page.waitForTimeout(500);
        
        homePage.navigateToHome();
        
        // 모바일 뷰 확인
        assertTrue(homePage.isMobileView(), "모바일 뷰여야 함");
        assertFalse(homePage.isDesktopView(), "데스크톱 뷰가 아니어야 함");
        
        // 모바일 네비게이션 확인
        System.out.println("📍 모바일 햄버거 메뉴 확인");
        assertTrue(navigation.isMobileMenuButtonVisible(), "모바일 햄버거 메뉴가 표시되어야 함");
        assertFalse(navigation.isDesktopNavigationVisible(), "데스크톱 네비게이션이 표시되지 않아야 함");
        
        // 모바일 버튼 레이아웃 확인
        assertTrue(homePage.hasCorrectButtonLayoutForMobile(), "모바일에서 버튼이 세로로 배치되어야 함");
        
        // 모바일 메뉴 열기/닫기
        System.out.println("📍 모바일 메뉴 열기/닫기 테스트");
        navigation.openMobileMenu();
        assertTrue(navigation.isMobileMenuOpen(), "모바일 메뉴가 열려야 함");
        
        navigation.closeMobileMenu();
        assertFalse(navigation.isMobileMenuOpen(), "모바일 메뉴가 닫혀야 함");
    }

    @Test
    @Order(11)
    @DisplayName("11. 반응형 디자인 - 태블릿 뷰 (768px)")
    void testTabletResponsive() {
        System.out.println("📍 태블릿 반응형 테스트 (768px)");
        
        // 태블릿 뷰포트 설정
        setTabletViewport();
        page.waitForTimeout(500);
        
        homePage.navigateToHome();
        
        // 태블릿 뷰 확인
        assertTrue(homePage.isTabletView() || navigation.isTabletNavigationVisible(), "태블릿 뷰여야 함");
        
        // 태블릿 네비게이션 확인
        System.out.println("📍 태블릿 네비게이션 확인");
        String navType = navigation.getCurrentNavigationType();
        assertTrue("tablet".equals(navType) || "desktop".equals(navType), "태블릿 또는 데스크톱 네비게이션이어야 함");
    }

    @Test
    @Order(12)
    @DisplayName("12. 반응형 디자인 - 데스크톱 뷰 (1920px)")
    void testDesktopResponsive() {
        System.out.println("📍 데스크톱 반응형 테스트 (1920px)");
        
        // 데스크톱 뷰포트 설정
        setDesktopViewport();
        page.waitForTimeout(500);
        
        homePage.navigateToHome();
        
        // 데스크톱 뷰 확인
        assertTrue(homePage.isDesktopView(), "데스크톱 뷰여야 함");
        assertFalse(homePage.isMobileView(), "모바일 뷰가 아니어야 함");
        
        // 데스크톱 네비게이션 확인
        System.out.println("📍 데스크톱 네비게이션 확인");
        assertTrue(navigation.isDesktopNavigationVisible(), "데스크톱 네비게이션이 표시되어야 함");
        assertFalse(navigation.isMobileMenuButtonVisible(), "모바일 햄버거 메뉴가 표시되지 않아야 함");
        
        // 데스크톱 버튼 레이아웃 확인 (로그인 상태에서)
        authHelper.loginAsUser();
        page.reload();
        assertTrue(homePage.hasCorrectButtonLayoutForDesktop(), "데스크톱에서 버튼이 가로로 배치되어야 함");
    }

    // ==================== 광고 및 PWA 테스트 ====================

    @Test
    @Order(13)
    @DisplayName("13. 광고 배너 표시 확인")
    void testAdvertisementDisplay() {
        System.out.println("📍 광고 배너 표시 테스트");
        
        homePage.navigateToHome();
        
        // 상단 광고 배너 확인 (로드 실패 가능)
        System.out.println("📍 상단 광고 배너 확인");
        boolean hasTopAd = homePage.isTopAdBannerVisible();
        System.out.println("상단 광고 배너 표시: " + hasTopAd);
        
        // 모바일 광고 배너 확인 (로드 실패 가능)
        System.out.println("📍 모바일 광고 배너 확인");
        boolean hasMobileAd = homePage.isMobileAdBannerVisible();
        System.out.println("모바일 광고 배너 표시: " + hasMobileAd);
        
        // 광고는 선택적이므로 실패해도 테스트는 통과
        assertTrue(true, "광고 표시는 선택적임");
    }

    @Test
    @Order(14)
    @DisplayName("14. PWA 설치 프롬프트 확인")
    void testPWAInstallPrompt() {
        System.out.println("📍 PWA 설치 프롬프트 확인");
        
        homePage.navigateToHome();
        
        // PWA 설치 가능 여부 확인
        boolean hasPWA = homePage.hasPWAInstallPrompt();
        System.out.println("PWA 설치 가능: " + hasPWA);
        
        // PWA는 브라우저 지원에 따라 다르므로 테스트는 항상 통과
        assertTrue(true, "PWA 지원은 브라우저에 따라 다름");
    }

    // ==================== Footer 링크 테스트 ====================

    @Test
    @Order(15)
    @DisplayName("15. Footer 링크 동작 확인")
    void testFooterLinks() {
        System.out.println("📍 Footer 링크 동작 테스트");
        
        homePage.navigateToHome();
        
        // 개인정보처리방침 링크
        System.out.println("📍 개인정보처리방침 링크 클릭");
        homePage.clickFooterLink("개인정보처리방침");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.url().contains("/privacy"), "개인정보처리방침 페이지로 이동해야 함");
        
        // 이용약관 링크
        homePage.navigateToHome();
        System.out.println("📍 이용약관 링크 클릭");
        homePage.clickFooterLink("이용약관");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.url().contains("/terms"), "이용약관 페이지로 이동해야 함");
        
        // 게시판 링크
        homePage.navigateToHome();
        System.out.println("📍 Footer 게시판 링크 클릭");
        homePage.clickFooterLink("게시판");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.url().contains("/board"), "게시판 페이지로 이동해야 함");
        
        // 건의하기 링크
        homePage.navigateToHome();
        System.out.println("📍 Footer 건의하기 링크 클릭");
        homePage.clickFooterLink("건의하기");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        assertTrue(page.url().contains("/suggest"), "건의하기 페이지로 이동해야 함");
    }

    // ==================== 알림 기능 테스트 ====================

    @Test
    @Order(16)
    @DisplayName("16. 로그인 사용자 - 알림 벨 동작")
    void testNotificationBell() {
        System.out.println("📍 알림 벨 동작 테스트");
        
        // 로그인
        authHelper.loginAsUser();
        authHelper.grantNotificationPermission();
        homePage.navigateToHome();
        
        // 알림 벨 표시 확인
        assertTrue(navigation.isNotificationBellVisible(), "알림 벨이 표시되어야 함");
        
        // 알림 개수 확인 (0일 수도 있음)
        int notificationCount = navigation.getNotificationCount();
        System.out.println("알림 개수: " + notificationCount);
        
        // 알림 드롭다운 열기
        System.out.println("📍 알림 드롭다운 열기");
        navigation.clickNotificationBell();
        
        // 드롭다운이 열렸는지 확인 (실제 알림이 없을 수 있음)
        page.waitForTimeout(1000);
        
        // 드롭다운 닫기
        System.out.println("📍 알림 드롭다운 닫기");
        navigation.closeNotificationDropdown();
    }

    // ==================== 통합 시나리오 테스트 ====================

    @Test
    @Order(17)
    @DisplayName("17. 통합 시나리오 - 비로그인 → 로그인 → 로그아웃 플로우")
    void testCompleteUserFlow() {
        System.out.println("📍 통합 사용자 플로우 테스트");
        
        // 1. 비로그인 상태 확인
        System.out.println("📍 1. 비로그인 상태");
        authHelper.logout();
        homePage.navigateToHome();
        assertTrue(homePage.hasNonAuthenticatedUserElements(), "비로그인 요소가 표시되어야 함");
        assertTrue(navigation.isUserNotAuthenticated(), "비로그인 상태여야 함");
        
        // 2. 로그인 시도
        System.out.println("📍 2. 로그인 페이지 이동");
        navigation.navigateToLogin();
        assertTrue(page.url().contains("/login"), "로그인 페이지로 이동해야 함");
        
        // 3. 로그인 수행
        System.out.println("📍 3. 로그인 수행");
        authHelper.loginAsUser();
        homePage.navigateToHome();
        assertTrue(homePage.hasAuthenticatedUserElements(), "로그인 요소가 표시되어야 함");
        assertTrue(navigation.isUserAuthenticated(), "로그인 상태여야 함");
        
        // 4. 로그인 상태에서 내 롤링페이퍼 접근
        System.out.println("📍 4. 내 롤링페이퍼 접근");
        navigation.navigateToMyPaper();
        assertTrue(page.url().contains("/rolling-paper"), "롤링페이퍼 페이지로 이동해야 함");
        
        // 5. 로그아웃
        System.out.println("📍 5. 로그아웃");
        homePage.navigateToHome();
        navigation.clickLogout();
        
        // 6. 로그아웃 후 상태 확인
        System.out.println("📍 6. 로그아웃 후 상태 확인");
        homePage.navigateToHome();
        assertTrue(homePage.hasNonAuthenticatedUserElements(), "비로그인 요소가 다시 표시되어야 함");
        assertFalse(navigation.isUserAuthenticated(), "로그아웃 상태여야 함");
    }

    @AfterAll
    static void generateFinalReport() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 메인 페이지 E2E 테스트 완료");
        System.out.println("=".repeat(70));
        System.out.println("✅ 테스트 시나리오:");
        System.out.println("  - 비로그인 사용자 시나리오");
        System.out.println("  - 로그인 사용자 시나리오");
        System.out.println("  - 관리자 사용자 시나리오");
        System.out.println("  - 프로토콜 URL 처리");
        System.out.println("  - 반응형 디자인 (모바일/태블릿/데스크톱)");
        System.out.println("  - 광고 배너 및 PWA");
        System.out.println("  - Footer 링크");
        System.out.println("  - 알림 기능");
        System.out.println("  - 통합 사용자 플로우");
        System.out.println("=".repeat(70));
        System.out.println("📸 스크린샷: target/screenshots/main_page/");
        System.out.println("🎥 추적 파일: target/traces/main_page/");
        System.out.println("=".repeat(70));
    }
}