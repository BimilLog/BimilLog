package jaeik.bimillog.e2e.pages.paper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import java.util.ArrayList;
import java.util.List;

/**
 * 롤링페이퍼 방문 페이지 객체
 * /visit 페이지 조작 및 검색, 최근 방문 기록 관리
 */
public class RollingPaperVisitPage {

    private final Page page;

    // 셀렉터 정의 - 실제 프론트엔드에 맞게 수정
    private static final String SEARCH_INPUT = "input[placeholder*='닉네임'], input[placeholder*='입력'], input[type='search']";
    private static final String SEARCH_BUTTON = "button:has-text('롤링페이퍼 방문하기'), button:has-text('검색'), button[type='submit']";
    private static final String LOADING_SPINNER = ".spinner, .loading, [aria-label='로딩']";
    private static final String ERROR_MESSAGE = ".error, [role='alert'], .text-red-500, .error-message";
    private static final String RECENT_VISITS_SECTION = "[class*='recent'], .recent-visits";
    private static final String RECENT_VISIT_ITEM = "[class*='recent-item'], .visit-item";
    private static final String DELETE_BUTTON = "button:has-text('삭제'), button[aria-label*='삭제']";
    private static final String DELETE_ALL_BUTTON = "button:has-text('전체 삭제'), button:has-text('모두 삭제')";
    private static final String SHARE_BUTTON = "button:has-text('공유'), button[aria-label*='공유']";
    private static final String CONFIRM_DIALOG = "[role='alertdialog'], .confirm-dialog";
    private static final String CONFIRM_BUTTON = "button:has-text('확인'), button:has-text('예')";
    private static final String CANCEL_BUTTON = "button:has-text('취소'), button:has-text('아니오')";
    private static final String INFO_SECTION = "[class*='info'], .guide-section";

    public RollingPaperVisitPage(Page page) {
        this.page = page;
    }

