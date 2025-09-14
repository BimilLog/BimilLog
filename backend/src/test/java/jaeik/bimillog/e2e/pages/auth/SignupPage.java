package jaeik.bimillog.e2e.pages.auth;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 회원가입 페이지 Page Object
 *
 * 시나리오 2: 회원가입 페이지 테스트
 * - 카카오 인증 후 닉네임 설정
 * - 닉네임 유효성 검사 (2-20자 한글/영문/숫자)
 * - 실시간 중복 체크
 * - 이용약관 동의
 * - 회원가입 완료
 */
public class SignupPage {
    private final Page page;
    private static final int DEFAULT_TIMEOUT = 30000;

    // 페이지 요소 선택자
    private static final String PAGE_TITLE = "text=회원가입";
    private static final String PAGE_DESCRIPTION = "text=나만의 롤링페이퍼를 만들어보세요";
    private static final String KAKAO_START_BUTTON = "button:has-text('카카오로 시작하기')";
    private static final String LOGIN_LINK = "a[href='/login']";

    // 닉네임 설정 폼 요소
    private static final String NICKNAME_INPUT = "input[placeholder*='닉네임']";
    private static final String NICKNAME_ERROR = "p.text-red-500";
    private static final String NICKNAME_SUCCESS = "p.text-green-500";
    private static final String TERMS_CHECKBOX = "input[type='checkbox'][id*='terms']";
    private static final String PRIVACY_CHECKBOX = "input[type='checkbox'][id*='privacy']";
    private static final String ALL_AGREE_CHECKBOX = "input[type='checkbox']:has(~label:has-text('모두 동의'))";
    private static final String SUBMIT_BUTTON = "button:has-text('가입하기')";
    private static final String TERMS_LINK = "a:has-text('이용약관')";
    private static final String PRIVACY_LINK = "a:has-text('개인정보처리방침')";

    // 닉네임 설정 페이지 특정 요소
    private static final String NICKNAME_SETUP_TITLE = "text=닉네임 설정";
    private static final String NICKNAME_SETUP_DESC = "text=비밀로그에서 사용할 닉네임을 입력해주세요";

    public SignupPage(Page page) {
        this.page = page;
    }

    /**
     * 회원가입 페이지로 이동
     */
    public void navigate() {
        page.navigate("http://localhost:3000/signup");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitForPageLoad();
    }

    /**
     * 닉네임 설정 페이지로 이동 (UUID 포함)
     */
    public void navigateToNicknameSetup(String uuid) {
        page.navigate("http://localhost:3000/signup?required=true&uuid=" + uuid);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitForNicknameSetupLoad();
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
     * 닉네임 설정 페이지 로드 대기
     */
    public void waitForNicknameSetupLoad() {
        page.waitForSelector(NICKNAME_SETUP_TITLE, new Page.WaitForSelectorOptions()
            .setTimeout(DEFAULT_TIMEOUT)
            .setState(WaitForSelectorState.VISIBLE));
    }

    /**
     * 카카오로 시작하기 버튼 클릭
     */
    public void clickKakaoStartButton() {
        page.locator(KAKAO_START_BUTTON).click();
    }

    /**
     * 로그인 링크 클릭
     */
    public void clickLoginLink() {
        page.locator(LOGIN_LINK).click();
        page.waitForURL("**/login");
    }

    /**
     * 닉네임 입력
     */
    public void enterNickname(String nickname) {
        Locator input = page.locator(NICKNAME_INPUT);
        input.clear();
        input.fill(nickname);
        // 디바운싱을 위한 대기 (실시간 중복 체크)
        page.waitForTimeout(500);
    }

    /**
     * 닉네임 유효성 에러 메시지 확인
     */
    public boolean hasNicknameError() {
        return page.locator(NICKNAME_ERROR).isVisible();
    }

    /**
     * 닉네임 에러 메시지 텍스트 가져오기
     */
    public String getNicknameError() {
        if (hasNicknameError()) {
            return page.locator(NICKNAME_ERROR).textContent();
        }
        return null;
    }

    /**
     * 닉네임 사용 가능 메시지 확인
     */
    public boolean hasNicknameSuccess() {
        return page.locator(NICKNAME_SUCCESS).isVisible();
    }

    /**
     * 닉네임 성공 메시지 텍스트 가져오기
     */
    public String getNicknameSuccess() {
        if (hasNicknameSuccess()) {
            return page.locator(NICKNAME_SUCCESS).textContent();
        }
        return null;
    }

    /**
     * 이용약관 체크
     */
    public void checkTerms() {
        page.locator(TERMS_CHECKBOX).check();
    }

    /**
     * 개인정보처리방침 체크
     */
    public void checkPrivacy() {
        page.locator(PRIVACY_CHECKBOX).check();
    }

    /**
     * 모두 동의 체크
     */
    public void checkAllAgree() {
        Locator checkbox = page.locator(ALL_AGREE_CHECKBOX);
        if (checkbox.isVisible()) {
            checkbox.check();
        } else {
            // 모두 동의 체크박스가 없으면 개별 체크
            checkTerms();
            checkPrivacy();
        }
    }

    /**
     * 가입하기 버튼 클릭
     */
    public void clickSubmitButton() {
        page.locator(SUBMIT_BUTTON).click();
    }

    /**
     * 가입하기 버튼 활성화 상태 확인
     */
    public boolean isSubmitButtonEnabled() {
        return page.locator(SUBMIT_BUTTON).isEnabled();
    }

    /**
     * 회원가입 완료 후 홈으로 리다이렉트 확인
     */
    public boolean isRedirectedToHome() {
        try {
            page.waitForURL("http://localhost:3000/", new Page.WaitForURLOptions()
                .setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 이용약관 링크 클릭
     */
    public void clickTermsLink() {
        page.locator(TERMS_LINK).click();
    }

    /**
     * 개인정보처리방침 링크 클릭
     */
    public void clickPrivacyLink() {
        page.locator(PRIVACY_LINK).click();
    }

    /**
     * 닉네임 입력 필드 존재 확인
     */
    public boolean hasNicknameInput() {
        return page.locator(NICKNAME_INPUT).isVisible();
    }

    /**
     * 약관 체크박스 존재 확인
     */
    public boolean hasTermsCheckboxes() {
        return page.locator(TERMS_CHECKBOX).isVisible() &&
               page.locator(PRIVACY_CHECKBOX).isVisible();
    }

    /**
     * 유효하지 않은 UUID 에러 확인
     */
    public boolean hasInvalidUuidError() {
        return page.locator("text=회원가입 오류").isVisible() ||
               page.locator("text=유효하지 않은 회원가입 링크").isVisible();
    }

    /**
     * 이미 로그인된 사용자 확인 (홈으로 리다이렉트)
     */
    public boolean isAlreadyLoggedIn() {
        String currentUrl = page.url();
        return currentUrl.equals("http://localhost:3000/") &&
               !currentUrl.contains("signup");
    }

    /**
     * 닉네임 실시간 중복 체크 대기
     */
    public void waitForDuplicateCheck() {
        page.waitForTimeout(1000); // 디바운싱 대기
        page.waitForSelector("p.text-red-500, p.text-green-500", new Page.WaitForSelectorOptions()
            .setTimeout(5000)
            .setState(WaitForSelectorState.VISIBLE));
    }

    /**
     * 닉네임 형식 테스트 (특수문자 등)
     */
    public void testInvalidNicknameFormat(String invalidNickname) {
        enterNickname(invalidNickname);
        page.waitForTimeout(500);
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
     * 브라우저 뒤로가기
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
}