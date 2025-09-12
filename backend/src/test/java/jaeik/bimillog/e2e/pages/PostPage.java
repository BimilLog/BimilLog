package jaeik.bimillog.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * <h2>게시글 페이지 객체</h2>
 * <p>게시글 CRUD 및 댓글 기능을 관리합니다.</p>
 * <p>Quill 에디터 조작, 계층형 댓글, 검색 기능</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class PostPage extends BasePage {
    
    private static final String WRITE_BUTTON = "button:has-text('글쓰기')";
    private static final String TITLE_INPUT = "input[name='title']";
    private static final String QUILL_EDITOR = ".ql-editor";
    private static final String CATEGORY_SELECT = "select[name='category']";
    private static final String SUBMIT_BUTTON = "button[type='submit']";
    private static final String POST_LIST = ".post-list";
    private static final String POST_ITEM = ".post-item";
    private static final String EDIT_BUTTON = "button:has-text('수정')";
    private static final String DELETE_BUTTON = "button:has-text('삭제')";
    private static final String COMMENT_INPUT = "textarea[name='comment']";
    private static final String COMMENT_SUBMIT = "button:has-text('댓글 작성')";
    private static final String COMMENT_LIST = ".comment-list";
    private static final String COMMENT_ITEM = ".comment-item";
    private static final String REPLY_BUTTON = "button:has-text('답글')";
    private static final String SEARCH_INPUT = "input[placeholder*='검색']";
    private static final String SEARCH_BUTTON = "button[aria-label='검색']";
    
    public PostPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }
    
    /**
     * <h3>게시글 목록 페이지로 이동</h3>
     * <p>게시글 목록 페이지로 이동합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void navigateToPostList() {
        navigate("/board");
        waitForElement(POST_LIST);
    }
    
    /**
     * <h3>게시글 작성 페이지로 이동</h3>
     * <p>게시글 작성 페이지로 이동합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void navigateToWritePost() {
        navigate("/board/write");
        waitForElement(TITLE_INPUT);
    }
    
    /**
     * <h3>게시글 작성</h3>
     * <p>새 게시글을 작성합니다.</p>
     *
     * @param title 제목
     * @param content 내용
     * @param category 카테고리
     * @return 작성된 게시글 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String createPost(String title, String content, String category) {
        navigateToWritePost();
        
        fill(TITLE_INPUT, title);
        
        fillQuillEditor(content);
        
        if (category != null && !category.isEmpty()) {
            selectOption(CATEGORY_SELECT, category);
        }
        
        click(SUBMIT_BUTTON);
        
        page.waitForURL("**/board/post/**", new Page.WaitForURLOptions().setTimeout(5000));
        
        String postUrl = getCurrentUrl();
        System.out.println("Post created: " + postUrl);
        
        return postUrl;
    }
    
    /**
     * <h3>Quill 에디터 입력</h3>
     * <p>Quill 리치 텍스트 에디터에 내용을 입력합니다.</p>
     *
     * @param content 입력할 내용
     * @author Jaeik
     * @since 2.0.0
     */
    private void fillQuillEditor(String content) {
        Locator editor = waitForElement(QUILL_EDITOR);
        editor.click();
        
        page.keyboard().type(content);
    }
    
    /**
     * <h3>게시글 수정</h3>
     * <p>현재 게시글을 수정합니다.</p>
     *
     * @param newTitle 새 제목
     * @param newContent 새 내용
     * @return 수정 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean editPost(String newTitle, String newContent) {
        try {
            click(EDIT_BUTTON);
            
            waitForElement(TITLE_INPUT);
            
            if (newTitle != null) {
                fill(TITLE_INPUT, newTitle);
            }
            
            if (newContent != null) {
                Locator editor = page.locator(QUILL_EDITOR);
                editor.click();
                page.keyboard().press("Control+A");
                page.keyboard().type(newContent);
            }
            
            click(SUBMIT_BUTTON);
            
            wait(2000);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to edit post: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>게시글 삭제</h3>
     * <p>현재 게시글을 삭제합니다.</p>
     *
     * @return 삭제 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean deletePost() {
        try {
            click(DELETE_BUTTON);
            
            page.locator("button:has-text('확인')").click();
            
            page.waitForURL("**/board", new Page.WaitForURLOptions().setTimeout(5000));
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to delete post: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>댓글 작성</h3>
     * <p>게시글에 댓글을 작성합니다.</p>
     *
     * @param comment 댓글 내용
     * @return 작성 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean writeComment(String comment) {
        try {
            scrollToElement(COMMENT_INPUT);
            
            fill(COMMENT_INPUT, comment);
            
            click(COMMENT_SUBMIT);
            
            wait(1000);
            
            return isTextVisible(comment);
            
        } catch (Exception e) {
            System.err.println("Failed to write comment: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>대댓글 작성</h3>
     * <p>특정 댓글에 대댓글을 작성합니다.</p>
     *
     * @param parentCommentIndex 부모 댓글 인덱스
     * @param reply 대댓글 내용
     * @return 작성 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean writeReply(int parentCommentIndex, String reply) {
        try {
            Locator comments = page.locator(COMMENT_ITEM);
            
            if (parentCommentIndex >= comments.count()) {
                return false;
            }
            
            Locator parentComment = comments.nth(parentCommentIndex);
            parentComment.locator(REPLY_BUTTON).click();
            
            Locator replyInput = parentComment.locator("textarea");
            replyInput.fill(reply);
            
            parentComment.locator("button:has-text('답글 작성')").click();
            
            wait(1000);
            
            return isTextVisible(reply);
            
        } catch (Exception e) {
            System.err.println("Failed to write reply: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>게시글 검색</h3>
     * <p>제목 또는 내용으로 게시글을 검색합니다.</p>
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int searchPosts(String keyword) {
        navigateToPostList();
        
        fill(SEARCH_INPUT, keyword);
        click(SEARCH_BUTTON);
        
        wait(2000);
        
        return page.locator(POST_ITEM).count();
    }
    
    /**
     * <h3>댓글 개수 확인</h3>
     * <p>현재 게시글의 댓글 개수를 확인합니다.</p>
     *
     * @return 댓글 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getCommentCount() {
        return page.locator(COMMENT_ITEM).count();
    }
    
    /**
     * <h3>댓글 계층 깊이 확인</h3>
     * <p>특정 댓글의 계층 깊이를 확인합니다.</p>
     *
     * @param commentIndex 댓글 인덱스
     * @return 계층 깊이 (0: 최상위)
     * @author Jaeik
     * @since 2.0.0
     */
    public int getCommentDepth(int commentIndex) {
        try {
            Locator comment = page.locator(COMMENT_ITEM).nth(commentIndex);
            String classes = comment.getAttribute("class");
            
            if (classes.contains("depth-2")) return 2;
            if (classes.contains("depth-1")) return 1;
            return 0;
            
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * <h3>게시글 좋아요</h3>
     * <p>현재 게시글에 좋아요를 누릅니다.</p>
     *
     * @return 좋아요 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean likePost() {
        try {
            click("button[aria-label='좋아요']");
            wait(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * <h3>조회수 확인</h3>
     * <p>게시글의 조회수를 확인합니다.</p>
     *
     * @return 조회수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getViewCount() {
        try {
            String viewText = getText(".view-count");
            return Integer.parseInt(viewText.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}