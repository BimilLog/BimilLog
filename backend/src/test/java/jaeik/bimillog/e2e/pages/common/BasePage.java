package jaeik.bimillog.e2e.pages.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Page Object Model 베이스 클래스
 * 모든 페이지 객체가 상속받는 기본 클래스
 */
public abstract class BasePage {

    protected final Page page;
    protected final String baseUrl;
    protected static final int DEFAULT_TIMEOUT = 30000;

    public BasePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    // 페이지 네비게이션
    public void navigate(String path) {
        page.navigate(baseUrl + path);
        page.waitForLoadState();
    }

    // 요소 찾기
    protected Locator find(String selector) {
        return page.locator(selector);
    }

    protected Locator findByText(String text) {
        return page.locator("text=" + text);
    }

    protected Locator findByTestId(String testId) {
        return page.locator("[data-testid='" + testId + "']");
    }

    // 대기 메서드
    protected Locator waitFor(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(DEFAULT_TIMEOUT));
        return find(selector);
    }

    protected void waitForText(String text) {
        page.waitForSelector("text=" + text, new Page.WaitForSelectorOptions()
            .setTimeout(DEFAULT_TIMEOUT));
    }

    protected void waitForHidden(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.HIDDEN)
            .setTimeout(DEFAULT_TIMEOUT));
    }

    // 상호작용 메서드
    protected void click(String selector) {
        waitFor(selector).click();
    }

    protected void clickText(String text) {
        findByText(text).click();
    }

    protected void fill(String selector, String value) {
        Locator element = waitFor(selector);
        element.clear();
        element.fill(value);
    }

    protected void type(String selector, String value) {
        waitFor(selector).type(value);
    }

    protected void selectOption(String selector, String value) {
        waitFor(selector).selectOption(value);
    }

    protected void check(String selector) {
        waitFor(selector).check();
    }

    protected void uncheck(String selector) {
        waitFor(selector).uncheck();
    }

    // 검증 메서드
    protected boolean isVisible(String selector) {
        try {
            return find(selector).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean hasText(String text) {
        try {
            return findByText(text).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    protected String getText(String selector) {
        return waitFor(selector).textContent();
    }

    protected String getValue(String selector) {
        return waitFor(selector).inputValue();
    }

    protected int getElementCount(String selector) {
        return find(selector).count();
    }

    // 스크롤
    protected void scrollToElement(String selector) {
        waitFor(selector).scrollIntoViewIfNeeded();
    }

    protected void scrollToBottom() {
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
    }

    protected void scrollToTop() {
        page.evaluate("window.scrollTo(0, 0)");
    }

    // 키보드 액션
    protected void pressEnter() {
        page.keyboard().press("Enter");
    }

    protected void pressEscape() {
        page.keyboard().press("Escape");
    }

    protected void pressTab() {
        page.keyboard().press("Tab");
    }

    // 페이지 상태
    public String getUrl() {
        return page.url();
    }

    public String getTitle() {
        return page.title();
    }

    public void reload() {
        page.reload();
    }

    public void goBack() {
        page.goBack();
    }

    public void goForward() {
        page.goForward();
    }

    // 대화상자 처리
    protected void acceptAlert() {
        page.onceDialog(dialog -> dialog.accept());
    }

    protected void dismissAlert() {
        page.onceDialog(dialog -> dialog.dismiss());
    }

    protected void acceptConfirm(String expectedMessage) {
        page.onceDialog(dialog -> {
            if (dialog.message().contains(expectedMessage)) {
                dialog.accept();
            } else {
                dialog.dismiss();
            }
        });
    }

    // 파일 업로드
    protected void uploadFile(String selector, String filePath) {
        waitFor(selector).setInputFiles(java.nio.file.Paths.get(filePath));
    }

    // 추상 메서드 - 각 페이지에서 구현
    public abstract boolean isLoaded();
}