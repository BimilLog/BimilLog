package jaeik.bimillog.e2e.pages.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 네비게이션 컴포넌트 Page Object
 * 
 * 헤더 네비게이션 바의 모든 요소와 상호작용을 관리합니다.
 * - 로고
 * - 메뉴 아이템들 (게시판, 롤링페이퍼 방문, 건의하기 등)
 * - 사용자 프로필
 * - 알림 벨
 * - 모바일 햄버거 메뉴
 */
public class NavigationComponent {

    private final Page page;
    
    // Header Selectors
    private static final String HEADER = "header.border-b.bg-white\\/80";
    private static final String LOGO = "header img[alt='비밀로그']";
    
    // Navigation Menu Selectors - Desktop
    private static final String NAV_DESKTOP = "nav.hidden.lg\\:flex";
    private static final String NAV_TABLET = "nav.hidden.md\\:flex.lg\\:hidden";
    private static final String NAV_BOARD = "a:has-text('게시판')";
    private static final String NAV_VISIT = "a:has-text('다른 롤링페이퍼 방문')";
    private static final String NAV_MY_PAPER = "a:has-text('롤링페이퍼')";
    private static final String NAV_SUGGEST = "a:has-text('건의하기')";
    private static final String NAV_MYPAGE = "a:has-text('마이페이지')";
    private static final String NAV_ADMIN = "a:has-text('관리자')";
    private static final String NAV_LOGIN = "a:has-text('로그인')";
    
    // User Profile Selectors
    private static final String USER_AVATAR = "header [class*='avatar']";
    private static final String USER_NAME = "header span:has-text('님')";
    private static final String LOGOUT_BUTTON = "button:has-text('로그아웃')";
    private static final String SETTINGS_LINK = "a[title='설정']";
    
    // Notification Bell
    private static final String NOTIFICATION_BELL = "button[aria-label*='알림'], button:has(svg.lucide-bell)";
    private static final String NOTIFICATION_BADGE = ".absolute.top-0.right-0.bg-red-500";
    private static final String NOTIFICATION_DROPDOWN = "[role='menu']:has-text('알림')";
    
    // Mobile Navigation
    private static final String MOBILE_MENU_BUTTON = "button[aria-label*='menu'], button:has(svg.lucide-menu)";
    private static final String MOBILE_MENU_PANEL = "[data-state='open']:has(a:has-text('게시판'))";
    private static final String MOBILE_MENU_CLOSE = "button[aria-label*='닫기'], button:has(svg.lucide-x)";
    
    public NavigationComponent(Page page) {
        this.page = page;
    }
    
    // Logo Methods
    public boolean isLogoVisible() {
        return page.locator(LOGO).isVisible();
    }
    
    public void clickLogo() {
        page.locator(LOGO).click();
    }
    
    // Navigation State Checks
    public boolean isDesktopNavigationVisible() {
        return page.locator(NAV_DESKTOP).isVisible();
    }
    
    public boolean isTabletNavigationVisible() {
        return page.locator(NAV_TABLET).isVisible();
    }
    
    public boolean isMobileNavigationVisible() {
        return page.locator(MOBILE_MENU_BUTTON).isVisible();
    }
    
    // Menu Item Visibility - Non-authenticated
    public boolean hasNonAuthenticatedMenuItems() {
        return page.locator(NAV_BOARD).isVisible() &&
               page.locator(NAV_VISIT).isVisible() &&
               page.locator(NAV_SUGGEST).isVisible() &&
               page.locator(NAV_LOGIN).isVisible();
    }
    
    // Menu Item Visibility - Authenticated
    public boolean hasAuthenticatedMenuItems() {
        return page.locator(NAV_BOARD).isVisible() &&
               page.locator(NAV_VISIT).isVisible() &&
               page.locator(NAV_MY_PAPER).isVisible() &&
               page.locator(NAV_SUGGEST).isVisible() &&
               page.locator(NAV_MYPAGE).isVisible() &&
               !page.locator(NAV_LOGIN).isVisible();
    }
    
    // Admin Menu Check
    public boolean hasAdminMenuItem() {
        return page.locator(NAV_ADMIN).isVisible();
    }
    
    // Navigation Actions
    public void navigateToBoard() {
        page.locator(NAV_BOARD).first().click();
        page.waitForLoadState();
    }
    
    public void navigateToVisit() {
        page.locator(NAV_VISIT).first().click();
        page.waitForLoadState();
    }
    
    public void navigateToMyPaper() {
        page.locator(NAV_MY_PAPER).first().click();
        page.waitForLoadState();
    }
    
    public void navigateToSuggest() {
        page.locator(NAV_SUGGEST).first().click();
        page.waitForLoadState();
    }
    
    public void navigateToMyPage() {
        page.locator(NAV_MYPAGE).first().click();
        page.waitForLoadState();
    }
    
    public void navigateToAdmin() {
        page.locator(NAV_ADMIN).first().click();
        page.waitForLoadState();
    }
    
    public void navigateToLogin() {
        page.locator(NAV_LOGIN).first().click();
        page.waitForLoadState();
    }
    
    // User Profile Methods
    public boolean isUserAvatarVisible() {
        return page.locator(USER_AVATAR).isVisible();
    }
    
