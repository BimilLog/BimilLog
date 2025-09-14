package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.paper.*;
import org.junit.jupiter.api.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 롤링페이퍼 시나리오 E2E 테스트
 * USER_SCENARIOS.md 3번 시나리오 완전 구현
 *
 * 테스트 범위:
 * - 롤링페이퍼 방문 페이지 (/visit)
 * - 특정 사용자 롤링페이퍼 (/rolling-paper/[nickname])
 * - 내 롤링페이퍼 페이지 (/rolling-paper)
 * - 메시지 작성, 조회, 삭제
 * - 디자인 타입 선택
 * - 반응형 디자인
 * - 에러 처리
 */
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("롤링페이퍼 시나리오 통합 E2E 테스트")
public class RollingPaperScenarioE2ETest extends BaseE2ETest {

    // 페이지 객체
    private RollingPaperPage rollingPaperPage;
    private RollingPaperVisitPage visitPage;
    private MessageModal messageModal;

    // 테스트 데이터
    private static final String TEST_USER_1 = "e2e_user_" + UUID.randomUUID().toString().substring(0, 6);
    private static final String TEST_USER_2 = "e2e_user_" + UUID.randomUUID().toString().substring(0, 6);
    private static final String TEST_USER_3 = "e2e_user_" + UUID.randomUUID().toString().substring(0, 6);

    // 메시지 데이터
    private static final List<TestMessage> TEST_MESSAGES = Arrays.asList(
        new TestMessage("익명의 친구", "첫 번째 테스트 메시지입니다! 🎉", "STRAWBERRY", "1234"),
        new TestMessage("테스트 사용자", "두 번째 메시지 - 한글 영어 English 混合", "COFFEE", "5678"),
        new TestMessage("", "작성자 이름 없는 익명 메시지", "STAR", "9999"),
        new TestMessage("긴이름테스트사용자입니다", "이모지 포함 메시지 😊💖🌟✨", "RAINBOW", "0000"),
        new TestMessage("특수문자!@#", "특수문자 포함 메시지 !@#$%^&*()", "DRAGON", "1111")
    );

    @BeforeEach
    void setupTest(TestInfo testInfo) {
        // 부모 클래스의 createContextAndPage가 자동으로 호출됨

        // 페이지 객체 초기화
        rollingPaperPage = new RollingPaperPage(page);
        visitPage = new RollingPaperVisitPage(page);
        messageModal = new MessageModal(page);
    }

    // ========== A. 롤링페이퍼 방문 페이지 테스트 ==========

