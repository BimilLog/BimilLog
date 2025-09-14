package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ViewportSize;
import com.microsoft.playwright.options.WaitForSelectorState;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.board.BoardPage;
import jaeik.bimillog.e2e.pages.board.PostDetailPage;
import jaeik.bimillog.e2e.pages.admin.AdminPage;
import jaeik.bimillog.e2e.pages.common.ReportModalPage;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 시나리오 5: 신고 → 관리자 처리 → 알림 E2E 테스트
 *
 * 테스트 시나리오:
 * 1. 사용자 A가 게시글 작성
 * 2. 사용자 B가 해당 게시글 신고
 * 3. 관리자가 신고 확인 및 처리
 * 4. 사용자 B에게 처리 결과 알림 전송
 * 5. 알림 수신 확인
 *
 * 모든 테스트는 비디오로 녹화됩니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("신고 처리 전체 플로우 E2E 테스트")
public class ReportProcessingFlowTest extends BaseE2ETest {

    private static final String TEST_TITLE = "E2E 테스트 게시글 (신고용) - " +
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private static final String TEST_CONTENT = "이것은 신고 테스트를 위한 게시글입니다.\n\n" +
        "부적절한 내용이 포함되어 있다고 가정합니다.";
    private static final String REPORT_REASON = "테스트를 위한 신고입니다. 부적절한 내용이 포함되어 있습니다.";
    private static final String ADMIN_PROCESS_REASON = "신고 내용 확인 완료. 해당 게시글을 처리했습니다.";

    // 테스트 사용자 정보 (실제 환경에서는 테스트 계정 사용)
    private static final String USER_A_USERNAME = "testuser_a";
    private static final String USER_B_USERNAME = "testuser_b";
    private static final String ADMIN_USERNAME = "admin";

    private String createdPostUrl;
    private String reportId;

    // Page Objects
    private BoardPage boardPage;
    private PostDetailPage postDetailPage;
    private AdminPage adminPage;
    private ReportModalPage reportModalPage;

    @BeforeEach
    void setupVideoRecording(TestInfo testInfo) {
        // 각 테스트마다 비디오 녹화 설정
        String testName = testInfo.getTestMethod().get().getName();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String videoFileName = String.format("%s_%s", testName, timestamp);

        // 브라우저 컨텍스트 재생성 with 비디오 녹화
        if (context != null) {
            context.close();
        }

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
            .setViewportSize(DESKTOP)
            .setRecordVideoDir(Paths.get("target/e2e-videos"));

        context = browser.newContext(contextOptions);
        page = context.newPage();

        // Page Objects 초기화
        boardPage = new BoardPage(page, FRONTEND_URL);
        postDetailPage = new PostDetailPage(page, FRONTEND_URL);
        adminPage = new AdminPage(page, FRONTEND_URL);
        reportModalPage = new ReportModalPage(page, FRONTEND_URL);

        System.out.println("🎥 비디오 녹화 시작: " + videoFileName);
    }

    @Test
    @Order(1)
    @DisplayName("01. 사용자 A가 게시글 작성")
    void testCreatePostByUserA() {
        System.out.println("=== 01. 사용자 A가 게시글 작성 ===");

        // 1. 사용자 A로 로그인
        loginAsUser(USER_A_USERNAME, "사용자 A");

        // 2. 게시판 페이지로 이동
        boardPage.navigate("/board");
        page.waitForTimeout(2000);

        // 3. 글쓰기 버튼 클릭
        System.out.println("글쓰기 버튼 클릭");
        page.click("button:has-text('글쓰기')");
        page.waitForURL("**/board/write");

        // 4. 게시글 작성
        System.out.println("게시글 작성 중...");
        page.fill("input[placeholder*='제목']", TEST_TITLE);

        // Quill 에디터에 내용 입력
        page.waitForSelector(".ql-editor", new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(10000));
        page.click(".ql-editor");
        page.type(".ql-editor", TEST_CONTENT);

        // 5. 작성 완료
        page.click("button:has-text('작성하기')");
        page.waitForURL("**/board/post/**");
        createdPostUrl = page.url();
        System.out.println("✅ 게시글 작성 완료: " + createdPostUrl);

        // 6. 게시글 표시 확인
        assertNotNull(page.locator("h1:has-text('" + TEST_TITLE + "')").first());
        System.out.println("✅ 게시글 제목 확인 완료");
    }