    public boolean isUserNameVisible() {
        return page.locator(USER_NAME).isVisible();
    }
    
    public String getUserName() {
        Locator userNameElement = page.locator(USER_NAME);
        if (userNameElement.isVisible()) {
            String text = userNameElement.textContent();
            return text.replace("님", "").trim();
        }
        return null;
    }
    
    public void clickUserAvatar() {
        page.locator(USER_AVATAR).click();
    }
    
    public boolean isLogoutButtonVisible() {
        return page.locator(LOGOUT_BUTTON).isVisible();
    }
    
    public void clickLogout() {
        page.locator(LOGOUT_BUTTON).click();
        page.waitForLoadState();
    }
    
    public boolean isSettingsLinkVisible() {
        return page.locator(SETTINGS_LINK).isVisible();
    }
    
    public void clickSettings() {
        page.locator(SETTINGS_LINK).click();
        page.waitForLoadState();
    }
    
    // Notification Bell Methods
    public boolean isNotificationBellVisible() {
        return page.locator(NOTIFICATION_BELL).isVisible();
    }
    
    public boolean hasNotificationBadge() {
        return page.locator(NOTIFICATION_BADGE).isVisible();
    }
    
    public int getNotificationCount() {
        try {
            Locator badge = page.locator(NOTIFICATION_BADGE);
            if (badge.isVisible()) {
                String text = badge.textContent();
                return Integer.parseInt(text.replaceAll("[^0-9]", ""));
            }
        } catch (Exception e) {
            // 배지가 없거나 숫자를 파싱할 수 없는 경우
        }
        return 0;
    }
    
    public void clickNotificationBell() {
        page.locator(NOTIFICATION_BELL).click();
        // 드롭다운이 열릴 때까지 대기
        page.waitForSelector(NOTIFICATION_DROPDOWN, new Page.WaitForSelectorOptions()
            .setTimeout(3000)
            .setState(WaitForSelectorState.VISIBLE));
    }
    
    public boolean isNotificationDropdownOpen() {
        return page.locator(NOTIFICATION_DROPDOWN).isVisible();
    }
    
    public void closeNotificationDropdown() {
        if (isNotificationDropdownOpen()) {
            page.keyboard().press("Escape");
            page.waitForSelector(NOTIFICATION_DROPDOWN, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN));
        }
    }
    
    // Mobile Navigation Methods
    public boolean isMobileMenuButtonVisible() {
        return page.locator(MOBILE_MENU_BUTTON).isVisible();
    }
    
    public void openMobileMenu() {
        if (isMobileMenuButtonVisible()) {
            page.locator(MOBILE_MENU_BUTTON).click();
            // 모바일 메뉴 패널이 열릴 때까지 대기
            page.waitForSelector(MOBILE_MENU_PANEL, new Page.WaitForSelectorOptions()
                .setTimeout(3000)
                .setState(WaitForSelectorState.VISIBLE));
        }
    }
    
    public boolean isMobileMenuOpen() {
        return page.locator(MOBILE_MENU_PANEL).isVisible();
    }
    
    public void closeMobileMenu() {
        if (isMobileMenuOpen()) {
            Locator closeButton = page.locator(MOBILE_MENU_CLOSE);
            if (closeButton.isVisible()) {
                closeButton.click();
            } else {
                // ESC 키로 닫기 시도
                page.keyboard().press("Escape");
            }
            page.waitForSelector(MOBILE_MENU_PANEL, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN));
        }
    }
    
    // Mobile Menu Navigation
    public void navigateToBoardMobile() {
        openMobileMenu();
        page.locator(MOBILE_MENU_PANEL + " a:has-text('게시판')").click();
        page.waitForLoadState();
    }
    
    public void navigateToVisitMobile() {
        openMobileMenu();
        page.locator(MOBILE_MENU_PANEL + " a:has-text('다른 롤링페이퍼 방문')").click();
        page.waitForLoadState();
    }
    
    public void navigateToMyPaperMobile() {
        openMobileMenu();
        page.locator(MOBILE_MENU_PANEL + " a:has-text('롤링페이퍼')").click();
        page.waitForLoadState();
    }
    
    // Authentication State Check
    public boolean isUserAuthenticated() {
        return isUserAvatarVisible() || isLogoutButtonVisible();
    }
    
    public boolean isUserNotAuthenticated() {
        return page.locator(NAV_LOGIN).isVisible();
    }
    
    // Sticky Header Check
    public boolean isHeaderSticky() {
        String position = page.locator(HEADER).evaluate("el => window.getComputedStyle(el).position").toString();
        return "sticky".equals(position) || "fixed".equals(position);
    }
    
    // Responsive Breakpoint Checks
    public String getCurrentNavigationType() {
        if (isDesktopNavigationVisible()) {
            return "desktop";
        } else if (isTabletNavigationVisible()) {
            return "tablet";
        } else if (isMobileMenuButtonVisible()) {
            return "mobile";
        }
        return "unknown";
    }
    
    // Wait for Navigation Load
    public void waitForNavigationLoad() {
        page.waitForSelector(HEADER, new Page.WaitForSelectorOptions()
            .setTimeout(5000)
            .setState(WaitForSelectorState.VISIBLE));
    }
}