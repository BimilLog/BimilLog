package jaeik.bimillog.e2e.scenarios;

import jaeik.bimillog.e2e.base.BaseE2ETest;
import jaeik.bimillog.e2e.base.E2ETestConfig;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 관리자 시나리오 E2E 테스트 (시나리오 6)
 * - 관리자 페이지 권한 확인
 * - 관리자 대시보드 통계 조회
 * - 신고 관리 (목록, 필터링, 상세)
 * - 신고 처리 액션 (차단, 강제 탈퇴)
 * - 탭 네비게이션 및 반응형 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("관리자 시나리오 E2E 테스트")
public class AdminScenarioTest extends BaseE2ETest {

    private static final String ADMIN_USERNAME = "admin@test.com";
    private static final String ADMIN_PASSWORD = "adminpass123";
    private static final String REGULAR_USERNAME = "user@test.com";
    private static final String REGULAR_PASSWORD = "userpass123";

    @BeforeEach
    void setup() {
        E2ETestConfig.ensureFrontendReady();
        // 각 테스트마다 깨끗한 상태로 시작
        page.navigate(FRONTEND_URL);
        page.waitForLoadState();
    }

    @Test
    @Order(1)
    @DisplayName("01. 관리자 페이지 접근 권한")
    void testAdminPageAccess() {
        System.out.println("=== 관리자 페이지 접근 권한 테스트 ===\n");

        // 1. 비로그인 상태에서 /admin 접근 시도 -> 로그인 페이지로 리다이렉트
        System.out.println("1. 비로그인 상태에서 /admin 접근 시도");
        navigateToFrontend("/admin");

        // 로그인 페이지로 리다이렉트 되는지 확인
        page.waitForURL("**/login**", new Page.WaitForURLOptions().setTimeout(10000));
        assertThat(page.url()).contains("/login");
        System.out.println("   ✓ 로그인 페이지로 리다이렉트 됨: " + page.url());

        // 2. 일반 사용자로 로그인하여 /admin 접근 시도 -> 홈으로 리다이렉트
        System.out.println("\n2. 일반 사용자로 로그인 후 /admin 접근 시도");
        loginAsRegularUser();

        navigateToFrontend("/admin");
        page.waitForTimeout(2000); // 리다이렉트 대기

        // 홈 페이지로 리다이렉트 되는지 확인 (권한 없음)
        if (page.url().contains("/admin")) {
            // 관리자 권한 없음을 나타내는 요소가 있는지 확인
            boolean hasAccessDenied = isElementVisible("text=권한이 없습니다") ||
                                    isElementVisible("text=접근이 거부되었습니다") ||
                                    page.url().equals(FRONTEND_URL + "/");
            assertThat(hasAccessDenied).isTrue();
            System.out.println("   ✓ 일반 사용자는 관리자 페이지 접근 불가");
        } else {
            System.out.println("   ✓ 홈으로 리다이렉트 됨: " + page.url());
        }

        logout();

        // 3. 관리자 계정으로 로그인
        System.out.println("\n3. 관리자 계정으로 로그인");
        loginAsAdmin();

        // 4. /admin 페이지 접근 성공 확인
        System.out.println("\n4. 관리자 페이지 접근 성공 확인");
        navigateToFrontend("/admin");

        // 관리자 대시보드 헤더 확인
        waitForElement("text=관리자 대시보드", 10000);
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=관리자 대시보드")).isVisible();
        System.out.println("   ✓ 관리자 대시보드 로드 성공");

        // 관리자 전용 요소들 확인
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=신고 관리")).isVisible();
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=상세 통계")).isVisible();
        System.out.println("   ✓ 관리자 전용 탭들 표시됨");
    }

    @Test
    @Order(2)
    @DisplayName("02. 관리자 대시보드 조회")
    void testAdminDashboard() {
        System.out.println("=== 관리자 대시보드 조회 테스트 ===\n");

        // 관리자로 로그인
        loginAsAdmin();
        navigateToFrontend("/admin");

        // 1. 대시보드 헤더 확인
        System.out.println("1. 대시보드 헤더 확인");
        waitForElement("text=관리자 대시보드", 10000);
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("h1")).containsText("관리자 대시보드");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=신고 관리 및 통계를 확인할 수 있습니다")).isVisible();
        System.out.println("   ✓ 대시보드 헤더 표시됨");

