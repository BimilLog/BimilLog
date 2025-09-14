package jaeik.bimillog.e2e.pages.board;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import jaeik.bimillog.e2e.pages.common.BasePage;

/**
 * 게시판 목록 페이지 Page Object
 */
public class BoardPage extends BasePage {

    // Selectors
    private static final String WRITE_BUTTON = "button:has-text('글쓰기')";
    private static final String SEARCH_INPUT = "input[placeholder*='검색']";
    private static final String SEARCH_BUTTON = "button:has-text('검색')";
    private static final String POST_LIST = "[class*='post-list'], table tbody tr";
    private static final String PAGINATION = "[class*='pagination']";
    private static final String TAB_ALL = "button:has-text('전체')";
    private static final String TAB_POPULAR = "button:has-text('인기글')";

    public BoardPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * 게시판 페이지로 이동
     */
    public void navigateToBoard() {
        navigate("/board");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitFor(POST_LIST);
    }

    /**
     * 글쓰기 버튼 클릭
     */
    public void clickWriteButton() {
        click(WRITE_BUTTON);
        page.waitForURL("**/board/write");
    }

    /**
     * 게시글 검색
     */
    public void searchPost(String keyword) {
        fill(SEARCH_INPUT, keyword);
        click(SEARCH_BUTTON);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 특정 제목의 게시글 클릭
     */
    public void clickPostByTitle(String title) {
        Locator postLink = page.locator("a:has-text('" + title + "')").first();
        if (postLink.count() == 0) {
            // 테이블 형태인 경우
            postLink = page.locator("tr:has-text('" + title + "') a").first();
        }
        postLink.click();
        page.waitForURL("**/board/post/**");
    }

    /**
     * 첫 번째 게시글 클릭
     */
    public void clickFirstPost() {
        Locator firstPost = page.locator(POST_LIST + " a").first();
        firstPost.click();
        page.waitForURL("**/board/post/**");
    }

    /**
     * 전체 탭 클릭
     */
    public void clickAllTab() {
        click(TAB_ALL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 인기글 탭 클릭
     */
    public void clickPopularTab() {
        click(TAB_POPULAR);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 게시글 목록이 표시되는지 확인
     */
    public boolean isPostListVisible() {
        return find(POST_LIST).isVisible();
    }

    /**
     * 게시글 개수 가져오기
     */
    public int getPostCount() {
        return page.locator(POST_LIST).count();
    }

    /**
     * 페이지네이션이 표시되는지 확인
     */
    public boolean isPaginationVisible() {
        return find(PAGINATION).isVisible();
    }

    /**
     * 다음 페이지로 이동
     */
    public void goToNextPage() {
        Locator nextButton = page.locator("button:has-text('다음'), a:has-text('>')").first();
        if (nextButton.isEnabled()) {
            nextButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }
    }

    /**
     * 이전 페이지로 이동
     */
    public void goToPreviousPage() {
        Locator prevButton = page.locator("button:has-text('이전'), a:has-text('<')").first();
        if (prevButton.isEnabled()) {
            prevButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
        }
    }

    /**
     * 특정 페이지 번호로 이동
     */
    public void goToPage(int pageNumber) {
        Locator pageButton = page.locator("button:has-text('" + pageNumber + "'), a:has-text('" + pageNumber + "')").first();
        pageButton.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 페이지가 로드되었는지 확인
     */
    @Override
    public boolean isLoaded() {
        return isPostListVisible();
    }
}