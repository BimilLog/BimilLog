package jaeik.bimillog.e2e.scenarios;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.base.E2ETestConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 에러 페이지 시나리오 E2E 테스트 (시나리오 10)
 * - 404 Not Found 페이지 테스트
 * - 403 Forbidden 접근 권한 테스트
 * - 500 Server Error 페이지 테스트
 * - 에러 복구 및 네비게이션 테스트
 * - 반응형 디자인 및 에지 케이스 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("에러 페이지 시나리오 E2E 테스트")
public class ErrorPageScenarioTest extends BaseE2ETest {

    private static final String TEST_USERNAME = "testuser@test.com";
    private static final String TEST_PASSWORD = "testpass123";
    private static final String ADMIN_USERNAME = "admin@test.com";
    private static final String ADMIN_PASSWORD = "adminpass123";

    @BeforeEach
    void setup() {
        E2ETestConfig.ensureFrontendReady();
        // 각 테스트마다 깨끗한 상태로 시작
        page.navigate(FRONTEND_URL);
        page.waitForLoadState();
    }

    @Test
    @Order(1)
    @DisplayName("01. 404 Not Found 페이지 UI 및 기능")
    void test404NotFoundPage() {
        System.out.println("=== 404 Not Found 페이지 테스트 ===\n");

        // 1. 존재하지 않는 페이지로 이동
        System.out.println("1. 존재하지 않는 페이지 접근");
        navigateToFrontend("/this-page-does-not-exist");
        page.waitForLoadState();

        // 2. 404 페이지 요소 확인
        System.out.println("\n2. 404 페이지 UI 요소 확인");
        
        // 404 숫자 표시 확인
        waitForElement("text=404", 5000);
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=404")).isVisible();
        System.out.println("   ✓ 404 숫자 표시됨");

        // 메인 제목 확인
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(
            page.locator("text=페이지를 찾을 수 없어요")
        ).isVisible();
        System.out.println("   ✓ '페이지를 찾을 수 없어요' 제목 표시됨");

        // 설명 텍스트 확인
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(
            page.locator("text=요청하신 페이지가 삭제되었거나 주소가 변경되었을 수 있어요")
        ).isVisible();
        System.out.println("   ✓ 설명 텍스트 표시됨");

        // 3. 비밀로그 로고 확인
        System.out.println("\n3. 로고 및 디자인 요소 확인");
        Locator logo = page.locator("img[alt='비밀로그']");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(logo).isVisible();
        System.out.println("   ✓ 비밀로그 로고 표시됨");

        // 애니메이션 요소 확인 (장식용 원)
        boolean hasAnimatedElements = page.locator(".animate-bounce").count() > 0;
        if (hasAnimatedElements) {
            System.out.println("   ✓ 애니메이션 장식 요소 표시됨");
        }

        // 4. 그라데이션 배경 확인
        System.out.println("\n4. 디자인 스타일 확인");
        boolean hasGradientBackground = page.evaluate("() => {" +
            "const element = document.querySelector('.bg-gradient-to-br');" +
            "return element !== null;" +
        "}").toString().equals("true");
        
        if (hasGradientBackground) {
            System.out.println("   ✓ 그라데이션 배경 적용됨");
        }

        // 5. 메타데이터 확인
        System.out.println("\n5. SEO 메타데이터 확인");
        String title = page.title();
        assertThat(title).contains("404");
        System.out.println("   ✓ 페이지 타이틀에 404 포함: " + title);

        // robots meta 태그 확인 (noindex)
        String robotsContent = page.evaluate("() => {" +
            "const meta = document.querySelector('meta[name=\"robots\"]');" +
            "return meta ? meta.content : null;" +
        "}").toString();
        
        if (robotsContent != null && robotsContent.contains("noindex")) {
            System.out.println("   ✓ robots noindex 설정 확인됨");
        }
    }

