package jaeik.bimillog.e2e.tests;

import com.microsoft.playwright.Locator;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.pages.LoginPage;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <h2>건의사항 E2E 테스트</h2>
 * <p>건의사항 작성, 신고 기능을 테스트합니다.</p>
 * <p>익명/로그인 사용자 건의, 부적절한 콘텐츠 신고</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("건의사항 E2E 테스트")
public class SuggestE2ETest extends BaseE2ETest {
    
    private LoginPage loginPage;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        super.beforeEach(testInfo);
        loginPage = new LoginPage(page, BASE_URL);
    }
    
    @Test
    @Order(1)
    @DisplayName("01. 익명 사용자 건의사항 작성")
    void writeSuggestionAsAnonymous() {
        System.out.println(">>> 익명 건의사항 작성 테스트 시작");
        
        if (loginPage.isLoggedIn()) {
            loginPage.performLogout();
        }
        
        page.navigate(BASE_URL + "/suggest");
        waitForPageLoad();
        
        String title = "익명 건의사항 " + generateTimestamp();
        String content = "이것은 익명 사용자의 건의사항입니다.";
        
        Locator titleInput = page.locator("input[name='title'], input[placeholder*='제목']").first();
        titleInput.fill(title);
        
        Locator contentTextarea = page.locator("textarea[name='content'], textarea[placeholder*='내용']").first();
        contentTextarea.fill(content);
        
        Locator categorySelect = page.locator("select[name='category']").first();
        if (categorySelect.isVisible()) {
            categorySelect.selectOption("건의");
        }
        
        Locator submitButton = page.locator("button[type='submit'], button:has-text('제출'), button:has-text('등록')").first();
        submitButton.click();
        
        page.waitForTimeout(2000);
        
        boolean isSubmitted = page.locator("text=감사합니다").isVisible() ||
                            page.locator("text=접수되었습니다").isVisible() ||
                            page.url().contains("/suggest/complete");
        
        assertThat(isSubmitted).isTrue();
        System.out.println("익명 건의사항 제출 완료");
    }
    
    @Test
    @Order(2)
    @DisplayName("02. 로그인 사용자 건의사항 작성")
    void writeSuggestionAsLoggedIn() {
        System.out.println(">>> 로그인 사용자 건의사항 작성 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        page.navigate(BASE_URL + "/suggest");
        waitForPageLoad();
        
        String title = "로그인 사용자 건의 " + generateTimestamp();
        String content = "로그인한 사용자의 건의사항입니다. 답변을 받고 싶습니다.";
        String email = "test_" + generateTimestamp() + "@example.com";
        
        Locator titleInput = page.locator("input[name='title'], input[placeholder*='제목']").first();
        titleInput.fill(title);
        
        Locator contentTextarea = page.locator("textarea[name='content'], textarea[placeholder*='내용']").first();
        contentTextarea.fill(content);
        
        Locator emailInput = page.locator("input[name='email'], input[type='email']").first();
        if (emailInput.isVisible()) {
            emailInput.fill(email);
        }
        
        Locator replyCheckbox = page.locator("input[type='checkbox'][name='wantReply']").first();
        if (replyCheckbox.isVisible()) {
            replyCheckbox.check();
        }
        
        Locator submitButton = page.locator("button[type='submit'], button:has-text('제출')").first();
        submitButton.click();
        
        page.waitForTimeout(2000);
        
        boolean isSubmitted = page.locator("text=감사합니다").isVisible() ||
                            page.locator("text=접수되었습니다").isVisible();
        
        assertThat(isSubmitted).isTrue();
        System.out.println("로그인 사용자 건의사항 제출 완료");
    }
    
    @Test
    @Order(3)
    @DisplayName("03. 게시글 신고")
    void reportPost() {
        System.out.println(">>> 게시글 신고 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        page.navigate(BASE_URL + "/board");
        waitForPageLoad();
        
        Locator firstPost = page.locator(".post-item, [class*='post']").first();
        if (!firstPost.isVisible()) {
            System.out.println("신고할 게시글이 없음");
            return;
        }
        
        firstPost.click();
        waitForPageLoad();
        
        Locator reportButton = page.locator("button:has-text('신고'), button[aria-label='신고']").first();
        if (!reportButton.isVisible()) {
            Locator moreButton = page.locator("button[aria-label='더보기'], button:has(svg.lucide-more-vertical)").first();
            if (moreButton.isVisible()) {
                moreButton.click();
                page.waitForTimeout(500);
                reportButton = page.locator("text=신고").first();
            }
        }
        
        if (reportButton.isVisible()) {
            reportButton.click();
            page.waitForTimeout(1000);
            
            Locator reasonSelect = page.locator("select[name='reason'], [role='dialog'] select").first();
            if (reasonSelect.isVisible()) {
                reasonSelect.selectOption("스팸");
            }
            
            Locator detailTextarea = page.locator("textarea[name='detail'], [role='dialog'] textarea").first();
            if (detailTextarea.isVisible()) {
                detailTextarea.fill("테스트 신고입니다.");
            }
            
            Locator confirmButton = page.locator("[role='dialog'] button:has-text('신고'), button:has-text('제출')").first();
            confirmButton.click();
            
            page.waitForTimeout(1000);
            
            boolean isReported = page.locator("text=신고가 접수되었습니다").isVisible() ||
                               page.locator("text=감사합니다").isVisible();
            
            assertThat(isReported).isTrue();
            System.out.println("게시글 신고 완료");
        } else {
            System.out.println("신고 버튼을 찾을 수 없음");
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("04. 댓글 신고")
    void reportComment() {
        System.out.println(">>> 댓글 신고 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        page.navigate(BASE_URL + "/board");
        waitForPageLoad();
        
        Locator postWithComments = page.locator(".post-item").first();
        postWithComments.click();
        waitForPageLoad();
        
        Locator firstComment = page.locator(".comment-item, [class*='comment']").first();
        if (!firstComment.isVisible()) {
            System.out.println("신고할 댓글이 없음");
            return;
        }
        
        firstComment.hover();
        
        Locator commentReportButton = firstComment.locator("button[aria-label='신고'], button:has(svg.lucide-flag)").first();
        if (!commentReportButton.isVisible()) {
            Locator moreButton = firstComment.locator("button:has(svg.lucide-more-horizontal)").first();
            if (moreButton.isVisible()) {
                moreButton.click();
                page.waitForTimeout(500);
                commentReportButton = page.locator("text=신고").first();
            }
        }
        
        if (commentReportButton.isVisible()) {
            commentReportButton.click();
            page.waitForTimeout(1000);
            
            Locator reasonRadio = page.locator("input[type='radio'][value='욕설']").first();
            if (reasonRadio.isVisible()) {
                reasonRadio.check();
            }
            
            Locator confirmButton = page.locator("[role='dialog'] button:has-text('신고')").first();
            confirmButton.click();
            
            page.waitForTimeout(1000);
            
            boolean isReported = page.locator("text=신고가 접수되었습니다").isVisible();
            assertThat(isReported).isTrue();
            
            System.out.println("댓글 신고 완료");
        } else {
            System.out.println("댓글 신고 버튼을 찾을 수 없음");
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("05. 롤링페이퍼 메시지 신고")
    void reportPaperMessage() {
        System.out.println(">>> 롤링페이퍼 메시지 신고 테스트 시작");
        
        if (!loginPage.isLoggedIn()) {
            loginPage.performKakaoLogin();
        }
        
        String username = loginPage.getUsername();
        if (username == null || username.isEmpty()) {
            System.out.println("사용자명을 가져올 수 없음");
            return;
        }
        
        page.navigate(BASE_URL + "/rolling-paper/" + username);
        waitForPageLoad();
        
        Locator filledCell = page.locator(".grid-cell.filled, [class*='filled']").first();
        if (!filledCell.isVisible()) {
            System.out.println("신고할 메시지가 없음");
            return;
        }
        
        filledCell.click();
        page.waitForTimeout(1000);
        
        Locator reportIcon = page.locator("button:has(svg.lucide-flag), button[aria-label='신고']").first();
        if (reportIcon.isVisible()) {
            reportIcon.click();
            page.waitForTimeout(1000);
            
            Locator confirmButton = page.locator("button:has-text('신고'), button:has-text('확인')").last();
            confirmButton.click();
            
            page.waitForTimeout(1000);
            
            boolean isReported = page.locator("text=신고되었습니다").isVisible() ||
                               page.locator("text=처리되었습니다").isVisible();
            
            assertThat(isReported).isTrue();
            System.out.println("롤링페이퍼 메시지 신고 완료");
        } else {
            System.out.println("메시지 신고 버튼을 찾을 수 없음");
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("06. 건의사항 카테고리별 작성")
    void writeSuggestionByCategory() {
        System.out.println(">>> 건의사항 카테고리별 작성 테스트 시작");
        
        String[] categories = {"버그", "기능요청", "개선사항", "기타"};
        
        for (String category : categories) {
            page.navigate(BASE_URL + "/suggest");
            waitForPageLoad();
            
            String title = category + " 테스트 " + generateTimestamp();
            String content = category + " 관련 내용입니다.";
            
            Locator titleInput = page.locator("input[name='title']").first();
            titleInput.fill(title);
            
            Locator contentTextarea = page.locator("textarea[name='content']").first();
            contentTextarea.fill(content);
            
            Locator categorySelect = page.locator("select[name='category']").first();
            if (categorySelect.isVisible()) {
                try {
                    categorySelect.selectOption(category);
                } catch (Exception e) {
                    System.out.println(category + " 카테고리를 찾을 수 없음");
                    continue;
                }
            }
            
            Locator submitButton = page.locator("button[type='submit']").first();
            submitButton.click();
            
            page.waitForTimeout(1500);
            
            System.out.println(category + " 카테고리 건의사항 제출");
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("07. 건의사항 필수 항목 검증")
    void validateRequiredFieldsForSuggestion() {
        System.out.println(">>> 건의사항 필수 항목 검증 테스트 시작");
        
        page.navigate(BASE_URL + "/suggest");
        waitForPageLoad();
        
        Locator submitButton = page.locator("button[type='submit']").first();
        submitButton.click();
        
        page.waitForTimeout(1000);
        
        boolean hasValidationError = page.locator("text=필수").isVisible() ||
                                    page.locator("text=입력해주세요").isVisible() ||
                                    page.locator(".error, .invalid").isVisible();
        
        assertThat(hasValidationError).isTrue();
        
        Locator titleInput = page.locator("input[name='title']").first();
        titleInput.fill("제목만 입력");
        submitButton.click();
        
        page.waitForTimeout(1000);
        
        boolean contentRequired = page.locator("text=내용").isVisible() &&
                                (page.locator("text=필수").isVisible() ||
                                 page.locator("text=입력해주세요").isVisible());
        
        assertThat(contentRequired).isTrue();
        
        System.out.println("필수 항목 검증 완료");
    }
    
    @Test
    @Order(8)
    @DisplayName("08. 건의사항 글자 수 제한")
    void validateCharacterLimitForSuggestion() {
        System.out.println(">>> 건의사항 글자 수 제한 테스트 시작");
        
        page.navigate(BASE_URL + "/suggest");
        waitForPageLoad();
        
        String longTitle = "긴".repeat(101);
        String longContent = "내용".repeat(501);
        
        Locator titleInput = page.locator("input[name='title']").first();
        titleInput.fill(longTitle);
        
        String actualTitle = titleInput.inputValue();
        assertThat(actualTitle.length()).isLessThanOrEqualTo(100);
        
        Locator contentTextarea = page.locator("textarea[name='content']").first();
        contentTextarea.fill(longContent);
        
        Locator charCount = page.locator("text=/[0-9]+\\/1000/").first();
        if (charCount.isVisible()) {
            String countText = charCount.textContent();
            System.out.println("글자 수 표시: " + countText);
        }
        
        System.out.println("글자 수 제한 확인 완료");
    }
    
    /**
     * <h3>페이지 로드 대기</h3>
     * <p>페이지가 완전히 로드될 때까지 대기합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    private void waitForPageLoad() {
        page.waitForLoadState();
        page.waitForTimeout(500);
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        super.afterEach(testInfo);
    }
}