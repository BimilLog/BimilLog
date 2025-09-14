package jaeik.bimillog.e2e.pages.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 메인 페이지 Page Object Model
 * 
 * 메인 페이지의 모든 요소와 상호작용을 관리합니다.
 * - Hero 섹션 (환영 메시지, CTA 버튼)
 * - Features 섹션 (4가지 주요 기능)
 * - Footer 섹션 (링크, 정책)
 * - 광고 배너
 */
public class HomePage extends BasePage {

    // Selectors - Playwright의 올바른 selector 문법 사용
    private static final String HERO_TITLE = "h1:text('익명으로 마음을 전해보세요')";
    private static final String HERO_SUBTITLE = "text=/비밀로그에서 소중한 사람에게/";

    // CTA 버튼들 - 링크 선택자 수정
    private static final String START_BUTTON = "text='내 롤링페이퍼 만들기'";
    private static final String VISIT_BUTTON = "text='다른 롤링페이퍼 방문하기'";
    private static final String KAKAO_FRIENDS_BUTTON = "button:text('카카오 친구 확인하기')";
    private static final String KAKAO_SHARE_BUTTON = "button:text('카카오톡으로 공유')";

    // Features 섹션
    private static final String FEATURES_SECTION = "h2:text('비밀로그의 특별한 기능들')";
    private static final String FEATURE_CARDS = ".grid > div";  // 실제 구조에 맞게 수정

    // Footer
    private static final String FOOTER = "footer";
    private static final String FOOTER_LOGO = "footer h2:text('비밀로그')";
    private static final String FOOTER_VERSION = "footer span:text('v2.0.0')";

    // 광고
    private static final String TOP_AD_BANNER = "ins.kakao_ad_area";
    private static final String MOBILE_AD_BANNER = "ins.kakao_ad_area";

    // 모달
    private static final String KAKAO_FRIENDS_MODAL = "[role='dialog']";
    
