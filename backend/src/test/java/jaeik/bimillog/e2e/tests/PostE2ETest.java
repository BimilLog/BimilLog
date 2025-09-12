package jaeik.bimillog.e2e.tests;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import jaeik.bimillog.e2e.pages.PostPage;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>게시글 CRUD E2E 테스트</h2>
 * <p>게시글 작성, 수정, 삭제, 검색 기능을 테스트합니다.</p>
 * <p>Quill 에디터, 댓글 작성, 좋아요, 조회수 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("게시글 CRUD E2E 테스트")
public class PostE2ETest extends BaseE2ETest {
    
    private PostPage postPage;
    private LoginPage loginPage;
    private String createdPostUrl;
    private static String testPostTitle;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        postPage = new PostPage(page, BASE_URL);
        loginPage = new LoginPage(page, BASE_URL);
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 로그인 후 게시글 작성")
    void createPostAfterLogin() {
        System.out.println(">>> 게시글 작성 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        testPostTitle = "E2E 테스트 게시글 " + generateTimestamp();
        String content = "이것은 E2E 테스트를 위한 게시글 내용입니다.\n" +
                        "Playwright를 사용하여 작성되었습니다.";
        String category = "일반";
        
        createdPostUrl = postPage.createPost(testPostTitle, content, category);
        
        assertThat(createdPostUrl).isNotNull();
        assertThat(createdPostUrl).contains("/board/post/");
        
        assertThat(page.locator("h1:has-text('" + testPostTitle + "')").isVisible()).isTrue();
        
        System.out.println("게시글 작성 완료: " + createdPostUrl);
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 작성한 게시글 수정")
    void editPost() {
        System.out.println(">>> 게시글 수정 테스트 시작");
        
        if (createdPostUrl == null) {
            testPostTitle = "수정 테스트 게시글 " + generateTimestamp();
            createdPostUrl = postPage.createPost(testPostTitle, "원본 내용", null);
        }
        
        page.navigate(createdPostUrl);
        postPage.waitForPageLoad();
        
        String newTitle = testPostTitle + " (수정됨)";
        String newContent = "수정된 게시글 내용입니다.\n수정 시간: " + generateTimestamp();
        
        boolean editSuccess = postPage.editPost(newTitle, newContent);
        assertThat(editSuccess).isTrue();
        
        assertThat(page.locator("h1:has-text('" + newTitle + "')").isVisible()).isTrue();
        assertThat(page.locator("text=수정된 게시글 내용입니다").isVisible()).isTrue();
        
        testPostTitle = newTitle;
        System.out.println("게시글 수정 완료");
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 댓글 작성")
    void writeComment() {
        System.out.println(">>> 댓글 작성 테스트 시작");
        
        if (createdPostUrl == null) {
            testPostTitle = "댓글 테스트 게시글 " + generateTimestamp();
            createdPostUrl = postPage.createPost(testPostTitle, "댓글 테스트용", null);
        }
        
        page.navigate(createdPostUrl);
        postPage.waitForPageLoad();
        
        String comment = "E2E 테스트 댓글입니다. 작성 시간: " + generateTimestamp();
        boolean commentSuccess = postPage.writeComment(comment);
        assertThat(commentSuccess).isTrue();
        
        int commentCount = postPage.getCommentCount();
        assertThat(commentCount).isGreaterThan(0);
        
        System.out.println("댓글 작성 완료, 총 댓글 수: " + commentCount);
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 대댓글 작성 (계층형 댓글)")
    void writeReply() {
        System.out.println(">>> 대댓글 작성 테스트 시작");
        
        if (createdPostUrl == null) {
            testPostTitle = "대댓글 테스트 게시글 " + generateTimestamp();
            createdPostUrl = postPage.createPost(testPostTitle, "대댓글 테스트용", null);
            postPage.writeComment("부모 댓글");
        }
        
        page.navigate(createdPostUrl);
        postPage.waitForPageLoad();
        
        String reply = "대댓글입니다. " + generateTimestamp();
        boolean replySuccess = postPage.writeReply(0, reply);
        assertThat(replySuccess).isTrue();
        
        int depth = postPage.getCommentDepth(1);
        assertThat(depth).isGreaterThan(0);
        
        System.out.println("대댓글 작성 완료, 댓글 깊이: " + depth);
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 게시글 좋아요")
    void likePost() {
        System.out.println(">>> 게시글 좋아요 테스트 시작");
        
        if (createdPostUrl == null) {
            testPostTitle = "좋아요 테스트 게시글 " + generateTimestamp();
            createdPostUrl = postPage.createPost(testPostTitle, "좋아요 테스트용", null);
        }
        
        page.navigate(createdPostUrl);
        postPage.waitForPageLoad();
        
        boolean likeSuccess = postPage.likePost();
        assertThat(likeSuccess).isTrue();
        
        System.out.println("좋아요 완료");
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 게시글 조회수 확인")
    void checkPostViewCount() {
        System.out.println(">>> 조회수 확인 테스트 시작");
        
        if (createdPostUrl == null) {
            testPostTitle = "조회수 테스트 게시글 " + generateTimestamp();
            createdPostUrl = postPage.createPost(testPostTitle, "조회수 테스트용", null);
        }
        
        page.navigate(createdPostUrl);
        postPage.waitForPageLoad();
        
        int initialViews = postPage.getViewCount();
        
        page.context().clearCookies();
        page.reload();
        postPage.waitForPageLoad();
        
        int updatedViews = postPage.getViewCount();
        assertThat(updatedViews).isGreaterThanOrEqualTo(initialViews);
        
        System.out.println("조회수: " + initialViews + " -> " + updatedViews);
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 게시글 검색 (풀텍스트)")
    void searchPosts() {
        System.out.println(">>> 게시글 검색 테스트 시작");
        
        String uniqueKeyword = "유니크검색" + generateTimestamp();
        String searchTitle = "검색 테스트 " + uniqueKeyword;
        postPage.createPost(searchTitle, "검색 테스트 내용 " + uniqueKeyword, null);
        
        postPage.wait(2000);
        
        int searchResults = postPage.searchPosts(uniqueKeyword);
        assertThat(searchResults).isGreaterThan(0);
        
        System.out.println("검색 결과: " + searchResults + "개");
    }
    
    @Test
    @Order(8)
    @DisplayName("08. 여러 댓글 작성 후 계층 확인")
    void testCommentHierarchyStructure() {
        System.out.println(">>> 댓글 계층 구조 테스트 시작");
        
        testPostTitle = "계층 댓글 테스트 " + generateTimestamp();
        createdPostUrl = postPage.createPost(testPostTitle, "계층 구조 테스트", null);
        
        postPage.writeComment("1단계 댓글 A");
        postPage.writeComment("1단계 댓글 B");
        
        postPage.writeReply(0, "2단계 댓글 A-1");
        postPage.writeReply(0, "2단계 댓글 A-2");
        
        postPage.writeReply(1, "2단계 댓글 B-1");
        
        int totalComments = postPage.getCommentCount();
        assertThat(totalComments).isEqualTo(5);
        
        System.out.println("계층형 댓글 총 " + totalComments + "개 작성 완료");
    }
    
    @Test
    @Order(9)
    @DisplayName("09. 게시글 삭제")
    void deletePost() {
        System.out.println(">>> 게시글 삭제 테스트 시작");
        
        testPostTitle = "삭제할 게시글 " + generateTimestamp();
        createdPostUrl = postPage.createPost(testPostTitle, "이 게시글은 삭제됩니다", null);
        
        page.navigate(createdPostUrl);
        postPage.waitForPageLoad();
        
        boolean deleteSuccess = postPage.deletePost();
        assertThat(deleteSuccess).isTrue();
        
        assertThat(page.url()).contains("/board");
        assertThat(page.url()).doesNotContain("/post/");
        
        System.out.println("게시글 삭제 완료");
    }
    
    @Test
    @Order(10)
    @DisplayName("10. 익명 사용자 게시글 작성 제한")
    void restrictAnonymousUserPostCreation() {
        System.out.println(">>> 익명 사용자 제한 테스트 시작");
        
        if (loginPage.isLoggedIn()) {
            loginPage.performLogout();
        }
        
        postPage.navigateToPostList();
        
        if (page.locator("button:has-text('글쓰기')").isVisible()) {
            page.click("button:has-text('글쓰기')");
            postPage.wait(2000);
            
            String currentUrl = page.url();
            assertThat(currentUrl).doesNotContain("/board/write");
            
            System.out.println("익명 사용자 글쓰기 제한 확인");
        } else {
            System.out.println("글쓰기 버튼이 익명 사용자에게 표시되지 않음");
        }
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        createdPostUrl = null;
        super.afterEach(testInfo);
    }
}