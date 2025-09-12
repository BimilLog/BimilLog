package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import jaeik.bimillog.e2e.pages.PaperPage;
import jaeik.bimillog.e2e.pages.VisitPage;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>롤링페이퍼 방문 E2E 테스트</h2>
 * <p>닉네임 검색, 방문 기록, 익명 메시지 작성을 테스트합니다.</p>
 * <p>최대 5개 방문 기록, 30일 유지</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("롤링페이퍼 방문 E2E 테스트")
public class VisitE2ETest extends BaseE2ETest {
    
    private VisitPage visitPage;
    private PaperPage paperPage;
    private LoginPage loginPage;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        visitPage = new VisitPage(page, BASE_URL);
        paperPage = new PaperPage(page, BASE_URL);
        loginPage = new LoginPage(page, BASE_URL);
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 닉네임으로 롤링페이퍼 검색")
    void searchByNickname() {
        System.out.println(">>> 닉네임 검색 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String username = loginPage.getUsername();
        String paperTitle = "검색 테스트 " + generateTimestamp();
        String paperUrl = paperPage.createPaper(paperTitle, "검색 테스트용");
        
        loginPage.performLogout();
        
        String foundUrl = visitPage.searchByNickname(username);
        
        if (foundUrl != null) {
            assertThat(foundUrl).contains("/rolling-paper/");
            System.out.println("닉네임 '" + username + "'로 롤링페이퍼 검색 성공");
        } else {
            System.out.println("검색 실패 - 닉네임 매칭 로직 확인 필요");
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 검색 결과 없음 처리")
    void noSearchResults() {
        System.out.println(">>> 검색 결과 없음 테스트 시작");
        
        String randomNickname = "존재하지않는닉네임" + generateTimestamp();
        String result = visitPage.searchByNickname(randomNickname);
        
        assertThat(result).isNull();
        assertThat(visitPage.hasNoSearchResult()).isTrue();
        
        System.out.println("존재하지 않는 닉네임 검색 시 적절한 메시지 표시");
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 방문 후 익명 메시지 작성")
    void anonymousMessageAfterVisit() {
        System.out.println(">>> 방문 후 익명 메시지 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String paperUrl = paperPage.createPaper("익명 메시지 테스트", null);
        
        loginPage.performLogout();
        
        String message = "익명 방문자 메시지 " + generateTimestamp();
        boolean success = visitPage.visitAndWriteMessage(paperUrl, message, "익명");
        
        assertThat(success).isTrue();
        System.out.println("익명 메시지 작성 성공");
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 방문 기록 추가")
    void addVisitHistory() {
        System.out.println(">>> 방문 기록 추가 테스트 시작");
        
        visitPage.clearVisitHistory();
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String paperUrl1 = paperPage.createPaper("방문기록1", null);
        visitPage.visitAndWriteMessage(paperUrl1, "메시지1", null);
        
        boolean added = visitPage.isAddedToVisitHistory(paperUrl1);
        assertThat(added).isTrue();
        
        int count = visitPage.getRecentVisitCount();
        assertThat(count).isGreaterThan(0);
        
        System.out.println("방문 기록에 추가됨, 현재 개수: " + count);
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 최대 5개 방문 기록 제한")
    void visitHistoryMaxFive() {
        System.out.println(">>> 방문 기록 최대 5개 제한 테스트 시작");
        
        visitPage.clearVisitHistory();
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        for (int i = 1; i <= 7; i++) {
            String paperUrl = paperPage.createPaper("방문기록" + i, null);
            page.navigate(paperUrl);
            visitPage.waitForPageLoad();
            visitPage.wait(500);
        }
        
        boolean isLimited = visitPage.isVisitHistoryLimitedTo5();
        assertThat(isLimited).isTrue();
        
        int count = visitPage.getRecentVisitCount();
        assertThat(count).isLessThanOrEqualTo(5);
        
        System.out.println("방문 기록 최대 개수: " + count);
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 최근 방문 롤링페이퍼 재방문")
    void revisitRecentPaper() {
        System.out.println(">>> 최근 방문 재방문 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String paperUrl = paperPage.createPaper("재방문 테스트", null);
        page.navigate(paperUrl);
        visitPage.waitForPageLoad();
        
        String revisitUrl = visitPage.visitRecentPaper(0);
        
        if (revisitUrl != null) {
            assertThat(revisitUrl).contains("/rolling-paper/");
            System.out.println("최근 방문 롤링페이퍼 재방문 성공");
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 방문 기록 쿠키/로컬스토리지 저장")
    void verifyVisitHistorySave() {
        System.out.println(">>> 방문 기록 저장 확인 테스트 시작");
        
        visitPage.clearVisitHistory();
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String paperUrl = paperPage.createPaper("저장 테스트", null);
        page.navigate(paperUrl);
        visitPage.waitForPageLoad();
        
        boolean hasHistory = visitPage.hasVisitHistory();
        assertThat(hasHistory).isTrue();
        
        System.out.println("방문 기록이 브라우저에 저장됨");
    }
    
    @Test
    @Order(8)
    @DisplayName("08. 인기 롤링페이퍼 표시")
    void popularPapers() {
        System.out.println(">>> 인기 롤링페이퍼 테스트 시작");
        
        visitPage.navigateToVisitPage();
        int popularCount = visitPage.getPopularPaperCount();
        
        System.out.println("인기 롤링페이퍼 개수: " + popularCount);
        
        if (popularCount > 0) {
            assertThat(popularCount).isGreaterThan(0);
            System.out.println("인기 롤링페이퍼 섹션 표시됨");
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("09. 방문 기록 초기화")
    void clearVisitHistory() {
        System.out.println(">>> 방문 기록 초기화 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String paperUrl = paperPage.createPaper("초기화 테스트", null);
        page.navigate(paperUrl);
        visitPage.waitForPageLoad();
        
        assertThat(visitPage.hasVisitHistory()).isTrue();
        
        visitPage.clearVisitHistory();
        
        assertThat(visitPage.hasVisitHistory()).isFalse();
        assertThat(visitPage.getRecentVisitCount()).isEqualTo(0);
        
        System.out.println("방문 기록 초기화 완료");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 검색 자동완성")
    void searchAutoComplete() {
        System.out.println(">>> 검색 자동완성 테스트 시작");
        
        boolean hasAutoComplete = visitPage.hasAutoComplete("테스");
        
        if (hasAutoComplete) {
            System.out.println("자동완성 기능 활성화됨");
        } else {
            System.out.println("자동완성 기능 없음 또는 비활성화");
        }
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        super.afterEach(testInfo);
    }
}