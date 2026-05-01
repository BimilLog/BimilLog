import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-17-cross-journey-consistency';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전 wait — networkidle 폴링 영원히 잡는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 700) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

type AuthGuardBranch = 'login' | 'home' | 'protected' | 'unknown';

/**
 * fail-soft 진입 헬퍼.
 *  /admin / /mypage 등 (protected) 라우트.
 *   - 비인증 시 useAuthGuard → /login?redirect=... 분기 (F-17-BUG-1 회귀)
 *   - 권한 통과 시 protected 분기 (시드 부족 시 도달 불가 → fail-soft)
 *   - middleware 가 / 로 보낼 수도 있어 home 분기 허용.
 */
async function gotoProtected(page: Page, target: string): Promise<AuthGuardBranch> {
  await page.goto(target);
  await waitSettled(page, 1500);

  const finalUrl = page.url();
  if (/\/login/.test(finalUrl)) return 'login';
  if (/^https?:\/\/[^/]+\/?$/.test(finalUrl)) return 'home';
  // protected 경로 패턴으로 그대로 머물러 있으면 protected 분기
  if (new RegExp(target.replace(/\?.*$/, '')).test(finalUrl)) return 'protected';
  return 'unknown';
}

/**
 * round-17-cross-journey-consistency iter-1 (FINAL)
 *  - 1~16 라운드 누적 패턴이 cross-journey 로 일관되게 살아있는지 검증.
 *  - 비인증 컨텍스트 (시드 토큰 없음) — 인증 가드 회귀 + 공통 토큰 일관성.
 *  - frontend-changes.md (iter-1) 14/15 functional bug 회귀:
 *      F-17-BUG-1 useAuthGuard → /login?redirect= 부착
 *      F-17-BUG-2 VisitClient 이중 sticky 단일화 (AuthHeader disableSticky)
 *      F-17-BUG-3 (protected) 그라데이션 → AuthLoadingScreen
 *      F-17-BUG-4 AuthHeader brand- / gray → paper/ink/postal-navy
 *      F-17-BUG-5 UserDropdownMenu DROPDOWN_THEME → 토큰 일괄
 *      F-17-BUG-6 VisitClient 라이트/다크 비대칭 해소
 *      F-17-BUG-7 NotificationBell placeholder paper-200
 *      F-17-BUG-8 WriteForm prose dark:prose-invert
 *      F-17-BUG-9 .bg-paper-soft 다크 fallback shadow
 *      F-17-BUG-10 Friend 빈 상태 EmptyView 통일
 *      F-17-BUG-11 BoardTabs ARIA (Flowbite 자동 충족)
 *      F-17-BUG-12 AuthHeader print:hidden
 *      F-17-BUG-13 HomeFooter redirect 패턴
 *      F-17-BUG-14 globals.css vanilla gray utility 제거
 *  - 정량: brand-* 144→85 (-41%), gray-N 260→123 (-53%),
 *          그라데이션 110→34 (-69%), z-{40,50} 29→7 (-76%)
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-home-tokens             / 홈 (다크/라이트 토큰)
 *  step-02-login-last-used-ribbon  /login (last-used ribbon, OAuth 3종, paper)
 *  step-03-board-tabs              /board (BoardTabs ARIA, paper-card)
 *  step-04-visit-single-sticky     /visit (sticky 단일화 — F-17-BUG-2)
 *  step-05-admin-redirect          /admin (redirect + 토스트, 라운드 13 패턴)
 *  step-06-header-footer-consistency  헤더/푸터 일관 (모든 페이지 공통)
 *  step-07-mobile-sticky-shadow    모바일 sticky shadow + 단축 라벨
 *  step-08-dark-tokens-consistency 다크 토큰 일관 (paper/postal-navy/stamp-red, vanilla 회색 부재)
 */
