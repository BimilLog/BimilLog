package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import jaeik.bimillog.e2e.pages.PaperPage;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>롤링페이퍼 E2E 테스트</h2>
 * <p>롤링페이퍼 생성, 메시지 작성, 그리드 레이아웃 테스트를 수행합니다.</p>
 * <p>PC/모바일 레이아웃, 익명 메시지, 방문 기록 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("롤링페이퍼 시나리오 E2E 테스트")
public class PaperE2ETest extends BaseE2ETest {
    
    private PaperPage paperPage;
    private LoginPage loginPage;
    private String createdPaperUrl;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        paperPage = new PaperPage(page, BASE_URL);
        loginPage = new LoginPage(page, BASE_URL);
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 익명 사용자 롤링페이퍼 생성 테스트")
    void createPaperAsAnonymousUser() {
        System.out.println(">>> 익명 롤링페이퍼 생성 테스트 시작");
        
        if (loginPage.isLoggedIn()) {
            loginPage.performLogout();
        }
        
        String title = "테스트 롤링페이퍼";
        String description = "E2E 테스트용 롤링페이퍼입니다";
        
        createdPaperUrl = paperPage.createPaper(title, description);
        assertThat(createdPaperUrl).isNotNull();
        assertThat(createdPaperUrl).contains("/paper/");
        
        System.out.println("롤링페이퍼 생성 완료: " + createdPaperUrl);
    }
    
