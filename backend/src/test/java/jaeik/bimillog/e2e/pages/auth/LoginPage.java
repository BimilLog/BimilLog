package jaeik.bimillog.e2e.pages.auth;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 로그인 페이지 Page Object
 *
 * 시나리오 2: 인증 관련 페이지 테스트
 * - 카카오 로그인 버튼 클릭
 * - 에러 메시지 확인
 * - 회원가입 링크 이동
 * - 뒤로가기 버튼 동작
 */
public class LoginPage {
    private final Page page;
    private static final int DEFAULT_TIMEOUT = 30000;

    // 페이지 요소 선택자
    private static final String KAKAO_LOGIN_BUTTON = "button:has-text('카카오로 시작하기'), button:has-text('카카오 로그인')";
    private static final String ERROR_MESSAGE = "div[class*='bg-red-50'] div[class*='text-red-800']";
    private static final String ERROR_TITLE = "p:has-text('로그인 오류')";
    private static final String INFO_MESSAGE = "div[class*='bg-blue-50']";
    private static final String SIGNUP_LINK = "a[href='/signup']";
    private static final String PAGE_TITLE = "text=로그인";
    private static final String PAGE_DESCRIPTION = "text=나만의 롤링페이퍼를 만들어 보세요";
    private static final String INFO_TEXT_NO_LOGIN = "text=로그인 없이도 이용 가능!";
    private static final String NO_ACCOUNT_SIGNUP_LINK = "text=이미 계정이 있으신가요?";

    public LoginPage(Page page) {
        this.page = page;
    }

    /**
     * 로그인 페이지로 이동
     */
    public void navigate() {
        page.navigate("http://localhost:3000/login");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitForPageLoad();
    }

    /**
     * 페이지 로드 대기
     */
    public void waitForPageLoad() {
        page.waitForSelector(PAGE_TITLE, new Page.WaitForSelectorOptions()
            .setTimeout(DEFAULT_TIMEOUT)
            .setState(WaitForSelectorState.VISIBLE));
    }

    /**
     * 카카오 로그인 버튼 클릭
     */
    public void clickKakaoLoginButton() {
        Locator button = page.locator(KAKAO_LOGIN_BUTTON);
        button.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));
        button.click();
    }

    /**
     * 에러 메시지 확인
     */
    public boolean hasErrorMessage() {
        try {
            page.waitForSelector(ERROR_MESSAGE, new Page.WaitForSelectorOptions()
                .setTimeout(5000)
                .setState(WaitForSelectorState.VISIBLE));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 에러 메시지 텍스트 가져오기
     */
    public String getErrorMessage() {
        if (hasErrorMessage()) {
            return page.locator(ERROR_MESSAGE).textContent();
        }
        return null;
    }

    /**
     * 정보 메시지 확인 (로그인 없이도 이용 가능)
     */
    public boolean hasInfoMessage() {
        return page.locator(INFO_MESSAGE).isVisible();
    }

    /**
     * 회원가입 링크 클릭
     */
    public void clickSignupLink() {
        page.locator(SIGNUP_LINK).click();
        page.waitForURL("**/signup");
    }

    /**
     * 페이지 제목 확인
     */
    public boolean hasCorrectTitle() {
        return page.locator(PAGE_TITLE).isVisible() &&
               page.locator(PAGE_DESCRIPTION).isVisible();
    }

    /**
     * 카카오 로그인 버튼 존재 확인
     */
    public boolean hasKakaoLoginButton() {
        return page.locator(KAKAO_LOGIN_BUTTON).isVisible();
    }

    /**
     * URL에서 에러 파라미터 확인
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
     * 에러 메시지가 자동으로 사라지는지 확인 (5초 후)
     */
    public boolean waitForErrorToClear(int timeoutMs) {
        try {
            Thread.sleep(timeoutMs);
            return !hasErrorMessage();
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 뒤로가기 버튼 동작 테스트
     */
    public void goBack() {
        page.goBack();
    }

    /**
     * 페이지 새로고침
     */
    public void refresh() {
        page.reload();
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
     * 페이지 스크린샷 캡처
     */
    public void takeScreenshot(String filename) {
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(java.nio.file.Paths.get("target/screenshots/" + filename + ".png"))
            .setFullPage(true));
    }

    /**
     * 이미 로그인된 상태인지 확인 (홈으로 리다이렉트 되는지)
     */
    public boolean isRedirectedToHome() {
        try {
            page.waitForURL("**/", new Page.WaitForURLOptions()
                .setTimeout(5000));
            return page.url().equals("http://localhost:3000/");
        } catch (Exception e) {
            return false;
        }
    }
}