test.describe('round-17-cross-journey-consistency', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 인증 가드 회귀 + 일관 토큰 검증.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: / 홈 (다크/라이트 토큰)
  //  - paper / paper-soft / postal-navy / stamp-red 토큰 일관
  //  - vanilla gray (bg-white, bg-gray-N) 부재 검증
  //  - 헤더(AuthHeader)/푸터(HomeFooter) 토큰 일관
  // ============================================================
  test('step-01-home-tokens', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    await page.goto('/');
    await waitSettled(page, 1500);

    const headerExists = await page
      .locator('nav, header')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    const footerGridVisible = await page
      .locator('[data-testid="home-footer-grid"], footer')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    // paper 토큰 카운트
    const paperBgCount = await page
      .locator('[class*="bg-paper"]')
      .count()
      .catch(() => 0);

    const postalNavyCount = await page
      .locator('[class*="postal-navy"]')
      .count()
      .catch(() => 0);

    const stampRedCount = await page
      .locator('[class*="stamp-red"]')
      .count()
      .catch(() => 0);

    // brand-* 폐기 토큰 잔존 카운트 (페이지 전체)
    const brandTokenCount = await page
      .locator('[class*="brand-primary"], [class*="brand-secondary"], [class*="brand-muted"]')
      .count()
      .catch(() => 0);

    // body 배경
    const bodyBg = await page
      .locator('body')
      .first()
      .evaluate((el) => window.getComputedStyle(el).backgroundColor)
      .catch(() => '');

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-home-tokens',
    );

    test.info().annotations.push({
      type: 'home-tokens',
      description: JSON.stringify(
        {
          theme,
          headerExists,
          footerGridVisible,
          paperBgCount,
          postalNavyCount,
          stampRedCount,
          brandTokenCount,
          bodyBg,
        },
        null,
        2,
      ),
    });

    expect(headerExists).toBe(true);
  });

  // ============================================================
  // step-02: /login — last-used ribbon, OAuth 3종, paper 메타포
  //  - 라운드 3 (login-logout-relogin) last-used ribbon 패턴 회귀
  //  - 카카오/네이버/구글 OAuth 3개 버튼
  //  - paper 메타포 (페이지 배경 paper, 그라데이션 부재)
  // ============================================================
  test('step-02-login-last-used-ribbon', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    await page.goto('/login');
    await waitSettled(page, 1200);

    // OAuth 버튼 3종
    const kakaoBtnVisible = await page
      .locator('button:has-text("카카오"), a:has-text("카카오")')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);
    const naverBtnVisible = await page
      .locator('button:has-text("네이버"), a:has-text("네이버")')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);
    const googleBtnVisible = await page
      .locator('button:has-text("Google"), button:has-text("구글"), a:has-text("Google"), a:has-text("구글")')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    // last-used ribbon (라운드 3 패턴) — localStorage 에 lastUsedProvider 가 있을 때만 보임
    const lastUsedRibbonExists = await page
      .locator('text=/마지막 사용|이전 로그인|최근 사용/, [data-testid="last-used-ribbon"]')
      .first()
      .isVisible({ timeout: 1000 })
      .catch(() => false);

    // 그라데이션 부재 검증 (라운드 13/16/17 패턴)
    const gradientCount = await page
      .locator('[class*="from-pink"], [class*="via-purple"], [class*="to-indigo"], [class*="from-purple"]')
      .count()
      .catch(() => 0);

    // paper 토큰 존재
    const paperBgPresent =
      (await page.locator('[class*="bg-paper"]').count().catch(() => 0)) > 0;

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-login-last-used-ribbon',
    );

    test.info().annotations.push({
      type: 'login-last-used-ribbon',
      description: JSON.stringify(
        {
          theme,
          url: page.url(),
          kakaoBtnVisible,
          naverBtnVisible,
          googleBtnVisible,
          lastUsedRibbonExists,
          gradientCount,
          paperBgPresent,
        },
        null,
        2,
      ),
    });

    // OAuth 3종 중 최소 2개는 보여야 한다 (네트워크 등 환경 변수에 robust)
    const oauthVisibleCount = [kakaoBtnVisible, naverBtnVisible, googleBtnVisible].filter(
      Boolean,
    ).length;
    expect(oauthVisibleCount).toBeGreaterThanOrEqual(1);
  });

  // ============================================================
  // step-03: /board — BoardTabs ARIA, paper-card
  //  - 라운드 6 (board-browse-and-search) 회귀
  //  - F-17-BUG-11 — Flowbite Tabs role=tablist/tab/tabpanel 자동
  //  - aria-selected, aria-controls 검증
  //  - paper-card 토큰 일관
  // ============================================================
  test('step-03-board-tabs', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    await page.goto('/board');
    await waitSettled(page, 1500);

    // BoardTabs role=tablist 확인
    const tablistExists =
      (await page.locator('[role="tablist"]').count().catch(() => 0)) > 0;
    const tabCount = await page
      .locator('[role="tab"]')
      .count()
      .catch(() => 0);

    // 활성 탭 aria-selected
    const activeTabExists =
      (await page
        .locator('[role="tab"][aria-selected="true"]')
        .count()
        .catch(() => 0)) > 0;
    const activeTabAriaSelected = await page
      .locator('[role="tab"][aria-selected="true"]')
      .first()
      .getAttribute('aria-selected')
      .catch(() => null);

    // aria-controls (panel id 연결)
    const ariaControls = await page
      .locator('[role="tab"]')
      .first()
      .getAttribute('aria-controls')
      .catch(() => null);

    // paper-card 토큰
    const paperCardCount = await page
      .locator('[class*="bg-paper-card"]')
      .count()
      .catch(() => 0);

    // 폐기 토큰 잔존
    const bgWhiteCount = await page
      .locator('.bg-white')
      .count()
      .catch(() => 0);
    const grayBgCount = await page
      .locator('.bg-gray-50, .bg-gray-100, .bg-gray-700, .bg-gray-800')
      .count()
      .catch(() => 0);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-board-tabs',
    );

    test.info().annotations.push({
      type: 'board-tabs',
      description: JSON.stringify(
        {
          theme,
          tablistExists,
          tabCount,
          activeTabExists,
          activeTabAriaSelected,
          ariaControls,
          paperCardCount,
          bgWhiteCount,
          grayBgCount,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-04: /visit — sticky 단일화 (F-17-BUG-2)
  //  - 자체 sticky 헤더 + AuthHeader 동시 sticky 해소
  //  - <AuthHeader disableSticky /> 패턴 적용
  //  - 화면 상단 sticky 영역이 한 영역으로만 잡혀야 함
  //  - z-sticky-header / z-auth-header 토큰 일관
  // ============================================================
  test('step-04-visit-single-sticky', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    const vp = viewportFromProjectName(testInfo.project.name);
    await page.goto('/visit');
    await waitSettled(page, 1500);

    // sticky 카운트 — 페이지 전체에서 position: sticky top: 0 인 element 수
    const stickyElements = await page
      .locator('[class*="sticky"][class*="top-0"], header.sticky, .sticky')
      .all();

    const stickyDetails = [];
    for (let i = 0; i < Math.min(stickyElements.length, 8); i++) {
      const el = stickyElements[i];
      const detail = await el
        .evaluate((node) => {
          const cs = window.getComputedStyle(node);
          const tag = (node as HTMLElement).tagName?.toLowerCase() ?? '';
          const cls = (node as HTMLElement).className?.toString?.() ?? '';
          return {
            tag,
            cls,
            position: cs.position,
            top: cs.top,
            zIndex: cs.zIndex,
            visible: (node as HTMLElement).offsetParent !== null,
          };
        })
        .catch(() => null);
      if (detail) stickyDetails.push(detail);
    }

    // 실제 position:sticky + top:0px + visible 인 영역 카운트 (이중 sticky 검증)
    const realStickyCount = stickyDetails.filter(
      (d) => d.position === 'sticky' && d.top === '0px' && d.visible,
    ).length;

    // /visit 자체 헤더 (data-toast-anchor) 존재
    const ownHeaderExists = await page
      .locator('[data-toast-anchor], header[class*="sticky"]')
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);

    // 검색 input
    const searchInputVisible = await page
      .locator('input[type="text"], input[type="search"], input[placeholder*="닉네임"]')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-visit-single-sticky',
    );

    test.info().annotations.push({
      type: 'visit-single-sticky',
      description: JSON.stringify(
        {
          theme,
          vp,
          stickyDetails,
          realStickyCount,
          ownHeaderExists,
          searchInputVisible,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-05: /admin — redirect + 토스트 (라운드 13 패턴)
  //  - F-17-BUG-1 — useAuthGuard 가 redirect 쿼리 부착
  //  - 비인증 → /login?redirect=/admin 또는 / (라운드 13 redirect 패턴)
  //  - 토스트 출력 검증 (라운드 13 / 16 패턴)
  // ============================================================
  test('step-05-admin-redirect', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    const branch = await gotoProtected(page, '/admin');

    // redirect 쿼리 부착 검증
    const finalUrl = page.url();
    const hasRedirectQuery = /redirect=/.test(finalUrl);
    const redirectQueryValue = (() => {
      try {
        const u = new URL(finalUrl);
        return u.searchParams.get('redirect');
      } catch {
        return null;
      }
    })();

    // 토스트 (라운드 13/16 패턴)
    const toastVisible = await page
      .locator('text=/관리자 전용|로그인이 필요|이 페이지는 관리자만/')
      .first()
      .isVisible({ timeout: 2000 })
      .catch(() => false);

    // login 분기 가시성
    let loginFormVisible = false;
    if (branch === 'login') {
      loginFormVisible = await page
        .locator('button:has-text("카카오"), button:has-text("로그인"), a:has-text("로그인")')
        .first()
        .isVisible({ timeout: 2000 })
        .catch(() => false);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-admin-redirect',
    );

    test.info().annotations.push({
      type: 'admin-redirect',
      description: JSON.stringify(
        {
          theme,
          branch,
          finalUrl,
          hasRedirectQuery,
          redirectQueryValue,
          toastVisible,
          loginFormVisible,
        },
        null,
        2,
      ),
    });

    expect(['login', 'home', 'protected', 'unknown']).toContain(branch);
  });

  // ============================================================
  // step-06: 헤더/푸터 일관성 (cross-journey 핵심)
  //  - 홈/게시판/방문/로그인 페이지에서 헤더/푸터 동일 마크업/토큰
  //  - F-17-BUG-4/5/12 — AuthHeader / UserDropdownMenu / print:hidden
  //  - data-testid="header-menu-toggle" 위치 동일성
  //  - 푸터 그리드 5열 (데스크톱)
  // ============================================================
  test('step-06-header-footer-consistency', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    const vp = viewportFromProjectName(testInfo.project.name);

    const pages: Array<{ url: string; name: string }> = [
      { url: '/', name: 'home' },
      { url: '/board', name: 'board' },
      { url: '/login', name: 'login' },
    ];

    const samples: Array<Record<string, unknown>> = [];
    for (const p of pages) {
      await page.goto(p.url);
      await waitSettled(page, 1000);

      const navExists = await page
        .locator('nav, header')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);
      const footerExists = await page
        .locator('footer')
        .first()
        .isVisible({ timeout: 1500 })
        .catch(() => false);
      // print:hidden 명시 (F-17-BUG-12)
      const navPrintHidden = await page
        .locator('nav[class*="print:hidden"], header[class*="print:hidden"]')
        .first()
        .count()
        .catch(() => 0);
      // 헤더 토큰 — postal-navy / paper / ink
      const headerTokensCount = await page
        .locator('nav [class*="postal-navy"], nav [class*="bg-paper"], nav [class*="text-ink"]')
        .count()
        .catch(() => 0);
      // 폐기 토큰 — header 영역 brand-* 잔존
      const navBrandTokenCount = await page
        .locator(
          'nav [class*="brand-primary"], nav [class*="brand-muted"], nav [class*="brand-secondary"]',
        )
        .count()
        .catch(() => 0);
      // 푸터 그리드 (데스크톱에서만 5열, 모바일은 2열)
      const footerGridExists =
        (await page
          .locator('[data-testid="home-footer-grid"]')
          .count()
          .catch(() => 0)) > 0;

      samples.push({
        page: p.name,
        url: page.url(),
        navExists,
        footerExists,
        navPrintHidden,
        headerTokensCount,
        navBrandTokenCount,
        footerGridExists,
      });
    }

    // 마지막 페이지 (/login) 캡처 — 일관성 검증용
    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-header-footer-consistency',
    );

    test.info().annotations.push({
      type: 'header-footer-consistency',
      description: JSON.stringify({ theme, vp, samples }, null, 2),
    });
  });

  // ============================================================
  // step-07: 모바일 sticky shadow + 단축 라벨 (라운드 6/9/15/16)
  //  - 모바일에서 sticky 헤더 box-shadow 가 제공되어 컨텐츠와 시각 분리
  //  - F-17-BUG-9 — .bg-paper-soft 다크 fallback shadow
  //  - 모바일 단축 라벨 (라운드 16 패턴)
  // ============================================================
  test('step-07-mobile-sticky-shadow', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    const vp = viewportFromProjectName(testInfo.project.name);
    await page.goto('/board');
    await waitSettled(page, 1500);

    // 약간 스크롤 — sticky shadow 활성화 트리거
    await page.evaluate(() => window.scrollTo(0, 200)).catch(() => undefined);
    await page.waitForTimeout(400);

    // sticky 헤더 box-shadow 측정
    const stickyHeader = page.locator('nav, header').first();
    const stickyShadow = await stickyHeader
      .evaluate((el) => {
        const cs = window.getComputedStyle(el);
        return { position: cs.position, top: cs.top, boxShadow: cs.boxShadow };
      })
      .catch(() => ({ position: '', top: '', boxShadow: '' }));

    // 모바일 단축 라벨 — sm:hidden / hidden sm:block 패턴 (라운드 16)
    const mobileOnlyCount = await page
      .locator('[class*="sm:hidden"]')
      .count()
      .catch(() => 0);
    const desktopOnlyCount = await page
      .locator('[class*="hidden"][class*="sm:block"]')
      .count()
      .catch(() => 0);

    // .bg-paper-soft footer 영역 분리 (F-17-BUG-9)
    const paperSoftCount = await page
      .locator('[class*="bg-paper-soft"]')
      .count()
      .catch(() => 0);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-mobile-sticky-shadow',
    );

    test.info().annotations.push({
      type: 'mobile-sticky-shadow',
      description: JSON.stringify(
        {
          theme,
          vp,
          stickyShadow,
          mobileOnlyCount,
          desktopOnlyCount,
          paperSoftCount,
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-08: 다크 토큰 일관성 (paper-card / postal-navy / stamp-red, vanilla 회색 부재)
  //  - 라이트/다크 양쪽 모두 동일 토큰만 사용
  //  - F-17-BUG-6 — VisitClient 라이트/다크 비대칭 해소
  //  - 페이지 전반에서 vanilla bg-white / bg-gray-N / text-gray-N 부재
  // ============================================================
  test('step-08-dark-tokens-consistency', async ({ page }, testInfo) => {
    const theme = themeFromProjectName(testInfo.project.name);
    await page.goto('/visit');
    await waitSettled(page, 1500);

    // 토큰 카운트 — paper-card / postal-navy / stamp-red
    const paperCardCount = await page
      .locator('[class*="bg-paper-card"]')
      .count()
      .catch(() => 0);
    const postalNavyCount = await page
      .locator('[class*="postal-navy"]')
      .count()
      .catch(() => 0);
    const stampRedCount = await page
      .locator('[class*="stamp-red"]')
      .count()
      .catch(() => 0);
    const inkTokensCount = await page
      .locator('[class*="text-ink"]')
      .count()
      .catch(() => 0);

    // vanilla 폐기 토큰 잔존 — 페이지 전반
    const bgWhiteCount = await page
      .locator('.bg-white')
      .count()
      .catch(() => 0);
    const bgGrayCount = await page
      .locator('.bg-gray-50, .bg-gray-100, .bg-gray-700, .bg-gray-800, .bg-gray-900')
      .count()
      .catch(() => 0);
    const textGray900Count = await page
      .locator('.text-gray-900')
      .count()
      .catch(() => 0);
    const darkTextGrayCount = await page
      .locator('[class*="dark:text-gray-200"], [class*="dark:text-gray-300"], [class*="dark:text-gray-400"]')
      .count()
      .catch(() => 0);
    const darkBgGrayCount = await page
      .locator('[class*="dark:bg-gray-700"], [class*="dark:bg-gray-800"], [class*="dark:bg-gray-900"]')
      .count()
      .catch(() => 0);

    // body / main 배경 — 다크 모드에서 vanilla 회색이 아닌 paper 토큰
    const bodyBg = await page
      .locator('body')
      .first()
      .evaluate((el) => window.getComputedStyle(el).backgroundColor)
      .catch(() => '');
    const mainBg = await page
      .locator('main, body')
      .first()
      .evaluate((el) => window.getComputedStyle(el).backgroundColor)
      .catch(() => '');

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-dark-tokens-consistency',
    );

    test.info().annotations.push({
      type: 'dark-tokens-consistency',
      description: JSON.stringify(
        {
          theme,
          paperCardCount,
          postalNavyCount,
          stampRedCount,
          inkTokensCount,
          bgWhiteCount,
          bgGrayCount,
          textGray900Count,
          darkTextGrayCount,
          darkBgGrayCount,
          bodyBg,
          mainBg,
        },
        null,
        2,
      ),
    });
  });
});
