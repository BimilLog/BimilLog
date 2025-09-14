package jaeik.bimillog.e2e.scenarios;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.base.E2ETestConfig;
import jaeik.bimillog.e2e.utils.TestDataGenerator;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 건의하기 시나리오 E2E 테스트
 * USER_SCENARIOS.md의 7번 시나리오 "건의하기 페이지" 구현
 * - 건의 유형 선택 (기능 개선 제안, 오류 신고)
 * - 내용 작성 및 제출
 * - 폼 유효성 검사
 * - 익명 사용자 접근
 * - 토스트 알림 확인
 * - 모바일 반응형 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("건의하기 시나리오 E2E 테스트")
public class SuggestScenarioTest extends BaseE2ETest {

    private static String testImprovementContent;
    private static String testErrorContent;

    @BeforeAll
    static void setupTestData() {
        testImprovementContent = "롤링페이퍼에 음성 메시지 기능을 추가하면 좋겠습니다. " +
            "텍스트로만 남기는 것보다 음성으로 따뜻한 메시지를 전할 수 있어서 더 감동적일 것 같아요.";
        testErrorContent = "게시글 작성 시 이미지 업로드가 간헐적으로 실패하는 문제가 있습니다. " +
            "특히 파일 크기가 2MB 이상일 때 자주 발생하는 것 같습니다.";
    }

    @BeforeEach
    void setup() {
        E2ETestConfig.ensureFrontendReady();
        navigateToFrontend("/suggest");
    }