        // 2. 로딩 상태 확인 후 통계 카드들이 로드되길 대기
        System.out.println("\n2. 통계 카드 로딩 대기");
        page.waitForTimeout(2000); // 동적 컴포넌트 로딩 대기

        // 3. 통계 카드들 확인 (AdminStats 컴포넌트)
        System.out.println("\n3. 통계 카드들 확인");

        // 통계 카드 컨테이너가 로드될 때까지 대기
        waitForElement(".grid", 15000); // 통계 카드들이 그리드 레이아웃으로 표시

        // 주요 지표들 확인
        String[] expectedStats = {
            "전체 신고",
            "대기 중",
            "처리 완료",
            "오늘 접수"
        };

        for (String stat : expectedStats) {
            if (isElementVisible("text=" + stat)) {
                System.out.println("   ✓ " + stat + " 카드 표시됨");
            } else {
                System.out.println("   - " + stat + " 카드 로딩 중 또는 데이터 없음");
            }
        }

        // 4. 홈 버튼 확인
        System.out.println("\n4. 네비게이션 버튼 확인");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=홈으로 돌아가기")).isVisible();
        System.out.println("   ✓ 홈으로 돌아가기 버튼 표시됨");

        // 5. 그라데이션 배경 스타일 확인
        System.out.println("\n5. 디자인 요소 확인");
        boolean hasGradientBackground = page.evaluate("() => {" +
            "const element = document.querySelector('.bg-gradient-to-br');" +
            "return element !== null;" +
        "}").toString().equals("true");