    @Test
    @Order(2)
    @DisplayName("02. PC 그리드 레이아웃 확인 (6x10)")
    void checkPCGridLayout() {
        System.out.println(">>> PC 그리드 레이아웃 테스트 시작");
        
        if (createdPaperUrl != null) {
            page.navigate(createdPaperUrl);
        } else {
            createdPaperUrl = paperPage.createPaper("PC 레이아웃 테스트", null);
        }
        
        String pcLayout = paperPage.getPCGridLayout();
        System.out.println("PC 레이아웃: " + pcLayout);
        
        assertThat(pcLayout).contains("10");
        assertThat(pcLayout.split("x")[1]).isEqualTo("6");
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 모바일 그리드 레이아웃 확인 (4x10)")
    void checkMobileGridLayout() {
        System.out.println(">>> 모바일 그리드 레이아웃 테스트 시작");
        
        if (createdPaperUrl != null) {
            page.navigate(createdPaperUrl);
        } else {
            createdPaperUrl = paperPage.createPaper("모바일 레이아웃 테스트", null);
        }
        
        String mobileLayout = paperPage.getMobileGridLayout();
        System.out.println("모바일 레이아웃: " + mobileLayout);
        
        assertThat(mobileLayout).contains("10");
        assertThat(mobileLayout.split("x")[1]).isEqualTo("4");
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 익명 메시지 작성 테스트")
    void writeAnonymousMessage() {
        System.out.println(">>> 익명 메시지 작성 테스트 시작");
        
        if (createdPaperUrl != null) {
            page.navigate(createdPaperUrl);
        } else {
            createdPaperUrl = paperPage.createPaper("메시지 테스트", null);
        }
        
        String message = "E2E 테스트 메시지 " + generateTimestamp();
        boolean success = paperPage.writeMessage(0, message, null);
        assertThat(success).isTrue();
        
        int usedCells = paperPage.getUsedCellsCount();
        assertThat(usedCells).isGreaterThan(0);
        
        System.out.println("메시지 작성 완료, 사용된 셀: " + usedCells);
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 메시지 읽기 테스트")
    void readMessage() {
        System.out.println(">>> 메시지 읽기 테스트 시작");
        
        if (createdPaperUrl != null) {
            page.navigate(createdPaperUrl);
        } else {
            createdPaperUrl = paperPage.createPaper("읽기 테스트", null);
            String testMessage = "읽기 테스트 메시지";
            paperPage.writeMessage(0, testMessage, "테스터");
        }
        
        String readMessage = paperPage.readMessage(0);
        assertThat(readMessage).isNotNull();
        
        System.out.println("읽은 메시지: " + readMessage);
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 여러 메시지 작성 테스트")
    void writeMultipleMessages() {
        System.out.println(">>> 여러 메시지 작성 테스트 시작");
        
        createdPaperUrl = paperPage.createPaper("다중 메시지 테스트", null);
        
        for (int i = 0; i < 5; i++) {
            String message = "메시지 #" + (i + 1);
            String author = (i % 2 == 0) ? null : "작성자" + i;
            
            boolean success = paperPage.writeMessage(i, message, author);
            assertThat(success).isTrue();
        }
        
        int usedCells = paperPage.getUsedCellsCount();
        assertThat(usedCells).isEqualTo(5);
        
        int emptyCells = paperPage.getEmptyCellsCount();
        System.out.println("사용된 셀: " + usedCells + ", 빈 셀: " + emptyCells);
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 로그인 사용자 롤링페이퍼 생성")
    void createPaperAsLoggedInUser() {
        System.out.println(">>> 로그인 사용자 롤링페이퍼 생성 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String title = "로그인 사용자 롤링페이퍼";
        String description = "로그인한 사용자가 생성한 롤링페이퍼";
        
        createdPaperUrl = paperPage.createPaper(title, description);
        assertThat(createdPaperUrl).isNotNull();
        
        System.out.println("로그인 사용자 롤링페이퍼 생성: " + createdPaperUrl);
    }
    
    @Test
    @Order(8)
    @DisplayName("08. 방문 기록 테스트")
    void checkVisitHistory() {
        System.out.println(">>> 방문 기록 테스트 시작");
        
        String paper1 = paperPage.createPaper("방문기록1", null);
        page.navigate(BASE_URL);
        
        String paper2 = paperPage.createPaper("방문기록2", null);
        page.navigate(BASE_URL);
        
        String paper3 = paperPage.createPaper("방문기록3", null);
        page.navigate(BASE_URL);
        
        int visitCount = paperPage.getVisitHistoryCount();
        System.out.println("방문 기록 개수: " + visitCount);
        
        assertThat(visitCount).isGreaterThanOrEqualTo(3);
        assertThat(visitCount).isLessThanOrEqualTo(5);
    }
    
    @Test
    @Order(9)
    @DisplayName("09. 공유 URL 테스트")
    void testShareURL() {
        System.out.println(">>> 공유 URL 테스트 시작");
        
        createdPaperUrl = paperPage.createPaper("공유 테스트", null);
        
        String shareUrl = paperPage.getShareUrl();
        assertThat(shareUrl).isNotNull();
        assertThat(shareUrl).contains("/paper/");
        
        System.out.println("공유 URL: " + shareUrl);
        
        page.navigate(shareUrl);
        paperPage.waitForPageLoad();
        
        assertThat(page.url()).isEqualTo(shareUrl);
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 그리드 셀 가득 채우기 테스트")
    void fillAllGridCells() {
        System.out.println(">>> 그리드 셀 가득 채우기 테스트 시작");
        
        createdPaperUrl = paperPage.createPaper("가득 채우기 테스트", null);
        
        int maxMessages = 10;
        for (int i = 0; i < maxMessages; i++) {
            boolean success = paperPage.writeMessage(i, "메시지 " + i, null);
            if (!success) {
                System.out.println("메시지 작성 실패 at index: " + i);
                break;
            }
        }
        
        int usedCells = paperPage.getUsedCellsCount();
        int emptyCells = paperPage.getEmptyCellsCount();
        
        System.out.println("최종 상태 - 사용: " + usedCells + ", 빈 셀: " + emptyCells);
        
        assertThat(usedCells).isGreaterThan(0);
        assertThat(usedCells + emptyCells).isEqualTo(60);
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        createdPaperUrl = null;
        super.afterEach(testInfo);
    }
}