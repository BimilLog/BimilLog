package jaeik.bimillog.e2e.pages.paper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import java.util.List;

/**
 * 롤링페이퍼 페이지 객체
 * 내 롤링페이퍼(/rolling-paper) 및 특정 사용자 롤링페이퍼(/rolling-paper/[nickname]) 페이지 조작
 */
public class RollingPaperPage {

    private final Page page;

    // 셀렉터 정의
    private static final String GRID_CONTAINER = "[class*='grid'], .rolling-paper-grid, .message-grid";
    private static final String GRID_CELL = ".grid-cell, [class*='cell'], .message-cell";
    private static final String EMPTY_CELL = ".grid-cell:not(.has-message), .empty-cell, [data-empty='true']";
    private static final String MESSAGE_CELL = ".grid-cell.has-message, .message-cell, [data-has-message='true']";
    private static final String SHARE_BUTTON = "button:has-text('공유'), button[aria-label*='공유']";
    private static final String COPY_URL_BUTTON = "button:has-text('URL 복사'), button:has-text('링크 복사')";
    private static final String KAKAO_SHARE_BUTTON = "button:has-text('카카오톡'), button[aria-label*='카카오']";
    private static final String MESSAGE_LIST_BUTTON = "button:has-text('전체 메시지'), button:has-text('목록')";
    private static final String PAGINATION_PREV = "button:has-text('이전'), button[aria-label='이전 페이지']";
    private static final String PAGINATION_NEXT = "button:has-text('다음'), button[aria-label='다음 페이지']";
    private static final String STATS_SECTION = "[class*='stats'], [class*='summary'], .statistics";
    private static final String TOTAL_MESSAGES = "[class*='total-messages'], [data-stat='total']";
    private static final String TODAY_MESSAGES = "[class*='today-messages'], [data-stat='today']";
    private static final String RECENT_VISITORS = "[class*='recent-visitors'], [data-stat='visitors']";
    private static final String RECENT_MESSAGES_SECTION = "[class*='recent-messages'], .recent-section";

    public RollingPaperPage(Page page) {
        this.page = page;
    }