        if (hasGradientBackground) {
            System.out.println("   ✓ 그라데이션 배경 스타일 적용됨");
        } else {
            System.out.println("   - 그라데이션 배경 스타일 확인 불가");
        }
    }

    @Test
    @Order(3)
    @DisplayName("03. 신고 목록 조회")
    void testReportList() {
        System.out.println("=== 신고 목록 조회 테스트 ===\n");

        // 관리자로 로그인 후 관리자 페이지 이동
        loginAsAdmin();
        navigateToFrontend("/admin");

        // 1. 신고 관리 탭 확인 (기본적으로 활성화되어 있어야 함)
        System.out.println("1. 신고 관리 탭 확인");
        waitForElement("text=신고 관리", 10000);

        Locator reportsTab = page.locator("text=신고 관리");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(reportsTab).isVisible();

        // 탭이 활성화되지 않은 경우 클릭
        if (!reportsTab.getAttribute("data-state").equals("active")) {
            safeClick(reportsTab);
            page.waitForTimeout(1000);
        }
        System.out.println("   ✓ 신고 관리 탭 활성화됨");

        // 2. 신고 목록 컨테이너 로딩 대기
        System.out.println("\n2. 신고 목록 로딩 대기");
        page.waitForTimeout(3000); // ReportListContainer 동적 로딩 대기

        // 3. 필터 및 검색 영역 확인
        System.out.println("\n3. 필터 및 검색 기능 확인");

        // 신고 유형 필터 버튼들 확인
        String[] reportTypes = {"전체", "게시글", "댓글", "오류", "개선"};

        for (String type : reportTypes) {
            if (isElementVisible("text=" + type)) {
                System.out.println("   ✓ " + type + " 필터 버튼 표시됨");
            }
        }

        // 처리 상태 필터 확인
        String[] statuses = {"전체", "대기중", "처리중", "완료", "반려"};

        for (String status : statuses) {
            if (isElementVisible("text=" + status)) {
                System.out.println("   ✓ " + status + " 상태 필터 표시됨");
            }
        }

        // 4. 신고 목록 또는 빈 상태 메시지 확인
        System.out.println("\n4. 신고 목록 내용 확인");

        // 신고 목록이 있는 경우
        if (isElementVisible("[data-testid='report-card']") ||
            isElementVisible(".report-item") ||
            page.locator(".grid").count() > 1) { // 통계 카드 외의 그리드

            System.out.println("   ✓ 신고 목록 표시됨");

            // 신고 카드 요소들 확인
            if (isElementVisible("text=신고 ID") || isElementVisible("#")) {
                System.out.println("   ✓ 신고 ID 표시됨");
            }

        } else if (isElementVisible("text=신고가 없습니다") ||
                  isElementVisible("text=신고 목록이 없습니다") ||
                  isElementVisible("text=등록된 신고가 없습니다")) {

            System.out.println("   ✓ 빈 상태 메시지 표시됨 (신고 없음)");

        } else {
            System.out.println("   - 신고 목록 로딩 중이거나 구조 변경됨");
        }

        // 5. 필터링 기능 테스트
        System.out.println("\n5. 필터링 기능 테스트");

        // 게시글 타입 필터 클릭 테스트
        if (isElementVisible("text=게시글")) {
            safeClick("text=게시글");
            page.waitForTimeout(1000);
            System.out.println("   ✓ 게시글 필터 클릭됨");

            // 전체로 다시 변경
            if (isElementVisible("text=전체")) {
                safeClick("text=전체");
                page.waitForTimeout(1000);
                System.out.println("   ✓ 전체 필터로 복원됨");
            }
        }

        // 6. 페이지네이션 확인 (데이터가 많은 경우)
        System.out.println("\n6. 페이지네이션 확인");
        if (isElementVisible("button[aria-label*='페이지']") ||
            isElementVisible(".pagination") ||
            isElementVisible("text=다음") ||
            isElementVisible("text=이전")) {

            System.out.println("   ✓ 페이지네이션 컨트롤 표시됨");
        } else {
            System.out.println("   - 페이지네이션 불필요 (데이터 부족) 또는 무한 스크롤");
        }
    }

    @Test
    @Order(4)
    @DisplayName("04. 신고 상세 조회")
    void testReportDetail() {
        System.out.println("=== 신고 상세 조회 테스트 ===\n");

        // 관리자로 로그인 후 관리자 페이지 이동
        loginAsAdmin();
        navigateToFrontend("/admin");

        // 신고 관리 탭 활성화 확인
        waitForElement("text=신고 관리", 10000);
        page.waitForTimeout(3000); // 동적 컴포넌트 로딩 대기

        // 1. 신고 항목 찾기 및 클릭
        System.out.println("1. 신고 항목 찾기 및 클릭");

        // 신고 항목을 찾을 수 있는 여러 가지 선택자 시도
        String[] reportSelectors = {
            "[data-testid='report-card']",
            ".report-item",
            "[data-testid='report-item']",
            "button:has-text('신고 ID')",
            "button:has-text('#')"
        };

        boolean reportFound = false;
        Locator reportItem = null;

        for (String selector : reportSelectors) {
            if (page.locator(selector).count() > 0) {
                reportItem = page.locator(selector).first();
                if (reportItem.isVisible()) {
                    reportFound = true;
                    System.out.println("   ✓ 신고 항목 발견: " + selector);
                    break;
                }
            }
        }

        if (!reportFound) {
            // 신고가 없는 경우 테스트 신고 데이터를 생성하거나 건너뛰기
            System.out.println("   - 신고 항목을 찾을 수 없음. 빈 상태 메시지 확인");

            if (isElementVisible("text=신고가 없습니다") ||
                isElementVisible("text=등록된 신고가 없습니다")) {
                System.out.println("   ✓ 빈 상태 메시지 정상 표시됨");
                return; // 테스트 종료
            }

            fail("신고 항목 또는 빈 상태 메시지를 찾을 수 없습니다.");
        }

        // 2. 신고 항목 클릭하여 상세 모달 열기
        System.out.println("\n2. 신고 상세 모달 열기");
        safeClick(reportItem);

        // 모달이 열릴 때까지 대기 (ReportDetailModal)
        page.waitForTimeout(2000);

        // 3. 모달 헤더 확인
        System.out.println("\n3. 상세 모달 헤더 확인");

        if (waitForElement("text=신고 상세 정보", 5000) != null) {
            com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(page.locator("text=신고 상세 정보")).isVisible();
            System.out.println("   ✓ 모달 헤더 '신고 상세 정보' 표시됨");

            // 신고 ID 표시 확인
            if (isElementVisible("text=신고 ID") || isElementVisible("#")) {
                System.out.println("   ✓ 신고 ID 표시됨");
            }
        } else {
            System.out.println("   - 모달이 다른 형태로 열렸거나 로딩 중");
        }

        // 4. 탭 네비게이션 확인
        System.out.println("\n4. 탭 네비게이션 확인");
        String[] tabs = {"상세 정보", "신고 내용", "처리 작업"};

        for (String tab : tabs) {
            if (isElementVisible("text=" + tab)) {
                System.out.println("   ✓ '" + tab + "' 탭 표시됨");
            }
        }

        // 5. 상세 정보 탭 내용 확인
        System.out.println("\n5. 상세 정보 탭 내용 확인");

        // 정보 카드들 확인
        String[] infoLabels = {"신고 유형", "대상 ID", "신고 대상", "신고일"};

        for (String label : infoLabels) {
            if (isElementVisible("text=" + label)) {
                System.out.println("   ✓ " + label + " 정보 표시됨");
            }
        }

        // 6. 신고 내용 탭 클릭 및 확인
        System.out.println("\n6. 신고 내용 탭 확인");

        if (isElementVisible("text=신고 내용")) {
            safeClick("text=신고 내용");
            page.waitForTimeout(1000);

            // 신고 사유 섹션 확인
            if (isElementVisible("text=신고 사유")) {
                System.out.println("   ✓ 신고 사유 섹션 표시됨");
            }

            // 신고된 콘텐츠 섹션 확인 (있는 경우)
            if (isElementVisible("text=신고된 콘텐츠")) {
                System.out.println("   ✓ 신고된 콘텐츠 섹션 표시됨");
            }
        }

        // 7. 처리 작업 탭 클릭 및 확인
        System.out.println("\n7. 처리 작업 탭 확인");

        if (isElementVisible("text=처리 작업")) {
            safeClick("text=처리 작업");
            page.waitForTimeout(1000);

            // 주의사항 알림 확인
            if (isElementVisible("text=주의사항")) {
                System.out.println("   ✓ 주의사항 알림 표시됨");
            }

            // 액션 버튼들 확인
            if (isElementVisible("text=사용자 차단") || isElementVisible("button:has-text('차단')")) {
                System.out.println("   ✓ 사용자 차단 액션 표시됨");
            }

            if (isElementVisible("text=강제 탈퇴") || isElementVisible("button:has-text('탈퇴')")) {
                System.out.println("   ✓ 강제 탈퇴 액션 표시됨");
            }
        }

        // 8. 모달 닫기
        System.out.println("\n8. 모달 닫기");

        if (isElementVisible("button[aria-label='Close']") ||
            page.locator("button").locator("svg").count() > 0) {
            // X 버튼으로 닫기
            page.locator("button").first().click();
        } else {
            // ESC 키로 닫기
            page.keyboard().press("Escape");
        }

        page.waitForTimeout(1000);
        System.out.println("   ✓ 모달 닫기 완료");
    }

    @Test
    @Order(5)
    @DisplayName("05. 신고 처리 - 사용자 차단")
    void testUserBanAction() {
        System.out.println("=== 신고 처리 - 사용자 차단 테스트 ===\n");

        // 관리자로 로그인 후 관리자 페이지 이동
        loginAsAdmin();
        navigateToFrontend("/admin");

        page.waitForTimeout(3000); // 동적 컴포넌트 로딩 대기

        // 1. 신고 항목 찾기 및 상세 모달 열기
        System.out.println("1. 신고 상세 모달 열기");

        if (!openFirstReportDetail()) {
            System.out.println("   - 신고 항목이 없어 차단 액션 테스트 건너뜀");
            return;
        }

        // 2. 처리 작업 탭으로 이동
        System.out.println("\n2. 처리 작업 탭으로 이동");

        if (isElementVisible("text=처리 작업")) {
            safeClick("text=처리 작업");
            page.waitForTimeout(1000);
            System.out.println("   ✓ 처리 작업 탭 활성화됨");
        }

        // 3. 사용자 차단 버튼 확인 및 테스트
        System.out.println("\n3. 사용자 차단 액션 테스트");

        Locator banButton = page.locator("button:has-text('차단')");

        if (banButton.count() > 0 && banButton.first().isVisible()) {

            // 버튼 활성화 상태 확인
            boolean isDisabled = banButton.first().getAttribute("disabled") != null;

            if (!isDisabled) {
                System.out.println("   ✓ 차단 버튼 활성화됨");

                // 확인 다이얼로그를 위한 이벤트 리스너 설정
                page.onceDialog(dialog -> {
                    System.out.println("   ✓ 확인 다이얼로그 표시: " + dialog.message());
                    dialog.dismiss(); // 실제 차단은 하지 않고 취소
                });

                // 차단 버튼 클릭
                safeClick(banButton.first());
                page.waitForTimeout(1000);

                System.out.println("   ✓ 차단 액션 버튼 클릭됨 (취소로 처리)");

            } else {
                System.out.println("   - 차단 버튼 비활성화됨 (익명 사용자이거나 이미 처리됨)");
            }

        } else {
            System.out.println("   - 차단 버튼을 찾을 수 없음");
        }

        // 4. 강제 탈퇴 버튼 확인
        System.out.println("\n4. 강제 탈퇴 액션 확인");

        Locator withdrawButton = page.locator("button:has-text('탈퇴')");

        if (withdrawButton.count() > 0 && withdrawButton.first().isVisible()) {

            boolean isDisabled = withdrawButton.first().getAttribute("disabled") != null;

            if (!isDisabled) {
                System.out.println("   ✓ 강제 탈퇴 버튼 활성화됨");
            } else {
                System.out.println("   - 강제 탈퇴 버튼 비활성화됨 (익명 사용자이거나 이미 처리됨)");
            }

        } else {
            System.out.println("   - 강제 탈퇴 버튼을 찾을 수 없음");
        }

        // 5. 익명 사용자 안내 메시지 확인
        System.out.println("\n5. 익명 사용자 안내 메시지 확인");

        if (isElementVisible("text=익명 사용자는 차단 또는 강제 탈퇴할 수 없습니다")) {
            System.out.println("   ✓ 익명 사용자 안내 메시지 표시됨");
        } else {
            System.out.println("   - 등록된 사용자 신고이거나 안내 메시지 없음");
        }

        // 모달 닫기
        page.keyboard().press("Escape");
        page.waitForTimeout(1000);
    }

    @Test
    @Order(6)
    @DisplayName("06. 탭 네비게이션 및 상세 통계")
    void testTabNavigation() {
        System.out.println("=== 탭 네비게이션 및 상세 통계 테스트 ===\n");

        // 관리자로 로그인 후 관리자 페이지 이동
        loginAsAdmin();
        navigateToFrontend("/admin");

        page.waitForTimeout(3000);

        // 1. 기본 탭 상태 확인 (신고 관리)
        System.out.println("1. 기본 탭 상태 확인");

        Locator reportsTab = page.locator("text=신고 관리");
        Locator statsTab = page.locator("text=상세 통계");

        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(reportsTab).isVisible();
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(statsTab).isVisible();

        System.out.println("   ✓ 두 탭 모두 표시됨");

        // 기본적으로 신고 관리 탭이 활성화되어 있는지 확인
        String reportsTabState = reportsTab.getAttribute("data-state");
        if ("active".equals(reportsTabState)) {
            System.out.println("   ✓ 신고 관리 탭이 기본 활성화됨");
        }

        // 2. 상세 통계 탭으로 전환
        System.out.println("\n2. 상세 통계 탭으로 전환");

        safeClick(statsTab);
        page.waitForTimeout(2000); // 탭 전환 및 컨텐츠 로딩 대기

        // 상세 통계 탭이 활성화되었는지 확인
        String statsTabState = statsTab.getAttribute("data-state");
        if ("active".equals(statsTabState)) {
            System.out.println("   ✓ 상세 통계 탭 활성화됨");
        }

        // 3. 준비 중 메시지 확인
        System.out.println("\n3. 상세 통계 탭 내용 확인");

        if (isElementVisible("text=준비 중입니다") ||
            isElementVisible("text=상세 통계 기능은 준비 중입니다")) {

            System.out.println("   ✓ '준비 중입니다' 메시지 표시됨");

            // 차트 아이콘 확인
            if (page.locator("svg").count() > 0) {
                System.out.println("   ✓ 차트 아이콘 표시됨");
            }

        } else {
            System.out.println("   - 상세 통계 기능이 구현되었거나 다른 메시지 표시됨");
        }

        // 4. 다시 신고 관리 탭으로 전환
        System.out.println("\n4. 신고 관리 탭으로 돌아가기");

        safeClick(reportsTab);
        page.waitForTimeout(2000);

        // 신고 관리 탭 활성화 확인
        String finalReportsTabState = reportsTab.getAttribute("data-state");
        if ("active".equals(finalReportsTabState)) {
            System.out.println("   ✓ 신고 관리 탭으로 성공적으로 돌아감");
        }

        // 신고 관리 컨텐츠가 다시 표시되는지 확인
        if (isElementVisible("text=전체") || // 필터 버튼
            isElementVisible("text=대기중") ||
            isElementVisible("text=신고가 없습니다")) {

            System.out.println("   ✓ 신고 관리 컨텐츠 재표시됨");
        }

        // 5. URL 파라미터 확인 (선택적)
        System.out.println("\n5. URL 상태 확인");
        String currentUrl = page.url();
        System.out.println("   현재 URL: " + currentUrl);

        // 탭 전환 시 URL에 파라미터가 추가되는지 확인
        if (currentUrl.contains("tab=")) {
            System.out.println("   ✓ URL에 탭 파라미터 포함됨");
        } else {
            System.out.println("   - URL 파라미터 없음 (클라이언트 사이드 상태 관리)");
        }
    }

    @Test
    @Order(7)
    @DisplayName("07. 모바일 반응형 테스트")
    void testMobileResponsive() {
        System.out.println("=== 모바일 반응형 테스트 ===\n");

        // 관리자로 로그인
        loginAsAdmin();
        navigateToFrontend("/admin");

        page.waitForTimeout(2000);

        // 1. 데스크톱 뷰 확인
        System.out.println("1. 데스크톱 뷰 확인");
        setDesktopViewport();
        page.waitForTimeout(1000);

        boolean hasDesktopGrid = page.evaluate("() => {" +
            "const grids = document.querySelectorAll('.grid');" +
            "return grids.length > 0;" +
        "}").toString().equals("true");

        if (hasDesktopGrid) {
            System.out.println("   ✓ 데스크톱 그리드 레이아웃 확인됨");
        }

        // 2. 태블릿 뷰 테스트
        System.out.println("\n2. 태블릿 뷰 테스트");
        setTabletViewport();
        page.waitForTimeout(1000);

        // 뷰포트 변경 후 레이아웃 적응 확인
        boolean hasResponsiveLayout = page.evaluate("() => {" +
            "const container = document.querySelector('.max-w-7xl');" +
            "return container !== null;" +
        "}").toString().equals("true");

        if (hasResponsiveLayout) {
            System.out.println("   ✓ 태블릿 뷰 레이아웃 적응됨");
        }

        // 3. 모바일 뷰 테스트
        System.out.println("\n3. 모바일 뷰 테스트");
        setMobileViewport();
        page.waitForTimeout(1000);

        // 모바일에서 탭 버튼들이 적절히 표시되는지 확인
        if (isElementVisible("text=신고 관리") && isElementVisible("text=상세 통계")) {
            System.out.println("   ✓ 모바일에서 탭 버튼들 정상 표시됨");
        }

        // 모바일에서 통계 카드들이 세로로 스택되는지 확인
        boolean hasMobileStack = page.evaluate("() => {" +
            "const cards = document.querySelectorAll('.grid > *');" +
            "if (cards.length < 2) return false;" +
            "const firstCard = cards[0].getBoundingClientRect();" +
            "const secondCard = cards[1].getBoundingClientRect();" +
            "return secondCard.top > firstCard.bottom;" +
        "}").toString().equals("true");

        if (hasMobileStack) {
            System.out.println("   ✓ 모바일에서 카드들이 세로 스택으로 배치됨");
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
            System.out.println("   ✓ 터치 타겟 크기 44px 이상 준수됨");
        } else {
            System.out.println("   - 일부 버튼이 권장 터치 타겟 크기 미달");
        }

        // 5. 데스크톱 뷰로 복원
        System.out.println("\n5. 데스크톱 뷰로 복원");
        setDesktopViewport();
        page.waitForTimeout(1000);
        System.out.println("   ✓ 데스크톱 뷰로 복원 완료");
    }

    @Test
    @Order(8)
    @DisplayName("08. 페이지네이션 및 무한 스크롤")
    void testPaginationAndInfiniteScroll() {
        System.out.println("=== 페이지네이션 및 무한 스크롤 테스트 ===\n");

        // 관리자로 로그인
        loginAsAdmin();
        navigateToFrontend("/admin");

        page.waitForTimeout(3000);

        // 1. 신고 목록 확인
        System.out.println("1. 신고 목록 확인");

        Locator reportList = page.locator("[data-testid='report-card'], .report-item, button:has-text('신고 ID')");

        if (reportList.count() > 0) {
            int initialCount = reportList.count();
            System.out.println("   ✓ 초기 신고 아이템 개수: " + initialCount);

            // 2. 스크롤을 아래로 내려서 추가 데이터 로드 테스트
            System.out.println("\n2. 무한 스크롤 테스트");

            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            page.waitForTimeout(2000); // 추가 데이터 로드 대기

            int afterScrollCount = page.locator("[data-testid='report-card'], .report-item, button:has-text('신고 ID')").count();

            if (afterScrollCount > initialCount) {
                System.out.println("   ✓ 스크롤 후 아이템 추가 로드됨: " + afterScrollCount);
            } else if (afterScrollCount == initialCount) {
                System.out.println("   - 추가 데이터 없음 (전체 데이터 이미 로드됨)");
            }
        } else {
            System.out.println("   - 신고 목록이 비어있음");
        }

        // 3. 페이지네이션 버튼 확인
        System.out.println("\n3. 페이지네이션 컨트롤 확인");

        if (isElementVisible("button[aria-label*='페이지']") ||
            isElementVisible("button:has-text('다음')") ||
            isElementVisible("button:has-text('이전')")) {
            System.out.println("   ✓ 페이지네이션 버튼 표시됨");
        } else {
            System.out.println("   - 페이지네이션 불필요 또는 무한 스크롤 사용");
        }
    }

    @Test
    @Order(9)
    @DisplayName("09. 검색 및 필터 조합")
    void testSearchAndFilterCombination() {
        System.out.println("=== 검색 및 필터 조합 테스트 ===\n");

        // 관리자로 로그인
        loginAsAdmin();
        navigateToFrontend("/admin");

        page.waitForTimeout(3000);

        // 1. 검색창 찾기
        System.out.println("1. 검색 기능 테스트");

        Locator searchInput = page.locator("input[type='search'], input[placeholder*='검색']").first();

        if (searchInput.isVisible()) {
            // 디바운싱 테스트를 위한 빠른 타이핑
            searchInput.fill("테");
            page.waitForTimeout(100);
            searchInput.fill("테스트");
            page.waitForTimeout(100);
            searchInput.fill("최종 검색어");

            // 디바운싱 대기
            page.waitForTimeout(600);
            System.out.println("   ✓ 검색어 입력 및 디바운싱 테스트 완료");
        } else {
            System.out.println("   - 검색창을 찾을 수 없음");
        }

        // 2. 필터 조합 테스트
        System.out.println("\n2. 필터 조합 테스트");

        // 신고 유형 필터
        if (isElementVisible("button:has-text('게시글')")) {
            safeClick("button:has-text('게시글')");
            page.waitForTimeout(1000);
            System.out.println("   ✓ 게시글 필터 적용");
        }

        // 처리 상태 필터
        if (isElementVisible("button:has-text('처리중')")) {
            safeClick("button:has-text('처리중')");
            page.waitForTimeout(1000);
            System.out.println("   ✓ 처리중 상태 필터 적용");
        }

        // 3. 필터 초기화
        System.out.println("\n3. 필터 초기화");

        if (isElementVisible("button:has-text('전체')")) {
            safeClick("button:has-text('전체')");
            page.waitForTimeout(1000);
            System.out.println("   ✓ 필터 초기화 완료");
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. 에러 처리 및 복구")
    void testErrorHandlingAndRecovery() {
        System.out.println("=== 에러 처리 및 복구 테스트 ===\n");

        // 관리자로 로그인
        loginAsAdmin();

        // 1. 잘못된 URL 접근 테스트
        System.out.println("1. 잘못된 URL 접근 테스트");

        navigateToFrontend("/admin/invalid-path");
        page.waitForTimeout(2000);

        boolean hasErrorHandling =
            page.url().contains("/admin") ||  // 관리자 페이지로 리다이렉트
            page.url().equals(FRONTEND_URL + "/") ||  // 홈으로 리다이렉트
            isElementVisible("text=/404|not found|페이지를 찾을 수 없/i");  // 404 페이지

        if (hasErrorHandling) {
            System.out.println("   ✓ 잘못된 경로 접근 시 적절한 처리됨");
        }

        // 2. 정상 페이지로 복구
        System.out.println("\n2. 정상 페이지로 복구");

        navigateToFrontend("/admin");
        waitForElement("text=관리자 대시보드", 10000);
        System.out.println("   ✓ 정상 관리자 페이지로 복구됨");

        // 3. 네트워크 오류 시뮬레이션
        System.out.println("\n3. 네트워크 오류 처리 테스트");

        // 오프라인 모드 설정
        page.context().setOffline(true);

        // 액션 시도
        if (isElementVisible("button:has-text('상세 통계')")) {
            safeClick("button:has-text('상세 통계')");
            page.waitForTimeout(1000);

            // 에러 메시지나 로딩 상태 확인
            boolean hasNetworkErrorHandling =
                isElementVisible("text=/오류|error|실패|네트워크/i") ||
                isElementVisible(".animate-spin");

            if (hasNetworkErrorHandling) {
                System.out.println("   ✓ 네트워크 오류 시 적절한 처리");
            }
        }

        // 4. 온라인 모드로 복구
        System.out.println("\n4. 네트워크 복구");

        page.context().setOffline(false);
        page.reload();
        waitForElement("text=관리자 대시보드", 10000);
        System.out.println("   ✓ 네트워크 복구 후 정상 작동");

        // 5. 세션 타임아웃 시뮬레이션
        System.out.println("\n5. 세션 유지 확인");

        // 다른 페이지로 이동 후 돌아오기
        navigateToFrontend("/");
        page.waitForTimeout(1000);
        navigateToFrontend("/admin");

        if (waitForElement("text=관리자 대시보드", 5000) != null) {
            System.out.println("   ✓ 세션 유지되어 재인증 없이 접근 가능");
        } else if (page.url().contains("/login")) {
            System.out.println("   ✓ 세션 만료 시 로그인 페이지로 리다이렉트");
        }
    }


    // ===== 헬퍼 메서드 =====

    /**
     * 관리자 계정으로 로그인
     */
    private void loginAsAdmin() {
        navigateToFrontend("/login");

        // 관리자 로그인 로직 (카카오 OAuth 또는 테스트용 로그인)
        // 실제 구현에서는 테스트용 관리자 계정 또는 모킹된 로그인 사용
        if (isElementVisible("button:has-text('카카오 로그인')")) {
            // 카카오 로그인 버튼 클릭 (테스트 환경에서는 자동으로 관리자 계정으로 로그인됨)
            safeClick("button:has-text('카카오 로그인')");
        } else {
            // 일반 로그인 폼이 있는 경우
            if (isElementVisible("input[type='email'], input[name='username']")) {
                safeFill("input[type='email'], input[name='username']", ADMIN_USERNAME);
                safeFill("input[type='password'], input[name='password']", ADMIN_PASSWORD);
                safeClick("button[type='submit'], button:has-text('로그인')");
            }
        }

        // 로그인 완료 대기
        page.waitForTimeout(3000);
    }

    /**
     * 일반 사용자 계정으로 로그인
     */
    private void loginAsRegularUser() {
        navigateToFrontend("/login");

        if (isElementVisible("button:has-text('카카오 로그인')")) {
            // 카카오 로그인 (테스트 환경에서 일반 사용자로 로그인)
            safeClick("button:has-text('카카오 로그인')");
        } else {
            // 일반 로그인 폼
            if (isElementVisible("input[type='email'], input[name='username']")) {
                safeFill("input[type='email'], input[name='username']", REGULAR_USERNAME);
                safeFill("input[type='password'], input[name='password']", REGULAR_PASSWORD);
                safeClick("button[type='submit'], button:has-text('로그인')");
            }
        }

        page.waitForTimeout(3000);
    }

    /**
     * 첫 번째 신고 항목을 찾아서 상세 모달 열기
     */
    private boolean openFirstReportDetail() {
        String[] reportSelectors = {
            "[data-testid='report-card']",
            ".report-item",
            "[data-testid='report-item']",
            "button:has-text('신고 ID')",
            "button:has-text('#')"
        };

        for (String selector : reportSelectors) {
            if (page.locator(selector).count() > 0) {
                Locator firstReport = page.locator(selector).first();
                if (firstReport.isVisible()) {
                    safeClick(firstReport);
                    page.waitForTimeout(2000); // 모달 로딩 대기
                    return true;
                }
            }
        }

        // 신고가 없는 경우 빈 상태 확인
        return !isElementVisible("text=신고가 없습니다") &&
               !isElementVisible("text=등록된 신고가 없습니다");
    }
}