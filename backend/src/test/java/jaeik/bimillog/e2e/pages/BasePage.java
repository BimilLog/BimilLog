package jaeik.bimillog.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * <h2>베이스 페이지 객체</h2>
 * <p>모든 페이지 객체의 기본 클래스입니다.</p>
 * <p>공통 요소 접근, 대기 처리, 유틸리티 메소드 제공</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public abstract class BasePage {
    protected final Page page;
    protected final String baseUrl;
    protected static final int DEFAULT_TIMEOUT = 30000;
    
    public BasePage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }
    
    /**
     * <h3>페이지 이동</h3>
     * <p>지정된 경로로 페이지를 이동합니다.</p>
     *
     * @param path 이동할 경로
     * @author Jaeik
     * @since 2.0.0
     */
    public void navigate(String path) {
        page.navigate(baseUrl + path);
        waitForPageLoad();
    }
    
    /**
     * <h3>페이지 로드 대기</h3>
     * <p>페이지가 완전히 로드될 때까지 대기합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void waitForPageLoad() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
    
    /**
     * <h3>요소 클릭</h3>
     * <p>지정된 선택자의 요소를 클릭합니다.</p>
     *
     * @param selector CSS 선택자
     * @author Jaeik
     * @since 2.0.0
     */
    protected void click(String selector) {
        waitForElement(selector).click();
    }
    
    /**
     * <h3>텍스트로 클릭</h3>
     * <p>지정된 텍스트를 포함하는 요소를 클릭합니다.</p>
     *
     * @param text 클릭할 텍스트
     * @author Jaeik
     * @since 2.0.0
     */
    protected void clickByText(String text) {
        page.locator("text=" + text).click();
    }
    
    /**
     * <h3>입력 필드 채우기</h3>
     * <p>입력 필드를 클리어하고 새 값을 입력합니다.</p>
     *
     * @param selector CSS 선택자
     * @param text 입력할 텍스트
     * @author Jaeik
     * @since 2.0.0
     */
    protected void fill(String selector, String text) {
        Locator element = waitForElement(selector);
        element.clear();
        element.fill(text);
    }
    
    /**
     * <h3>요소 대기</h3>
     * <p>요소가 표시될 때까지 대기합니다.</p>
     *
     * @param selector CSS 선택자
     * @return Locator 객체
     * @author Jaeik
     * @since 2.0.0
     */
    protected Locator waitForElement(String selector) {
        return waitForElement(selector, DEFAULT_TIMEOUT);
    }
    
    /**
     * <h3>요소 대기 (타임아웃 지정)</h3>
     * <p>지정된 시간 동안 요소가 표시될 때까지 대기합니다.</p>
     *
     * @param selector CSS 선택자
     * @param timeout 타임아웃 (밀리초)
     * @return Locator 객체
     * @author Jaeik
     * @since 2.0.0
     */
    protected Locator waitForElement(String selector, int timeout) {
        Locator element = page.locator(selector);
        element.waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(timeout));
        return element;
    }
    
    /**
     * <h3>텍스트 대기</h3>
     * <p>특정 텍스트가 표시될 때까지 대기합니다.</p>
     *
     * @param text 대기할 텍스트
     * @return Locator 객체
     * @author Jaeik
     * @since 2.0.0
     */
    protected Locator waitForText(String text) {
        return waitForElement("text=" + text);
    }
    
    /**
     * <h3>요소 존재 확인</h3>
     * <p>요소가 존재하는지 확인합니다.</p>
     *
     * @param selector CSS 선택자
     * @return 존재하면 true
     * @author Jaeik
     * @since 2.0.0
     */
    protected boolean isElementVisible(String selector) {
        return page.locator(selector).isVisible();
    }
    
    /**
     * <h3>텍스트 존재 확인</h3>
     * <p>특정 텍스트가 페이지에 존재하는지 확인합니다.</p>
     *
     * @param text 확인할 텍스트
     * @return 존재하면 true
     * @author Jaeik
     * @since 2.0.0
     */
    protected boolean isTextVisible(String text) {
        return page.locator("text=" + text).isVisible();
    }
    
    /**
     * <h3>요소 텍스트 가져오기</h3>
     * <p>요소의 텍스트 내용을 가져옵니다.</p>
     *
     * @param selector CSS 선택자
     * @return 텍스트 내용
     * @author Jaeik
     * @since 2.0.0
     */
    protected String getText(String selector) {
        return waitForElement(selector).textContent();
    }
    
    /**
     * <h3>셀렉트박스 선택</h3>
     * <p>드롭다운에서 옵션을 선택합니다.</p>
     *
     * @param selector CSS 선택자
     * @param value 선택할 값
     * @author Jaeik
     * @since 2.0.0
     */
    protected void selectOption(String selector, String value) {
        page.selectOption(selector, value);
    }
    
    /**
     * <h3>체크박스 체크</h3>
     * <p>체크박스를 체크합니다.</p>
     *
     * @param selector CSS 선택자
     * @author Jaeik
     * @since 2.0.0
     */
    protected void check(String selector) {
        page.check(selector);
    }
    
    /**
     * <h3>체크박스 언체크</h3>
     * <p>체크박스를 언체크합니다.</p>
     *
     * @param selector CSS 선택자
     * @author Jaeik
     * @since 2.0.0
     */
    protected void uncheck(String selector) {
        page.uncheck(selector);
    }
    
    /**
     * <h3>키보드 입력</h3>
     * <p>키보드 키를 입력합니다.</p>
     *
     * @param key 키 이름 (예: "Enter", "Escape")
     * @author Jaeik
     * @since 2.0.0
     */
    protected void pressKey(String key) {
        page.keyboard().press(key);
    }
    
    /**
     * <h3>스크롤</h3>
     * <p>페이지를 스크롤합니다.</p>
     *
     * @param direction 방향 ("up", "down", "top", "bottom")
     * @author Jaeik
     * @since 2.0.0
     */
    protected void scroll(String direction) {
        switch (direction.toLowerCase()) {
            case "up":
                page.evaluate("window.scrollBy(0, -500)");
                break;
            case "down":
                page.evaluate("window.scrollBy(0, 500)");
                break;
            case "top":
                page.evaluate("window.scrollTo(0, 0)");
                break;
            case "bottom":
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                break;
        }
    }
    
    /**
     * <h3>요소까지 스크롤</h3>
     * <p>특정 요소가 보일 때까지 스크롤합니다.</p>
     *
     * @param selector CSS 선택자
     * @author Jaeik
     * @since 2.0.0
     */
    protected void scrollToElement(String selector) {
        page.locator(selector).scrollIntoViewIfNeeded();
    }
    
    /**
     * <h3>페이지 제목 가져오기</h3>
     * <p>현재 페이지의 제목을 반환합니다.</p>
     *
     * @return 페이지 제목
     * @author Jaeik
     * @since 2.0.0
     */
    public String getTitle() {
        return page.title();
    }
    
    /**
     * <h3>현재 URL 가져오기</h3>
     * <p>현재 페이지의 URL을 반환합니다.</p>
     *
     * @return 현재 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String getCurrentUrl() {
        return page.url();
    }
    
    /**
     * <h3>대기</h3>
     * <p>지정된 시간 동안 대기합니다.</p>
     *
     * @param milliseconds 대기 시간 (밀리초)
     * @author Jaeik
     * @since 2.0.0
     */
    public void wait(int milliseconds) {
        page.waitForTimeout(milliseconds);
    }
}