    /**
     * 페이지 로드 대기
     */
    public void waitForPageLoad() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector(GRID_CONTAINER, new Page.WaitForSelectorOptions().setTimeout(10000));
    }

    /**
     * 특정 사용자의 롤링페이퍼로 이동
     */
    public void navigateToUserPaper(String baseUrl, String nickname) {
        page.navigate(baseUrl + "/rolling-paper/" + nickname);
        waitForPageLoad();
    }

    /**
     * 내 롤링페이퍼로 이동
     */
    public void navigateToMyPaper(String baseUrl) {
        page.navigate(baseUrl + "/rolling-paper");
        waitForPageLoad();
    }

    /**
     * 그리드가 표시되는지 확인
     */
    public boolean isGridVisible() {
        return page.locator(GRID_CONTAINER).isVisible();
    }

    /**
     * 그리드 컬럼 수 확인
     */
    public int getGridColumnCount() {
        Locator grid = page.locator(GRID_CONTAINER).first();
        String gridStyle = grid.evaluate("el => window.getComputedStyle(el).gridTemplateColumns").toString();

        // "repeat(4, ...)" 또는 "100px 100px 100px 100px" 형태에서 컬럼 수 추출
        if (gridStyle.contains("repeat")) {
            String count = gridStyle.replaceAll(".*repeat\\((\\d+).*", "$1");
            return Integer.parseInt(count);
        } else {
            return gridStyle.split(" ").length;
        }
    }

    /**
     * 빈 셀 클릭
     */
    public void clickEmptyCell() {
        clickEmptyCell(0);
    }

    /**
     * 특정 인덱스의 빈 셀 클릭
     */
    public void clickEmptyCell(int index) {
        Locator emptyCells = page.locator(EMPTY_CELL);
        if (emptyCells.count() > index) {
            emptyCells.nth(index).click();
        }
    }

    /**
     * 메시지가 있는 셀 클릭
     */
    public void clickMessageCell() {
        clickMessageCell(0);
    }

    /**
     * 특정 인덱스의 메시지 셀 클릭
     */
    public void clickMessageCell(int index) {
        Locator messageCells = page.locator(MESSAGE_CELL);
        if (messageCells.count() > index) {
            messageCells.nth(index).click();
        }
    }

    /**
     * 빈 셀 개수 반환
     */
    public int getEmptyCellCount() {
        return page.locator(EMPTY_CELL).count();
    }

    /**
     * 메시지 셀 개수 반환
     */
    public int getMessageCellCount() {
        return page.locator(MESSAGE_CELL).count();
    }

    /**
     * 공유 버튼 클릭
     */
    public void clickShareButton() {
        page.click(SHARE_BUTTON);
    }

    /**
     * URL 복사 버튼 클릭
     */
    public void clickCopyUrlButton() {
        page.click(COPY_URL_BUTTON);
    }

    /**
     * 카카오톡 공유 버튼 클릭
     */
    public void clickKakaoShareButton() {
        page.click(KAKAO_SHARE_BUTTON);
    }

    /**
     * 전체 메시지 목록 보기 버튼 클릭
     */
    public void clickMessageListButton() {
        page.click(MESSAGE_LIST_BUTTON);
    }

    /**
     * 이전 페이지로 이동
     */
    public void clickPreviousPage() {
        if (page.locator(PAGINATION_PREV).isEnabled()) {
            page.click(PAGINATION_PREV);
        }
    }

    /**
     * 다음 페이지로 이동
     */
    public void clickNextPage() {
        if (page.locator(PAGINATION_NEXT).isEnabled()) {
            page.click(PAGINATION_NEXT);
        }
    }

    /**
     * 통계 섹션이 표시되는지 확인
     */
    public boolean isStatsSectionVisible() {
        return page.locator(STATS_SECTION).isVisible();
    }

    /**
     * 총 메시지 수 가져오기
     */
    public String getTotalMessages() {
        Locator total = page.locator(TOTAL_MESSAGES);
        if (total.count() > 0) {
            return total.textContent();
        }
        return "";
    }

    /**
     * 오늘 받은 메시지 수 가져오기
     */
    public String getTodayMessages() {
        Locator today = page.locator(TODAY_MESSAGES);
        if (today.count() > 0) {
            return today.textContent();
        }
        return "";
    }

    /**
     * 최근 방문자 수 가져오기
     */
    public String getRecentVisitors() {
        Locator visitors = page.locator(RECENT_VISITORS);
        if (visitors.count() > 0) {
            return visitors.textContent();
        }
        return "";
    }

    /**
     * 최근 메시지 섹션이 표시되는지 확인
     */
    public boolean isRecentMessagesSectionVisible() {
        return page.locator(RECENT_MESSAGES_SECTION).isVisible();
    }

    /**
     * 메시지 셀 호버
     */
    public void hoverMessageCell(int index) {
        Locator messageCells = page.locator(MESSAGE_CELL);
        if (messageCells.count() > index) {
            messageCells.nth(index).hover();
        }
    }

    /**
     * 특정 디자인 타입의 메시지 셀 찾기
     */
    public int getMessageCellCountByDesign(String designType) {
        String selector = String.format("[data-design='%s'], [class*='%s']",
            designType.toLowerCase(), designType.toLowerCase());
        return page.locator(selector).count();
    }

    /**
     * 페이지 제목 가져오기
     */
    public String getPageTitle() {
        Locator title = page.locator("h1, h2").first();
        return title.textContent();
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
     * 모달이 열려있는지 확인
     */
    public boolean isModalOpen() {
        return page.locator("[role='dialog'], .modal").isVisible();
    }

    /**
     * 모달 닫기
     */
    public void closeModal() {
        // ESC 키로 닫기 시도
        page.keyboard().press("Escape");
        page.waitForTimeout(500);

        // 여전히 열려있으면 닫기 버튼 클릭
        if (isModalOpen()) {
            Locator closeButton = page.locator(
                "button[aria-label*='닫기'], .close-button, button:has-text('닫기'), button:has-text('취소')"
            );
            if (closeButton.isVisible()) {
                closeButton.click();
            }
        }
    }
}