    /**
     * 페이지 로드 대기
     */
    public void waitForPageLoad() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector(SEARCH_INPUT, new Page.WaitForSelectorOptions().setTimeout(10000));
    }

    /**
     * 롤링페이퍼 방문 페이지로 이동
     */
    public void navigate(String baseUrl) {
        page.navigate(baseUrl + "/visit");
        waitForPageLoad();
    }

    /**
     * 닉네임 검색
     */
    public void searchNickname(String nickname) {
        Locator searchInput = page.locator(SEARCH_INPUT);
        searchInput.fill(nickname);
        page.waitForTimeout(500); // 디바운싱 대기
    }

    /**
     * Enter 키로 검색
     */
    public void searchByEnter(String nickname) {
        Locator searchInput = page.locator(SEARCH_INPUT);
        searchInput.fill(nickname);
        searchInput.press("Enter");
    }

    /**
     * 검색 버튼 클릭
     */
    public void clickSearchButton() {
        page.click(SEARCH_BUTTON);
    }

    /**
     * 로딩 스피너가 표시되는지 확인
     */
    public boolean isLoadingSpinnerVisible() {
        return page.locator(LOADING_SPINNER).isVisible();
    }

    /**
     * 로딩 완료 대기
     */
    public void waitForLoadingComplete() {
        if (isLoadingSpinnerVisible()) {
            page.waitForSelector(LOADING_SPINNER,
                new Page.WaitForSelectorOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                    .setTimeout(10000));
        }
    }

    /**
     * 에러 메시지가 표시되는지 확인
     */
    public boolean isErrorMessageVisible() {
        return page.locator(ERROR_MESSAGE).isVisible();
    }

    /**
     * 에러 메시지 텍스트 가져오기
     */
    public String getErrorMessage() {
        Locator error = page.locator(ERROR_MESSAGE);
        if (error.count() > 0) {
            return error.textContent();
        }
        return "";
    }

    /**
     * 최근 방문 섹션이 표시되는지 확인
     */
    public boolean isRecentVisitsSectionVisible() {
        return page.locator(RECENT_VISITS_SECTION).isVisible();
    }

    /**
     * 최근 방문 기록 개수
     */
    public int getRecentVisitsCount() {
        return page.locator(RECENT_VISIT_ITEM).count();
    }

    /**
     * 최근 방문 기록 목록 가져오기
     */
    public List<String> getRecentVisitsList() {
        List<String> visits = new ArrayList<>();
        Locator items = page.locator(RECENT_VISIT_ITEM);
        for (int i = 0; i < items.count(); i++) {
            visits.add(items.nth(i).textContent());
        }
        return visits;
    }

    /**
     * 특정 인덱스의 최근 방문 기록 클릭
     */
    public void clickRecentVisit(int index) {
        Locator items = page.locator(RECENT_VISIT_ITEM);
        if (items.count() > index) {
            items.nth(index).click();
        }
    }

    /**
     * 특정 닉네임의 최근 방문 기록 클릭
     */
    public void clickRecentVisitByNickname(String nickname) {
        Locator item = page.locator(RECENT_VISIT_ITEM + ":has-text('" + nickname + "')");
        if (item.count() > 0) {
            item.first().click();
        }
    }

    /**
     * 특정 인덱스의 최근 방문 기록 삭제
     */
    public void deleteRecentVisit(int index) {
        Locator items = page.locator(RECENT_VISIT_ITEM);
        if (items.count() > index) {
            Locator deleteBtn = items.nth(index).locator(DELETE_BUTTON);
            if (deleteBtn.count() > 0) {
                deleteBtn.click();
            }
        }
    }

    /**
     * 전체 최근 방문 기록 삭제
     */
    public void deleteAllRecentVisits() {
        Locator deleteAllBtn = page.locator(DELETE_ALL_BUTTON);
        if (deleteAllBtn.isVisible()) {
            deleteAllBtn.click();
        }
    }

    /**
     * 확인 다이얼로그가 표시되는지 확인
     */
    public boolean isConfirmDialogVisible() {
        return page.locator(CONFIRM_DIALOG).isVisible();
    }

    /**
     * 확인 다이얼로그에서 확인 클릭
     */
    public void clickConfirm() {
        if (isConfirmDialogVisible()) {
            page.click(CONFIRM_BUTTON);
        }
    }

    /**
     * 확인 다이얼로그에서 취소 클릭
     */
    public void clickCancel() {
        if (isConfirmDialogVisible()) {
            page.click(CANCEL_BUTTON);
        }
    }

    /**
     * 공유 버튼 클릭
     */
    public void clickShareButton() {
        page.click(SHARE_BUTTON);
    }

    /**
     * 안내 섹션이 표시되는지 확인
     */
    public boolean isInfoSectionVisible() {
        return page.locator(INFO_SECTION).isVisible();
    }

    /**
     * 페이지 제목 가져오기
     */
    public String getPageTitle() {
        Locator title = page.locator("h1, h2").first();
        return title.textContent();
    }

    /**
     * 실시간 검색 시뮬레이션 (디바운싱 테스트)
     */
    public void simulateRealtimeSearch(String text) {
        Locator searchInput = page.locator(SEARCH_INPUT);
        searchInput.fill("");

        // 한 글자씩 입력하며 디바운싱 테스트
        for (int i = 0; i < text.length(); i++) {
            searchInput.type(text.substring(i, i + 1));
            page.waitForTimeout(100); // 각 입력 사이 짧은 대기
        }

        page.waitForTimeout(1000); // 디바운싱 시간 대기
    }

    /**
     * 검색어 지우기
     */
    public void clearSearch() {
        Locator searchInput = page.locator(SEARCH_INPUT);
        searchInput.fill("");
    }

    /**
     * 로컬스토리지에서 최근 방문 기록 확인
     */
    public String getLocalStorageVisits() {
        return page.evaluate("() => localStorage.getItem('recentVisits') || '[]'").toString();
    }

    /**
     * 로컬스토리지 최근 방문 기록 삭제
     */
    public void clearLocalStorageVisits() {
        page.evaluate("() => localStorage.removeItem('recentVisits')");
    }

    /**
     * 검색 결과로 이동했는지 확인
     */
    public boolean isNavigatedToRollingPaper(String nickname) {
        String currentUrl = page.url();
        return currentUrl.contains("/rolling-paper/" + nickname);
    }

    /**
     * 토스트 메시지 확인
     */
    public boolean isToastVisible(String message) {
        Locator toast = page.locator(".toast, [role='alert']");
        if (toast.count() > 0) {
            return toast.textContent().contains(message);
        }
        return false;
    }

    /**
     * 검색 입력 필드가 포커스되어 있는지 확인
     */
    public boolean isSearchInputFocused() {
        return (boolean) page.locator(SEARCH_INPUT).evaluate("el => el === document.activeElement");
    }

    /**
     * 검색 입력 필드에 포커스
     */
    public void focusSearchInput() {
        page.locator(SEARCH_INPUT).focus();
    }
}