    @Test
    @Order(2)
    @DisplayName("02. 404 페이지 네비게이션 버튼 및 BackButton 컴포넌트")
    void test404NavigationButtons() {
        System.out.println("=== 404 페이지 네비게이션 테스트 ===\n");

        // 존재하지 않는 페이지로 이동
        navigateToFrontend("/non-existent-page-12345");
        waitForElement("text=404", 5000);

        // 1. 홈으로 돌아가기 버튼 테스트
        System.out.println("1. 홈으로 돌아가기 버튼 테스트");
        Locator homeButton = page.locator("text=홈으로 돌아가기");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(homeButton).isVisible();
        
        safeClick(homeButton);
        page.waitForURL(FRONTEND_URL + "/", new Page.WaitForURLOptions().setTimeout(5000));
        assertThat(page.url()).isEqualTo(FRONTEND_URL + "/");
        System.out.println("   ✓ 홈으로 이동 성공");

        // 2. 게시판 버튼 테스트
        System.out.println("\n2. 게시판 버튼 테스트");
        navigateToFrontend("/another-non-existent");
        waitForElement("text=404", 5000);
        
        Locator boardButton = page.locator("text=게시판");
        if (boardButton.count() > 0) {
            safeClick(boardButton.first());
            page.waitForURL("**/board**", new Page.WaitForURLOptions().setTimeout(5000));
            assertThat(page.url()).contains("/board");
            System.out.println("   ✓ 게시판으로 이동 성공");
        }

        // 3. 롤링페이퍼 버튼 테스트
        System.out.println("\n3. 롤링페이퍼 버튼 테스트");
        navigateToFrontend("/yet-another-404");
        waitForElement("text=404", 5000);
        
        Locator paperButton = page.locator("text=롤링페이퍼");
        if (paperButton.count() > 0) {
            safeClick(paperButton.first());
            page.waitForURL("**/visit**", new Page.WaitForURLOptions().setTimeout(5000));
            assertThat(page.url()).contains("/visit");
            System.out.println("   ✓ 롤링페이퍼 방문 페이지로 이동 성공");
        }

        // 4. BackButton 컴포넌트 테스트 (이전 페이지로)
        System.out.println("\n4. BackButton 컴포넌트 테스트");
        navigateToFrontend("/");
        page.waitForTimeout(1000);
        navigateToFrontend("/404-test-page");
        waitForElement("text=404", 5000);
        
        // BackButton은 브라우저 history.back()을 사용
        Locator backButton = page.locator("text=이전 페이지로");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(backButton).isVisible();
        
        // ArrowLeft 아이콘 확인
        Locator arrowIcon = page.locator("button:has-text('이전 페이지로') svg");
        if (arrowIcon.count() > 0) {
            System.out.println("   ✓ ArrowLeft 아이콘 표시됨");
        }
        
        safeClick(backButton.first());
        page.waitForTimeout(1000);
        // 브라우저 히스토리를 통해 이전 페이지로 돌아가는지 확인
        assertThat(page.url()).isEqualTo(FRONTEND_URL + "/");
        System.out.println("   ✓ BackButton으로 이전 페이지 복귀 성공");

        // 5. 하단 링크 테스트
        System.out.println("\n5. 하단 링크 테스트");
        navigateToFrontend("/404-bottom-links-test");
        waitForElement("text=404", 5000);
        
        // 건의하기 링크
        if (isElementVisible("text=건의하기")) {
            Locator suggestLink = page.locator("text=건의하기");
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(suggestLink).isVisible();
            System.out.println("   ✓ 건의하기 링크 표시됨");
        }
        
        // 도움말 링크
        if (isElementVisible("text=도움말")) {
            Locator helpLink = page.locator("text=도움말");
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(helpLink).isVisible();
            System.out.println("   ✓ 도움말 링크 표시됨");
        }
    }

    @Test
    @Order(3)
    @DisplayName("03. 인증되지 않은 접근 - Protected 라우트")
    void testProtectedRouteAccess() {
        System.out.println("=== Protected 라우트 접근 테스트 ===\n");

        // 1. 비로그인 상태에서 마이페이지 접근
        System.out.println("1. 비로그인 상태에서 마이페이지 접근");
        navigateToFrontend("/mypage");
        
        // 로그인 페이지로 리다이렉트 확인 (redirect 파라미터 없이)
        page.waitForURL("**/login", new Page.WaitForURLOptions().setTimeout(5000));
        assertThat(page.url()).isEqualTo(FRONTEND_URL + "/login");
        System.out.println("   ✓ 로그인 페이지로 리다이렉트됨 (redirect 파라미터 없음)");

        // 2. 비로그인 상태에서 설정 페이지 접근
        System.out.println("\n2. 비로그인 상태에서 설정 페이지 접근");
        navigateToFrontend("/settings");
        
        page.waitForURL("**/login**", new Page.WaitForURLOptions().setTimeout(5000));
        assertThat(page.url()).contains("/login");
        System.out.println("   ✓ 로그인 페이지로 리다이렉트됨");

        // 3. 비로그인 상태에서 관리자 페이지 접근
        System.out.println("\n3. 비로그인 상태에서 관리자 페이지 접근");
        navigateToFrontend("/admin");
        
        page.waitForURL("**/login**", new Page.WaitForURLOptions().setTimeout(5000));
        assertThat(page.url()).contains("/login");
        System.out.println("   ✓ 로그인 페이지로 리다이렉트됨");

        // 4. 비로그인 상태에서 글쓰기 페이지 접근
        System.out.println("\n4. 비로그인 상태에서 글쓰기 페이지 접근");
        navigateToFrontend("/board/write");
        
        // 로그인 유도 모달 또는 리다이렉트 확인
        page.waitForTimeout(2000);
        boolean hasLoginPrompt = page.url().contains("/login") || 
                                isElementVisible("text=로그인이 필요합니다") ||
                                isElementVisible("text=로그인 후 이용");
        
        assertThat(hasLoginPrompt).isTrue();
        System.out.println("   ✓ 로그인 유도 확인됨");

        // 5. 직접 API 호출 시 401 응답 확인 (인증 없음)
        System.out.println("\n5. 보호된 API 직접 호출 테스트");
        
        // API 응답 리스너 설정
        Response[] apiResponse = new Response[1];
        page.onResponse(response -> {
            if (response.url().contains("/api/user/setting")) {
                apiResponse[0] = response;
            }
        });
        
        // JavaScript로 직접 API 호출
        page.evaluate("() => {" +
            "fetch('/api/user/setting', {" +
                "method: 'GET'," +
                "headers: { 'Content-Type': 'application/json' }" +
            "}).catch(() => {});" +
        "}");
        
        page.waitForTimeout(2000);
        
        if (apiResponse[0] != null) {
            int status = apiResponse[0].status();
            assertThat(status).isEqualTo(401); // 인증되지 않음
            System.out.println("   ✓ API 응답 상태 코드: 401 (Unauthorized)");
        }
    }

