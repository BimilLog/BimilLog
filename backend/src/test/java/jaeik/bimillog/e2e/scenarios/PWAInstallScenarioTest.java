package jaeik.bimillog.e2e.scenarios;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.base.E2ETestConfig;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * PWA 설치 시나리오 E2E 테스트
 * USER_SCENARIOS.md의 8번 시나리오 "PWA 설치 페이지" 구현
 * - PWA 설치 가능 여부 체크
 * - 브라우저별 설치 가이드 표시
 * - 설치 프롬프트 트리거
 * - 반응형 디자인 검증
 * - 매니페스트 및 서비스 워커 검증
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PWA 설치 시나리오 E2E 테스트")
public class PWAInstallScenarioTest extends BaseE2ETest {

    @BeforeEach
    void setup() {
        E2ETestConfig.ensureFrontendReady();
        navigateToFrontend("/install");
    }

    @Test
    @Order(1)
    @DisplayName("01. PWA 설치 페이지 접근 및 기본 요소 확인")
    void testPWAInstallPageAccess() {
        // 페이지 로드 대기
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 페이지 타이틀 확인
        String title = page.title();
        assertThat(title).containsIgnoringCase("비밀로그");

        // 헤더 네비게이션 확인
        assertThat(page.locator("header").isVisible()).isTrue();

        // 뒤로가기 버튼 확인
        Locator backButton = page.locator("a:has-text('뒤로가기')");
        assertThat(backButton.isVisible()).isTrue();
        assertThat(backButton.getAttribute("href")).isEqualTo("/");

        // 페이지 헤더 텍스트 확인
        assertThat(page.locator("text=비밀로그 설치").isVisible()).isTrue();

        // Hero 섹션 확인
        assertThat(page.locator("h1:has-text('비밀로그를')").isVisible()).isTrue();
        assertThat(page.locator("text=앱으로 설치하세요").isVisible()).isTrue();
        assertThat(page.locator("text=더 빠르고 편리한 앱 경험으로").isVisible()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("02. 브라우저 감지 및 표시")
    void testBrowserDetection() {
        // 브라우저 정보 표시 확인
        Locator browserInfo = page.locator("text=현재 브라우저:");
        assertThat(browserInfo.isVisible()).isTrue();

        // 브라우저 이름이 표시되는지 확인 (Chrome, Firefox, Safari 등)
        String browserText = browserInfo.textContent();
        assertThat(browserText).matches("현재 브라우저: .+");

        // 브라우저 상태 인디케이터 (녹색 점) 확인
        Locator statusIndicator = page.locator(".w-3.h-3.bg-green-500.rounded-full");
        assertThat(statusIndicator.isVisible()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("03. PWA 설치 버튼 동작 확인")
    void testPWAInstallButton() {
        // PWA 설치 버튼 찾기
        Locator installButtons = page.locator("button:has-text('앱 설치'), button:has-text('홈 화면에 추가'), button:has-text('앱 설치 안내')");
        assertThat(installButtons.count()).isGreaterThan(0);

        // 첫 번째 설치 버튼 확인
        Locator mainInstallButton = installButtons.first();
        assertThat(mainInstallButton.isVisible()).isTrue();
        assertThat(mainInstallButton.isEnabled()).isTrue();

        // 버튼 스타일 확인 (그라데이션 배경)
        String className = mainInstallButton.getAttribute("class");
        assertThat(className).containsAnyOf("from-cyan", "to-teal", "bg-gradient");

        // beforeinstallprompt 이벤트 시뮬레이션 확인
        Boolean isPWAInstallable = (Boolean) page.evaluate("() => window.deferredPrompt !== undefined");

        // 버튼 클릭 시뮬레이션
        mainInstallButton.click();
        page.waitForTimeout(1000);

        if (isPWAInstallable) {
            // PWA 설치 가능한 경우: 브라우저 프롬프트 트리거 확인
            // 실제 설치는 사용자 상호작용 필요
            System.out.println("PWA 설치 프롬프트 트리거됨");
        } else {
            // PWA 설치 불가능한 경우: 가이드 모달 확인
            Locator modal = page.locator("[role='dialog'], .modal, [data-state='open'], [aria-modal='true']");
            if (modal.count() > 0) {
                assertThat(modal.isVisible()).isTrue();
                // 모달 내용 확인
                assertThat(page.locator("text=설치 가이드, text=브라우저 가이드").count()).isGreaterThan(0);

                // 모달 닫기
                Locator closeButton = page.locator("button[aria-label*='Close'], button:has-text('닫기'), button:has-text('취소'), button[aria-label*='close']");
                if (closeButton.count() > 0 && closeButton.isVisible()) {
                    closeButton.click();
                } else {
                    page.keyboard().press("Escape");
                }
                page.waitForTimeout(500);
                assertThat(modal.isVisible()).isFalse();
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("04. 설치 가이드 표시 - Chrome/Android/PC")
    void testChromeInstallGuide() {
        // Chrome/Android/PC 설치 가이드 섹션 찾기
        Locator guideSection = page.locator("text=Android/PC 설치 방법, text=Chrome 설치 방법").first();

        if (guideSection.count() > 0) {
            assertThat(guideSection.isVisible()).isTrue();

            // 설치 단계 확인
            assertThat(page.locator("text=Chrome 브라우저에서 이 페이지를 여세요").isVisible()).isTrue();
            assertThat(page.locator("text=앱 설치").isVisible()).isTrue();
            assertThat(page.locator("text=설치 확인 창에서").isVisible()).isTrue();

            // 단계 번호 확인 (1, 2, 3)
            Locator stepNumbers = page.locator(".w-8.h-8.rounded-full.flex.items-center.justify-center");
            assertThat(stepNumbers.count()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @Order(5)
    @DisplayName("05. 설치 가이드 표시 - iOS/Safari")
    void testIOSInstallGuide() {
        // iOS Safari 설치 가이드 섹션 찾기
        Locator iosGuide = page.locator("text=iPhone/iPad 설치 방법, text=iOS 설치 방법").first();

        if (iosGuide.count() > 0) {
            assertThat(iosGuide.isVisible()).isTrue();

            // iOS 특유의 설치 단계 확인
            assertThat(page.locator("text=Safari 브라우저에서 이 페이지를 여세요").isVisible()).isTrue();
            assertThat(page.locator("text=공유").isVisible()).isTrue();
            assertThat(page.locator("text=홈 화면에 추가").isVisible()).isTrue();
        }
    }

    @Test
    @Order(6)
    @DisplayName("06. 앱 설치 혜택 섹션 확인")
    void testFeatureBenefits() {
        // 혜택 섹션으로 스크롤
        page.evaluate("window.scrollTo(0, document.querySelector('h2:has-text(\"앱으로 설치하면 더 좋은 점\")').offsetTop)");
        page.waitForTimeout(500);

        // 혜택 섹션 제목 확인
        assertThat(page.locator("h2:has-text('앱으로 설치하면 더 좋은 점')").isVisible()).isTrue();
        assertThat(page.locator("text=브라우저보다 훨씬 빠르고 편리한 앱 경험을 제공합니다").isVisible()).isTrue();

        // 4가지 혜택 카드 확인
        String[] benefits = {
            "빠른 접속",
            "알림 받기",
            "오프라인 지원",
            "보안 강화"
        };

        for (String benefit : benefits) {
            Locator benefitCard = page.locator("h3:has-text('" + benefit + "')");
            assertThat(benefitCard.isVisible()).isTrue();

            // 각 혜택의 설명 텍스트 확인
            Locator cardParent = benefitCard.locator("..");
            assertThat(cardParent.locator("p").isVisible()).isTrue();

            // 아이콘 확인
            assertThat(cardParent.locator("svg, .w-8.h-8").isVisible()).isTrue();
        }
    }

    @Test
    @Order(7)
    @DisplayName("07. 앱 미리보기 섹션 확인")
    void testAppPreview() {
        // 미리보기 섹션으로 스크롤
        page.evaluate("window.scrollTo(0, document.querySelector('h2:has-text(\"앱 미리보기\")').offsetTop)");
        page.waitForTimeout(500);

        // 미리보기 섹션 확인
        assertThat(page.locator("h2:has-text('앱 미리보기')").isVisible()).isTrue();
        assertThat(page.locator("text=실제 앱처럼 동작하는 비밀로그의 모습을 확인해보세요").isVisible()).isTrue();

        // 미리보기 카드 확인
        Locator previewCard = page.locator(".bg-gradient-to-r.from-cyan-500");
        assertThat(previewCard.isVisible()).isTrue();

        // 앱 이름과 설명 확인
        assertThat(page.locator("h3:has-text('비밀로그')").isVisible()).isTrue();
        assertThat(page.locator("text=익명으로 마음을 전하는 특별한 공간").isVisible()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("08. Call to Action 섹션 확인")
    void testCallToAction() {
        // CTA 섹션으로 스크롤
        scrollToBottom();
        page.waitForTimeout(500);

        // CTA 텍스트 확인
        assertThat(page.locator("text=지금 바로 설치해보세요!").isVisible()).isTrue();
        assertThat(page.locator("text=더 나은 비밀로그 경험이 기다리고 있습니다").isVisible()).isTrue();

        // 하단 설치 버튼 확인
        Locator ctaButton = page.locator("button").last();
        assertThat(ctaButton.isVisible()).isTrue();
        assertThat(ctaButton.textContent()).containsAnyOf("앱 설치", "홈 화면에 추가", "앱 설치 안내");
    }

    @Test
    @Order(9)
    @DisplayName("09. 반응형 디자인 - 모바일 뷰포트")
    void testMobileResponsive() {
        // 모바일 뷰포트로 변경
        setMobileViewport();
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 모바일에서 주요 요소들이 제대로 표시되는지 확인
        assertThat(page.locator("h1").isVisible()).isTrue();

        // 설치 버튼이 모바일에서도 잘 보이는지 확인
        Locator mobileInstallButton = page.locator("button").first();
        assertThat(mobileInstallButton.isVisible()).isTrue();

        // 설치 가이드가 세로로 잘 정렬되는지 확인
        Locator guideCards = page.locator(".max-w-2xl.mx-auto");
        assertThat(guideCards.count()).isGreaterThan(0);

        // 혜택 카드들이 세로로 스택되는지 확인
        Locator benefitCards = page.locator(".grid > div");
        if (benefitCards.count() > 0) {
            // 모바일에서는 1열로 표시되어야 함
            Locator firstCard = benefitCards.first();
            Locator secondCard = benefitCards.nth(1);

            if (secondCard.count() > 0) {
                // 두 카드의 Y 좌표를 비교하여 세로 정렬 확인
                double firstY = firstCard.boundingBox().y;
                double secondY = secondCard.boundingBox().y;
                assertThat(secondY).isGreaterThan(firstY);
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. 반응형 디자인 - 태블릿 뷰포트")
    void testTabletResponsive() {
        // 태블릿 뷰포트로 변경
        setTabletViewport();
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 태블릿에서 주요 요소 확인
        assertThat(page.locator("h1").isVisible()).isTrue();
        assertThat(page.locator("button").first().isVisible()).isTrue();

        // 혜택 카드가 2열로 표시되는지 확인
        Locator benefitSection = page.locator(".grid.md\\:grid-cols-2, .grid.lg\\:grid-cols-4");
        if (benefitSection.count() > 0) {
            assertThat(benefitSection.isVisible()).isTrue();
        }
    }

    @Test
    @Order(11)
    @DisplayName("11. PWA 매니페스트 파일 접근 가능성 확인")
    void testManifestAccessibility() {
        // manifest.json 파일 직접 접근 테스트
        page.navigate(FRONTEND_URL + "/manifest.json");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // JSON 응답 확인
        String content = page.textContent("body");
        assertThat(content).contains("비밀로그");
        assertThat(content).contains("short_name");
        assertThat(content).contains("icons");
        assertThat(content).contains("start_url");
        assertThat(content).contains("display");
        assertThat(content).contains("standalone");

        // 원래 페이지로 돌아가기
        navigateToFrontend("/install");
    }

    @Test
    @Order(12)
    @DisplayName("12. 서비스 워커 등록 확인")
    void testServiceWorkerRegistration() {
        // 서비스 워커가 등록되었는지 JavaScript로 확인
        Object swRegistration = page.evaluate("() => navigator.serviceWorker ? navigator.serviceWorker.ready : null");

        if (swRegistration != null) {
            // 서비스 워커가 지원되는 브라우저인 경우
            Boolean hasServiceWorker = (Boolean) page.evaluate("() => navigator.serviceWorker.controller !== null");
            // 첫 방문시에는 서비스 워커가 아직 활성화되지 않을 수 있음
            System.out.println("Service Worker registered: " + hasServiceWorker);
        }

        // sw.js 파일 접근 가능성 확인
        page.navigate(FRONTEND_URL + "/sw.js");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        String swContent = page.textContent("body");
        assertThat(swContent).containsAnyOf("self.skipWaiting", "workbox", "precache", "firebase-messaging");

        // 원래 페이지로 돌아가기
        navigateToFrontend("/install");
    }

    @Test
    @Order(13)
    @DisplayName("13. 프로토콜 핸들러 설정 확인")
    void testProtocolHandler() {
        // manifest.json에서 프로토콜 핸들러 확인
        page.navigate(FRONTEND_URL + "/manifest.json");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        String manifestContent = page.textContent("body");
        assertThat(manifestContent).contains("protocol_handlers");
        assertThat(manifestContent).contains("web+bimillog");

        // 원래 페이지로 돌아가기
        navigateToFrontend("/install");
    }

    @Test
    @Order(14)
    @DisplayName("14. 페이지 Footer 링크 확인")
    void testFooterLinks() {
        // Footer로 스크롤
        scrollToBottom();
        page.waitForTimeout(500);

        // Footer 섹션 확인
        Locator footer = page.locator("footer");
        assertThat(footer.isVisible()).isTrue();

        // 비밀로그 타이틀 확인
        assertThat(footer.locator("h2:has-text('비밀로그')").isVisible()).isTrue();

        // Footer 링크 확인
        assertThat(footer.locator("a[href='/privacy']").isVisible()).isTrue();
        assertThat(footer.locator("a[href='/terms']").isVisible()).isTrue();

        // 버전 정보 확인
        assertThat(footer.locator("text=v2.0.0").isVisible()).isTrue();
        assertThat(footer.locator("text=© 2025 비밀로그").isVisible()).isTrue();
    }

    @Test
    @Order(15)
    @DisplayName("15. 설치 후 동작 시뮬레이션")
    void testPostInstallBehavior() {
        // PWA가 설치된 것처럼 시뮬레이션 (standalone 모드)
        // URL 파라미터로 standalone 모드 시뮬레이션
        navigateToFrontend("/install?display-mode=standalone");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // standalone 모드에서도 페이지가 정상 동작하는지 확인
        assertThat(page.locator("h1").isVisible()).isTrue();

        // 네비게이션이 제대로 동작하는지 확인
        Locator backButton = page.locator("a:has-text('뒤로가기')");
        if (backButton.isVisible()) {
            backButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 메인 페이지로 이동했는지 확인
            assertThat(page.url()).isEqualTo(FRONTEND_URL + "/");
        }
    }

    @Test
    @Order(16)
    @DisplayName("16. 인앱 브라우저에서 설치 버튼 숨김 확인")
    void testInAppBrowserHidesInstallButton() {
        // 카카오톡 인앱 브라우저 User-Agent 시뮬레이션
        context.close();
        context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
            .setUserAgent("Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36 KAKAOTALK")
            .setViewportSize(MOBILE.width, MOBILE.height));
        page = context.newPage();

        navigateToFrontend("/install");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // 인앱 브라우저에서는 PWA 설치 버튼이 표시되지 않아야 함
        Locator installButton = page.locator("button:has-text('앱 설치')");
        assertThat(installButton.count()).isEqualTo(0);

        // 대신 일반 브라우저로 열기 안내가 있을 수 있음
        Locator browserGuide = page.locator("text=브라우저에서 열기, text=Safari에서 열기, text=Chrome에서 열기");
        if (browserGuide.count() > 0) {
            assertThat(browserGuide.isVisible()).isTrue();
        }
    }

    @Test
    @Order(17)
    @DisplayName("17. PWA 아이콘 및 스크린샷 확인")
    void testPWAIconsAndScreenshots() {
        // manifest.json의 아이콘 확인
        page.navigate(FRONTEND_URL + "/manifest.json");
        String manifestContent = page.textContent("body");

        // 각 아이콘 사이즈 확인
        String[] iconSizes = {"48x48", "72x72", "96x96", "128x128", "192x192", "384x384", "512x512"};
        for (String size : iconSizes) {
            assertThat(manifestContent).contains(size);
        }

        // 아이콘 파일 접근 가능성 확인
        page.navigate(FRONTEND_URL + "/favicon192.png");
        assertThat(page.evaluate("() => document.contentType")).isNotEqualTo("text/html");

        // 스크린샷 확인
        assertThat(manifestContent).contains("screenshots");
        assertThat(manifestContent).contains("form_factor");
    }

    @Test
    @Order(18)
    @DisplayName("18. 오프라인 지원 메시지 확인")
    void testOfflineSupportMessage() {
        navigateToFrontend("/install");

        // 오프라인 지원 혜택 카드 확인
        Locator offlineCard = page.locator("h3:has-text('오프라인 지원')");
        assertThat(offlineCard.isVisible()).isTrue();

        // 오프라인 설명 텍스트 확인
        Locator offlineDescription = offlineCard.locator("..").locator("p");
        assertThat(offlineDescription.textContent()).contains("인터넷 연결이 없어도");
    }

    /**
     * 헬퍼 메서드: 설치 가이드 모달 닫기
     */
    private void closeInstallGuideModal() {
        Locator modal = page.locator("[role='dialog'], .modal");
        if (modal.isVisible()) {
            // ESC 키로 모달 닫기 시도
            page.keyboard().press("Escape");
            page.waitForTimeout(500);

            // 여전히 열려있다면 닫기 버튼 클릭
            if (modal.isVisible()) {
                Locator closeButton = page.locator("button[aria-label*='Close'], button:has-text('닫기')");
                if (closeButton.isVisible()) {
                    closeButton.click();
                }
            }
        }
    }

    /**
     * 헬퍼 메서드: 특정 텍스트가 포함된 요소로 스크롤
     */
    private void scrollToText(String text) {
        page.evaluate("(text) => {" +
            "const element = Array.from(document.querySelectorAll('*'))" +
            ".find(el => el.textContent && el.textContent.includes(text));" +
            "if (element) element.scrollIntoView({ behavior: 'smooth', block: 'center' });" +
            "}", text);
        page.waitForTimeout(500);
    }

    /**
     * 헬퍼 메서드: PWA 설치 가능 여부 체크
     */
    private boolean isPWAInstallable() {
        return (Boolean) page.evaluate("() => {" +
            "return 'serviceWorker' in navigator && " +
            "'BeforeInstallPromptEvent' in window;" +
            "}");
    }

    /**
     * 헬퍼 메서드: 브라우저가 iOS Safari인지 확인
     */
    private boolean isIOSSafari() {
        return (Boolean) page.evaluate("() => {" +
            "const ua = navigator.userAgent;" +
            "return /iPad|iPhone|iPod/.test(ua) && !window.MSStream;" +
            "}");
    }

    /**
     * 헬퍼 메서드: sleep 유틸리티
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}