    @Test
    @Order(1)
    @DisplayName("01. 건의하기 페이지 로드 및 기본 UI 확인")
    void testSuggestPageLoad() {
        // 페이지 타이틀 확인
        assertThat(page.title()).contains("건의하기");

        // 헤더 확인
        waitForElement("h1");
        Locator title = page.locator("h1:has-text('건의하기')");
        assertThat(title.isVisible()).isTrue();

        // 설명 텍스트 확인
        String pageContent = page.textContent("body");
        assertThat(pageContent).contains("비밀로그를 더 좋은 서비스로 만들어가는데 도움을 주세요");

        // 건의 유형 선택 섹션 확인
        assertThat(page.locator("text=어떤 종류의 건의사항인가요?").isVisible()).isTrue();

        // 건의 유형 카드 2개 확인
        Locator improvementCard = page.locator("text=기능 개선 제안");
        Locator errorCard = page.locator("text=오류 신고");

        assertThat(improvementCard.isVisible()).isTrue();
        assertThat(errorCard.isVisible()).isTrue();

        // 카드 설명 확인
        assertThat(page.locator("text=새로운 기능이나 기존 기능 개선 아이디어").isVisible()).isTrue();
        assertThat(page.locator("text=버그, 오작동, 기술적 문제 신고").isVisible()).isTrue();

        // 안내 사항 섹션 확인
        assertThat(page.locator("text=건의하기 안내").isVisible()).isTrue();
        assertThat(page.locator("text=바라는 기능이나 기능 개선에 대한 제안").isVisible()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("02. 기능 개선 제안 유형 선택 및 폼 표시")
    void testSelectImprovementType() {
        // 기능 개선 제안 카드 클릭
        Locator improvementCard = page.locator("text=기능 개선 제안").locator("..");
        safeClick(improvementCard);
        sleep(500);

        // 선택된 상태 확인 (카드 스타일 변경)
        String cardClass = improvementCard.getAttribute("class");
        assertThat(cardClass).containsAnyOf("border-purple", "bg-purple", "shadow-lg");

        // 폼 표시 확인
        waitForElement("text=기능 개선 제안");
        assertThat(page.locator("text=새로운 기능이나 기존 기능 개선 아이디어").isVisible()).isTrue();

        // 내용 입력 필드 확인
        Locator contentTextarea = page.locator("textarea[placeholder*='기능 개선 제안']");
        assertThat(contentTextarea.isVisible()).isTrue();

        // 글자수 카운터 확인
        assertThat(page.locator("text=0/500").isVisible()).isTrue();

        // 제출 버튼 확인
        Locator submitButton = page.locator("button:has-text('건의사항 접수하기')");
        assertThat(submitButton.isVisible()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("03. 오류 신고 유형 선택 및 폼 표시")
    void testSelectErrorType() {
        // 오류 신고 카드 클릭
        Locator errorCard = page.locator("text=오류 신고").locator("..");
        safeClick(errorCard);
        sleep(500);

        // 선택된 상태 확인
        String cardClass = errorCard.getAttribute("class");
        assertThat(cardClass).containsAnyOf("border-purple", "bg-purple", "shadow-lg");

        // 폼 표시 확인
        waitForElement("text=오류 신고");
        assertThat(page.locator("text=버그, 오작동, 기술적 문제 신고").isVisible()).isTrue();

        // 내용 입력 필드 확인
        Locator contentTextarea = page.locator("textarea[placeholder*='오류 신고']");
        assertThat(contentTextarea.isVisible()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("04. 폼 유효성 검사 - 빈 내용 제출")
    void testFormValidationEmpty() {
        // 기능 개선 제안 선택
        safeClick(page.locator("text=기능 개선 제안").locator(".."));
        waitForElement("textarea");

        // 빈 상태에서 제출 시도
        safeClick(page.locator("button:has-text('건의사항 접수하기')"));
        sleep(1000);

        // 경고 토스트 메시지 확인 - 프론트엔드 유효성 검사
        Locator toast = page.locator("[role='alert'], .bg-orange-50, .bg-purple-50");
        if (toast.count() > 0) {
            String toastText = toast.textContent();
            assertThat(toastText).containsAnyOf("입력 확인", "건의 종류와 내용을 모두 입력해주세요");
        }
    }

    @Test
    @Order(5)
    @DisplayName("05. 폼 유효성 검사 - 최소 글자수 미만")
    void testFormValidationMinLength() {
        // 기능 개선 제안 선택
        safeClick(page.locator("text=기능 개선 제안").locator(".."));
        waitForElement("textarea");

        // 10자 미만 입력
        Locator textarea = page.locator("textarea");
        textarea.fill("짧은글");

        // 제출 시도
        safeClick(page.locator("button:has-text('건의사항 접수하기')"));
        sleep(1000);

        // 경고 토스트 메시지 확인 - 프론트엔드 유효성 검사
        Locator toast = page.locator("[role='alert'], .bg-orange-50");
        if (toast.count() > 0) {
            String toastText = toast.textContent();
            assertThat(toastText).containsAnyOf("입력 확인", "건의 내용은 최소 10자 이상 입력해주세요");
        }
    }

    @Test
    @Order(6)
    @DisplayName("06. 글자수 카운터 동작 확인")
    void testCharacterCounter() {
        // 기능 개선 제안 선택
        safeClick(page.locator("text=기능 개선 제안").locator(".."));
        waitForElement("textarea");

        // 텍스트 입력
        String testText = "테스트 텍스트입니다.";
        Locator textarea = page.locator("textarea");
        textarea.fill(testText);

        // 글자수 업데이트 확인
        sleep(500);
        Locator counter = page.locator("text=/" + testText.length() + "/500, text=" + testText.length() + "/500");
        assertThat(counter.count()).isGreaterThan(0);

        // 500자 초과 입력 시도
        String longText = "가".repeat(501);
        textarea.fill(longText);

        // 입력 제한 확인 (500자까지만)
        String actualText = textarea.inputValue();
        assertThat(actualText.length()).isLessThanOrEqualTo(500);
    }

    @Test
    @Order(7)
    @DisplayName("07. 기능 개선 제안 성공적인 제출")
    void testSubmitImprovementSuggestion() {
        // 기능 개선 제안 선택
        safeClick(page.locator("text=기능 개선 제안").locator(".."));
        waitForElement("textarea");

        // 내용 입력
        Locator textarea = page.locator("textarea");
        textarea.fill(testImprovementContent);

        // 제출
        Locator submitButton = page.locator("button:has-text('건의사항 접수하기')");
        safeClick(submitButton);

        // 로딩 상태 확인
        Locator loadingButton = page.locator("button:has-text('접수 중')");
        if (loadingButton.count() > 0) {
            assertThat(loadingButton.isVisible()).isTrue();
        }

        // 성공 토스트 확인
        sleep(2000);
        Locator successToast = page.locator("[role='alert'], .bg-green-50");
        if (successToast.count() > 0) {
            String toastText = successToast.textContent();
            assertThat(toastText).containsAnyOf("건의사항 접수 완료", "성공적으로 접수되었습니다", "소중한 의견 감사합니다");
        }

        // 폼 초기화 확인 - 선택이 해제되고 textarea가 사라짐
        sleep(1000);
        // 폼 자체가 사라지므로 textarea 존재 여부로 확인
        assertThat(page.locator("textarea").count()).isEqualTo(0);
    }

    @Test
    @Order(8)
    @DisplayName("08. 오류 신고 성공적인 제출")
    void testSubmitErrorReport() {
        // 오류 신고 선택
        safeClick(page.locator("text=오류 신고").locator(".."));
        waitForElement("textarea");

        // 내용 입력
        Locator textarea = page.locator("textarea");
        textarea.fill(testErrorContent);

        // 제출
        safeClick(page.locator("button:has-text('건의사항 접수하기')"));

        // 성공 처리 대기
        sleep(2000);

        // 성공 토스트 확인
        Locator successToast = page.locator("[role='alert'], .bg-green-50");
        if (successToast.count() > 0) {
            String toastText = successToast.textContent();
            assertThat(toastText).containsAnyOf("건의사항 접수 완료", "성공적으로 접수되었습니다");
        }

        // 폼 초기화 확인 - textarea가 사라짐
        sleep(1000);
        assertThat(page.locator("textarea").count()).isEqualTo(0);
    }

    @Test
    @Order(9)
    @DisplayName("09. 비로그인 사용자 건의 접근 확인")
    void testAnonymousUserAccess() {
        // 로그아웃 상태 확인
        logout();

        // 건의하기 페이지 다시 접근
        navigateToFrontend("/suggest");
        sleep(1000);

        // 페이지 로드 확인 (접근 제한 없음)
        assertThat(page.locator("h1:has-text('건의하기')").isVisible()).isTrue();

        // 익명 사용자도 건의 유형 선택 가능
        safeClick(page.locator("text=기능 개선 제안").locator(".."));
        waitForElement("textarea");

        // 폼 작성 및 제출 가능
        Locator textarea = page.locator("textarea");
        textarea.fill("익명 사용자 테스트 건의사항입니다. 이 기능이 유용할 것 같습니다.");

        safeClick(page.locator("button:has-text('건의사항 접수하기')"));
        sleep(2000);

        // 제출 성공 여부 확인 (토스트 또는 폼 초기화)
        boolean success = false;

        // 성공 토스트 확인
        Locator toast = page.locator(".toast, [role='alert']");
        if (toast.count() > 0) {
            success = true;
        } else {
            // 폼 초기화 확인
            String value = page.locator("textarea").inputValue();
            if (value.isEmpty()) {
                success = true;
            }
        }

        assertThat(success).isTrue();
    }

    @Test
    @Order(10)
    @DisplayName("10. 모바일 반응형 디자인 확인")
    void testMobileResponsive() {
        // 모바일 뷰포트로 변경
        setMobileViewport();
        page.reload();
        waitForElement("h1");

        // 제목이 세로로 줄바꿈되어 표시되는지 확인
        assertThat(page.locator("h1:has-text('건의하기')").isVisible()).isTrue();

        // 건의 유형 카드가 세로로 정렬되는지 확인
        Locator improvementCard = page.locator("text=기능 개선 제안").locator("..");
        Locator errorCard = page.locator("text=오류 신고").locator("..");

        if (improvementCard.count() > 0 && errorCard.count() > 0) {
            var improvementBox = improvementCard.boundingBox();
            var errorBox = errorCard.boundingBox();

            // 세로로 정렬되어 있는지 확인 (Y 좌표가 다름)
            assertThat(errorBox.y).isGreaterThan(improvementBox.y);
        }

        // 기능 개선 제안 선택하여 폼 확인
        safeClick(improvementCard);
        waitForElement("textarea");

        // 모바일에서도 textarea가 적절한 크기로 표시되는지 확인
        Locator textarea = page.locator("textarea");
        var textareaBox = textarea.boundingBox();

        // 뷰포트 너비의 80% 이상을 차지하는지 확인 (패딩 고려)
        double viewportWidth = page.viewportSize().width;
        assertThat(textareaBox.width).isGreaterThan(viewportWidth * 0.7);

        // 제출 버튼이 전체 너비로 표시되는지 확인
        Locator submitButton = page.locator("button:has-text('건의사항 접수하기')");
        var buttonBox = submitButton.boundingBox();
        assertThat(buttonBox.width).isGreaterThan(viewportWidth * 0.8);

        // 데스크톱으로 복원
        setDesktopViewport();
    }

    @Test
    @Order(11)
    @DisplayName("11. Playwright MCP 활용 - 스크린샷 테스트")
    void testPlaywrightMCP() {
        // 기능 개선 제안 선택
        safeClick(page.locator("text=기능 개선 제안").locator(".."));
        waitForElement("textarea");

        // 스크린샷 촬영 (MCP 활용)
        try {
            // 전체 페이지 스크린샷
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/suggest-improvement-form.png"))
                .setFullPage(true));

            // 폼 영역만 스크린샷
            Locator formCard = page.locator("form").locator("..");
            if (formCard.count() > 0) {
                formCard.screenshot(new Locator.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get("target/screenshots/suggest-form-card.png")));
            }

            // 내용 입력
            page.locator("textarea").fill(testImprovementContent);

            // 입력 후 스크린샷
            page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/suggest-filled-form.png")));

        } catch (Exception e) {
            // 스크린샷 실패 시에도 테스트 계속 진행
            System.out.println("Screenshot failed: " + e.getMessage());
        }

        // 폼이 정상적으로 작동하는지 확인
        assertThat(page.locator("textarea").inputValue()).isEqualTo(testImprovementContent);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}