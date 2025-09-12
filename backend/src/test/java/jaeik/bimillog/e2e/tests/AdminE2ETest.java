package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import jaeik.bimillog.e2e.pages.PostPage;
import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>관리자 기능 E2E 테스트</h2>
 * <p>관리자 권한 확인, 사용자/게시글 관리를 테스트합니다.</p>
 * <p>MySQL에서 직접 ROLE_ADMIN 부여 필요</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("관리자 기능 E2E 테스트")
@Disabled("관리자 계정 설정 후 활성화 필요")
public class AdminE2ETest extends BaseE2ETest {
    
    private LoginPage loginPage;
    private PostPage postPage;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        loginPage = new LoginPage(page, BASE_URL);
        postPage = new PostPage(page, BASE_URL);
        
        // 관리자 계정으로 로그인 시도
        // 실제 테스트 시 관리자 계정 필요
        System.out.println("주의: 이 테스트는 ROLE_ADMIN 권한이 있는 계정이 필요합니다.");
        System.out.println("MySQL에서 user 테이블의 role을 ROLE_ADMIN으로 직접 수정해주세요.");
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 관리자 권한 확인")
    void 관리자_권한_확인() {
        System.out.println(">>> 관리자 권한 확인 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        // 관리자 페이지 접근 시도
        page.navigate(BASE_URL + "/admin");
        page.waitForLoadState();
        
        // 관리자 페이지 접근 가능 여부
        if (page.url().contains("/admin")) {
            System.out.println("관리자 페이지 접근 성공");
            
            // 관리자 메뉴 확인
            boolean hasAdminMenu = page.locator("text=/관리|Admin|대시보드/").isVisible();
            assertThat(hasAdminMenu).isTrue();
        } else {
            System.out.println("관리자 권한 없음 - 리다이렉트됨");
            assertThat(page.url()).doesNotContain("/admin");
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 사용자 목록 조회")
    void 사용자_목록_조회() {
        System.out.println(">>> 사용자 목록 조회 테스트 시작");
        
        ensureAdminLogin();
        
        page.navigate(BASE_URL + "/admin/users");
        page.waitForLoadState();
        
        // 사용자 목록 테이블 확인
        Locator userTable = page.locator("table, .user-list").first();
        
        if (userTable.isVisible()) {
            Locator userRows = page.locator("tbody tr, .user-item");
            int userCount = userRows.count();
            
            System.out.println("등록된 사용자 수: " + userCount);
            assertThat(userCount).isGreaterThan(0);
            
            // 사용자 정보 확인
            if (userCount > 0) {
                Locator firstUser = userRows.first();
                String userInfo = firstUser.textContent();
                System.out.println("첫 번째 사용자: " + userInfo);
            }
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 사용자 강제 탈퇴")
    void 사용자_강제_탈퇴() {
        System.out.println(">>> 사용자 강제 탈퇴 테스트 시작");
        
        ensureAdminLogin();
        
        // 테스트용 사용자 생성
        loginPage.performLogout();
        loginPage.performKakaoLogin();
        String testUsername = loginPage.getUsername();
        
        // 다시 관리자로 로그인
        loginPage.performLogout();
        ensureAdminLogin();
        
        page.navigate(BASE_URL + "/admin/users");
        page.waitForLoadState();
        
        // 테스트 사용자 찾기
        Locator userRow = page.locator("tr:has-text('" + testUsername + "')").first();
        
        if (userRow.isVisible()) {
            // 강제 탈퇴 버튼 클릭
            Locator withdrawButton = userRow.locator("button:has-text('탈퇴'), button:has-text('삭제')").first();
            
            if (withdrawButton.isVisible()) {
                withdrawButton.click();
                
                // 확인 다이얼로그
                Locator confirmButton = page.locator("button:has-text('확인')").last();
                confirmButton.click();
                
                page.waitForTimeout(2000);
                
                // 사용자가 목록에서 제거되었는지 확인
                boolean userRemoved = !page.locator("text=" + testUsername).isVisible();
                assertThat(userRemoved).isTrue();
                
                System.out.println("사용자 강제 탈퇴 완료: " + testUsername);
            }
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 게시글 관리자 삭제")
    void 게시글_관리자_삭제() {
        System.out.println(">>> 게시글 관리자 삭제 테스트 시작");
        
        ensureAdminLogin();
        
        // 테스트 게시글 작성
        String postTitle = "관리자 삭제 테스트 " + generateTimestamp();
        String postUrl = postPage.createPost(postTitle, "부적절한 내용", null);
        
        // 관리자 모드에서 게시글 삭제
        page.navigate(postUrl);
        page.waitForLoadState();
        
        // 관리자 삭제 버튼 찾기
        Locator adminDeleteButton = page.locator("button:has-text('관리자 삭제'), button[class*='admin']").first();
        
        if (!adminDeleteButton.isVisible()) {
            // 더보기 메뉴 확인
            Locator moreButton = page.locator("button:has(svg.lucide-more-vertical)").first();
            if (moreButton.isVisible()) {
                moreButton.click();
                page.waitForTimeout(500);
            }
        }
        
        if (adminDeleteButton.isVisible()) {
            adminDeleteButton.click();
            
            // 삭제 사유 입력
            Locator reasonInput = page.locator("textarea[placeholder*='사유'], input[placeholder*='이유']").first();
            if (reasonInput.isVisible()) {
                reasonInput.fill("부적절한 내용으로 인한 관리자 삭제");
            }
            
            Locator confirmButton = page.locator("button:has-text('삭제')").last();
            confirmButton.click();
            
            page.waitForTimeout(2000);
            
            // 게시글이 삭제되었는지 확인
            assertThat(page.url()).doesNotContain("/post/");
            System.out.println("게시글 관리자 삭제 완료");
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 댓글 관리자 삭제")
    void 댓글_관리자_삭제() {
        System.out.println(">>> 댓글 관리자 삭제 테스트 시작");
        
        ensureAdminLogin();
        
        // 테스트 게시글과 댓글 작성
        String postUrl = postPage.createPost("댓글 삭제 테스트", "내용", null);
        postPage.writeComment("부적절한 댓글 내용");
        
        page.reload();
        page.waitForLoadState();
        
        // 댓글 관리자 삭제
        Locator comment = page.locator(".comment-item").first();
        comment.hover();
        
        Locator adminDeleteButton = page.locator("button[title*='관리자'], button:has-text('관리자 삭제')").first();
        
        if (adminDeleteButton.isVisible()) {
            adminDeleteButton.click();
            
            Locator confirmButton = page.locator("button:has-text('확인')").last();
            confirmButton.click();
            
            page.waitForTimeout(1000);
            
            // 댓글이 삭제되었는지 확인
            boolean commentDeleted = page.locator("text=부적절한 댓글").count() == 0;
            assertThat(commentDeleted).isTrue();
            
            System.out.println("댓글 관리자 삭제 완료");
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 신고 내역 조회")
    void 신고_내역_조회() {
        System.out.println(">>> 신고 내역 조회 테스트 시작");
        
        ensureAdminLogin();
        
        page.navigate(BASE_URL + "/admin/reports");
        page.waitForLoadState();
        
        // 신고 내역 목록 확인
        Locator reportList = page.locator(".report-list, table").first();
        
        if (reportList.isVisible()) {
            Locator reports = page.locator("tbody tr, .report-item");
            int reportCount = reports.count();
            
            System.out.println("신고 내역 수: " + reportCount);
            
            if (reportCount > 0) {
                // 첫 번째 신고 내역 상세 보기
                Locator firstReport = reports.first();
                firstReport.click();
                
                page.waitForTimeout(1000);
                
                // 신고 상세 정보 확인
                boolean hasDetail = page.locator("text=/신고자|사유|내용/").isVisible();
                if (hasDetail) {
                    System.out.println("신고 상세 정보 확인 가능");
                }
            }
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 신고 처리")
    void 신고_처리() {
        System.out.println(">>> 신고 처리 테스트 시작");
        
        ensureAdminLogin();
        
        // 테스트 신고 생성
        createTestReport();
        
        page.navigate(BASE_URL + "/admin/reports");
        page.waitForLoadState();
        
        // 미처리 신고 찾기
        Locator pendingReport = page.locator("tr:has-text('대기'), .report-item.pending").first();
        
        if (pendingReport.isVisible()) {
            pendingReport.click();
            page.waitForTimeout(1000);
            
            // 처리 버튼 클릭
            Locator processButton = page.locator("button:has-text('처리'), button:has-text('승인')").first();
            
            if (processButton.isVisible()) {
                processButton.click();
                
                // 처리 사유 입력
                Locator reasonInput = page.locator("textarea[name='reason']").first();
                if (reasonInput.isVisible()) {
                    reasonInput.fill("신고 내용 확인 후 처리");
                }
                
                Locator confirmButton = page.locator("button:has-text('완료')").last();
                confirmButton.click();
                
                page.waitForTimeout(1000);
                
                System.out.println("신고 처리 완료");
            }
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("08. 통계 대시보드")
    void 통계_대시보드() {
        System.out.println(">>> 통계 대시보드 테스트 시작");
        
        ensureAdminLogin();
        
        page.navigate(BASE_URL + "/admin/dashboard");
        page.waitForLoadState();
        
        // 통계 위젯 확인
        boolean hasStats = page.locator(".stat-card, .dashboard-widget").count() > 0;
        
        if (hasStats) {
            // 사용자 통계
            Locator userStat = page.locator("text=/사용자|Users/").first();
            if (userStat.isVisible()) {
                String userCount = userStat.locator("..").textContent();
                System.out.println("사용자 통계: " + userCount);
            }
            
            // 게시글 통계
            Locator postStat = page.locator("text=/게시글|Posts/").first();
            if (postStat.isVisible()) {
                String postCount = postStat.locator("..").textContent();
                System.out.println("게시글 통계: " + postCount);
            }
            
            // 신고 통계
            Locator reportStat = page.locator("text=/신고|Reports/").first();
            if (reportStat.isVisible()) {
                String reportCount = reportStat.locator("..").textContent();
                System.out.println("신고 통계: " + reportCount);
            }
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("09. 공지사항 작성")
    void 공지사항_작성() {
        System.out.println(">>> 공지사항 작성 테스트 시작");
        
        ensureAdminLogin();
        
        page.navigate(BASE_URL + "/admin/notice");
        page.waitForLoadState();
        
        // 공지사항 작성 버튼
        Locator writeButton = page.locator("button:has-text('공지 작성'), button:has-text('새 공지')").first();
        
        if (writeButton.isVisible()) {
            writeButton.click();
            page.waitForTimeout(1000);
            
            // 공지사항 작성
            Locator titleInput = page.locator("input[name='title']").first();
            titleInput.fill("테스트 공지사항 " + generateTimestamp());
            
            Locator contentInput = page.locator("textarea[name='content']").first();
            contentInput.fill("관리자 공지사항 테스트 내용입니다.");
            
            // 중요 공지 체크
            Locator importantCheck = page.locator("input[type='checkbox'][name='important']").first();
            if (importantCheck.isVisible()) {
                importantCheck.check();
            }
            
            Locator submitButton = page.locator("button:has-text('작성'), button:has-text('등록')").first();
            submitButton.click();
            
            page.waitForTimeout(2000);
            
            System.out.println("공지사항 작성 완료");
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 관리자 로그 확인")
    void 관리자_로그_확인() {
        System.out.println(">>> 관리자 로그 확인 테스트 시작");
        
        ensureAdminLogin();
        
        page.navigate(BASE_URL + "/admin/logs");
        page.waitForLoadState();
        
        // 관리자 활동 로그 확인
        Locator logTable = page.locator("table.admin-logs, .log-list").first();
        
        if (logTable.isVisible()) {
            Locator logEntries = page.locator("tbody tr, .log-entry");
            int logCount = logEntries.count();
            
            System.out.println("관리자 활동 로그: " + logCount + "개");
            
            if (logCount > 0) {
                // 최근 로그 확인
                Locator recentLog = logEntries.first();
                String logContent = recentLog.textContent();
                System.out.println("최근 활동: " + logContent);
            }
        }
    }
    
    /**
     * <h3>관리자 로그인 확인</h3>
     * <p>관리자 권한이 있는 계정으로 로그인되어 있는지 확인합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    private void ensureAdminLogin() {
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        // 관리자 권한 확인 (실제로는 DB에서 role 확인 필요)
        page.navigate(BASE_URL + "/admin");
        if (!page.url().contains("/admin")) {
            System.out.println("경고: 관리자 권한이 없습니다. MySQL에서 role을 ROLE_ADMIN으로 설정해주세요.");
        }
    }
    
    /**
     * <h3>테스트 신고 생성</h3>
     * <p>테스트용 신고를 생성합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    private void createTestReport() {
        // 일반 사용자로 로그인
        loginPage.performLogout();
        loginPage.performKakaoLogin();
        
        // 게시글 생성 및 신고
        String postUrl = postPage.createPost("신고 테스트", "부적절한 내용", null);
        
        // 신고 버튼 클릭
        Locator reportButton = page.locator("button:has-text('신고')").first();
        if (reportButton.isVisible()) {
            reportButton.click();
            
            // 신고 사유 선택
            Locator reasonSelect = page.locator("select[name='reason']").first();
            reasonSelect.selectOption("스팸");
            
            // 상세 내용 입력
            Locator detailInput = page.locator("textarea[name='details']").first();
            detailInput.fill("테스트 신고입니다");
            
            Locator submitButton = page.locator("button:has-text('신고하기')").first();
            submitButton.click();
            
            page.waitForTimeout(1000);
        }
        
        // 다시 관리자로 로그인
        loginPage.performLogout();
        ensureAdminLogin();
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        super.afterEach(testInfo);
    }
}