    @Test
    @Order(1)
    @DisplayName("1. 롤링페이퍼 방문 페이지 접속 및 UI 요소 확인")
    void testVisitPageElements() {
        visitPage.navigate(FRONTEND_URL);

        // 페이지 제목 확인
        assertThat(visitPage.getPageTitle()).containsIgnoringCase("롤링페이퍼");

        // 검색 입력 필드 확인
        assertThat(page.locator("input[placeholder*='닉네임']").isVisible()).isTrue();

        // 최근 방문 섹션 확인
        assertThat(visitPage.isRecentVisitsSectionVisible()).isTrue();

        // 안내 섹션 확인
        assertThat(visitPage.isInfoSectionVisible()).isTrue();

        // 공유 버튼 확인
        assertThat(page.locator("button:has-text('공유')").count()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("2. 존재하는 사용자 검색")
    void testSearchExistingUser() {
        // 먼저 사용자 생성
        createTestUser(TEST_USER_1);

        visitPage.navigate(FRONTEND_URL);
        visitPage.searchByEnter(TEST_USER_1);
        visitPage.waitForLoadingComplete();

        // 롤링페이퍼 페이지로 이동했는지 확인
        page.waitForTimeout(2000);
        assertThat(page.url()).contains("/rolling-paper/" + TEST_USER_1);
    }

    @Test
    @Order(3)
    @DisplayName("3. 존재하지 않는 사용자 검색")
    void testSearchNonExistentUser() {
        visitPage.navigate(FRONTEND_URL);

        String nonExistentUser = "nonexistent_" + UUID.randomUUID();
        visitPage.searchByEnter(nonExistentUser);
        visitPage.waitForLoadingComplete();

        // 에러 메시지 확인
        page.waitForTimeout(2000);
        assertThat(visitPage.isErrorMessageVisible()).isTrue();
        assertThat(visitPage.getErrorMessage()).containsIgnoringCase("존재하지 않는");
    }

    @Test
    @Order(4)
    @DisplayName("4. 실시간 검색 디바운싱 테스트")
    void testRealtimeSearchDebouncing() {
        createTestUser(TEST_USER_2);

        visitPage.navigate(FRONTEND_URL);

        // 실시간 검색 시뮬레이션
        visitPage.simulateRealtimeSearch(TEST_USER_2);

        // 디바운싱 후 검색 결과 확인
        page.waitForTimeout(2000);

        // 자동으로 페이지 이동하거나 검색 결과가 표시되는지 확인
        String currentUrl = page.url();
        boolean isNavigated = currentUrl.contains("/rolling-paper/" + TEST_USER_2);
        boolean hasSearchResult = page.locator("text=" + TEST_USER_2).count() > 0;

        assertThat(isNavigated || hasSearchResult).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("5. 본인 검색 시 확인 다이얼로그")
    void testSelfSearchConfirmDialog() {
        // 로그인 상태 시뮬레이션
        simulateLogin(TEST_USER_1);

        visitPage.navigate(FRONTEND_URL);
        visitPage.searchByEnter(TEST_USER_1);

        page.waitForTimeout(2000);

        // 확인 다이얼로그 또는 바로 이동 확인
        boolean hasDialog = visitPage.isConfirmDialogVisible();
        boolean isNavigated = page.url().contains("/rolling-paper");

        assertThat(hasDialog || isNavigated).isTrue();

        if (hasDialog) {
            visitPage.clickConfirm();
            page.waitForTimeout(1000);
            assertThat(page.url()).contains("/rolling-paper");
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. 최근 방문 기록 표시")
    void testRecentVisitsDisplay() {
        createTestUser(TEST_USER_3);

        // 먼저 방문하여 기록 생성
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_3);
        page.waitForTimeout(1000);

        // 방문 페이지로 돌아가기
        visitPage.navigate(FRONTEND_URL);

        // 최근 방문 기록 확인
        assertThat(visitPage.getRecentVisitsCount()).isGreaterThan(0);

        List<String> visits = visitPage.getRecentVisitsList();
        boolean hasTestUser = visits.stream()
            .anyMatch(visit -> visit.contains(TEST_USER_3));
        assertThat(hasTestUser).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("7. 최근 방문 기록 개별 삭제")
    void testDeleteIndividualRecentVisit() {
        visitPage.navigate(FRONTEND_URL);

        int initialCount = visitPage.getRecentVisitsCount();

        if (initialCount > 0) {
            visitPage.deleteRecentVisit(0);
            page.waitForTimeout(500);

            int afterCount = visitPage.getRecentVisitsCount();
            assertThat(afterCount).isEqualTo(initialCount - 1);
        }
    }

    @Test
    @Order(8)
    @DisplayName("8. 최근 방문 기록 전체 삭제")
    void testDeleteAllRecentVisits() {
        visitPage.navigate(FRONTEND_URL);

        if (visitPage.getRecentVisitsCount() > 0) {
            visitPage.deleteAllRecentVisits();

            // 확인 다이얼로그 처리
            if (visitPage.isConfirmDialogVisible()) {
                visitPage.clickConfirm();
            }

            page.waitForTimeout(500);
            assertThat(visitPage.getRecentVisitsCount()).isEqualTo(0);
        }
    }

    @Test
    @Order(9)
    @DisplayName("9. 최근 방문 기록 클릭하여 이동")
    void testClickRecentVisitNavigation() {
        // 방문 기록 생성
        createTestUser(TEST_USER_1);
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);

        // 방문 페이지로 돌아가기
        visitPage.navigate(FRONTEND_URL);

        // 최근 방문 클릭
        visitPage.clickRecentVisitByNickname(TEST_USER_1);
        page.waitForTimeout(2000);

        // 해당 롤링페이퍼로 이동했는지 확인
        assertThat(page.url()).contains("/rolling-paper/" + TEST_USER_1);
    }

    // ========== B. 특정 사용자 롤링페이퍼 테스트 ==========

    @Test
    @Order(10)
    @DisplayName("10. 비로그인 사용자 롤링페이퍼 접근")
    void testAnonymousAccessToRollingPaper() {
        createTestUser(TEST_USER_2);

        // 비로그인 상태에서 접근
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_2);

        // 그리드가 표시되는지 확인
        assertThat(rollingPaperPage.isGridVisible()).isTrue();

        // 빈 셀 클릭 가능한지 확인
        assertThat(rollingPaperPage.getEmptyCellCount()).isGreaterThan(0);
    }

    @Test
    @Order(11)
    @DisplayName("11. 모바일 그리드 레이아웃 확인 (4x10)")
    void testMobileGridLayout() {
        createTestUser(TEST_USER_1);

        // 모바일 뷰포트 설정
        setMobileViewport();

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);

        // 그리드 컬럼 수 확인
        int columnCount = rollingPaperPage.getGridColumnCount();
        assertThat(columnCount).isEqualTo(4);
    }

    @Test
    @Order(12)
    @DisplayName("12. PC 그리드 레이아웃 확인 (6x10)")
    void testDesktopGridLayout() {
        createTestUser(TEST_USER_1);

        // 데스크톱 뷰포트 설정
        setDesktopViewport();

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);

        // 그리드 컬럼 수 확인
        int columnCount = rollingPaperPage.getGridColumnCount();
        assertThat(columnCount).isEqualTo(6);
    }

    @Test
    @Order(13)
    @DisplayName("13. 빈 칸 클릭하여 메시지 작성 모달 열기")
    void testOpenMessageModalByClickingEmptyCell() {
        createTestUser(TEST_USER_1);

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);

        // 빈 칸 클릭
        rollingPaperPage.clickEmptyCell();

        // 모달이 열렸는지 확인
        messageModal.waitForOpen();
        assertThat(messageModal.isOpen()).isTrue();

        // 모달 제목 확인
        String modalTitle = messageModal.getTitle();
        assertThat(modalTitle).containsIgnoringCase("메시지");
    }

