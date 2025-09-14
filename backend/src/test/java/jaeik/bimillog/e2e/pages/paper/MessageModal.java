package jaeik.bimillog.e2e.pages.paper;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 모달 페이지 객체
 * 메시지 작성 및 상세 보기 모달 조작
 */
public class MessageModal {

    private final Page page;

    // 셀렉터 정의
    private static final String MODAL = "[role='dialog'], .modal, .message-modal";
    private static final String MODAL_TITLE = ".modal-title, h2, h3";
    private static final String CLOSE_BUTTON = "button[aria-label*='닫기'], .close-button, button:has-text('닫기')";

    // 메시지 작성 폼
    private static final String AUTHOR_INPUT = "input[name='author'], input[placeholder*='작성자'], input[placeholder*='이름']";
    private static final String CONTENT_TEXTAREA = "textarea[name='content'], textarea[placeholder*='메시지']";
    private static final String PASSWORD_INPUT = "input[name='password'], input[type='password']:not([name='confirmPassword'])";
    private static final String DESIGN_OPTIONS = ".design-option, .message-style, [data-design]";
    private static final String SELECTED_DESIGN = ".design-option.selected, .design-option[aria-selected='true']";
    private static final String SUBMIT_BUTTON = "button:has-text('작성'), button:has-text('보내기'), button[type='submit']";
    private static final String CHAR_COUNT = ".char-count, .text-count, [class*='count']";

    // 메시지 상세 보기
    private static final String MESSAGE_AUTHOR = ".message-author, [class*='author']";
    private static final String MESSAGE_CONTENT = ".message-content, [class*='content']";
    private static final String MESSAGE_TIME = ".message-time, [class*='time'], time";
    private static final String MESSAGE_DESIGN = ".message-design, [class*='design-badge']";
    private static final String DELETE_BUTTON = "button:has-text('삭제'), button[aria-label='삭제']";

    // 삭제 확인
    private static final String DELETE_PASSWORD_INPUT = "input[type='password']";
    private static final String DELETE_CONFIRM_BUTTON = "button:has-text('확인'), button:has-text('삭제하기')";

    public MessageModal(Page page) {
        this.page = page;
    }

    /**
     * 모달이 열려있는지 확인
     */
    public boolean isOpen() {
        return page.locator(MODAL).isVisible();
    }

    /**
     * 모달이 열릴 때까지 대기
     */
    public void waitForOpen() {
        page.waitForSelector(MODAL, new Page.WaitForSelectorOptions().setTimeout(5000));
    }

    /**
     * 모달 닫기
     */
    public void close() {
        if (isOpen()) {
            // ESC 키로 닫기 시도
            page.keyboard().press("Escape");
            page.waitForTimeout(500);

            // 여전히 열려있으면 닫기 버튼 클릭
            if (isOpen()) {
                Locator closeBtn = page.locator(CLOSE_BUTTON);
                if (closeBtn.isVisible()) {
                    closeBtn.click();
                }
            }
        }
    }

    /**
     * 모달 제목 가져오기
     */
    public String getTitle() {
        Locator title = page.locator(MODAL + " " + MODAL_TITLE);
        if (title.count() > 0) {
            return title.textContent();
        }
        return "";
    }

    // ===== 메시지 작성 관련 메소드 =====

    /**
     * 작성자 이름 입력
     */
    public void fillAuthor(String author) {
        page.fill(AUTHOR_INPUT, author);
    }

    /**
     * 메시지 내용 입력
     */
    public void fillContent(String content) {
        page.fill(CONTENT_TEXTAREA, content);
    }

    /**
     * 비밀번호 입력
     */
    public void fillPassword(String password) {
        page.fill(PASSWORD_INPUT, password);
    }

    /**
     * 디자인 타입 선택
     */
    public void selectDesign(String designType) {
        Locator designs = page.locator(DESIGN_OPTIONS);
        for (int i = 0; i < designs.count(); i++) {
            Locator design = designs.nth(i);
            String dataDesign = design.getAttribute("data-design");
            String text = design.textContent();

            if ((dataDesign != null && dataDesign.equalsIgnoreCase(designType)) ||
                (text != null && text.toLowerCase().contains(designType.toLowerCase()))) {
                design.click();
                break;
            }
        }
    }

    /**
     * 인덱스로 디자인 선택
     */
    public void selectDesignByIndex(int index) {
        Locator designs = page.locator(DESIGN_OPTIONS);
        if (designs.count() > index) {
            designs.nth(index).click();
        }
    }

