package jaeik.bimillog.e2e.pages.board;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import jaeik.bimillog.e2e.pages.common.BasePage;

/**
 * 게시글 상세 페이지 Page Object
 */
public class PostDetailPage extends BasePage {

    // Selectors
    private static final String POST_TITLE = "h1, [class*='title']";
    private static final String POST_CONTENT = "[class*='content'], .ql-editor";
    private static final String POST_AUTHOR = "[class*='author']";
    private static final String POST_DATE = "[class*='date'], time";
    private static final String LIKE_BUTTON = "button:has-text('좋아요'), button:has([class*='heart'])";
    private static final String REPORT_BUTTON = "button:has-text('신고')";
    private static final String MORE_MENU_BUTTON = "button[aria-label*='더보기'], button:has-text('⋮')";
    private static final String EDIT_BUTTON = "button:has-text('수정'), a:has-text('수정')";
    private static final String DELETE_BUTTON = "button:has-text('삭제')";
    private static final String COMMENT_INPUT = "textarea[placeholder*='댓글']";
    private static final String COMMENT_SUBMIT = "button:has-text('작성'), button:has-text('등록')";
    private static final String COMMENT_LIST = "[class*='comment-list'], [class*='comment']";
    private static final String BACK_TO_LIST = "button:has-text('목록'), a:has-text('목록')";

    public PostDetailPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * 게시글 상세 페이지로 이동
     */
    public void navigateToPost(String postId) {
        navigate("/board/post/" + postId);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        waitFor(POST_TITLE);
    }

    /**
     * 게시글 제목 가져오기
     */
    public String getPostTitle() {
        return find(POST_TITLE).innerText();
    }

    /**
     * 게시글 내용 가져오기
     */
    public String getPostContent() {
        return find(POST_CONTENT).innerText();
    }

    /**
     * 작성자 이름 가져오기
     */
    public String getAuthorName() {
        return find(POST_AUTHOR).innerText();
    }

    /**
     * 좋아요 버튼 클릭
     */
    public void clickLikeButton() {
        click(LIKE_BUTTON);
        page.waitForTimeout(500); // API 응답 대기
    }

    /**
     * 신고 버튼 클릭
     */
    public void clickReportButton() {
        // 직접 신고 버튼이 있는 경우
        if (page.locator(REPORT_BUTTON).count() > 0) {
            click(REPORT_BUTTON);
        } else {
            // 더보기 메뉴에서 신고 찾기
            clickMoreMenu();
            page.waitForTimeout(500);
            clickText("신고하기");
        }

        // 신고 모달 대기
        page.waitForSelector("[role='dialog']", new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(5000));
    }

    /**
     * 더보기 메뉴 클릭
     */
    public void clickMoreMenu() {
        click(MORE_MENU_BUTTON);
    }

    /**
     * 수정 버튼 클릭
     */
    public void clickEditButton() {
        if (page.locator(EDIT_BUTTON).count() > 0) {
            click(EDIT_BUTTON);
        } else {
            clickMoreMenu();
            page.waitForTimeout(500);
            clickText("수정");
        }
        page.waitForURL("**/edit");
    }

    /**
     * 삭제 버튼 클릭
     */
    public void clickDeleteButton() {
        if (page.locator(DELETE_BUTTON).count() > 0) {
            click(DELETE_BUTTON);
        } else {
            clickMoreMenu();
            page.waitForTimeout(500);
            clickText("삭제");
        }

        // 확인 다이얼로그 처리
        page.onDialog(dialog -> {
            System.out.println("삭제 확인 다이얼로그: " + dialog.message());
            dialog.accept();
        });
    }

    /**
     * 댓글 작성
     */
    public void writeComment(String comment) {
        fill(COMMENT_INPUT, comment);
        click(COMMENT_SUBMIT);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * 댓글 목록이 표시되는지 확인
     */
    public boolean areCommentsVisible() {
        return page.locator(COMMENT_LIST).count() > 0;
    }

    /**
     * 댓글 개수 가져오기
     */
    public int getCommentCount() {
        return page.locator(COMMENT_LIST + " > div, " + COMMENT_LIST + " > li").count();
    }

    /**
     * 특정 텍스트를 포함한 댓글 찾기
     */
    public Locator findCommentByText(String text) {
        return page.locator(COMMENT_LIST + " :has-text('" + text + "')").first();
    }

    /**
     * 댓글 좋아요 클릭
     */
    public void likeComment(String commentText) {
        Locator comment = findCommentByText(commentText);
        comment.locator("button:has([class*='heart']), button:has-text('좋아요')").first().click();
        page.waitForTimeout(500);
    }

    /**
     * 댓글 신고
     */
    public void reportComment(String commentText) {
        Locator comment = findCommentByText(commentText);

        // 댓글 더보기 메뉴 클릭
        comment.locator("button[aria-label*='더보기'], button:has-text('⋮')").first().click();
        page.waitForTimeout(300);

        // 신고 옵션 클릭
        page.locator("text=신고").first().click();

        // 신고 모달 대기
        page.waitForSelector("[role='dialog']", new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(5000));
    }

    /**
     * 목록으로 돌아가기
     */
    public void backToList() {
        click(BACK_TO_LIST);
        page.waitForURL("**/board");
    }

    /**
     * 게시글이 삭제되었는지 확인
     */
    public boolean isPostDeleted() {
        return page.locator("text=/삭제된 게시글|존재하지 않는/").count() > 0;
    }

    /**
     * 좋아요 수 가져오기
     */
    public int getLikeCount() {
        Locator likeCount = page.locator("[class*='like-count'], span:near(" + LIKE_BUTTON + ")").first();
        String countText = likeCount.innerText();
        return Integer.parseInt(countText.replaceAll("[^0-9]", ""));
    }

    /**
     * 페이지가 로드되었는지 확인
     */
    @Override
    public boolean isLoaded() {
        return page.locator(POST_TITLE).isVisible() && page.locator(POST_CONTENT).isVisible();
    }
}