    @Test
    @Order(4)
    @DisplayName("04. 관리자 권한 검증 - 일반 사용자 접근 차단")
    void testAdminPageAccessControl() {
        System.out.println("=== 관리자 페이지 권한 테스트 ===\n");

        // 1. 일반 사용자로 로그인
        System.out.println("1. 일반 사용자로 로그인");
        loginAsRegularUser();
        page.waitForTimeout(2000);

        // 2. 관리자 페이지 접근 시도
        System.out.println("\n2. 일반 사용자가 관리자 페이지 접근 시도");
        navigateToFrontend("/admin");
        
        // AdminClient의 동적 로딩 및 useAdminAuth 훅 처리 대기
        System.out.println("   - AdminClient 동적 컴포넌트 로딩 대기...");
        page.waitForTimeout(4000); // dynamic import 로딩 시간 고려

        // 홈으로 리다이렉트 확인 (AdminClient.tsx의 router.push("/"))
        page.waitForURL(FRONTEND_URL + "/", new Page.WaitForURLOptions().setTimeout(5000));
        assertThat(page.url()).isEqualTo(FRONTEND_URL + "/");
        System.out.println("   ✓ 관리자 권한 없음 → 홈으로 리다이렉트됨");
        System.out.println("   현재 URL: " + page.url());

        // 3. 타인의 게시글 수정 페이지 접근
        System.out.println("\n3. 타인의 게시글 수정 페이지 접근 시도");
        navigateToFrontend("/board/post/1/edit"); // 타인의 게시글 ID
        page.waitForTimeout(2000);

        // 접근 거부 확인
        boolean isEditDenied = !page.url().contains("/edit") ||
                              isElementVisible("text=권한이 없습니다") ||
                              isElementVisible("text=작성자만 수정");
        
        if (isEditDenied) {
            System.out.println("   ✓ 타인의 게시글 수정 접근 차단됨");
        }

        // 4. 로그아웃
        logout();
    }