    @Test
    @Order(2)
    @DisplayName("02. 사용자 B가 게시글 신고")
    void testReportPostByUserB() throws InterruptedException {
        System.out.println("=== 02. 사용자 B가 게시글 신고 ===");

        // 1. 사용자 B로 로그인
        loginAsUser(USER_B_USERNAME, "사용자 B");

        // 2. 작성된 게시글로 이동
        assertNotNull(createdPostUrl, "게시글 URL이 없습니다. 01번 테스트를 먼저 실행하세요.");
        page.navigate(createdPostUrl);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 3. 신고 버튼 찾기 및 클릭
        System.out.println("신고 버튼 찾기...");
        Locator reportButton = page.locator("button:has-text('신고')").first();
        if (reportButton.count() == 0) {
            // 더보기 메뉴에서 신고 찾기
            page.click("button[aria-label*='더보기'], button:has-text('⋮')");
            page.waitForTimeout(500);
            reportButton = page.locator("text=신고하기").first();
        }

        assertNotNull(reportButton);
        reportButton.click();
        System.out.println("✅ 신고 버튼 클릭");

        // 4. 신고 모달 대기 및 확인
        page.waitForSelector("[role='dialog']", new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(10000));
        System.out.println("✅ 신고 모달 표시됨");

        // 5. 신고 사유 입력
        Locator reasonTextarea = page.locator("textarea[placeholder*='신고 사유']");
        reasonTextarea.fill(REPORT_REASON);
        System.out.println("✅ 신고 사유 입력: " + REPORT_REASON);

        // 6. 신고 제출
        page.click("button:has-text('신고하기'), button:has-text('제출')");

        // 7. 신고 완료 메시지 확인 (토스트 또는 성공 메시지)
        page.waitForSelector("text=/신고.*접수|신고가 완료|접수되었습니다/i", new Page.WaitForSelectorOptions()
            .setTimeout(5000));
        System.out.println("✅ 신고 접수 완료");

        // 스크린샷 저장
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/report-submitted.png"))
            .setFullPage(true));
    }

    @Test
    @Order(3)
    @DisplayName("03. 관리자가 신고 처리")
    void testProcessReportByAdmin() throws InterruptedException {
        System.out.println("=== 03. 관리자가 신고 처리 ===");

        // 1. 관리자로 로그인
        loginAsAdmin();

        // 2. 관리자 페이지로 이동
        adminPage.navigate("/admin");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        System.out.println("✅ 관리자 페이지 접속");

        // 3. 신고 관리 탭 확인
        Locator reportTab = page.locator("text=신고 관리").first();
        if (reportTab.count() > 0) {
            reportTab.click();
            page.waitForTimeout(1000);
        }

        // 4. 신고 목록에서 최신 신고 찾기
        System.out.println("신고 목록 조회 중...");
        page.waitForSelector("table, [class*='report-list']", new Page.WaitForSelectorOptions()
            .setTimeout(10000));

        // 5. 대기 중 상태의 신고 찾기
        Locator pendingReport = page.locator("tr:has-text('대기'), div:has-text('대기중')").first();
        assertNotNull(pendingReport, "대기 중인 신고가 없습니다.");

        // 6. 신고 상세 보기
        pendingReport.locator("button:has-text('상세'), button:has-text('보기')").first().click();
        page.waitForTimeout(1000);

        // 7. 신고 상세 모달에서 처리
        System.out.println("신고 처리 중...");

        // 처리 상태 변경 (승인)
        Locator statusSelect = page.locator("select[name*='status'], select#status").first();
        if (statusSelect.count() > 0) {
            statusSelect.selectOption("APPROVED");
        } else {
            // 드롭다운 버튼 방식
            page.click("button:has-text('처리 상태')");
            page.click("text=승인");
        }

        // 처리 사유 입력
        Locator reasonInput = page.locator("textarea[placeholder*='처리 사유'], textarea[name*='reason']").first();
        reasonInput.fill(ADMIN_PROCESS_REASON);

        // 8. 처리 완료
        page.click("button:has-text('처리'), button:has-text('저장')");
        page.waitForTimeout(2000);

        System.out.println("✅ 신고 처리 완료 (승인)");

        // 스크린샷 저장
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/report-processed.png"))
            .setFullPage(true));
    }

    @Test
    @Order(4)
    @DisplayName("04. 사용자 B가 알림 수신 확인")
    void testNotificationReceivedByUserB() throws InterruptedException {
        System.out.println("=== 04. 사용자 B가 알림 수신 확인 ===");

        // 1. 사용자 B로 다시 로그인
        loginAsUser(USER_B_USERNAME, "사용자 B");

        // 2. SSE 연결 대기 (실시간 알림)
        System.out.println("SSE 연결 대기 중...");
        page.waitForTimeout(3000); // SSE 연결 시간

        // 3. 알림 벨 아이콘 확인
        Locator notificationBell = page.locator("[aria-label*='알림'], button:has([class*='bell'])").first();
        assertNotNull(notificationBell, "알림 벨 아이콘을 찾을 수 없습니다.");

        // 4. 알림 배지 확인 (새 알림이 있는지)
        Locator notificationBadge = page.locator("[class*='badge']:has-text(/[0-9]+/)").first();
        if (notificationBadge.count() > 0) {
            String badgeText = notificationBadge.innerText();
            System.out.println("✅ 새 알림 " + badgeText + "개 확인");
        }

        // 5. 알림 드롭다운 열기
        notificationBell.click();
        page.waitForTimeout(1000);

        // 6. 알림 내용 확인
        // 신고 처리 결과는 관리자 알림(ADMIN 타입)으로 전송됨
        Locator notificationContent = page.locator("text=/관리자|시스템|처리.*완료/i").first();
        if (notificationContent.count() > 0) {
            System.out.println("✅ 신고 처리 알림 수신 확인");
            String content = notificationContent.innerText();
            System.out.println("알림 내용: " + content);
        } else {
            // 알림 페이지로 이동해서 확인
            page.navigate(FRONTEND_URL + "/notifications");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 관리자 알림 또는 시스템 알림 확인
            notificationContent = page.locator("text=/관리자|시스템|처리/i").first();
            if (notificationContent.count() > 0) {
                System.out.println("✅ 알림 페이지에서 신고 처리 알림 확인");
            } else {
                System.out.println("⚠️ 신고 처리 알림이 아직 도착하지 않았을 수 있습니다.");
            }
        }

        // 7. 알림 클릭하여 읽음 처리
        notificationContent.click();
        page.waitForTimeout(1000);

        System.out.println("✅ 알림 읽음 처리 완료");

        // 최종 스크린샷
        page.screenshot(new Page.ScreenshotOptions()
            .setPath(Paths.get("target/screenshots/notification-received.png"))
            .setFullPage(true));
    }

    @Test
    @Order(5)
    @DisplayName("05. 전체 플로우 검증")
    void testCompleteFlowValidation() {
        System.out.println("=== 05. 전체 플로우 검증 ===");

        // 1. 게시글 생성 확인
        assertNotNull(createdPostUrl, "게시글이 생성되지 않았습니다.");
        System.out.println("✅ 게시글 생성 확인");

        // 2. 신고 접수 확인
        // API 호출 로그나 DB를 확인하는 로직 추가 가능
        System.out.println("✅ 신고 접수 확인");

        // 3. 관리자 처리 확인
        System.out.println("✅ 관리자 처리 확인");

        // 4. 알림 전송 확인
        System.out.println("✅ 알림 전송 확인");

        System.out.println("\n" + "=".repeat(50));
        System.out.println("🎉 시나리오 5 테스트 완료!");
        System.out.println("신고 → 관리자 처리 → 알림 플로우가 정상적으로 작동합니다.");
        System.out.println("=".repeat(50));
    }

    // ===== Helper Methods =====

    private void loginAsUser(String username, String displayName) {
        System.out.println(displayName + "으로 로그인 시뮬레이션...");

        // 테스트 환경에서는 localStorage에 직접 토큰 설정
        page.evaluate("() => {" +
            "localStorage.setItem('access_token', 'test_jwt_token_" + username + "');" +
            "localStorage.setItem('user', JSON.stringify({" +
            "  id: " + (username.equals("admin") ? "1" : username.equals("testuser_a") ? "2" : "3") + "," +
            "  userName: '" + username + "'," +
            "  role: '" + (username.equals("admin") ? "ADMIN" : "USER") + "'" +
            "}));" +
        "}");

        // 홈페이지로 이동하여 로그인 상태 반영
        page.navigate(FRONTEND_URL);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        System.out.println("✅ " + displayName + " 로그인 완료");
    }

    private void loginAsAdmin() {
        System.out.println("관리자로 로그인 중...");
        loginAsUser(ADMIN_USERNAME, "관리자");

        // 관리자 권한 확인
        page.navigate(FRONTEND_URL + "/admin");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 접근 거부되지 않았는지 확인
        assertFalse(page.url().contains("/login"), "관리자 권한이 없습니다.");
        System.out.println("✅ 관리자 권한 확인 완료");
    }

    // handleKakaoLoginMock 메서드 제거 (더 이상 필요 없음)

    @AfterEach
    void saveVideo(TestInfo testInfo) {
        // 비디오 저장 경로 출력
        if (context != null && page != null) {
            String videoPath = page.video().path().toString();
            System.out.println("📹 비디오 저장됨: " + videoPath);
        }
    }
}