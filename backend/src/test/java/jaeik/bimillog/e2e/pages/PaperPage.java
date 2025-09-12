package jaeik.bimillog.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <h2>롤링페이퍼 페이지 객체</h2>
 * <p>롤링페이퍼 생성, 메시지 작성, 조회 기능을 관리합니다.</p>
 * <p>그리드 레이아웃 확인, 방문 기록 관리</p>
 *
 * @author Jaeik
 * @version 2.0.0
 */
public class PaperPage extends BasePage {
    
    private static final String CREATE_BUTTON = "button:has-text('롤링페이퍼 만들기')";
    private static final String TITLE_INPUT = "input[name='title']";
    private static final String DESCRIPTION_INPUT = "textarea[name='description']";
    private static final String SUBMIT_BUTTON = "button[type='submit']";
    private static final String GRID_CONTAINER = ".paper-grid";
    private static final String GRID_CELL = ".grid-cell";
    private static final String MESSAGE_MODAL = "[data-testid='message-modal']";
    private static final String MESSAGE_INPUT = "textarea[name='message']";
    private static final String MESSAGE_AUTHOR_INPUT = "input[name='author']";
    private static final String MESSAGE_SUBMIT = "button:has-text('작성')";
    private static final String VISIT_HISTORY = "[data-testid='visit-history']";
    
