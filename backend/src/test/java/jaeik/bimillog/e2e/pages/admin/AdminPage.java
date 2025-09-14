package jaeik.bimillog.e2e.pages.admin;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import jaeik.bimillog.e2e.pages.common.BasePage;

/**
 * 관리자 페이지 Page Object
 */
public class AdminPage extends BasePage {

    // Selectors - 대시보드
    private static final String ADMIN_HEADER = "[class*='admin-header'], h1:has-text('관리자')";
    private static final String STATS_CARDS = "[class*='stats-card'], [class*='stat-card']";
    private static final String TOTAL_REPORTS = "[data-testid='total-reports']";
    private static final String PENDING_REPORTS = "[data-testid='pending-reports']";
    private static final String PROCESSED_REPORTS = "[data-testid='processed-reports']";

    // Selectors - 탭 네비게이션
    private static final String TAB_REPORTS = "button:has-text('신고 관리'), [role='tab']:has-text('신고')";
    private static final String TAB_USERS = "button:has-text('사용자 관리'), [role='tab']:has-text('사용자')";
    private static final String TAB_STATS = "button:has-text('통계'), [role='tab']:has-text('통계')";

    // Selectors - 신고 관리
    private static final String REPORT_LIST = "[class*='report-list'], table tbody tr";
    private static final String REPORT_FILTERS = "[class*='filter'], [class*='search']";
    private static final String REPORT_TYPE_FILTER = "select[name*='type'], [data-testid='report-type-filter']";
    private static final String REPORT_STATUS_FILTER = "select[name*='status'], [data-testid='report-status-filter']";
    private static final String REPORT_SEARCH = "input[placeholder*='검색']";
    private static final String REPORT_DETAIL_BUTTON = "button:has-text('상세'), button:has-text('보기')";

    // Selectors - 신고 상세 모달
    private static final String REPORT_MODAL = "[role='dialog'], [class*='modal']";
    private static final String PROCESS_STATUS_SELECT = "select[name*='status'], select#status";
    private static final String PROCESS_REASON_TEXTAREA = "textarea[placeholder*='처리 사유'], textarea[name*='reason']";
    private static final String PROCESS_SUBMIT_BUTTON = "button:has-text('처리'), button:has-text('저장')";
    private static final String MODAL_CLOSE_BUTTON = "button[aria-label*='닫기'], button:has-text('취소')";

    public AdminPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * 관리자 페이지로 이동
     */
    public void navigateToAdmin() {
        navigate("/admin");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitFor(ADMIN_HEADER);
    }

    /**
     * 페이지가 로드되었는지 확인
     */
    @Override
    public boolean isLoaded() {
        return isVisible(ADMIN_HEADER) && isVisible(STATS_CARDS);
    }

    /**
     * 권한 확인 (접근 거부 시 로그인 페이지로 리다이렉트)
     */
    public boolean hasAdminAccess() {
        return !page.url().contains("/login") && isVisible(ADMIN_HEADER);
    }

    // ===== 대시보드 메서드 =====

    /**
     * 통계 카드에서 값 가져오기
     */
    public String getTotalReports() {
        return getText(TOTAL_REPORTS);
    }

    public String getPendingReports() {
        return getText(PENDING_REPORTS);
    }

    public String getProcessedReports() {
        return getText(PROCESSED_REPORTS);
    }

    // ===== 탭 네비게이션 메서드 =====

    /**
     * 신고 관리 탭 클릭
     */
    public void clickReportsTab() {
        click(TAB_REPORTS);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitFor(REPORT_LIST);
    }