    public HomePage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    @Override
    public boolean isLoaded() {
        try {
            // Hero 섹션이 표시되면 페이지가 로드된 것으로 간주
            page.waitForSelector(HERO_TITLE, new Page.WaitForSelectorOptions()
                .setTimeout(10000)
                .setState(WaitForSelectorState.VISIBLE));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Navigation
    public void navigateToHome() {
        navigate("/");
        waitForPageLoad();
    }

    // Hero Section Methods
    public boolean isHeroSectionVisible() {
        return isVisible(HERO_TITLE) && isVisible(HERO_SUBTITLE);
    }

    public String getHeroTitle() {
        return getText(HERO_TITLE);
    }

    public String getHeroSubtitle() {
        return getText(HERO_SUBTITLE);
    }

    // CTA Button Methods
    public boolean isStartButtonVisible() {
        return isVisible(START_BUTTON);
    }

    public void clickStartButton() {
        click(START_BUTTON);
    }

    public boolean isVisitButtonVisible() {
        return isVisible(VISIT_BUTTON);
    }

    public void clickVisitButton() {
        click(VISIT_BUTTON);
    }

    public boolean isKakaoFriendsButtonVisible() {
        return isVisible(KAKAO_FRIENDS_BUTTON);
    }

    public void clickKakaoFriendsButton() {
        click(KAKAO_FRIENDS_BUTTON);
    }

    public boolean isKakaoShareButtonVisible() {
        return isVisible(KAKAO_SHARE_BUTTON);
    }

    // Features Section Methods
    public boolean isFeaturesSectionVisible() {
        return isVisible(FEATURES_SECTION);
    }

    public int getFeatureCardsCount() {
        return getElementCount(FEATURE_CARDS);
    }

    public boolean hasFeatureWithTitle(String title) {
        String selector = String.format("h3:text('%s')", title);
        return isVisible(selector);
    }

    public boolean hasAllFeatures() {
        return hasFeatureWithTitle("익명 메시지") &&
               hasFeatureWithTitle("카카오 연동") &&
               hasFeatureWithTitle("다양한 디자인") &&
               hasFeatureWithTitle("커뮤니티");
    }

    // Footer Methods
    public boolean isFooterVisible() {
        return isVisible(FOOTER);
    }

    public boolean hasFooterLogo() {
        return isVisible(FOOTER_LOGO);
    }

    public boolean hasFooterVersion() {
        return isVisible(FOOTER_VERSION);
    }

    public boolean hasFooterLink(String linkText) {
        String selector = String.format("footer a:text('%s')", linkText);
        return isVisible(selector);
    }

    public void clickFooterLink(String linkText) {
        String selector = String.format("footer a:text('%s')", linkText);
        click(selector);
    }

    public boolean hasAllPolicyLinks() {
        return hasFooterLink("개인정보처리방침") && hasFooterLink("이용약관");
    }

    // Advertisement Methods
    public boolean isTopAdBannerVisible() {
        try {
            // 광고는 동적으로 로드되므로 짧은 대기 시간 설정
            page.waitForSelector(TOP_AD_BANNER, new Page.WaitForSelectorOptions()
                .setTimeout(3000)
                .setState(WaitForSelectorState.VISIBLE));
            return true;
        } catch (Exception e) {
            // 광고가 없거나 로드 실패해도 테스트는 계속 진행
            return false;
        }
    }

    public boolean isMobileAdBannerVisible() {
        try {
            page.waitForSelector(MOBILE_AD_BANNER, new Page.WaitForSelectorOptions()
                .setTimeout(3000)
                .setState(WaitForSelectorState.VISIBLE));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Modal Methods
    public boolean isKakaoFriendsModalOpen() {
        return isVisible(KAKAO_FRIENDS_MODAL);
    }

    public void closeKakaoFriendsModal() {
        if (isKakaoFriendsModalOpen()) {
            // ESC 키로 모달 닫기
            pressEscape();
            waitForHidden(KAKAO_FRIENDS_MODAL);
        }
    }

    // Protocol URL Navigation
    public void navigateWithProtocolUrl(String path) {
        String protocolUrl = "/?url=" + java.net.URLEncoder.encode("web+bimillog://" + path, java.nio.charset.StandardCharsets.UTF_8);
        navigate(protocolUrl);
    }

    // Responsive Design Checks
    public boolean isMobileView() {
        // 햄버거 메뉴가 보이면 모바일 뷰
        return isVisible("button[aria-label*='menu'], button:has(svg.lucide-menu)");
    }

    public boolean isDesktopView() {
        // 전체 네비게이션이 보이면 데스크톱 뷰
        return isVisible("nav.hidden.lg\\:flex");
    }

    public boolean isTabletView() {
        // 중간 크기 네비게이션이 보이면 태블릿 뷰
        return isVisible("nav.hidden.md\\:flex.lg\\:hidden");
    }

    // Button Layout Checks for Different Views
    public boolean hasCorrectButtonLayoutForDesktop() {
        // 데스크톱에서는 버튼들이 가로로 배치
        return page.locator(".hidden.sm\\:flex.flex-row").isVisible();
    }

    public boolean hasCorrectButtonLayoutForMobile() {
        // 모바일에서는 버튼들이 세로로 배치
        return page.locator(".flex.flex-col.gap-4").isVisible();
    }

    // PWA Install Prompt Check
    public boolean hasPWAInstallPrompt() {
        // PWA 설치 프롬프트 관련 요소 확인
        return page.evaluate("() => 'BeforeInstallPromptEvent' in window").toString().equals("true");
    }

    // Helper Methods
    private void waitForPageLoad() {
        page.waitForLoadState();
        // Hero 섹션이 보일 때까지 대기
        waitFor(HERO_TITLE);
    }

    // User State Checks
    public boolean isUserAuthenticated() {
        // 로그인 상태는 카카오 친구 확인 버튼의 존재로 판단
        return isKakaoFriendsButtonVisible();
    }

    public boolean hasAuthenticatedUserElements() {
        return isKakaoFriendsButtonVisible() || isKakaoShareButtonVisible();
    }

    public boolean hasNonAuthenticatedUserElements() {
        return isStartButtonVisible() && !isKakaoFriendsButtonVisible();
    }
}