    public PaperPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }
    
    /**
     * <h3>롤링페이퍼 생성 페이지로 이동</h3>
     * <p>롤링페이퍼 생성 페이지로 이동합니다.</p>
     *
     * @author Jaeik
     * @since 2.0.0
     */
    public void navigateToCreate() {
        navigate("/paper/create");
        waitForElement(TITLE_INPUT);
    }
    
    /**
     * <h3>롤링페이퍼 생성</h3>
     * <p>새로운 롤링페이퍼를 생성합니다.</p>
     *
     * @param title 롤링페이퍼 제목
     * @param description 설명
     * @return 생성된 롤링페이퍼 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String createPaper(String title, String description) {
        navigateToCreate();
        
        String uniqueTitle = title + "_" + generateTimestamp();
        
        fill(TITLE_INPUT, uniqueTitle);
        if (description != null && !description.isEmpty()) {
            fill(DESCRIPTION_INPUT, description);
        }
        
        click(SUBMIT_BUTTON);
        
        page.waitForURL("**/paper/**", new Page.WaitForURLOptions().setTimeout(5000));
        
        String paperUrl = getCurrentUrl();
        System.out.println("Paper created: " + paperUrl);
        
        return paperUrl;
    }
    
    /**
     * <h3>메시지 작성</h3>
     * <p>롤링페이퍼에 메시지를 작성합니다.</p>
     *
     * @param cellIndex 그리드 셀 인덱스 (0부터 시작)
     * @param message 메시지 내용
     * @param author 작성자 이름 (익명인 경우 null)
     * @return 작성 성공 여부
     * @author Jaeik
     * @since 2.0.0
     */
    public boolean writeMessage(int cellIndex, String message, String author) {
        try {
            Locator cells = page.locator(GRID_CELL);
            
            if (cellIndex >= cells.count()) {
                System.err.println("Invalid cell index: " + cellIndex);
                return false;
            }
            
            cells.nth(cellIndex).click();
            
            waitForElement(MESSAGE_MODAL);
            
            fill(MESSAGE_INPUT, message);
            
            if (author != null && !author.isEmpty()) {
                fill(MESSAGE_AUTHOR_INPUT, author);
            }
            
            click(MESSAGE_SUBMIT);
            
            wait(1000);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to write message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * <h3>그리드 레이아웃 확인</h3>
     * <p>현재 뷰포트에 따른 그리드 레이아웃을 확인합니다.</p>
     *
     * @return 그리드 정보 (행x열)
     * @author Jaeik
     * @since 2.0.0
     */
    public String getGridLayout() {
        waitForElement(GRID_CONTAINER);
        
        int totalCells = page.locator(GRID_CELL).count();
        
        Object gridColumns = page.evaluate(
            "window.getComputedStyle(document.querySelector('.paper-grid')).gridTemplateColumns"
        );
        
        String columnsStr = gridColumns.toString();
        int columns = columnsStr.split(" ").length;
        int rows = totalCells / columns;
        
        String layout = rows + "x" + columns;
        System.out.println("Grid layout: " + layout + " (Total cells: " + totalCells + ")");
        
        return layout;
    }
    
    /**
     * <h3>모바일 레이아웃 확인</h3>
     * <p>모바일 뷰포트에서 그리드 레이아웃을 확인합니다.</p>
     *
     * @return 모바일 그리드 레이아웃 (4x10 예상)
     * @author Jaeik
     * @since 2.0.0
     */
    public String getMobileGridLayout() {
        page.setViewportSize(375, 667);
        wait(500);
        
        String layout = getGridLayout();
        
        page.setViewportSize(1920, 1080);
        
        return layout;
    }
    
    /**
     * <h3>PC 레이아웃 확인</h3>
     * <p>PC 뷰포트에서 그리드 레이아웃을 확인합니다.</p>
     *
     * @return PC 그리드 레이아웃 (6x10 예상)
     * @author Jaeik
     * @since 2.0.0
     */
    public String getPCGridLayout() {
        page.setViewportSize(1920, 1080);
        wait(500);
        
        return getGridLayout();
    }
    
    /**
     * <h3>메시지 읽기</h3>
     * <p>특정 셀의 메시지를 읽습니다.</p>
     *
     * @param cellIndex 그리드 셀 인덱스
     * @return 메시지 내용
     * @author Jaeik
     * @since 2.0.0
     */
    public String readMessage(int cellIndex) {
        try {
            Locator cells = page.locator(GRID_CELL);
            cells.nth(cellIndex).click();
            
            waitForElement(MESSAGE_MODAL);
            
            String messageContent = getText(".message-content");
            
            pressKey("Escape");
            
            return messageContent;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * <h3>방문 기록 확인</h3>
     * <p>최근 방문한 롤링페이퍼 목록을 확인합니다.</p>
     *
     * @return 방문 기록 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getVisitHistoryCount() {
        navigate("/");
        
        if (!isElementVisible(VISIT_HISTORY)) {
            return 0;
        }
        
        return page.locator(VISIT_HISTORY + " .history-item").count();
    }
    
    /**
     * <h3>롤링페이퍼 공유 URL 가져오기</h3>
     * <p>현재 롤링페이퍼의 공유 URL을 가져옵니다.</p>
     *
     * @return 공유 URL
     * @author Jaeik
     * @since 2.0.0
     */
    public String getShareUrl() {
        click("button:has-text('공유')");
        
        waitForElement("[data-testid='share-modal']");
        
        String shareUrl = page.locator("input[readonly]").inputValue();
        
        pressKey("Escape");
        
        return shareUrl;
    }
    
    /**
     * <h3>사용된 셀 개수 확인</h3>
     * <p>메시지가 작성된 셀의 개수를 확인합니다.</p>
     *
     * @return 사용된 셀 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getUsedCellsCount() {
        return page.locator(GRID_CELL + ".filled").count();
    }
    
    /**
     * <h3>빈 셀 개수 확인</h3>
     * <p>메시지를 작성할 수 있는 빈 셀의 개수를 확인합니다.</p>
     *
     * @return 빈 셀 개수
     * @author Jaeik
     * @since 2.0.0
     */
    public int getEmptyCellsCount() {
        return page.locator(GRID_CELL + ":not(.filled)").count();
    }
    
    /**
     * <h3>타임스탬프 생성</h3>
     * <p>고유한 식별자를 위한 타임스탬프를 생성합니다.</p>
     *
     * @return 타임스탬프 문자열
     * @author Jaeik
     * @since 2.0.0
     */
    private String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}