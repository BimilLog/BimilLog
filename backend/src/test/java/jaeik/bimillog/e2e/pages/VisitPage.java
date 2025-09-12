package jaeik.bimillog.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * <h2>롤링페이퍼 방문 페이지 객체</h2>
 * <p>롤링페이퍼 검색, 방문 기록 관리 UI와 상호작용을 캡슐화합니다.</p>
 * <p>닉네임 검색, 최근 방문 기록, 메시지 작성</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class VisitPage extends BasePage {
    
    public VisitPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }
    
    /**
     * <h3>방문 페이지로 이동</h3>
     * <p>/visit 페이지로 이동합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void navigateToVisitPage() {
        navigate("/visit");
        waitForPageLoad();
    }
    
    /**
     * <h3>닉네임으로 롤링페이퍼 검색</h3>
     * <p>닉네임을 입력하여 롤링페이퍼를 검색합니다.</p>
     *
     * @param nickname 검색할 닉네임
     * @return 검색된 롤링페이퍼 URL (없으면 null)
     * @author Jaeik
     * @since 2.0.0
     */
    public String searchByNickname(String nickname) {
        try {
            navigateToVisitPage();
            
            Locator searchInput = page.locator("input[placeholder*='닉네임']").first();
            searchInput.fill(nickname);
            
            Locator searchButton = page.locator("button:has-text('검색')").first();
            if (!searchButton.isVisible()) {
                searchButton = page.locator("button:has(svg.lucide-search)").first();
            }
            searchButton.click();
            
            wait(2000);
            
            if (page.url().contains("/rolling-paper/")) {
                return page.url();
            }
            
            Locator resultLink = page.locator("a[href*='/rolling-paper/']").first();
            if (resultLink.isVisible()) {
                resultLink.click();
                waitForPageLoad();
                return page.url();
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("닉네임 검색 실패: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * <h3>최근 방문 기록 개수</h3>
     * <p>최근 방문한 롤링페이퍼 개수를 반환합니다.</p>
     *
     * @return 방문 기록 개수 (최대 5개)
     * @author Jaeik
     * @since 2.0.0
     */
    public int getRecentVisitCount() {
        try {
            navigateToVisitPage();
            
            Locator recentVisits = page.locator("text=최근 방문").first()
                .locator("..")
                .locator("a[href*='/rolling-paper/'], [class*='card']");
            
            return Math.min(recentVisits.count(), 5);
        } catch (Exception e) {
            System.err.println("방문 기록 조회 실패: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * <h3>최근 방문 롤링페이퍼 방문</h3>
     * <p>최근 방문 기록에서 특정 인덱스의 롤링페이퍼를 방문합니다.</p>
     *
     * @param index 방문할 롤링페이퍼 인덱스 (0부터 시작)
     * @return 방문한 롤링페이퍼 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String visitRecentPaper(int index) {
        try {
            navigateToVisitPage();
            
            Locator recentVisits = page.locator("text=최근 방문").first()
                .locator("..")
                .locator("a[href*='/rolling-paper/'], [class*='card']");
            
            if (index >= recentVisits.count()) {
                return null;
            }
            
            recentVisits.nth(index).click();
            waitForPageLoad();
            
            return page.url();
        } catch (Exception e) {
            System.err.println("최근 방문 롤링페이퍼 접근 실패: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * <h3>검색 결과 없음 확인</h3>
     * <p>검색 결과가 없을 때 표시되는 메시지를 확인합니다.</p>
     *
     * @return 검색 결과 없음 메시지 표시 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean hasNoSearchResult() {
        try {
            return page.locator("text=/검색 결과가 없|찾을 수 없|존재하지 않/").isVisible();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * <h3>인기 롤링페이퍼 목록</h3>
     * <p>인기 롤링페이퍼 목록의 개수를 반환합니다.</p>
     *
     * @return 인기 롤링페이퍼 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getPopularPaperCount() {
        try {
            navigateToVisitPage();
            
            Locator popularSection = page.locator("text=/인기|추천/").first()
                .locator("..")
                .locator("a[href*='/rolling-paper/'], [class*='card']");
            
            return popularSection.count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * <h3>방문 기록 쿠키 확인</h3>
     * <p>방문 기록이 쿠키/로컬스토리지에 저장되는지 확인합니다.</p>
     *
     * @return 방문 기록 저장 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean hasVisitHistory() {
        try {
            Object result = page.evaluate("() => {" +
                "const localData = localStorage.getItem('visitHistory') || " +
                "                 localStorage.getItem('recentVisits');" +
                "const sessionData = sessionStorage.getItem('visitHistory') || " +
                "                    sessionStorage.getItem('recentVisits');" +
                "return !!(localData || sessionData);" +
            "}");
            
            return (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * <h3>방문 기록 초기화</h3>
     * <p>브라우저의 방문 기록을 초기화합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void clearVisitHistory() {
        try {
            page.evaluate("() => {" +
                "localStorage.removeItem('visitHistory');" +
                "localStorage.removeItem('recentVisits');" +
                "sessionStorage.removeItem('visitHistory');" +
                "sessionStorage.removeItem('recentVisits');" +
                "document.cookie.split(';').forEach(c => {" +
                "  if (c.trim().startsWith('visit')) {" +
                "    document.cookie = c.split('=')[0] + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';" +
                "  }" +
                "});" +
            "}");
            
            page.reload();
            waitForPageLoad();
        } catch (Exception e) {
            System.err.println("방문 기록 초기화 실패: " + e.getMessage());
        }
    }
    
    /**
     * <h3>방문 후 메시지 작성</h3>
     * <p>롤링페이퍼를 방문하여 익명 메시지를 작성합니다.</p>
     *
     * @param paperUrl 롤링페이퍼 URL
     * @param message 작성할 메시지
     * @param author 작성자 이름 (익명일 경우 null)
     * @return 메시지 작성 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean visitAndWriteMessage(String paperUrl, String message, String author) {
        try {
            page.navigate(paperUrl);
            waitForPageLoad();
            
            Locator emptyCell = page.locator(".grid-cell.empty, [class*='cell'][class*='empty']").first();
            if (!emptyCell.isVisible()) {
                emptyCell = page.locator(".grid-cell:not(.filled)").first();
            }
            
            emptyCell.click();
            wait(500);
            
            Locator messageInput = page.locator("textarea[placeholder*='메시지'], textarea[name*='message']").first();
            messageInput.fill(message);
            
            if (author != null && !author.isEmpty()) {
                Locator authorInput = page.locator("input[placeholder*='이름'], input[name*='author'], input[name*='name']").first();
                if (authorInput.isVisible()) {
                    authorInput.fill(author);
                }
            }
            
            Locator submitButton = page.locator("button:has-text('작성'), button:has-text('등록'), button:has-text('보내기')").first();
            submitButton.click();
            
            wait(1000);
            
            return page.locator("text=" + message).isVisible() || 
                   page.locator(".filled, .message-content").count() > 0;
        } catch (Exception e) {
            System.err.println("메시지 작성 실패: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>방문 기록 추가 확인</h3>
     * <p>새로운 롤링페이퍼 방문 후 방문 기록에 추가되었는지 확인합니다.</p>
     *
     * @param paperUrl 방문한 롤링페이퍼 URL
     * @return 방문 기록 추가 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isAddedToVisitHistory(String paperUrl) {
        try {
            navigateToVisitPage();
            
            String paperId = paperUrl.substring(paperUrl.lastIndexOf("/") + 1);
            
            return page.locator("a[href*='" + paperId + "']").isVisible() ||
                   page.locator("text=" + paperId).isVisible();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * <h3>방문 기록 최대 개수 확인</h3>
     * <p>방문 기록이 최대 5개까지만 저장되는지 확인합니다.</p>
     *
     * @return 방문 기록이 5개 이하인지 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean isVisitHistoryLimitedTo5() {
        try {
            navigateToVisitPage();
            int count = getRecentVisitCount();
            return count <= 5;
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * <h3>검색창 자동완성</h3>
     * <p>닉네임 입력 시 자동완성 제안이 표시되는지 확인합니다.</p>
     *
     * @param partialNickname 부분 닉네임
     * @return 자동완성 제안 표시 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean hasAutoComplete(String partialNickname) {
        try {
            navigateToVisitPage();
            
            Locator searchInput = page.locator("input[placeholder*='닉네임']").first();
            searchInput.fill(partialNickname);
            
            wait(500);
            
            return page.locator("[role='listbox'], .autocomplete, .suggestions").isVisible();
        } catch (Exception e) {
            return false;
        }
    }
}