    @Test
    @Order(5)
    @DisplayName("05. 500 Server Error 페이지 UI")
    void test500ServerError() {
        System.out.println("=== 500 Server Error 페이지 테스트 ===\n");

        // 서버 오류를 시뮬레이션하기 위한 방법
        System.out.println("1. 서버 오류 시뮬레이션 설정");
        
        // 모든 API 호출에 대해 500 오류 반환
        page.route("**/api/**", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setStatus(500)
                .setBody("{\"error\": \"Internal Server Error\", \"digest\": \"ERROR-TEST-12345\"}")
                .setContentType("application/json"));
        });

        // API 호출이 필요한 페이지로 이동 (예: 게시판)
        // Next.js의 error.tsx는 컴포넌트 오류 시 표시됨
        
        // 2. API 호출이 필요한 페이지 접근 (예: 게시판 목록)
        System.out.println("\n2. API 호출이 있는 페이지로 이동");
        
        navigateToFrontend("/board"); // 게시판 목록 API 호출
        page.waitForTimeout(3000); // 에러 처리 대기

        // 3. 오류 처리 확인 - 토스트 메시지 또는 에러 UI
        System.out.println("\n3. 오류 표시 확인");
        
        // 에러 토스트, 에러 메시지, 또는 에러 페이지 확인
        boolean hasErrorIndication = 
            isElementVisible("text=오류") ||
            isElementVisible("text=실패") ||
            isElementVisible("text=문제가 발생") ||
            isElementVisible("text=다시 시도") ||
            isElementVisible("[role='alert']") ||
            isElementVisible(".animate-spin"); // 로딩 스피너
        
        if (hasErrorIndication) {
            System.out.println("   ✓ 오류 상태 표시됨");
        }
        
        // error.tsx 페이지가 표시되는 경우
        if (isElementVisible("text=500") || 
            isElementVisible("text=일시적인 오류가 발생했어요") ||
            isElementVisible("text=예상치 못한 문제")) {
            
            System.out.println("   ✓ 500 에러 페이지 표시됨");

            // 500 숫자 표시 확인
            if (isElementVisible("text=500")) {
                com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(
                    page.locator("text=500")
                ).isVisible();
                System.out.println("   ✓ 500 숫자 표시됨");
            }

            // 오류 아이콘 확인 (AlertTriangle)
            if (page.locator("svg").count() > 0) {
                System.out.println("   ✓ 오류 아이콘 표시됨");
            }

            // 오류 메시지 확인
            if (isElementVisible("text=일시적인 오류가 발생했어요")) {
                System.out.println("   ✓ 오류 제목 표시됨");
            }

            if (isElementVisible("text=예상치 못한 문제가 발생했어요")) {
                System.out.println("   ✓ 오류 설명 표시됨");
            }

            // 오류 코드 (digest) 확인
            if (isElementVisible("text=오류 코드") || isElementVisible("text=ERROR")) {
                System.out.println("   ✓ 오류 코드(digest) 표시됨");
            }
            
            // 다시 시도 버튼 확인
            if (isElementVisible("text=다시 시도하기")) {
                System.out.println("   ✓ '다시 시도하기' 버튼 표시됨");
            }
        }

        // 4. 그라데이션 배경 확인
        System.out.println("\n4. 디자인 요소 확인");
        
        // API 라우트 초기화 (정상 동작 테스트를 위해)
        page.unroute("**/api/**");
        boolean hasGradientBackground = page.evaluate("() => {" +
            "const element = document.querySelector('.bg-gradient-to-br');" +
            "return element !== null;" +
        "}").toString().equals("true");
        
        if (hasGradientBackground) {
            System.out.println("   ✓ 그라데이션 배경 적용됨");
        }
    }

    @Test
    @Order(6)
    @DisplayName("06. 500 에러 복구 액션")
    void test500ErrorRecovery() {
        System.out.println("=== 500 에러 복구 액션 테스트 ===\n");

        // 네트워크 오프라인 설정으로 오류 유발
        System.out.println("1. 네트워크 오류 시뮬레이션");
        page.context().setOffline(true);
        
        // API 호출이 있는 페이지로 이동
        navigateToFrontend("/board");
        page.waitForTimeout(3000);

        // 오류 상태 확인
        boolean hasError = isElementVisible("text=오류") || 
                          isElementVisible("text=실패") ||
                          isElementVisible("text=네트워크") ||
                          isElementVisible("text=다시 시도");
        
        if (hasError) {
            System.out.println("   ✓ 오류 상태 감지됨");

            // 2. 다시 시도하기 버튼 테스트
            System.out.println("\n2. 다시 시도하기 버튼 테스트");
            
            // 네트워크 복구
            page.context().setOffline(false);
            
            if (isElementVisible("text=다시 시도")) {
                Locator retryButton = page.locator("text=다시 시도").first();
                safeClick(retryButton);
                page.waitForTimeout(2000);
                System.out.println("   ✓ 다시 시도 버튼 클릭됨");
            }
        }

        // 3. 홈으로 돌아가기 테스트
        System.out.println("\n3. 홈으로 돌아가기 테스트");
        
        // 네트워크 오프라인으로 다시 오류 유발
        page.context().setOffline(true);
        page.reload();
        page.waitForTimeout(2000);
        
        if (isElementVisible("text=홈으로")) {
            Locator homeButton = page.locator("text=홈으로").first();
            
            // 네트워크 복구 후 클릭
            page.context().setOffline(false);
            safeClick(homeButton);
            page.waitForTimeout(2000);
            
            if (page.url().equals(FRONTEND_URL + "/")) {
                System.out.println("   ✓ 홈으로 이동 성공");
            }
        }

        // 4. 새로고침 테스트
        System.out.println("\n4. 페이지 새로고침 복구 테스트");
        
        // 정상 상태에서 새로고침
        page.reload();
        page.waitForLoadState();
        System.out.println("   ✓ 페이지 새로고침 완료");

        // 네트워크 상태 확인
        boolean isOnline = page.evaluate("() => navigator.onLine").toString().equals("true");
        if (isOnline) {
            System.out.println("   ✓ 네트워크 연결 상태 정상");
        }
    }

    @Test
    @Order(7)
    @DisplayName("07. 에러 페이지 반응형 디자인")
    void testErrorPageResponsive() {
        System.out.println("=== 에러 페이지 반응형 테스트 ===\n");

        // 404 페이지로 이동
        navigateToFrontend("/responsive-test-404");
        waitForElement("text=404", 5000);

        // 1. 데스크톱 뷰
        System.out.println("1. 데스크톱 뷰 테스트");
        setDesktopViewport();
        page.waitForTimeout(1000);
        
        // 레이아웃 확인
        boolean hasDesktopLayout = page.evaluate("() => {" +
            "const container = document.querySelector('.max-w-md');" +
            "return container !== null;" +
        "}").toString().equals("true");
        
        if (hasDesktopLayout) {
            System.out.println("   ✓ 데스크톱 레이아웃 적용됨");
        }

        // 버튼 크기 확인
        Locator homeButton = page.locator("text=홈으로 돌아가기");
        if (homeButton.count() > 0) {
            Object boundingBox = homeButton.first().boundingBox();
            System.out.println("   ✓ 홈 버튼 표시됨");
        }

        // 2. 태블릿 뷰
        System.out.println("\n2. 태블릿 뷰 테스트");
        setTabletViewport();
        page.waitForTimeout(1000);
        
        // 컨텐츠가 중앙 정렬되는지 확인
        boolean hasCenteredContent = page.evaluate("() => {" +
            "const element = document.querySelector('.text-center');" +
            "return element !== null;" +
        "}").toString().equals("true");
        
        if (hasCenteredContent) {
            System.out.println("   ✓ 태블릿에서 컨텐츠 중앙 정렬됨");
        }

        // 3. 모바일 뷰
        System.out.println("\n3. 모바일 뷰 테스트");
        setMobileViewport();
        page.waitForTimeout(1000);
        
        // 모바일에서 버튼이 전체 너비로 표시되는지 확인
        boolean hasFullWidthButtons = page.evaluate("() => {" +
            "const buttons = document.querySelectorAll('.w-full');" +
            "return buttons.length > 0;" +
        "}").toString().equals("true");
        
        if (hasFullWidthButtons) {
            System.out.println("   ✓ 모바일에서 버튼이 전체 너비로 표시됨");
        }

        // 4. 터치 타겟 크기 확인 (44px 이상)
        System.out.println("\n4. 터치 타겟 크기 확인");
        
        boolean hasProperTouchTargets = page.evaluate("() => {" +
            "const buttons = document.querySelectorAll('button');" +
            "for (let button of buttons) {" +
                "const rect = button.getBoundingClientRect();" +
                "if (rect.width > 0 && rect.height > 0 && (rect.width < 44 || rect.height < 44)) {" +
                    "return false;" +
                "}" +
            "}" +
            "return true;" +
        "}").toString().equals("true");
        
        if (hasProperTouchTargets) {
            System.out.println("   ✓ 모바일 터치 타겟 크기 44px 이상");
        }

        // 5. 텍스트 가독성 확인
        System.out.println("\n5. 모바일 텍스트 가독성 확인");
        
        boolean hasReadableText = page.evaluate("() => {" +
            "const texts = document.querySelectorAll('p, h1, h2');" +
            "for (let text of texts) {" +
                "const style = window.getComputedStyle(text);" +
                "const fontSize = parseFloat(style.fontSize);" +
                "if (fontSize < 14) return false;" +
            "}" +
            "return true;" +
        "}").toString().equals("true");
        
        if (hasReadableText) {
            System.out.println("   ✓ 모바일에서 텍스트 크기 14px 이상");
        }

        // 데스크톱 뷰로 복원
        setDesktopViewport();
    }

    @Test
    @Order(8)
    @DisplayName("08. 에러 페이지 에지 케이스")
    void testErrorPageEdgeCases() {
        System.out.println("=== 에러 페이지 에지 케이스 테스트 ===\n");

        // 1. 한글 URL 404 테스트
        System.out.println("1. 한글 URL 404 테스트");
        navigateToFrontend("/한글페이지/테스트");
        page.waitForTimeout(2000);
        
        if (isElementVisible("text=404") || isElementVisible("text=페이지를 찾을 수 없")) {
            System.out.println("   ✓ 한글 URL 404 처리 정상");
        }

        // 2. 특수문자 URL 404 테스트
        System.out.println("\n2. 특수문자 URL 404 테스트");
        navigateToFrontend("/special!@#$%^&*()");
        page.waitForTimeout(2000);
        
        if (isElementVisible("text=404") || isElementVisible("text=페이지를 찾을 수 없")) {
            System.out.println("   ✓ 특수문자 URL 404 처리 정상");
        }

        // 3. 매우 긴 URL 404 테스트
        System.out.println("\n3. 매우 긴 URL 404 테스트");
        String longPath = "/very" + "long".repeat(100) + "path";
        navigateToFrontend(longPath);
        page.waitForTimeout(2000);
        
        if (isElementVisible("text=404") || isElementVisible("text=페이지를 찾을 수 없")) {
            System.out.println("   ✓ 긴 URL 404 처리 정상");
        }

        // 4. 쿼리 파라미터가 있는 404 테스트
        System.out.println("\n4. 쿼리 파라미터 404 테스트");
        navigateToFrontend("/invalid-page?param1=value1&param2=value2");
        page.waitForTimeout(2000);
        
        if (isElementVisible("text=404") || isElementVisible("text=페이지를 찾을 수 없")) {
            System.out.println("   ✓ 쿼리 파라미터 404 처리 정상");
        }

        // 5. 해시 프래그먼트가 있는 404 테스트
        System.out.println("\n5. 해시 프래그먼트 404 테스트");
        navigateToFrontend("/another-invalid#section");
        page.waitForTimeout(2000);
        
        if (isElementVisible("text=404") || isElementVisible("text=페이지를 찾을 수 없")) {
            System.out.println("   ✓ 해시 프래그먼트 404 처리 정상");
        }

        // 6. 연속적인 에러 페이지 이동
        System.out.println("\n6. 연속적인 에러 페이지 이동 테스트");
        
        for (int i = 1; i <= 3; i++) {
            navigateToFrontend("/error-" + i);
            page.waitForTimeout(1000);
            
            if (isElementVisible("text=404")) {
                System.out.println("   ✓ 에러 페이지 " + i + " 표시됨");
            }
        }

        // 7. 삭제된 콘텐츠 접근 시뮬레이션
        System.out.println("\n7. 삭제된 콘텐츠 접근 테스트");
        
        // 존재하지 않는 게시글 ID
        navigateToFrontend("/board/post/999999999");
        page.waitForTimeout(2000);
        
        boolean hasError = isElementVisible("text=404") || 
                          isElementVisible("text=게시글을 찾을 수 없") ||
                          isElementVisible("text=삭제된 게시글");
        
        if (hasError) {
            System.out.println("   ✓ 삭제된 게시글 에러 처리됨");
        }

        // 존재하지 않는 사용자 롤링페이퍼
        navigateToFrontend("/rolling-paper/nonexistentuser123456");
        page.waitForTimeout(2000);
        
        hasError = isElementVisible("text=404") || 
                  isElementVisible("text=사용자를 찾을 수 없") ||
                  isElementVisible("text=존재하지 않는 사용자");
        
        if (hasError) {
            System.out.println("   ✓ 존재하지 않는 사용자 에러 처리됨");
        }
    }

    @Test
    @Order(9)
    @DisplayName("09. 브라우저 네비게이션 동작")
    void testBrowserNavigation() {
        System.out.println("=== 브라우저 네비게이션 테스트 ===\n");

        // 1. 정상 페이지 → 404 → 뒤로가기
        System.out.println("1. 정상 페이지 → 404 → 뒤로가기");
        
        navigateToFrontend("/");
        page.waitForLoadState();
        String homeUrl = page.url();
        
        navigateToFrontend("/browser-nav-404");
        waitForElement("text=404", 5000);
        
        page.goBack();
        page.waitForLoadState();
        
        assertThat(page.url()).isEqualTo(homeUrl);
        System.out.println("   ✓ 브라우저 뒤로가기로 홈 복귀 성공");

        // 2. 404 → 정상 페이지 → 앞으로가기
        System.out.println("\n2. 404 → 정상 페이지 → 앞으로가기");
        
        page.goForward();
        page.waitForLoadState();
        
        if (isElementVisible("text=404")) {
            System.out.println("   ✓ 브라우저 앞으로가기로 404 페이지 복귀");
        }

        // 3. 404 페이지에서 새로고침
        System.out.println("\n3. 404 페이지에서 새로고침");
        
        navigateToFrontend("/refresh-test-404");
        waitForElement("text=404", 5000);
        
        page.reload();
        page.waitForLoadState();
        
        if (isElementVisible("text=404")) {
            System.out.println("   ✓ 404 페이지 새로고침 후에도 유지됨");
        }

        // 4. 히스토리 스택 테스트
        System.out.println("\n4. 히스토리 스택 테스트");
        
        // 여러 페이지 이동
        navigateToFrontend("/");
        page.waitForTimeout(500);
        navigateToFrontend("/board");
        page.waitForTimeout(500);
        navigateToFrontend("/404-history-test");
        page.waitForTimeout(500);
        
        // 2단계 뒤로가기
        page.goBack();
        page.waitForTimeout(500);
        page.goBack();
        page.waitForTimeout(500);
        
        assertThat(page.url()).isEqualTo(FRONTEND_URL + "/");
        System.out.println("   ✓ 다중 히스토리 네비게이션 정상 동작");

        // 5. 이전 페이지로 버튼 vs 브라우저 뒤로가기
        System.out.println("\n5. 이전 페이지로 버튼 동작 확인");
        
        navigateToFrontend("/board");
        page.waitForTimeout(1000);
        navigateToFrontend("/back-button-test-404");
        waitForElement("text=404", 5000);
        
        // BackButton 컴포넌트 클릭
        if (isElementVisible("text=이전 페이지로")) {
            Locator backButton = page.locator("text=이전 페이지로").first();
            safeClick(backButton);
            page.waitForTimeout(1000);
            
            if (page.url().contains("/board")) {
                System.out.println("   ✓ 이전 페이지로 버튼 정상 동작");
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. 에러 로깅 및 추적")
    void testErrorLogging() {
        System.out.println("=== 에러 로깅 테스트 ===\n");

        // 콘솔 메시지 수집을 위한 리스트
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        
        // 콘솔 에러 리스너 설정
        page.onConsoleMessage(msg -> {
            if (msg.type().equals("error")) {
                consoleErrors.add(msg.text());
            }
        });

        // 1. 404 에러 로깅
        System.out.println("1. 404 에러 로깅 확인");
        
        navigateToFrontend("/logging-test-404");
        page.waitForTimeout(2000);
        
        // 404 페이지가 로드되었는지 확인
        if (isElementVisible("text=404")) {
            System.out.println("   ✓ 404 페이지 로드됨");
            
            // 콘솔 에러가 있는지 확인
            if (!consoleErrors.isEmpty()) {
                System.out.println("   ✓ 콘솔 에러 감지: " + consoleErrors.size() + "개");
                for (String error : consoleErrors) {
                    System.out.println("     - " + error.substring(0, Math.min(error.length(), 100)));
                }
            }
        }

        // 2. 에러 코드(digest) 확인
        System.out.println("\n2. 에러 코드 표시 확인");
        
        // 500 에러를 시뮬레이션
        page.route("**/api/trigger-error", route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setStatus(500)
                .setBody("{\"error\": \"Test Error\", \"digest\": \"ERROR-12345\"}")
                .setContentType("application/json"));
        });
        
        // JavaScript로 에러 트리거
        page.evaluate("() => {" +
            "fetch('/api/trigger-error')" +
                ".then(res => { if (!res.ok) throw new Error('Server Error'); })" +
                ".catch(err => console.error('Error occurred:', err));" +
        "}");
        
        page.waitForTimeout(2000);
        
        // 에러 코드가 표시되는지 확인
        if (isElementVisible("text=오류 코드") || isElementVisible("text=ERROR")) {
            System.out.println("   ✓ 에러 코드 표시 확인됨");
        }

        // 3. 네트워크 에러 추적
        System.out.println("\n3. 네트워크 에러 추적");
        
        // 네트워크 응답 추적
        java.util.List<Integer> errorStatuses = new java.util.ArrayList<>();
        
        page.onResponse(response -> {
            if (response.status() >= 400) {
                errorStatuses.add(response.status());
                System.out.println("   ✓ HTTP 에러 감지: " + response.status() + " - " + response.url());
            }
        });
        
        // 404 API 호출
        page.evaluate("() => {" +
            "fetch('/api/non-existent-endpoint').catch(() => {});" +
        "}");
        
        page.waitForTimeout(2000);
        
        if (!errorStatuses.isEmpty()) {
            System.out.println("   ✓ 총 " + errorStatuses.size() + "개의 HTTP 에러 추적됨");
        }

        // 4. 에러 복구 시도 로깅
        System.out.println("\n4. 에러 복구 시도 로깅");
        
        // 에러 복구 시도 추적
        page.evaluate("() => {" +
            "console.log('Error recovery attempted');" +
            "window.location.reload();" +
        "}");
        
        page.waitForTimeout(1000);
        System.out.println("   ✓ 에러 복구 시도 로깅됨");

        // 5. 에러 페이지 메타데이터 확인
        System.out.println("\n5. 에러 페이지 메타데이터 확인");
        
        navigateToFrontend("/metadata-test-404");
        page.waitForTimeout(2000);
        
        // 페이지 타이틀 확인
        String title = page.title();
        if (title.contains("404") || title.contains("찾을 수 없")) {
            System.out.println("   ✓ 페이지 타이틀에 에러 정보 포함: " + title);
        }
        
        // robots 메타 태그 확인
        String robotsContent = page.evaluate("() => {" +
            "const meta = document.querySelector('meta[name=\"robots\"]');" +
            "return meta ? meta.content : null;" +
        "}").toString();
        
        if (robotsContent != null) {
            System.out.println("   ✓ robots 메타 태그: " + robotsContent);
        }
        
        // Open Graph 메타 태그 확인
        String ogTitle = page.evaluate("() => {" +
            "const meta = document.querySelector('meta[property=\"og:title\"]');" +
            "return meta ? meta.content : null;" +
        "}").toString();
        
        if (ogTitle != null) {
            System.out.println("   ✓ Open Graph 타이틀: " + ogTitle);
        }
    }

    @Test
    @Order(11)
    @DisplayName("11. 세션 만료 및 재인증")
    void testSessionExpirationAndReauth() {
        System.out.println("=== 세션 만료 및 재인증 테스트 ===\n");

        // 1. 로그인
        System.out.println("1. 사용자 로그인");
        loginAsRegularUser();
        page.waitForTimeout(2000);

        // 2. 마이페이지 접근 확인
        System.out.println("\n2. 마이페이지 정상 접근 확인");
        navigateToFrontend("/mypage");
        page.waitForTimeout(2000);
        
        if (!page.url().contains("/login")) {
            System.out.println("   ✓ 로그인 상태에서 마이페이지 접근 성공");
        }

        // 3. 세션 만료 시뮬레이션 (쿠키 삭제)
        System.out.println("\n3. 세션 만료 시뮬레이션");
        page.context().clearCookies();
        System.out.println("   ✓ 쿠키 삭제됨");

        // 4. 보호된 페이지 재접근
        System.out.println("\n4. 세션 만료 후 보호된 페이지 접근");
        navigateToFrontend("/mypage");
        page.waitForTimeout(2000);
        
        // 로그인 페이지로 리다이렉트 확인
        if (page.url().contains("/login")) {
            System.out.println("   ✓ 세션 만료 후 로그인 페이지로 리다이렉트됨");
        }

        // 5. 로그인 페이지 상태 확인
        System.out.println("\n5. 로그인 페이지 상태 확인");
        
        // Next.js의 (protected) 레이아웃은 redirect 파라미터를 사용하지 않음
        String currentUrl = page.url();
        assertThat(currentUrl).isEqualTo(FRONTEND_URL + "/login");
        System.out.println("   ✓ 단순 로그인 페이지 리다이렉트 (redirect 파라미터 없음)");
    }

    @Test
    @Order(12)
    @DisplayName("12. PWA 오프라인 에러 처리")
    void testPWAOfflineError() {
        System.out.println("=== PWA 오프라인 에러 처리 테스트 ===\n");

        // 1. 정상 상태에서 페이지 로드
        System.out.println("1. 온라인 상태에서 페이지 로드");
        navigateToFrontend("/");
        page.waitForLoadState();
        System.out.println("   ✓ 홈 페이지 로드 성공");

        // 2. 오프라인 전환
        System.out.println("\n2. 오프라인 모드 전환");
        page.context().setOffline(true);
        
        // navigator.onLine 확인
        boolean isOffline = page.evaluate("() => !navigator.onLine").toString().equals("true");
        if (isOffline) {
            System.out.println("   ✓ 브라우저 오프라인 상태 확인됨");
        }

        // 3. 오프라인 상태에서 새 페이지 접근
        System.out.println("\n3. 오프라인 상태에서 페이지 이동");
        navigateToFrontend("/board");
        page.waitForTimeout(3000);
        
        // 오프라인 메시지 또는 캐시된 페이지 확인
        boolean hasOfflineHandling = 
            isElementVisible("text=오프라인") ||
            isElementVisible("text=인터넷 연결") ||
            isElementVisible("text=네트워크") ||
            page.url().contains("/board"); // 캐시된 페이지
        
        if (hasOfflineHandling) {
            System.out.println("   ✓ 오프라인 처리 확인됨");
        }

        // 4. 온라인 복구
        System.out.println("\n4. 온라인 복구");
        page.context().setOffline(false);
        page.reload();
        page.waitForLoadState();
        
        boolean isOnline = page.evaluate("() => navigator.onLine").toString().equals("true");
        if (isOnline) {
            System.out.println("   ✓ 온라인 상태 복구됨");
        }

        // 5. Service Worker 확인
        System.out.println("\n5. Service Worker 상태 확인");
        
        boolean hasServiceWorker = page.evaluate("() => {" +
            "return 'serviceWorker' in navigator && " +
                   "navigator.serviceWorker.controller !== null;" +
        "}").toString().equals("true");
        
        if (hasServiceWorker) {
            System.out.println("   ✓ Service Worker 활성화됨");
        } else {
            System.out.println("   - Service Worker 미등록 또는 비활성");
        }
    }


    // ===== 헬퍼 메서드 =====

    /**
     * 일반 사용자로 로그인
     */
    private void loginAsRegularUser() {
        navigateToFrontend("/login");
        
        // 카카오 로그인 또는 테스트 로그인
        if (isElementVisible("button:has-text('카카오 로그인')")) {
            safeClick("button:has-text('카카오 로그인')");
        } else if (isElementVisible("input[type='email'], input[name='username']")) {
            safeFill("input[type='email'], input[name='username']", TEST_USERNAME);
            safeFill("input[type='password']", TEST_PASSWORD);
            safeClick("button[type='submit'], button:has-text('로그인')");
        }
        
        page.waitForTimeout(2000);
    }

    /**
     * 관리자로 로그인
     */
    private void loginAsAdmin() {
        navigateToFrontend("/login");
        
        if (isElementVisible("button:has-text('카카오 로그인')")) {
            safeClick("button:has-text('카카오 로그인')");
        } else if (isElementVisible("input[type='email'], input[name='username']")) {
            safeFill("input[type='email'], input[name='username']", ADMIN_USERNAME);
            safeFill("input[type='password']", ADMIN_PASSWORD);
            safeClick("button[type='submit'], button:has-text('로그인')");
        }
        
        page.waitForTimeout(2000);
    }

    /**
     * 네트워크 오류 시뮬레이션
     */
    private void simulateNetworkError() {
        page.context().setOffline(true);
    }

    /**
     * 네트워크 복구
     */
    private void restoreNetwork() {
        page.context().setOffline(false);
    }

    /**
     * API 응답 모킹
     */
    private void mockApiResponse(String path, int status, String body) {
        page.route("**" + path, route -> {
            route.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setStatus(status)
                .setBody(body)
                .setContentType("application/json"));
        });
    }

    /**
     * 에러 다이얼로그 확인
     */
    private boolean hasErrorDialog() {
        return isElementVisible("text=오류") ||
               isElementVisible("text=에러") ||
               isElementVisible("text=실패") ||
               isElementVisible("[role='alert']");
    }

    /**
     * 콘솔 에러 수집
     */
    private java.util.List<String> collectConsoleErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        page.onConsoleMessage(msg -> {
            if (msg.type().equals("error")) {
                errors.add(msg.text());
            }
        });
        
        return errors;
    }
}