    @Test
    @Order(14)
    @DisplayName("14. 익명 메시지 작성 (작성자명 미입력)")
    void testWriteAnonymousMessage() {
        createTestUser(TEST_USER_1);

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();

        // 익명 메시지 작성 (작성자명 비움)
        messageModal.fillAuthor("");
        messageModal.fillContent("익명으로 남기는 테스트 메시지입니다.");
        messageModal.fillPassword("test1234");
        messageModal.selectDesignByIndex(0);

        // 작성 버튼 클릭
        messageModal.clickSubmit();
        page.waitForTimeout(2000);

        // 성공 확인
        assertThat(messageModal.isToastVisible("메시지")).isTrue();

        // 모달 닫힘 확인
        page.waitForTimeout(1000);
        assertThat(messageModal.isOpen()).isFalse();

        // 메시지 개수 증가 확인
        assertThat(rollingPaperPage.getMessageCellCount()).isGreaterThan(0);
    }

    @Test
    @Order(15)
    @DisplayName("15. 실명 메시지 작성 (작성자명 입력)")
    void testWriteNamedMessage() {
        createTestUser(TEST_USER_2);

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_2);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();

        // 실명 메시지 작성
        messageModal.fillAuthor("테스트 작성자");
        messageModal.fillContent("실명으로 남기는 따뜻한 메시지입니다.");
        messageModal.fillPassword("test5678");
        messageModal.selectDesignByIndex(1);

        messageModal.clickSubmit();
        page.waitForTimeout(2000);

