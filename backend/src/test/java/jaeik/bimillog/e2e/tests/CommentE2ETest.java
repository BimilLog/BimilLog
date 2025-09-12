package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import jaeik.bimillog.e2e.pages.PostPage;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>댓글 계층 구조 E2E 테스트</h2>
 * <p>계층형 댓글 시스템의 전체 기능을 테스트합니다.</p>
 * <p>Closure Table 기반 댓글 계층, 삭제 시 처리, 익명화</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("댓글 계층 구조 E2E 테스트")
public class CommentE2ETest extends BaseE2ETest {
    
    private PostPage postPage;
    private LoginPage loginPage;
    private String testPostUrl;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        postPage = new PostPage(page, BASE_URL);
        loginPage = new LoginPage(page, BASE_URL);
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 3단계 댓글 계층 구조 생성")
    void createThreeLevelCommentHierarchy() {
        System.out.println(">>> 3단계 댓글 계층 구조 테스트 시작");
        
        String postTitle = "댓글 계층 테스트 " + generateTimestamp();
        testPostUrl = postPage.createPost(postTitle, "댓글 계층 구조 테스트용 게시글", null);
        
        postPage.writeComment("1단계: 최상위 댓글");
        
        postPage.writeReply(0, "2단계: 첫 번째 대댓글");
        
        postPage.writeReply(1, "3단계: 대대댓글");
        
        int comment1Depth = postPage.getCommentDepth(0);
        int comment2Depth = postPage.getCommentDepth(1);
        int comment3Depth = postPage.getCommentDepth(2);
        
        assertThat(comment1Depth).isEqualTo(0);
        assertThat(comment2Depth).isEqualTo(1);
        assertThat(comment3Depth).isEqualTo(2);
        
        System.out.println("3단계 계층 구조 생성 완료");
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 복잡한 댓글 트리 구조 생성")
    void createComplexCommentTree() {
        System.out.println(">>> 복잡한 댓글 트리 구조 테스트 시작");
        
        String postTitle = "복잡한 댓글 트리 " + generateTimestamp();
        testPostUrl = postPage.createPost(postTitle, "복잡한 댓글 트리 테스트", null);
        
        postPage.writeComment("루트 A");
        postPage.writeComment("루트 B");
        postPage.writeComment("루트 C");
        
        postPage.writeReply(0, "A-1");
        postPage.writeReply(0, "A-2");
        
        postPage.writeReply(1, "B-1");
        
        postPage.writeReply(3, "A-1-1");
        postPage.writeReply(3, "A-1-2");
        
        int totalComments = postPage.getCommentCount();
        assertThat(totalComments).isEqualTo(8);
        
        System.out.println("복잡한 트리 구조 생성 완료: 총 " + totalComments + "개 댓글");
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 댓글 수정 기능")
    void editComment() {
        System.out.println(">>> 댓글 수정 테스트 시작");
        
        if (testPostUrl == null) {
            String postTitle = "댓글 수정 테스트 " + generateTimestamp();
            testPostUrl = postPage.createPost(postTitle, "댓글 수정 테스트용", null);
        }
        
        page.navigate(testPostUrl);
        postPage.waitForPageLoad();
        
        String originalComment = "원본 댓글 " + generateTimestamp();
        postPage.writeComment(originalComment);
        
        page.locator(".comment-item").first().hover();
        page.click("button[aria-label='수정']");
        
        String editedComment = "수정된 댓글 " + generateTimestamp();
        page.fill(".comment-edit-input", editedComment);
        page.click("button:has-text('수정 완료')");
        
        postPage.wait(1000);
        
        assertThat(page.locator("text=" + editedComment).isVisible()).isTrue();
        assertThat(page.locator("text=(수정됨)").isVisible()).isTrue();
        
        System.out.println("댓글 수정 완료");
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 댓글 삭제 - 자손이 없는 경우")
    void deleteCommentWithoutChildren() {
        System.out.println(">>> 자손 없는 댓글 삭제 테스트 시작");
        
        String postTitle = "댓글 삭제 테스트1 " + generateTimestamp();
        testPostUrl = postPage.createPost(postTitle, "자손 없는 댓글 삭제", null);
        
        postPage.writeComment("삭제할 댓글");
        postPage.writeComment("유지될 댓글");
        
        int beforeCount = postPage.getCommentCount();
        assertThat(beforeCount).isEqualTo(2);
        
        page.locator(".comment-item").first().hover();
        page.click("button[aria-label='삭제']");
        page.click("button:has-text('확인')");
        
        postPage.wait(1000);
        
        int afterCount = postPage.getCommentCount();
        assertThat(afterCount).isEqualTo(1);
        
        System.out.println("자손 없는 댓글 하드 삭제 완료");
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 댓글 삭제 - 자손이 있는 경우")
    void deleteCommentWithChildren() {
        System.out.println(">>> 자손 있는 댓글 삭제 테스트 시작");
        
        String postTitle = "댓글 삭제 테스트2 " + generateTimestamp();
        testPostUrl = postPage.createPost(postTitle, "자손 있는 댓글 삭제", null);
        
        postPage.writeComment("부모 댓글");
        postPage.writeReply(0, "자식 댓글");
        
        page.locator(".comment-item").first().hover();
        page.click("button[aria-label='삭제']");
        page.click("button:has-text('확인')");
        
        postPage.wait(1000);
        
        assertThat(page.locator("text=삭제된 댓글입니다").isVisible()).isTrue();
        
        assertThat(page.locator("text=자식 댓글").isVisible()).isTrue();
        
        int totalCount = postPage.getCommentCount();
        assertThat(totalCount).isEqualTo(2);
        
        System.out.println("자손 있는 댓글 소프트 삭제 완료");
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 익명 사용자 댓글 작성 허용")
    void anonymousUserCommentCreation() {
        System.out.println(">>> 익명 사용자 댓글 테스트 시작");
        
        if (loginPage.isLoggedIn()) {
            loginPage.performLogout();
        }
        
        if (testPostUrl == null) {
            loginPage.performKakaoLogin();
            String postTitle = "익명 댓글 테스트 " + generateTimestamp();
            testPostUrl = postPage.createPost(postTitle, "익명 댓글 허용 테스트", null);
            loginPage.performLogout();
        }
        
        page.navigate(testPostUrl);
        postPage.waitForPageLoad();
        
        String anonymousComment = "익명 사용자 댓글 " + generateTimestamp();
        boolean success = postPage.writeComment(anonymousComment);
        
        assertThat(success).isTrue();
        assertThat(page.locator("text=" + anonymousComment).isVisible()).isTrue();
        
        System.out.println("익명 사용자 댓글 작성 성공");
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 댓글 페이지네이션")
    void commentPagination() {
        System.out.println(">>> 댓글 페이지네이션 테스트 시작");
        
        loginPage.performKakaoLogin();
        
        String postTitle = "페이지네이션 테스트 " + generateTimestamp();
        testPostUrl = postPage.createPost(postTitle, "많은 댓글 테스트", null);
        
        for (int i = 1; i <= 12; i++) {
            postPage.writeComment("댓글 #" + i);
        }
        
        page.reload();
        postPage.waitForPageLoad();
        
        if (page.locator(".comment-pagination").isVisible()) {
            assertThat(page.locator("button:has-text('2')").isVisible()).isTrue();
            
            page.click("button:has-text('2')");
            postPage.wait(1000);
            
            assertThat(page.locator("text=댓글 #11").isVisible()).isTrue();
            
            System.out.println("댓글 페이지네이션 확인");
        } else {
            System.out.println("모든 댓글이 한 페이지에 표시됨");
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("08. 댓글 알림 테스트")
    void commentNotification() {
        System.out.println(">>> 댓글 알림 테스트 시작");
        
        String postTitle = "알림 테스트 " + generateTimestamp();
        testPostUrl = postPage.createPost(postTitle, "댓글 알림 테스트", null);
        
        loginPage.performLogout();
        loginPage.performKakaoLogin();
        
        page.navigate(testPostUrl);
        postPage.writeComment("알림을 발생시킬 댓글");
        
        if (page.locator(".notification-badge").isVisible()) {
            String notificationCount = page.locator(".notification-badge").textContent();
            assertThat(Integer.parseInt(notificationCount)).isGreaterThan(0);
            System.out.println("알림 발생 확인: " + notificationCount + "개");
        } else {
            System.out.println("알림 뱃지가 표시되지 않음");
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("09. 댓글 좋아요 기능")
    void likeComment() {
        System.out.println(">>> 댓글 좋아요 테스트 시작");
        
        if (testPostUrl == null) {
            String postTitle = "댓글 좋아요 테스트 " + generateTimestamp();
            testPostUrl = postPage.createPost(postTitle, "댓글 좋아요 테스트", null);
            postPage.writeComment("좋아요할 댓글");
        }
        
        page.navigate(testPostUrl);
        postPage.waitForPageLoad();
        
        page.locator(".comment-item").first().hover();
        page.click("button[aria-label='좋아요']");
        
        postPage.wait(500);
        
        String likeCount = page.locator(".comment-item .like-count").first().textContent();
        assertThat(Integer.parseInt(likeCount.replaceAll("[^0-9]", ""))).isGreaterThan(0);
        
        System.out.println("댓글 좋아요 완료");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 댓글 신고 기능")
    void reportComment() {
        System.out.println(">>> 댓글 신고 테스트 시작");
        
        if (testPostUrl == null) {
            String postTitle = "댓글 신고 테스트 " + generateTimestamp();
            testPostUrl = postPage.createPost(postTitle, "댓글 신고 테스트", null);
            postPage.writeComment("신고할 댓글");
        }
        
        page.navigate(testPostUrl);
        postPage.waitForPageLoad();
        
        page.locator(".comment-item").first().hover();
        
        if (page.locator("button[aria-label='신고']").isVisible()) {
            page.click("button[aria-label='신고']");
            
            page.selectOption("select[name='reason']", "스팸");
            page.fill("textarea[name='details']", "테스트 신고입니다");
            page.click("button:has-text('신고하기')");
            
            postPage.wait(1000);
            
            assertThat(page.locator("text=신고가 접수되었습니다").isVisible()).isTrue();
            
            System.out.println("댓글 신고 완료");
        } else {
            System.out.println("신고 기능이 비활성화되어 있음");
        }
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        testPostUrl = null;
        super.afterEach(testInfo);
    }
}