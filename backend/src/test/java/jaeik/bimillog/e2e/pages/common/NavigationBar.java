package jaeik.bimillog.e2e.pages.common;

import com.microsoft.playwright.Page;

/**
 * 네비게이션 바 컴포넌트
 * 공통 헤더 네비게이션 기능
 */
public class NavigationBar extends BasePage {

    // 셀렉터 정의
    private static final String LOGO = "[data-testid='logo']";
    private static final String HOME_LINK = "[data-testid='nav-home']";
    private static final String BOARD_LINK = "[data-testid='nav-board']";
    private static final String ROLLING_PAPER_LINK = "[data-testid='nav-rolling-paper']";
    private static final String VISIT_LINK = "[data-testid='nav-visit']";
    private static final String SUGGEST_LINK = "[data-testid='nav-suggest']";
    private static final String USER_MENU = "[data-testid='user-menu']";
    private static final String LOGIN_BUTTON = "[data-testid='login-button']";
    private static final String NOTIFICATION_ICON = "[data-testid='notification-icon']";
    private static final String NOTIFICATION_BADGE = "[data-testid='notification-badge']";

    // 모바일 메뉴
    private static final String MOBILE_MENU_BUTTON = "[data-testid='mobile-menu-button']";
    private static final String MOBILE_MENU = "[data-testid='mobile-menu']";

    public NavigationBar(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    @Override
    public boolean isLoaded() {
        return isVisible(LOGO);
    }

    // 네비게이션 메서드
    public void goToHome() {
        click(HOME_LINK);
    }

    public void goToBoard() {
        click(BOARD_LINK);
    }

    public void goToRollingPaper() {
        click(ROLLING_PAPER_LINK);
    }

    public void goToVisit() {
        click(VISIT_LINK);
    }

    public void goToSuggest() {
        click(SUGGEST_LINK);
    }

    // 사용자 메뉴
    public boolean isLoggedIn() {
        return isVisible(USER_MENU);
    }

    public void openUserMenu() {
        if (isLoggedIn()) {
            click(USER_MENU);
        }
    }

    public void goToMyPage() {
        openUserMenu();
        clickText("마이페이지");
    }

    public void goToSettings() {
        openUserMenu();
        clickText("설정");
    }

    public void goToAdmin() {
        openUserMenu();
        clickText("관리자");
    }

    public void logout() {
        openUserMenu();
        clickText("로그아웃");
    }

    public void clickLogin() {
        if (!isLoggedIn()) {
            click(LOGIN_BUTTON);
        }
    }

    // 알림
    public boolean hasNotifications() {
        return isVisible(NOTIFICATION_BADGE);
    }

    public void openNotifications() {
        click(NOTIFICATION_ICON);
    }

    public int getNotificationCount() {
        if (hasNotifications()) {
            String badgeText = getText(NOTIFICATION_BADGE);
            try {
                return Integer.parseInt(badgeText);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // 모바일 메뉴
    public void openMobileMenu() {
        if (isVisible(MOBILE_MENU_BUTTON)) {
            click(MOBILE_MENU_BUTTON);
            waitFor(MOBILE_MENU);
        }
    }

    public void closeMobileMenu() {
        if (isVisible(MOBILE_MENU)) {
            pressEscape();
            waitForHidden(MOBILE_MENU);
        }
    }

    public boolean isMobileView() {
        return isVisible(MOBILE_MENU_BUTTON);
    }

    // 검색 (추후 구현 시)
    public void search(String query) {
        String searchInput = "[data-testid='search-input']";
        if (isVisible(searchInput)) {
            fill(searchInput, query);
            pressEnter();
        }
    }

    // 현재 페이지 확인
    public boolean isOnHomePage() {
        return getUrl().equals(baseUrl + "/");
    }

    public boolean isOnBoardPage() {
        return getUrl().contains("/board");
    }

    public boolean isOnRollingPaperPage() {
        return getUrl().contains("/rolling-paper");
    }

    public String getCurrentUsername() {
        if (isLoggedIn()) {
            openUserMenu();
            String username = getText("[data-testid='username-display']");
            pressEscape(); // 메뉴 닫기
            return username;
        }
        return null;
    }
}