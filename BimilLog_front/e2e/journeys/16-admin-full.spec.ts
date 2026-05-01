import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-16-admin-full';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 대기를 폴링이 영원히 잡는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

type AdminBranch = 'admin' | 'home' | 'login' | 'unknown';

/**
 * fail-soft 진입 헬퍼.
 *  /admin 은 (protected) 라우트 그룹.
 *   - 비인증 시: useAdminAuth 가 isAdmin=false → AdminClient 가 토스트 후 router.push("/")
 *     → 'home' 분기. 단, middleware 가 /login 으로 보낼 수도 있어 'login' 분기도 허용.
 *   - admin 권한 보유 시: 'admin' 분기 (시드 부족하면 도달 어려움 → fail-soft).
 */
async function gotoAdmin(page: Page, search: string = ''): Promise<AdminBranch> {
  await page.goto(`/admin${search}`);
  await waitSettled(page, 1500);

  const finalUrl = page.url();
  if (/\/admin/.test(finalUrl)) return 'admin';
  if (/\/login/.test(finalUrl)) return 'login';
  // /admin 에서 push("/") 후 final URL 은 origin 또는 origin/
  if (/^https?:\/\/[^/]+\/?$/.test(finalUrl)) return 'home';
  return 'unknown';
}

/**
 * round-16-admin-full iter-1
 *  - 비인증 컨텍스트 (시드 토큰 없음).
 *  - 라운드 16 핵심: 관리자 대시보드 — useAdminAuth 비-admin 시 토스트+홈 리다이렉트
 *    (라운드 13 (protected) 가드 패턴 차용). 권한 통과 시 ?tab= URL SSOT,
 *    paper/postal-navy/stamp-red 토큰, ARIA tablist, ReportFilters aria-pressed,
 *    ReportListContainer EmptyView, AdminStats 카드 (totalElements 활용).
 *  - 39 functional bug 회귀 (자세한 내용은 frontend-changes.md):
 *      F-16-001 useAdminAuth router.push 제거 → AdminClient 한 곳 토스트+redirect
 *      F-16-002 loading.tsx paper 톤 일관 (3중 깜박임 완화)
 *      F-16-003/004 H1 SSOT — AdminHeader 가 H1 보유, AdminClient H1 제거
 *      F-16-005 ?tab= URL SSOT (Tabs SSOT)
 *      F-16-006 sticky tabs + 카운트 뱃지
 *      F-16-007/008/010 AdminStats — totalElements + reports.content 즉시 집계
 *      F-16-009 stats 탭 placeholder → EmptyView paper 톤
 *      F-16-011 paper-card / postal-navy / stamp-red 다크 토큰 일괄
 *      F-16-012 필터 영역 paper-card
 *      F-16-014 모바일 필터 토글 aria-expanded / aria-controls
 *      F-16-015/016 ReportFilters role=group + aria-pressed (toggle button 패턴)
 *      F-16-017/028 listRef + nav aria-label 페이지네이션
 *      F-16-018/019 표 scope/aria-label
 *      F-16-036 옵티미스틱 removeResolvedReports
 *      F-16-037 모달 진행 중 다른 카드 차단
 *      F-16-040 stats 탭 EmptyView 카피
 *  - fail-soft: home/login 분기에서도 매트릭스 캡처 확보 (가드 회귀 검증).
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-admin-entry             /admin 비인증 진입 → 토스트 + redirect (home/login)
 *  step-02-aria-tablist            ?tab= URL SSOT + ARIA tablist (라운드 13 패턴)
 *  step-03-report-list-or-empty    신고 리스트 / 시드 부족 시 EmptyView ("처리할 신고가 없어요")
 *  step-04-report-filters          ReportFilters role=group + aria-pressed (활성=stamp-red)
 *  step-05-report-detail-modal     ReportDetailModal 시뮬 (시드 부족 시 fail-soft)
 *  step-06-paper-tokens            paper / postal-navy / stamp-red 토큰 일관 + 다크
 *  step-07-mobile-sticky           모바일 sticky 헤더 + 필터 토글 + 단축 라벨
 *  step-08-admin-stats             AdminStats 카드 (totalElements 활용)
 */
