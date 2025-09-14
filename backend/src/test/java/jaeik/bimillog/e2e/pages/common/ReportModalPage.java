package jaeik.bimillog.e2e.pages.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * 신고 모달 Page Object
 */
public class ReportModalPage extends BasePage {

    // Selectors
    private static final String MODAL_DIALOG = "[role='dialog']";
    private static final String MODAL_TITLE = "[role='dialog'] h2, .modal-title";
    private static final String REASON_TEXTAREA = "textarea[placeholder*='신고 사유']";
    private static final String CHARACTER_COUNT = "[class*='character-count'], span:has-text('/')";
    private static final String ERROR_MESSAGE = "[class*='alert'], [class*='error']";
    private static final String SUBMIT_BUTTON = "button:has-text('신고하기'), button:has-text('제출')";
    private static final String CANCEL_BUTTON = "button:has-text('취소')";
    private static final String CLOSE_BUTTON = "button[aria-label*='닫기']";

    // Constants
    private static final int MIN_REASON_LENGTH = 10;
    private static final int MAX_REASON_LENGTH = 500;

    public ReportModalPage(Page page, String baseUrl) {
        super(page, baseUrl);
    }

    /**
     * 모달이 표시될 때까지 대기
     */
    public void waitForModal() {
        page.waitForSelector(MODAL_DIALOG, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.VISIBLE)
            .setTimeout(5000));
    }

    /**
     * 모달이 숨겨질 때까지 대기
     */
    public void waitForModalToClose() {
        page.waitForSelector(MODAL_DIALOG, new Page.WaitForSelectorOptions()
            .setState(WaitForSelectorState.HIDDEN)
            .setTimeout(5000));
    }

    /**
     * 페이지가 로드되었는지 확인 (모달이 표시되었는지)
     */
    @Override
    public boolean isLoaded() {
        return isVisible(MODAL_DIALOG) && isVisible(REASON_TEXTAREA);
    }

    /**
     * 모달 제목 가져오기
     */
    public String getModalTitle() {
        return getText(MODAL_TITLE);
    }

    /**
     * 신고 사유 입력
     */
    public void enterReportReason(String reason) {
        fill(REASON_TEXTAREA, reason);
    }

    /**
     * 현재 입력된 신고 사유 가져오기
     */
    public String getReportReason() {
        return getValue(REASON_TEXTAREA);
    }

    /**
     * 입력된 문자 수 가져오기
     */
    public int getCharacterCount() {
        String countText = getText(CHARACTER_COUNT);
        String currentCount = countText.split("/")[0].trim();
        return Integer.parseInt(currentCount.replaceAll("[^0-9]", ""));
    }

    /**
     * 최대 문자 수 가져오기
     */
    public int getMaxCharacterCount() {
        String countText = getText(CHARACTER_COUNT);
        String maxCount = countText.split("/")[1].trim();
        return Integer.parseInt(maxCount.replaceAll("[^0-9]", ""));
    }

    /**
     * 유효한 입력인지 확인
     */
    public boolean isValidInput() {
        int count = getCharacterCount();
        return count >= MIN_REASON_LENGTH && count <= MAX_REASON_LENGTH;
    }

    /**
     * 제출 버튼이 활성화되어 있는지 확인
     */
    public boolean isSubmitButtonEnabled() {
        Locator submitButton = find(SUBMIT_BUTTON);
        return submitButton.isEnabled();
    }

    /**
     * 에러 메시지가 표시되는지 확인
     */
    public boolean isErrorMessageVisible() {
        return isVisible(ERROR_MESSAGE);
    }

    /**
     * 에러 메시지 텍스트 가져오기
     */
    public String getErrorMessage() {
        if (isErrorMessageVisible()) {
            return getText(ERROR_MESSAGE);
        }
        return "";
    }

    /**
     * 신고 제출
     */
    public void submitReport() {
        if (!isSubmitButtonEnabled()) {
            throw new IllegalStateException("제출 버튼이 비활성화되어 있습니다. 유효한 신고 사유를 입력하세요.");
        }
        click(SUBMIT_BUTTON);
    }

    /**
     * 신고 제출 (사유 입력 포함)
     */
    public void submitReportWithReason(String reason) {
        enterReportReason(reason);

        // 입력 유효성 검사 대기
        page.waitForTimeout(500);

        if (!isValidInput()) {
            throw new IllegalArgumentException(
                String.format("신고 사유는 %d자 이상 %d자 이하여야 합니다. 현재: %d자",
                    MIN_REASON_LENGTH, MAX_REASON_LENGTH, getCharacterCount())
            );
        }

        submitReport();
    }

    /**
     * 취소 버튼 클릭
     */
    public void clickCancel() {
        click(CANCEL_BUTTON);
        waitForModalToClose();
    }

    /**
     * 닫기 버튼 클릭 (X 버튼)
     */
    public void clickClose() {
        if (isVisible(CLOSE_BUTTON)) {
            click(CLOSE_BUTTON);
        } else {
            pressEscape();
        }
        waitForModalToClose();
    }

    /**
     * 모달이 표시되어 있는지 확인
     */
    public boolean isModalVisible() {
        return isVisible(MODAL_DIALOG);
    }

    /**
     * 신고 타입 확인 (게시글/댓글)
     */
    public String getReportType() {
        String title = getModalTitle();
        if (title.contains("게시글")) {
            return "POST";
        } else if (title.contains("댓글")) {
            return "COMMENT";
        }
        return "UNKNOWN";
    }

    /**
     * 최소 문자 수 미달 메시지 확인
     */
    public boolean hasMinLengthError() {
        String error = getErrorMessage();
        return error.contains("최소") || error.contains(String.valueOf(MIN_REASON_LENGTH));
    }

    /**
     * 최대 문자 수 초과 메시지 확인
     */
    public boolean hasMaxLengthError() {
        String error = getErrorMessage();
        return error.contains("최대") || error.contains(String.valueOf(MAX_REASON_LENGTH));
    }

    /**
     * 문자 수 표시 색상 확인 (유효/무효)
     */
    public boolean hasValidCharacterCountColor() {
        Locator countElement = find(CHARACTER_COUNT);
        String className = countElement.getAttribute("class");
        return className != null && (className.contains("green") || className.contains("success"));
    }

    /**
     * 신고 완료 메시지 확인
     */
    public boolean isReportSubmittedSuccessfully() {
        // 모달이 닫히고 성공 메시지가 표시되는지 확인
        return !isModalVisible() && (hasText("신고가 접수되었습니다") || hasText("신고 완료"));
    }
}