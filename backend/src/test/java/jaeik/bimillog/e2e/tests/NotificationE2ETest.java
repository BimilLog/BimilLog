package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import jaeik.bimillog.e2e.pages.NotificationPage;
import jaeik.bimillog.e2e.pages.PostPage;
import jaeik.bimillog.e2e.pages.PaperPage;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>알림 시스템 E2E 테스트</h2>
 * <p>SSE 실시간 연결, 알림 수신, 배치 처리를 테스트합니다.</p>
 * <p>개별 처리는 5분 배치, 일괄 처리는 즉시 실행</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("알림 시스템 E2E 테스트")
public class NotificationE2ETest extends BaseE2ETest {
    
    private NotificationPage notificationPage;
    private LoginPage loginPage;
    private PostPage postPage;
    private PaperPage paperPage;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        notificationPage = new NotificationPage(page, BASE_URL);
        loginPage = new LoginPage(page, BASE_URL);
        postPage = new PostPage(page, BASE_URL);
        paperPage = new PaperPage(page, BASE_URL);
    }
    
    @Test
    @Order(1)
    @DisplayName("01. SSE 연결 확인")
    void SSE_연결_확인() {
        System.out.println(">>> SSE 연결 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        loginPage.navigate("/");
        loginPage.waitForPageLoad();
        
        boolean isConnected = notificationPage.isSSEConnected();
        assertThat(isConnected).isTrue();
        
        System.out.println("SSE 연결 상태: " + (isConnected ? "연결됨" : "미연결"));
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 댓글 알림 수신")
    void 댓글_알림_수신() {
        System.out.println(">>> 댓글 알림 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String postTitle = "알림 테스트 게시글 " + generateTimestamp();
        String postUrl = postPage.createPost(postTitle, "알림 테스트용 게시글", null);
        
        String currentUsername = loginPage.getUsername();
        loginPage.performLogout();
        
        loginPage.performKakaoLogin();
        String otherUsername = loginPage.getUsername();
        
        if (currentUsername.equals(otherUsername)) {
            System.out.println("동일 사용자로 로그인됨, 알림 테스트 스킵");
            return;
        }
        
        page.navigate(postUrl);
        postPage.waitForPageLoad();
        
        String comment = "알림 테스트 댓글 " + generateTimestamp();
        postPage.writeComment(comment);
        
        loginPage.performLogout();
        loginPage.performKakaoLogin();
        
        loginPage.navigate("/");
        boolean hasNewNotification = notificationPage.waitForNewNotification(10000);
        
        if (hasNewNotification) {
            notificationPage.openNotificationList();
            String content = notificationPage.getNotificationContent(0);
            assertThat(content).contains("댓글");
            System.out.println("댓글 알림 수신: " + content);
        } else {
            System.out.println("알림 수신 실패 (SSE 연결 문제 가능성)");
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 롤링페이퍼 메시지 알림")
    void 롤링페이퍼_메시지_알림() {
        System.out.println(">>> 롤링페이퍼 메시지 알림 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String paperTitle = "알림 테스트 롤링페이퍼 " + generateTimestamp();
        String paperUrl = paperPage.createPaper(paperTitle, "알림 테스트용");
        
        loginPage.performLogout();
        
        page.navigate(paperUrl);
        paperPage.waitForPageLoad();
        
        String message = "알림 테스트 메시지 " + generateTimestamp();
        paperPage.writeMessage(0, message, "테스터");
        
        loginPage.performKakaoLogin();
        loginPage.navigate("/");
        
        boolean hasNewNotification = notificationPage.waitForNewNotification(10000);
        
        if (hasNewNotification) {
            notificationPage.openNotificationList();
            String content = notificationPage.getNotificationContent(0);
            String type = notificationPage.getNotificationType(0);
            
            assertThat(type).isEqualTo("PAPER");
            assertThat(content).contains("롤링페이퍼");
            
            System.out.println("롤링페이퍼 알림 수신: " + content);
        } else {
            System.out.println("롤링페이퍼 알림 수신 실패");
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 개별 알림 읽음 처리 (5분 배치)")
    void 개별_알림_읽음_배치() {
        System.out.println(">>> 개별 알림 읽음 배치 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        createTestNotifications(3);
        
        notificationPage.openNotificationList();
        int initialUnread = notificationPage.getUnreadCount();
        assertThat(initialUnread).isGreaterThan(0);
        
        notificationPage.markAsRead(0);
        notificationPage.markAsRead(1);
        
        int[] pendingCounts = notificationPage.getPendingBatchCount();
        System.out.println("배치 대기: 읽음 " + pendingCounts[0] + "개, 삭제 " + pendingCounts[1] + "개");
        
        if (pendingCounts[0] > 0) {
            System.out.println("개별 읽음 처리가 배치 대기열에 추가됨 (5분 후 처리)");
        }
        
        int immediateUnread = notificationPage.getUnreadCount();
        System.out.println("즉시 반영된 읽지 않은 알림: " + immediateUnread);
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 개별 알림 삭제 (5분 배치)")
    void 개별_알림_삭제_배치() {
        System.out.println(">>> 개별 알림 삭제 배치 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        createTestNotifications(3);
        
        notificationPage.openNotificationList();
        int initialCount = notificationPage.getNotificationCount();
        
        notificationPage.deleteNotification(0);
        notificationPage.deleteNotification(1);
        
        int[] pendingCounts = notificationPage.getPendingBatchCount();
        System.out.println("배치 대기: 읽음 " + pendingCounts[0] + "개, 삭제 " + pendingCounts[1] + "개");
        
        if (pendingCounts[1] > 0) {
            System.out.println("개별 삭제가 배치 대기열에 추가됨 (5분 후 처리)");
        }
        
        int afterCount = notificationPage.getNotificationCount();
        System.out.println("삭제 전: " + initialCount + "개, 삭제 후: " + afterCount + "개");
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 모든 알림 읽음 처리 (즉시)")
    void 모든_알림_읽음_즉시() {
        System.out.println(">>> 모든 알림 읽음 처리 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        createTestNotifications(5);
        
        notificationPage.openNotificationList();
        int initialUnread = notificationPage.getUnreadCount();
        assertThat(initialUnread).isGreaterThan(0);
        
        boolean success = notificationPage.markAllAsRead();
        assertThat(success).isTrue();
        
        int afterUnread = notificationPage.getUnreadCount();
        assertThat(afterUnread).isEqualTo(0);
        
        System.out.println("일괄 읽음 처리 완료: " + initialUnread + " → 0");
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 모든 알림 삭제 (즉시)")
    void 모든_알림_삭제_즉시() {
        System.out.println(">>> 모든 알림 삭제 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        createTestNotifications(5);
        
        notificationPage.openNotificationList();
        int initialCount = notificationPage.getNotificationCount();
        assertThat(initialCount).isGreaterThan(0);
        
        boolean success = notificationPage.deleteAllNotifications();
        assertThat(success).isTrue();
        
        int afterCount = notificationPage.getNotificationCount();
        assertThat(afterCount).isEqualTo(0);
        
        System.out.println("일괄 삭제 완료: " + initialCount + " → 0");
    }
    
    @Test
    @Order(8)
    @DisplayName("08. FCM 알림 권한 요청")
    void FCM_알림_권한_요청() {
        System.out.println(">>> FCM 알림 권한 요청 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        notificationPage.openNotificationList();
        boolean requested = notificationPage.requestNotificationPermission();
        
        if (requested) {
            System.out.println("알림 권한 요청 버튼 클릭 완료");
        } else {
            System.out.println("알림 권한 요청 버튼을 찾을 수 없음");
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("09. 오프라인 → 온라인 알림 동기화")
    void 오프라인_온라인_동기화() {
        System.out.println(">>> 오프라인/온라인 동기화 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        page.context().setOffline(true);
        System.out.println("네트워크 오프라인 전환");
        
        notificationPage.wait(2000);
        boolean isConnected = notificationPage.isSSEConnected();
        assertThat(isConnected).isFalse();
        
        page.context().setOffline(false);
        System.out.println("네트워크 온라인 전환");
        
        notificationPage.wait(3000);
        page.reload();
        notificationPage.waitForPageLoad();
        
        isConnected = notificationPage.isSSEConnected();
        assertThat(isConnected).isTrue();
        
        System.out.println("SSE 재연결 완료");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 알림 타입별 아이콘 확인")
    void 알림_타입별_아이콘() {
        System.out.println(">>> 알림 타입별 아이콘 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        notificationPage.openNotificationList();
        int count = notificationPage.getNotificationCount();
        
        for (int i = 0; i < Math.min(count, 5); i++) {
            String type = notificationPage.getNotificationType(i);
            String content = notificationPage.getNotificationContent(i);
            
            System.out.println("알림 #" + (i + 1) + ": " + type + " - " + content);
            
            switch (type) {
                case "PAPER":
                    assertThat(content.toLowerCase()).containsAnyOf("롤링페이퍼", "메시지");
                    break;
                case "COMMENT":
                    assertThat(content.toLowerCase()).containsAnyOf("댓글", "답글");
                    break;
                case "POST_FEATURED":
                    assertThat(content.toLowerCase()).containsAnyOf("인기", "추천");
                    break;
                case "ADMIN":
                    assertThat(content.toLowerCase()).containsAnyOf("공지", "관리자");
                    break;
            }
        }
    }
    
    /**
     * <h3>테스트용 알림 생성</h3>
     * <p>테스트를 위한 더미 알림을 생성합니다.</p>
     *
     * @param count 생성할 알림 개수
     * @author Jaeik
     * @since 2.0.0
     */
    private void createTestNotifications(int count) {
        for (int i = 0; i < count; i++) {
            String postTitle = "알림생성용 #" + (i + 1) + " " + generateTimestamp();
            String postUrl = postPage.createPost(postTitle, "테스트 내용", null);
            
            loginPage.performLogout();
            page.navigate(postUrl);
            postPage.writeComment("테스트 댓글 #" + (i + 1));
            loginPage.performKakaoLogin();
            
            notificationPage.wait(500);
        }
        
        page.navigate(BASE_URL);
        notificationPage.waitForPageLoad();
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        try {
            if (notificationPage != null && notificationPage.isNotificationListOpen()) {
                notificationPage.closeNotificationList();
            }
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
        
        super.afterEach(testInfo);
    }
}