    /**
     * 사용자 관리 탭 클릭
     */
    public void clickUsersTab() {
        click(TAB_USERS);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 통계 탭 클릭
     */
    public void clickStatsTab() {
        click(TAB_STATS);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    // ===== 신고 관리 메서드 =====

    /**
     * 신고 유형 필터 설정
     */
    public void filterByReportType(String type) {
        selectOption(REPORT_TYPE_FILTER, type);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 신고 상태 필터 설정
     */
    public void filterByReportStatus(String status) {
        selectOption(REPORT_STATUS_FILTER, status);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 신고 검색
     */
    public void searchReport(String keyword) {
        fill(REPORT_SEARCH, keyword);
        pressEnter();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 신고 목록이 표시되는지 확인
     */
    public boolean isReportListVisible() {
        return isVisible(REPORT_LIST);
    }

    /**
     * 신고 개수 가져오기
     */
    public int getReportCount() {
        return getElementCount(REPORT_LIST);
    }

    /**
     * 첫 번째 신고 상세 보기
     */
    public void viewFirstReportDetail() {
        Locator firstDetailButton = find(REPORT_LIST).first()
            .locator(REPORT_DETAIL_BUTTON).first();
        firstDetailButton.click();
        waitForReportModal();
    }

    /**
     * 특정 상태의 신고 찾아서 상세 보기
     */
    public void viewReportByStatus(String status) {
        Locator reportRow = page.locator(REPORT_LIST + ":has-text('" + status + "')").first();
        reportRow.locator(REPORT_DETAIL_BUTTON).first().click();
        waitForReportModal();
    }

    /**
     * 신고 모달 대기
     */
    private void waitForReportModal() {
        page.waitForSelector(REPORT_MODAL, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(5000));
    }

    // ===== 신고 처리 메서드 =====

    /**
     * 신고 처리 상태 변경
     */
    public void changeProcessStatus(String status) {
        // Select 태그 방식
        if (getElementCount(PROCESS_STATUS_SELECT) > 0) {
            selectOption(PROCESS_STATUS_SELECT, status);
        } else {
            // 드롭다운 버튼 방식
            click("button:has-text('처리 상태')");
            page.waitForTimeout(300);
            clickText(status);
        }
    }

    /**
     * 처리 사유 입력
     */
    public void enterProcessReason(String reason) {
        fill(PROCESS_REASON_TEXTAREA, reason);
    }

    /**
     * 신고 처리 제출
     */
    public void submitReportProcess() {
        click(PROCESS_SUBMIT_BUTTON);
        page.waitForTimeout(2000); // API 응답 대기

        // 성공 메시지 또는 모달 닫힘 확인
        if (isVisible(REPORT_MODAL)) {
            closeModal();
        }
    }

    /**
     * 신고 승인 처리
     */
    public void approveReport(String reason) {
        changeProcessStatus("APPROVED");
        enterProcessReason(reason);
        submitReportProcess();
    }

    /**
     * 신고 반려 처리
     */
    public void rejectReport(String reason) {
        changeProcessStatus("REJECTED");
        enterProcessReason(reason);
        submitReportProcess();
    }

    /**
     * 모달 닫기
     */
    public void closeModal() {
        if (isVisible(MODAL_CLOSE_BUTTON)) {
            click(MODAL_CLOSE_BUTTON);
        } else {
            pressEscape();
        }
        page.waitForSelector(REPORT_MODAL, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.HIDDEN)
            .setTimeout(3000));
    }

    // ===== 사용자 관리 메서드 =====

    /**
     * 사용자 차단
     */
    public void banUser(String userId) {
        // 사용자 관리 탭에서 사용자 찾기
        clickUsersTab();
        Locator userRow = page.locator("tr:has-text('" + userId + "')").first();
        userRow.locator("button:has-text('차단')").click();

        // 확인 다이얼로그 처리
        acceptAlert();
        page.waitForTimeout(1000);
    }

    /**
     * 사용자 강제 탈퇴
     */
    public void forceWithdrawUser(String userId) {
        clickUsersTab();
        Locator userRow = page.locator("tr:has-text('" + userId + "')").first();
        userRow.locator("button:has-text('강제 탈퇴')").click();

        // 확인 다이얼로그 처리
        acceptAlert();
        page.waitForTimeout(1000);
    }

    // ===== 검증 메서드 =====

    /**
     * 처리 완료 메시지 확인
     */
    public boolean isProcessSuccessMessageVisible() {
        return hasText("처리 완료") || hasText("성공적으로");
    }

    /**
     * 에러 메시지 확인
     */
    public boolean isErrorMessageVisible() {
        return hasText("오류") || hasText("실패");
    }
}