    /**
     * 사용 가능한 디자인 타입 목록 가져오기
     */
    public List<String> getAvailableDesigns() {
        List<String> designList = new ArrayList<>();
        Locator designs = page.locator(DESIGN_OPTIONS);

        for (int i = 0; i < designs.count(); i++) {
            String design = designs.nth(i).getAttribute("data-design");
            if (design == null) {
                design = designs.nth(i).textContent();
            }
            if (design != null) {
                designList.add(design);
            }
        }
        return designList;
    }

    /**
     * 선택된 디자인 타입 가져오기
     */
    public String getSelectedDesign() {
        Locator selected = page.locator(SELECTED_DESIGN);
        if (selected.count() > 0) {
            String design = selected.getAttribute("data-design");
            if (design == null) {
                design = selected.textContent();
            }
            return design;
        }
        return "";
    }

    /**
     * 작성 버튼 클릭
     */
    public void clickSubmit() {
        page.click(SUBMIT_BUTTON);
    }

    /**
     * 글자 수 제한 확인
     */
    public String getCharCount() {
        Locator count = page.locator(CHAR_COUNT);
        if (count.count() > 0) {
            return count.textContent();
        }
        return "";
    }

    /**
     * 긴 텍스트 입력하여 글자 수 제한 테스트
     */
    public void fillLongContent(int length) {
        StringBuilder sb = new StringBuilder();
        String sample = "테스트 메시지입니다. ";
        while (sb.length() < length) {
            sb.append(sample);
        }
        fillContent(sb.substring(0, Math.min(length, sb.length())));
    }

    // ===== 메시지 상세 보기 관련 메소드 =====

    /**
     * 메시지 작성자 가져오기
     */
    public String getMessageAuthor() {
        Locator author = page.locator(MESSAGE_AUTHOR);
        if (author.count() > 0) {
            return author.textContent();
        }
        return "";
    }

    /**
     * 메시지 내용 가져오기
     */
    public String getMessageContent() {
        Locator content = page.locator(MESSAGE_CONTENT);
        if (content.count() > 0) {
            return content.textContent();
        }
        return "";
    }

    /**
     * 메시지 작성 시간 가져오기
     */
    public String getMessageTime() {
        Locator time = page.locator(MESSAGE_TIME);
        if (time.count() > 0) {
            return time.textContent();
        }
        return "";
    }

    /**
     * 메시지 디자인 타입 가져오기
     */
    public String getMessageDesign() {
        Locator design = page.locator(MESSAGE_DESIGN);
        if (design.count() > 0) {
            return design.textContent();
        }
        return "";
    }

    /**
     * 삭제 버튼이 표시되는지 확인
     */
    public boolean isDeleteButtonVisible() {
        return page.locator(DELETE_BUTTON).isVisible();
    }

    /**
     * 삭제 버튼 클릭
     */
    public void clickDeleteButton() {
        if (isDeleteButtonVisible()) {
            page.click(DELETE_BUTTON);
        }
    }

    // ===== 삭제 확인 관련 메소드 =====

    /**
     * 삭제 비밀번호 입력
     */
    public void fillDeletePassword(String password) {
        page.fill(DELETE_PASSWORD_INPUT, password);
    }

    /**
     * 삭제 확인 버튼 클릭
     */
    public void clickDeleteConfirm() {
        page.click(DELETE_CONFIRM_BUTTON);
    }

    /**
     * 에러 메시지 확인
     */
    public boolean hasErrorMessage(String message) {
        Locator error = page.locator(".error, [role='alert'], .text-red-500");
        if (error.count() > 0) {
            return error.textContent().contains(message);
        }
        return false;
    }

    /**
     * 폼 유효성 검사 에러 확인
     */
    public boolean hasValidationError() {
        // HTML5 유효성 검사
        Boolean hasInvalid = (Boolean) page.evaluate(
            "() => document.querySelector(':invalid') !== null"
        );
        return hasInvalid || hasErrorMessage("");
    }

    /**
     * 작성 버튼이 활성화되어 있는지 확인
     */
    public boolean isSubmitButtonEnabled() {
        return page.locator(SUBMIT_BUTTON).isEnabled();
    }

    /**
     * 모든 필드 초기화
     */
    public void clearAllFields() {
        page.fill(AUTHOR_INPUT, "");
        page.fill(CONTENT_TEXTAREA, "");
        page.fill(PASSWORD_INPUT, "");
    }

    /**
     * 토스트 메시지 확인
     */
    public boolean isToastVisible(String message) {
        Locator toast = page.locator(".toast, [role='alert']");
        if (toast.count() > 0) {
            return toast.textContent().contains(message);
        }
        return false;
    }
}