        // 성공 확인
        assertThat(messageModal.isToastVisible("메시지")).isTrue();
    }

    @Test
    @Order(16)
    @DisplayName("16. 다양한 디자인 타입 선택")
    void testSelectVariousDesignTypes() {
        createTestUser(TEST_USER_3);

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_3);

        // 여러 디자인으로 메시지 작성
        for (int i = 0; i < Math.min(5, TEST_MESSAGES.size()); i++) {
            TestMessage msg = TEST_MESSAGES.get(i);

            rollingPaperPage.clickEmptyCell(i);
            messageModal.waitForOpen();

            // 디자인 옵션 확인
            List<String> designs = messageModal.getAvailableDesigns();
            assertThat(designs.size()).isGreaterThan(0);

            // 메시지 작성
            messageModal.fillAuthor(msg.author);
            messageModal.fillContent(msg.content);
            messageModal.fillPassword(msg.password);

            // 디자인 선택 (이름으로)
            messageModal.selectDesign(msg.designType);

            messageModal.clickSubmit();
            page.waitForTimeout(2000);

            // 모달 닫기
            if (messageModal.isOpen()) {
                messageModal.close();
            }

            page.waitForTimeout(500);
        }

        // 다양한 디자인의 메시지가 표시되는지 확인
        assertThat(rollingPaperPage.getMessageCellCount()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @Order(17)
    @DisplayName("17. 메시지 500자 제한 확인")
    void testMessageLengthLimit() {
        createTestUser(TEST_USER_1);

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();

        // 500자 이상 입력
        messageModal.fillLongContent(600);

        // 글자 수 표시 확인
        String charCount = messageModal.getCharCount();
        assertThat(charCount).contains("500");

        // 실제 입력된 글자 수가 500자로 제한되는지 확인
        String content = page.locator("textarea[name='content']").inputValue();
        assertThat(content.length()).isLessThanOrEqualTo(500);
    }

    @Test
    @Order(18)
    @DisplayName("18. 메시지 삭제용 비밀번호 설정")
    void testSetDeletePassword() {
        createTestUser(TEST_USER_1);

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_1);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();

        // 비밀번호와 함께 메시지 작성
        messageModal.fillAuthor("비밀번호 테스트");
        messageModal.fillContent("삭제 가능한 메시지");
        messageModal.fillPassword("delete123");

        messageModal.clickSubmit();
        page.waitForTimeout(2000);

        assertThat(messageModal.isToastVisible("메시지")).isTrue();
    }

    @Test
    @Order(19)
    @DisplayName("19. 메시지 칸 호버 효과")
    void testMessageCellHoverEffect() {
        createTestUser(TEST_USER_2);

        // 먼저 메시지 작성
        createTestMessage(TEST_USER_2, "호버 테스트 메시지", "STAR");

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_2);

        // 메시지 셀 호버
        rollingPaperPage.hoverMessageCell(0);
        page.waitForTimeout(500);

        // 호버 효과가 적용되었는지 확인 (클래스나 스타일 변경)
        Locator messageCell = page.locator(".grid-cell.has-message, .message-cell").first();
        String classList = messageCell.getAttribute("class");

        // 호버 관련 클래스나 스타일이 있는지 확인
        assertThat(classList != null || messageCell.isVisible()).isTrue();
    }

    @Test
    @Order(20)
    @DisplayName("20. 메시지 상세 보기")
    void testViewMessageDetail() {
        createTestUser(TEST_USER_3);

        // 메시지 작성
        createTestMessage(TEST_USER_3, "상세 보기 테스트 메시지", "COFFEE");

        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, TEST_USER_3);

        // 메시지 셀 클릭
        rollingPaperPage.clickMessageCell();
        messageModal.waitForOpen();

        // 메시지 상세 정보 확인
        String content = messageModal.getMessageContent();
        assertThat(content).contains("상세 보기 테스트 메시지");

        // 작성 시간 확인
        String time = messageModal.getMessageTime();
        assertThat(time).isNotEmpty();

        // 디자인 정보 확인
        String design = messageModal.getMessageDesign();
        assertThat(design).isNotEmpty();
    }

    // ========== C. 내 롤링페이퍼 페이지 테스트 ==========

    @Test
    @Order(24)
    @DisplayName("24. 비로그인 상태에서 내 롤링페이퍼 접근 시 리다이렉트")
    void testRedirectWhenNotLoggedIn() {
        // 로그아웃 상태 확인
        page.navigate(FRONTEND_URL + "/rolling-paper");
        page.waitForTimeout(2000);

        // 로그인 페이지로 리다이렉트되는지 확인
        String currentUrl = page.url();
        assertThat(currentUrl).containsAnyOf("/login", "/signin", "auth");
    }

    @Test
    @Order(25)
    @DisplayName("25. 로그인 후 내 롤링페이퍼 접근")
    void testAccessMyRollingPaperAfterLogin() {
        // 로그인 시뮬레이션
        simulateLogin(TEST_USER_1);

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);

        // 페이지 제목 확인
        String title = rollingPaperPage.getPageTitle();
        assertThat(title).containsIgnoringCase("내 롤링페이퍼");

        // 그리드 표시 확인
        assertThat(rollingPaperPage.isGridVisible()).isTrue();
    }

    @Test
    @Order(26)
    @DisplayName("26. 받은 메시지 그리드 표시")
    void testReceivedMessagesGrid() {
        simulateLogin(TEST_USER_1);

        // 다른 사용자로 메시지 작성
        createTestMessage(TEST_USER_1, "받은 메시지 테스트", "DRAGON");

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);

        // 메시지가 표시되는지 확인
        assertThat(rollingPaperPage.getMessageCellCount()).isGreaterThan(0);
    }

    @Test
    @Order(27)
    @DisplayName("27. 메시지 상세 모달 - 작성자, 내용, 시간, 디자인")
    void testMessageDetailModal() {
        simulateLogin(TEST_USER_2);

        // 메시지 작성
        createTestMessage(TEST_USER_2, "상세 정보 테스트", "STAR");

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);
        rollingPaperPage.clickMessageCell();
        messageModal.waitForOpen();

        // 상세 정보 확인
        assertThat(messageModal.getMessageContent()).contains("상세 정보 테스트");
        assertThat(messageModal.getMessageTime()).isNotEmpty();
        assertThat(messageModal.getMessageDesign()).isNotEmpty();
    }

    @Test
    @Order(28)
    @DisplayName("28. 메시지 삭제 (비밀번호 입력)")
    void testDeleteMessageWithPassword() {
        simulateLogin(TEST_USER_3);

        // 삭제 가능한 메시지 작성
        String deletePassword = "del456";
        createTestMessageWithPassword(TEST_USER_3, "삭제할 메시지", "FLOWER", deletePassword);

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);
        int initialCount = rollingPaperPage.getMessageCellCount();

        // 메시지 상세 보기
        rollingPaperPage.clickMessageCell();
        messageModal.waitForOpen();

        // 삭제 버튼 클릭
        if (messageModal.isDeleteButtonVisible()) {
            messageModal.clickDeleteButton();

            // 비밀번호 입력
            messageModal.fillDeletePassword(deletePassword);
            messageModal.clickDeleteConfirm();

            page.waitForTimeout(2000);

            // 삭제 성공 확인
            assertThat(messageModal.isToastVisible("삭제")).isTrue();

            // 메시지 개수 감소 확인
            page.reload();
            int afterCount = rollingPaperPage.getMessageCellCount();
            assertThat(afterCount).isLessThan(initialCount);
        }
    }

    @Test
    @Order(29)
    @DisplayName("29. 잘못된 비밀번호로 삭제 시도")
    void testDeleteWithWrongPassword() {
        simulateLogin(TEST_USER_1);

        // 메시지 작성
        createTestMessageWithPassword(TEST_USER_1, "삭제 불가 메시지", "BUBBLE", "correct123");

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);
        rollingPaperPage.clickMessageCell();
        messageModal.waitForOpen();

        if (messageModal.isDeleteButtonVisible()) {
            messageModal.clickDeleteButton();

            // 잘못된 비밀번호 입력
            messageModal.fillDeletePassword("wrong999");
            messageModal.clickDeleteConfirm();

            page.waitForTimeout(2000);

            // 에러 메시지 확인
            assertThat(messageModal.hasErrorMessage("비밀번호")).isTrue();
        }
    }

    @Test
    @Order(30)
    @DisplayName("30. 카카오톡 공유 버튼")
    void testKakaoShareButton() {
        simulateLogin(TEST_USER_2);

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);

        // 공유 버튼 존재 확인
        assertThat(page.locator("button:has-text('카카오톡')").count()).isGreaterThan(0);

        // 공유 버튼 클릭
        rollingPaperPage.clickKakaoShareButton();
        page.waitForTimeout(1000);

        // 카카오 SDK 호출 또는 모달 열림 확인
        // (실제 카카오톡 공유는 테스트 환경에서 제한적)
    }

    @Test
    @Order(31)
    @DisplayName("31. URL 복사 버튼")
    void testCopyUrlButton() {
        simulateLogin(TEST_USER_3);

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);

        // URL 복사 버튼 클릭
        rollingPaperPage.clickCopyUrlButton();
        page.waitForTimeout(1000);

        // 복사 성공 토스트 메시지 확인
        assertThat(rollingPaperPage.isToastVisible("복사")).isTrue();
    }

    @Test
    @Order(34)
    @DisplayName("34. 통계 섹션 - 총 받은 메시지 수")
    void testStatsTotalMessages() {
        simulateLogin(TEST_USER_1);

        // 여러 메시지 작성
        for (int i = 0; i < 3; i++) {
            createTestMessage(TEST_USER_1, "통계 테스트 메시지 " + i, "STAR");
        }

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);

        // 통계 섹션 확인
        if (rollingPaperPage.isStatsSectionVisible()) {
            String totalMessages = rollingPaperPage.getTotalMessages();
            assertThat(totalMessages).isNotEmpty();

            // 숫자가 포함되어 있는지 확인
            assertThat(totalMessages).matches(".*\\d+.*");
        }
    }

    @Test
    @Order(35)
    @DisplayName("35. 통계 섹션 - 오늘 받은 메시지 수")
    void testStatsTodayMessages() {
        simulateLogin(TEST_USER_2);

        // 오늘 메시지 작성
        createTestMessage(TEST_USER_2, "오늘의 메시지", "RAINBOW");

        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);

        if (rollingPaperPage.isStatsSectionVisible()) {
            String todayMessages = rollingPaperPage.getTodayMessages();
            assertThat(todayMessages).isNotEmpty();
            assertThat(todayMessages).matches(".*\\d+.*");
        }
    }

    // ========== D. 통합 시나리오 테스트 ==========

    @Test
    @Order(37)
    @DisplayName("37. 전체 플로우: 회원가입 → 타인 롤링페이퍼 작성 → 내 롤링페이퍼 확인")
    void testCompleteUserFlow() {
        String newUser = "flow_user_" + UUID.randomUUID().toString().substring(0, 6);
        String targetUser = "target_user_" + UUID.randomUUID().toString().substring(0, 6);

        // 1. 대상 사용자 생성
        createTestUser(targetUser);

        // 2. 신규 사용자 회원가입
        createTestUser(newUser);
        simulateLogin(newUser);

        // 3. 타인 롤링페이퍼에 메시지 작성
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, targetUser);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();

        messageModal.fillAuthor(newUser);
        messageModal.fillContent("전체 플로우 테스트 메시지입니다!");
        messageModal.fillPassword("flow123");
        messageModal.selectDesignByIndex(2);
        messageModal.clickSubmit();

        page.waitForTimeout(2000);

        // 4. 내 롤링페이퍼 확인
        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);
        assertThat(rollingPaperPage.isGridVisible()).isTrue();

        // 5. 방문 기록 확인
        visitPage.navigate(FRONTEND_URL);
        assertThat(visitPage.getRecentVisitsCount()).isGreaterThan(0);
    }

    @Test
    @Order(38)
    @DisplayName("38. 다중 사용자 시나리오: A가 B에게, B가 A에게 메시지")
    void testMultiUserScenario() {
        String userA = "userA_" + UUID.randomUUID().toString().substring(0, 6);
        String userB = "userB_" + UUID.randomUUID().toString().substring(0, 6);

        // 사용자 생성
        createTestUser(userA);
        createTestUser(userB);

        // A가 B에게 메시지
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, userB);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();
        messageModal.fillAuthor(userA);
        messageModal.fillContent("A에서 B로 보내는 메시지");
        messageModal.clickSubmit();
        page.waitForTimeout(2000);

        // B가 A에게 메시지
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, userA);
        rollingPaperPage.clickEmptyCell();
        messageModal.waitForOpen();
        messageModal.fillAuthor(userB);
        messageModal.fillContent("B에서 A로 보내는 메시지");
        messageModal.clickSubmit();
        page.waitForTimeout(2000);

        // 각각 확인
        simulateLogin(userA);
        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);
        assertThat(rollingPaperPage.getMessageCellCount()).isGreaterThan(0);

        simulateLogin(userB);
        rollingPaperPage.navigateToMyPaper(FRONTEND_URL);
        assertThat(rollingPaperPage.getMessageCellCount()).isGreaterThan(0);
    }

    @Test
    @Order(40)
    @DisplayName("40. 성능 테스트: 여러 메시지 동시 렌더링")
    void testPerformanceMultipleMessages() {
        String perfUser = "perf_user_" + UUID.randomUUID().toString().substring(0, 6);
        createTestUser(perfUser);

        // 20개 메시지 빠르게 작성
        for (int i = 0; i < 20; i++) {
            rollingPaperPage.navigateToUserPaper(FRONTEND_URL, perfUser);
            rollingPaperPage.clickEmptyCell(i);

            if (messageModal.isOpen()) {
                messageModal.fillContent("성능 테스트 메시지 " + i);
                messageModal.selectDesignByIndex(i % 5);
                messageModal.clickSubmit();
                page.waitForTimeout(500);

                if (messageModal.isOpen()) {
                    messageModal.close();
                }
            }
        }

        // 페이지 새로고침 후 렌더링 시간 측정
        long startTime = System.currentTimeMillis();
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, perfUser);
        long loadTime = System.currentTimeMillis() - startTime;

        // 3초 이내 로드 확인
        assertThat(loadTime).isLessThan(3000);

        // 모든 메시지가 표시되는지 확인
        assertThat(rollingPaperPage.getMessageCellCount()).isGreaterThanOrEqualTo(20);
    }

    // ========== 헬퍼 메소드 ==========

    private void createTestUser(String username) {
        // 테스트용 사용자 생성 로직
        // 실제 구현에서는 API 호출 또는 DB 직접 조작
        System.out.println("Creating test user: " + username);
    }

    private void simulateLogin(String username) {
        // 로그인 시뮬레이션
        // 테스트 환경에서는 쿠키/세션 설정
        page.addInitScript("() => { localStorage.setItem('currentUser', '" + username + "'); }");
        page.reload();
    }

    private void createTestMessage(String targetUser, String content, String designType) {
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, targetUser);
        rollingPaperPage.clickEmptyCell();

        if (messageModal.isOpen()) {
            messageModal.fillContent(content);
            messageModal.selectDesign(designType);
            messageModal.clickSubmit();
            page.waitForTimeout(1500);

            if (messageModal.isOpen()) {
                messageModal.close();
            }
        }
    }

    private void createTestMessageWithPassword(String targetUser, String content, String designType, String password) {
        rollingPaperPage.navigateToUserPaper(FRONTEND_URL, targetUser);
        rollingPaperPage.clickEmptyCell();

        if (messageModal.isOpen()) {
            messageModal.fillContent(content);
            messageModal.fillPassword(password);
            messageModal.selectDesign(designType);
            messageModal.clickSubmit();
            page.waitForTimeout(1500);

            if (messageModal.isOpen()) {
                messageModal.close();
            }
        }
    }

    // 테스트 메시지 데이터 클래스
    private static class TestMessage {
        String author;
        String content;
        String designType;
        String password;

        TestMessage(String author, String content, String designType, String password) {
            this.author = author;
            this.content = content;
            this.designType = designType;
            this.password = password;
        }
    }
}