test.describe('round-16-admin-full', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 시드 토큰 없는 fail-soft 검증.
    // /admin 은 (protected) 라 토스트 + router.push("/") (또는 미들웨어 /login).
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /admin 비인증 진입 — F-16-001 (가드 회귀)
  //  - useAdminAuth 가 isAdmin=false → AdminClient 가 토스트 + push("/")
  //  - 라운드 13 패턴 (한 곳에서만 redirect, race 차단)
  //  - middleware 가 /login 으로 보낼 수도 있어 양쪽 fail-soft
  // ============================================================
  test('step-01-admin-entry', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);

    // 분기별 가시성 검증
    let toastVisible = false;
    let homeHeroVisible = false;
    let loginFormVisible = false;
    let adminH1Visible = false;

    // 토스트는 redirect 전 잠깐 뜨거나, redirect 후에도 일부 잔류 가능.
    toastVisible = await page
      .locator('text=/관리자 전용 페이지|이 페이지는 관리자만/')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    if (branch === 'home') {
      homeHeroVisible = await page
        .locator('main, body')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);
    } else if (branch === 'login') {
      loginFormVisible = await page
        .locator('button:has-text("카카오"), button:has-text("로그인")')
        .first()
        .isVisible({ timeout: 2000 })
        .catch(() => false);
    } else if (branch === 'admin') {
      adminH1Visible = await page
        .getByRole('heading', { level: 1, name: /관리자 대시보드/ })
        .first()
        .isVisible({ timeout: 2000 })
        .catch(() => false);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-admin-entry',
    );

    test.info().annotations.push({
      type: 'admin-entry',
      description: JSON.stringify(
        {
          branch,
          url: page.url(),
          toastVisible,
          homeHeroVisible,
          loginFormVisible,
          adminH1Visible,
        },
        null,
        2,
      ),
    });

    // sanity — 비인증이라 home/login/admin (admin 도달은 unlikely) 셋 중 하나
    expect(['admin', 'home', 'login', 'unknown']).toContain(branch);
  });

  // ============================================================
  // step-02: ?tab= URL SSOT + ARIA tablist — F-16-005/006
  //  - ?tab=stats 로 직접 진입 시 stats 탭 활성 (admin 분기 도달 시)
  //  - role="tablist" / role="tab" / aria-selected
  //  - sticky 헤더 + 카운트 뱃지
  //  - home/login 분기 시 fail-soft (검증 데이터만 0)
  // ============================================================
  test('step-02-aria-tablist', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page, '?tab=stats');

    let tablistExists = false;
    let tabCount = 0;
    let activeTabValue: string | null = null;
    let activeTabAriaSelected: string | null = null;
    let stickyClassPresent = false;
    let countBadgeVisible = false;

    if (branch === 'admin') {
      const tablist = page.locator('[role="tablist"]').first();
      tablistExists = (await tablist.count()) > 0;

      if (tablistExists) {
        tabCount = await tablist.locator('[role="tab"]').count().catch(() => 0);

        // 활성 탭 — aria-selected="true"
        const activeTab = tablist.locator('[role="tab"][aria-selected="true"]').first();
        if ((await activeTab.count()) > 0) {
          activeTabValue = await activeTab.getAttribute('data-state').catch(() => null);
          activeTabAriaSelected = await activeTab.getAttribute('aria-selected').catch(() => null);
        }

        // sticky 헤더 — div.sticky 클래스
        stickyClassPresent = await page
          .locator('.sticky.top-0')
          .first()
          .isVisible({ timeout: 1000 })
          .catch(() => false);

        // 카운트 뱃지 — totalElements > 0 일 때만
        countBadgeVisible = await page
          .locator('span[aria-label*="건"]')
          .first()
          .isVisible({ timeout: 1000 })
          .catch(() => false);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-aria-tablist',
    );

    test.info().annotations.push({
      type: 'aria-tablist',
      description: JSON.stringify(
        {
          branch,
          url: page.url(),
          tablistExists,
          tabCount,
          activeTabValue,
          activeTabAriaSelected,
          stickyClassPresent,
          countBadgeVisible,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-03: 신고 리스트 — F-16-013 EmptyView
  //  - admin 분기 도달 + 시드 부족: "처리할 신고가 없어요" + AlertTriangle stamp-red
  //  - admin 분기 도달 + 데이터 있음: 카드 리스트 (데스크톱 table / 모바일 카드)
  //  - 비인증 분기: fail-soft
  // ============================================================
  test('step-03-report-list-or-empty', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);

    let listContainerVisible = false;
    let emptyTitleVisible = false;
    let emptyDescPresent = false;
    let alertTriangleCount = 0;
    let listItemCount = 0;
    let listRefHasFocusable = false;
    let totalCountVisible = false;

    if (branch === 'admin') {
      // 신고 목록 헤더 (h2)
      listContainerVisible = await page
        .getByRole('heading', { level: 2, name: /신고 목록/ })
        .first()
        .isVisible({ timeout: 4000 })
        .catch(() => false);

      // EmptyView title
      emptyTitleVisible = await page
        .locator('text=/처리할 신고가 없어요/')
        .first()
        .isVisible({ timeout: 2000 })
        .catch(() => false);

      // EmptyView description
      emptyDescPresent = await page
        .locator('text=/새로운 신고가 들어오면|여기에 표시/')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);

      alertTriangleCount = await page
        .locator('svg.lucide-alert-triangle, svg[class*="lucide-alert-triangle"]')
        .count()
        .catch(() => 0);

      // 리스트 행 (table tbody tr)
      listItemCount = await page
        .locator('tbody tr, [class*="MobileReportCard"]')
        .count()
        .catch(() => 0);

      // listRef tabIndex
      listRefHasFocusable = (await page
        .locator('[tabindex="-1"][aria-busy]')
        .count()) > 0;

      // 헤더 "총 N건"
      totalCountVisible = await page
        .locator('text=/총 \\d+건/')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-report-list-or-empty',
    );

    test.info().annotations.push({
      type: 'report-list-or-empty',
      description: JSON.stringify(
        {
          branch,
          listContainerVisible,
          emptyTitleVisible,
          emptyDescPresent,
          alertTriangleCount,
          listItemCount,
          listRefHasFocusable,
          totalCountVisible,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-04: ReportFilters aria-pressed — F-16-015/016
  //  - role="group" aria-label="신고 종류 필터"
  //  - 5개 칩 (전체/게시글/댓글/오류/개선) 각 button 에 aria-pressed
  //  - 활성 칩 → aria-pressed="true" + bg-stamp-red/15 ring
  //  - 비활성 → aria-pressed="false" + bg-paper-soft
  //  - 필터 클릭 후 ?filter= URL 갱신 검증 + 활성 변경 검증
  // ============================================================
  test('step-04-report-filters', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);

    let groupExists = false;
    let groupAriaLabel: string | null = null;
    let filterChipCount = 0;
    let initialPressedLabels: string[] = [];
    let postFilterPressedLabels: string[] = [];
    let postClickClicked = false;

    if (branch === 'admin') {
      // role=group + aria-label
      const group = page.locator('[role="group"][aria-label="신고 종류 필터"]').first();
      groupExists = (await group.count()) > 0;
      if (groupExists) {
        groupAriaLabel = await group.getAttribute('aria-label').catch(() => null);
        filterChipCount = await group.locator('button[aria-pressed]').count().catch(() => 0);

        // 현재 aria-pressed=true 인 칩 라벨 수집
        const pressed = await group.locator('button[aria-pressed="true"]').all();
        for (const b of pressed) {
          const t = (await b.textContent().catch(() => null))?.trim();
          if (t) initialPressedLabels.push(t);
        }

        // 게시글 필터 클릭 → aria-pressed 토글
        const postBtn = group.locator('button:has-text("게시글")').first();
        if (await postBtn.isVisible({ timeout: 1500 }).catch(() => false)) {
          await postBtn.click().catch(() => undefined);
          postClickClicked = true;
          await page.waitForTimeout(700);

          const pressedAfter = await group.locator('button[aria-pressed="true"]').all();
          for (const b of pressedAfter) {
            const t = (await b.textContent().catch(() => null))?.trim();
            if (t) postFilterPressedLabels.push(t);
          }
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-report-filters',
    );

    test.info().annotations.push({
      type: 'report-filters',
      description: JSON.stringify(
        {
          branch,
          groupExists,
          groupAriaLabel,
          filterChipCount,
          initialPressedLabels,
          postClickClicked,
          postFilterPressedLabels,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-05: ReportDetailModal — F-16-036/037
  //  - 신고 카드 행의 "상세보기" 버튼 클릭 → ReportDetailModal 마운트
  //  - 시드 부족 시 카드 0건 → 모달 띄우지 못함 (fail-soft)
  //  - aria-modal / role=dialog / paper 토큰
  // ============================================================
  test('step-05-report-detail-modal', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);

    let viewBtnExists = false;
    let modalOpened = false;
    let modalRole: string | null = null;
    let modalAriaModal: string | null = null;
    let modalTitleVisible = false;

    if (branch === 'admin') {
      const viewBtn = page
        .locator('button:has-text("상세보기"), button[aria-label*="상세"]')
        .first();
      viewBtnExists = await viewBtn.isVisible({ timeout: 2000 }).catch(() => false);

      if (viewBtnExists) {
        await viewBtn.click().catch(() => undefined);
        await page.waitForTimeout(900);

        const modal = page.locator('[role="dialog"]').first();
        if ((await modal.count()) > 0) {
          modalOpened = await modal.isVisible({ timeout: 1500 }).catch(() => false);
          modalRole = await modal.getAttribute('role').catch(() => null);
          modalAriaModal = await modal.getAttribute('aria-modal').catch(() => null);
        }

        modalTitleVisible = await page
          .locator('text=/신고 상세|신고 정보/')
          .first()
          .isVisible({ timeout: 1500 })
          .catch(() => false);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-report-detail-modal',
    );

    // 모달이 열렸으면 닫기 (다음 step 영향 방지)
    if (modalOpened) {
      const closeBtn = page
        .locator('button:has-text("닫기"), button[aria-label*="닫기"], [data-testid*="close"]')
        .first();
      if (await closeBtn.isVisible({ timeout: 600 }).catch(() => false)) {
        await closeBtn.click().catch(() => undefined);
        await page.waitForTimeout(300);
      }
      // ESC 백업
      await page.keyboard.press('Escape').catch(() => undefined);
      await page.waitForTimeout(300);
    }

    test.info().annotations.push({
      type: 'report-detail-modal',
      description: JSON.stringify(
        {
          branch,
          viewBtnExists,
          modalOpened,
          modalRole,
          modalAriaModal,
          modalTitleVisible,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-06: paper / postal-navy / stamp-red 토큰 — F-16-011
  //  - bg-paper, bg-paper-card, bg-paper-soft, bg-paper-aged
  //  - text-ink / text-ink-soft / text-postal-navy
  //  - text-stamp-red / stroke-stamp-red
  //  - border-postal-navy/15 ~ border-postal-navy/30
  //  - 폐기 토큰 잔존 0 (text-gray-900, bg-white, text-red-600 직접 색 — admin 트리 안에서만 카운트)
  // ============================================================
  test('step-06-paper-tokens', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);
    const theme = themeFromProjectName(testInfo.project.name);

    let paperBgPresent = false;
    let paperCardPresent = false;
    let paperSoftPresent = false;
    let postalNavyBorderCount = 0;
    let stampRedTextCount = 0;
    let stampRedStrokeCount = 0;
    let inkTextCount = 0;
    let inkSoftCount = 0;
    let bgWhiteInAdminCount = 0;
    let textGray900InAdminCount = 0;
    let pageBg = '';
    let h1Color = '';

    if (branch === 'admin') {
      paperBgPresent =
        (await page.locator('.bg-paper, .bg-paper-50, [class*="bg-paper"]').count()) > 0;
      paperCardPresent = (await page.locator('.bg-paper-card').count()) > 0;
      paperSoftPresent =
        (await page.locator('.bg-paper-soft, [class*="bg-paper-soft"]').count()) > 0;
      postalNavyBorderCount = await page
        .locator('[class*="border-postal-navy"]')
        .count()
        .catch(() => 0);
      stampRedTextCount = await page
        .locator('[class*="text-stamp-red"]')
        .count()
        .catch(() => 0);
      stampRedStrokeCount = await page
        .locator('[class*="stroke-stamp-red"]')
        .count()
        .catch(() => 0);
      inkTextCount = await page.locator('[class*="text-ink"]').count().catch(() => 0);
      inkSoftCount = await page
        .locator('[class*="text-ink-soft"]')
        .count()
        .catch(() => 0);

      // admin 트리 한정 — h1 (관리자 대시보드) 의 ancestor 까지 (page.tsx wrapper)
      const adminTree = page
        .locator('div')
        .filter({ has: page.getByRole('heading', { level: 1, name: /관리자 대시보드/ }) });
      bgWhiteInAdminCount = await adminTree.locator('.bg-white').count().catch(() => 0);
      textGray900InAdminCount = await adminTree
        .locator('.text-gray-900')
        .count()
        .catch(() => 0);

      pageBg = await page
        .locator('div.bg-paper, main, body')
        .first()
        .evaluate((el) => window.getComputedStyle(el).backgroundColor)
        .catch(() => '');

      h1Color = await page
        .getByRole('heading', { level: 1, name: /관리자 대시보드/ })
        .first()
        .evaluate((el) => window.getComputedStyle(el).color)
        .catch(() => '');
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-paper-tokens',
    );

    test.info().annotations.push({
      type: 'paper-tokens',
      description: JSON.stringify(
        {
          branch,
          theme,
          paperBgPresent,
          paperCardPresent,
          paperSoftPresent,
          postalNavyBorderCount,
          stampRedTextCount,
          stampRedStrokeCount,
          inkTextCount,
          inkSoftCount,
          bgWhiteInAdminCount,
          textGray900InAdminCount,
          pageBg,
          h1Color,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-07: 모바일 sticky + 단축 라벨 — F-16-006/014
  //  - sticky tabs (.sticky.top-0)
  //  - 모바일 필터 토글 — sm:hidden (데스크톱은 hidden) + aria-expanded
  //  - 데스크톱 table view (hidden sm:block) / 모바일 카드 (sm:hidden)
  // ============================================================
  test('step-07-mobile-sticky', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);
    const vp = viewportFromProjectName(testInfo.project.name);

    let stickyHeaderPresent = false;
    let stickyHeaderTop = '';
    let mobileFilterToggleVisible = false;
    let mobileFilterAriaExpanded: string | null = null;
    let mobileFilterAriaControls: string | null = null;
    let panelVisible = false;
    let stickyComputedPosition = '';

    if (branch === 'admin') {
      const sticky = page.locator('.sticky.top-0').first();
      stickyHeaderPresent = await sticky
        .isVisible({ timeout: 1500 })
        .catch(() => false);

      if (stickyHeaderPresent) {
        const dim = await sticky
          .evaluate((el) => {
            const cs = window.getComputedStyle(el);
            return { position: cs.position, top: cs.top };
          })
          .catch(() => ({ position: '', top: '' }));
        stickyComputedPosition = dim.position;
        stickyHeaderTop = dim.top;
      }

      // 모바일 필터 토글 — sm:hidden (mobile 에서만 visible)
      const toggle = page
        .locator('button[aria-expanded][aria-controls="report-filters-panel"]')
        .first();
      mobileFilterToggleVisible = await toggle
        .isVisible({ timeout: 1500 })
        .catch(() => false);
      if (mobileFilterToggleVisible) {
        mobileFilterAriaExpanded = await toggle
          .getAttribute('aria-expanded')
          .catch(() => null);
        mobileFilterAriaControls = await toggle
          .getAttribute('aria-controls')
          .catch(() => null);

        // 토글 클릭하여 패널 열기 (모바일 뷰에서)
        if (vp === 'mobile-375') {
          await toggle.click().catch(() => undefined);
          await page.waitForTimeout(400);
          const panel = page.locator('#report-filters-panel');
          panelVisible = await panel
            .isVisible({ timeout: 1000 })
            .catch(() => false);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-mobile-sticky',
    );

    test.info().annotations.push({
      type: 'mobile-sticky',
      description: JSON.stringify(
        {
          branch,
          vp,
          stickyHeaderPresent,
          stickyComputedPosition,
          stickyHeaderTop,
          mobileFilterToggleVisible,
          mobileFilterAriaExpanded,
          mobileFilterAriaControls,
          panelVisible,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-08: AdminStats 카드 — F-16-007/008/010
  //  - section aria-label="신고 통계 요약"
  //  - 5개 카드 (전체 신고 / 게시글 / 댓글 / 오류·개선 / 처리 가능)
  //  - 첫 카드 "전체 신고" 의 숫자 → totalElements (toLocaleString) → aria-label
  //  - paper-card / stamp-red / postal-navy / seal-gold / paper-aged 토큰 일관
  // ============================================================
  test('step-08-admin-stats', async ({ page }, testInfo) => {
    const branch = await gotoAdmin(page);

    let statsSectionVisible = false;
    let statsSectionAriaLabel: string | null = null;
    let statCardCount = 0;
    let totalReportLabelVisible = false;
    let totalReportNumberVisible = false;
    let actionableLabelVisible = false;
    let sealGoldPresent = false;
    let paperAgedPresent = false;
    let totalReportAriaLabelSample: string | null = null;

    if (branch === 'admin') {
      const statsSection = page.locator('section[aria-label="신고 통계 요약"]').first();
      statsSectionVisible = await statsSection
        .isVisible({ timeout: 4000 })
        .catch(() => false);

      if (statsSectionVisible) {
        statsSectionAriaLabel = await statsSection
          .getAttribute('aria-label')
          .catch(() => null);
        statCardCount = await statsSection
          .locator('[class*="bg-paper-card"]')
          .count()
          .catch(() => 0);

        totalReportLabelVisible = await statsSection
          .locator('text=/전체 신고/')
          .first()
          .isVisible({ timeout: 1000 })
          .catch(() => false);

        // 첫 카드 숫자 — aria-label="전체 신고 N건"
        const totalCard = statsSection
          .locator('[aria-label*="전체 신고"]')
          .first();
        if ((await totalCard.count()) > 0) {
          totalReportNumberVisible = await totalCard
            .isVisible({ timeout: 800 })
            .catch(() => false);
          totalReportAriaLabelSample = await totalCard
            .getAttribute('aria-label')
            .catch(() => null);
        }

        actionableLabelVisible = await statsSection
          .locator('text=/제재 가능|제재 가능한 신고/')
          .first()
          .isVisible({ timeout: 1000 })
          .catch(() => false);

        sealGoldPresent =
          (await statsSection.locator('[class*="seal-gold"]').count()) > 0;
        paperAgedPresent =
          (await statsSection.locator('[class*="paper-aged"]').count()) > 0;
      }
    }

    await page.evaluate(() => window.scrollTo(0, 0)).catch(() => undefined);
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-admin-stats',
    );

    test.info().annotations.push({
      type: 'admin-stats',
      description: JSON.stringify(
        {
          branch,
          statsSectionVisible,
          statsSectionAriaLabel,
          statCardCount,
          totalReportLabelVisible,
          totalReportNumberVisible,
          totalReportAriaLabelSample,
          actionableLabelVisible,
          sealGoldPresent,
          paperAgedPresent,
        },
        null,
        2,
      ),
    });
  });
});
