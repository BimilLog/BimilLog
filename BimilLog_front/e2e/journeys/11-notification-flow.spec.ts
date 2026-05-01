import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-11-notification-flow';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 background polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

type BellBranch =
  | 'bell-visible' // NotificationBell 마운트 (canUseNotifications true — 인증/SSE 가용)
  | 'bell-hidden'  // 비인증 또는 canUseNotifications false → null 렌더
  | 'no-header';   // 헤더 자체 마운트 실패

/**
 * 헤더의 NotificationBell 가시성 분기 판정.
 *
 *  - notification-bell.tsx: `if (!canUseNotifications) return null;`
 *  - canUseNotifications = isAuthenticated && canConnectSSE()
 *  - 비인증 컨텍스트에서는 NotificationBell 자체가 DOM 에 없음.
 *
 *  e2e 시드 토큰이 없으면 보통 'bell-hidden' 분기로 빠짐.
 *  fail-soft 로 분기를 annotation 에 남기고 캡처는 진행한다.
 */
async function detectBellBranch(page: Page): Promise<BellBranch> {
  // 1) 헤더 마운트 확인 — AuthHeader 또는 layout/Header 영역
  const headerVisible = await page
    .locator('header')
    .first()
    .isVisible({ timeout: 3000 })
    .catch(() => false);
  if (!headerVisible) return 'no-header';

  // 2) 알림 벨 버튼 마운트 — aria-label="알림"
  const bellVisible = await page
    .getByRole('button', { name: /^알림$/ })
    .first()
    .isVisible({ timeout: 1500 })
    .catch(() => false);

  return bellVisible ? 'bell-visible' : 'bell-hidden';
}

/**
 * 알림 벨 클릭 → popover/drawer 열기 시도.
 * bell-hidden 분기면 false 반환 (fail-soft).
 */
async function tryOpenBellPanel(page: Page): Promise<boolean> {
  const bell = page.getByRole('button', { name: /^알림$/ }).first();
  if (!(await bell.isVisible({ timeout: 1500 }).catch(() => false))) {
    return false;
  }
  await bell.click({ trial: false }).catch(() => undefined);
  await page.waitForTimeout(700);

  // popover 컨테이너 (#notification-popover) 가 mount 되었는지 확인
  const panel = page.locator('#notification-popover').first();
  return await panel.isVisible({ timeout: 2500 }).catch(() => false);
}

/**
 * round-11-notification-flow iter-1
 *  - 헤더 NotificationBell 자체는 (protected) 라우트가 아니라 모든 페이지 헤더에 노출.
 *  - 단, `canUseNotifications` 가드로 인증 사용자에게만 보임.
 *  - **backend 미가용 + 비인증 컨텍스트** 가 디폴트 환경:
 *    - bell-hidden 분기 → 헤더 base 캡처는 의미 있음 (헤더 일관성, 다크 토큰 회귀)
 *    - bell-visible 분기 → 라운드 11 전체 회귀 가능 (시드 토큰 있을 때)
 *  - fail-soft 로 모든 step 캡처 진행.
 *
 * 캡처 단계 (8 step × 4 프로젝트 = 32 매트릭스):
 *  step-01-header-bell-base       홈 진입 → 헤더 + (bell-visible 시) 알림 벨
 *  step-02-bell-clicked-panel     벨 클릭 → popover (desktop) / drawer (mobile)
 *  step-03-empty-state-paper      빈 상태 메타포 ("받은 편지가 없어요" + MailOpen)
 *  step-04-aria-dialog-attrs      ARIA 속성 검증 (aria-haspopup/expanded/role=dialog)
 *  step-05-dark-token-paper-card  다크 토큰 일관성 (paper-card / paper-soft / postal-navy)
 *  step-06-mobile-drawer-or-popover  뷰포트별 패널 분기 — mobile=drawer / desktop=popover
 *  step-07-icon-types             알림 종류별 아이콘 (FRIEND/PAPER/POST/COMMENT) — 시드 부족이면 빈 상태
 *  step-08-delete-all-confirm     "전체 삭제" → ConfirmModal trigger 시도 — 시드 부족이면 트리거 부재 캡처
 */
test.describe('round-11-notification-flow', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 라운드 11 도 시드 토큰 없는 fail-soft 검증.
    // canUseNotifications = isAuthenticated && canConnectSSE() 이므로
    // 비인증 시 NotificationBell 자체가 null 렌더.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: 홈 진입 → 헤더 NotificationBell 가시 (bell-visible / bell-hidden)
  // ============================================================
  test('step-01-header-bell-base', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);

    // 헤더 영역으로 스크롤 (홈 hero 가 길어서 벨이 viewport 밖일 수 있음)
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(200);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-header-bell-base',
    );

    test.info().annotations.push({
      type: 'bell-branch',
      description: `branch=${branch} url=${page.url()}`,
    });

    // 인터랙션 트리거 스캔 — bell-visible 분기일 때만 의미 있음
    if (branch === 'bell-visible') {
      const triggers = await scanInteractionTriggers(page);
      test.info().annotations.push({
        type: 'header-triggers',
        description: JSON.stringify(triggers, null, 2),
      });
    }

    // sanity: 페이지 자체는 mount 됨
    expect(page.url()).toMatch(/localhost:3000\/?(\?.*)?$/);
  });

  // ============================================================
  // step-02: 알림 버튼 클릭 → popover (desktop) 또는 drawer (mobile)
  //  - bell-visible 분기에서만 의미 있음
  //  - bell-hidden 분기여도 헤더 영역 캡처는 보존
  // ============================================================
  test('step-02-bell-clicked-panel', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);
    const opened = await tryOpenBellPanel(page);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-bell-clicked-panel',
    );

    test.info().annotations.push({
      type: 'panel-open',
      description: `branch=${branch} opened=${opened}`,
    });
  });

  // ============================================================
  // step-03: 빈 상태 메타포 ("받은 편지가 없어요" + MailOpen 아이콘)
  //  - frontend-changes.md: NotificationList 빈 상태 카피 + MailOpen 메타포
  //  - 백엔드 미가용 → 알림 0건 → 빈 상태 분기에 진입
  // ============================================================
  test('step-03-empty-state-paper', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);
    const opened = await tryOpenBellPanel(page);

    let emptyMatched = false;
    if (opened) {
      const empty = page
        .getByText(/받은 편지가 없어요|새 알림이 오면 여기로 도착해요/)
        .first();
      emptyMatched = await empty.isVisible({ timeout: 2000 }).catch(() => false);
      if (emptyMatched) {
        await empty.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-empty-state-paper',
    );

    test.info().annotations.push({
      type: 'empty-state',
      description: `branch=${branch} opened=${opened} emptyMatched=${emptyMatched}`,
    });
  });

  // ============================================================
  // step-04: ARIA dialog 속성 검증 (F-1111)
  //  - 트리거 Button: aria-haspopup="dialog" + aria-expanded={isOpen} + aria-controls
  //  - popover/drawer: id="notification-popover" + role="dialog" + aria-labelledby
  //  - bell-hidden 분기여도 캡처는 보존, 검증은 fail-soft.
  // ============================================================
  test('step-04-aria-dialog-attrs', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);

    // ARIA 속성 검증 (bell-visible 분기일 때만 의미 있음)
    let ariaHaspopup: string | null = null;
    let ariaExpandedClosed: string | null = null;
    let ariaControls: string | null = null;
    let ariaExpandedOpened: string | null = null;
    let dialogRole: string | null = null;
    let dialogLabelledby: string | null = null;

    if (branch === 'bell-visible') {
      const bell = page.getByRole('button', { name: /^알림$/ }).first();
      ariaHaspopup = await bell.getAttribute('aria-haspopup').catch(() => null);
      ariaExpandedClosed = await bell.getAttribute('aria-expanded').catch(() => null);
      ariaControls = await bell.getAttribute('aria-controls').catch(() => null);

      // 패널 열고 aria-expanded 변동 확인
      const opened = await tryOpenBellPanel(page);
      if (opened) {
        const bellAfter = page.getByRole('button', { name: /^알림$/ }).first();
        ariaExpandedOpened = await bellAfter.getAttribute('aria-expanded').catch(() => null);

        const dialog = page.locator('#notification-popover').first();
        dialogRole = await dialog.getAttribute('role').catch(() => null);
        dialogLabelledby = await dialog.getAttribute('aria-labelledby').catch(() => null);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-aria-dialog-attrs',
    );

    test.info().annotations.push({
      type: 'aria-dialog',
      description: JSON.stringify(
        {
          branch,
          trigger: { ariaHaspopup, ariaExpandedClosed, ariaControls, ariaExpandedOpened },
          dialog: { dialogRole, dialogLabelledby },
        },
        null,
        2,
      ),
    });
  });

  // ============================================================
  // step-05: 다크 토큰 일관성 (paper-card / paper-soft / postal-navy)
  //  - 다크 모드 프로젝트(*-dark) 에서 popover/drawer 배경이 paper-card 톤인지 회귀.
  //  - 라이트 모드에서도 paper-card/95 토큰이 적용되어 다크와 명도 차 일관 검증.
  //  - bell-hidden 분기여도 헤더 자체의 다크 토큰 회귀 시각 확인 가능.
  // ============================================================
  test('step-05-dark-token-paper-card', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);
    const opened = await tryOpenBellPanel(page);

    // popover/drawer 가 열려있다면 헤더 영역으로 스크롤
    if (opened) {
      const heading = page.locator('#notification-heading').first();
      await heading.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(200);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-dark-token-paper-card',
    );

    test.info().annotations.push({
      type: 'dark-token',
      description: `branch=${branch} opened=${opened} theme=${themeFromProjectName(testInfo.project.name)}`,
    });
  });

  // ============================================================
  // step-06: 뷰포트별 패널 분기 — mobile-375 → drawer / desktop-1280 → popover
  //  - useMediaQuery("(max-width: 767px)") 기준
  //  - mobile-375 프로젝트: NotificationDrawer (Flowbite Drawer, position="bottom", 80vh)
  //  - desktop-1280 프로젝트: NotificationPopover (320px Card, fixed top/left)
  // ============================================================
  test('step-06-mobile-drawer-or-popover', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);
    const opened = await tryOpenBellPanel(page);

    const vp = viewportFromProjectName(testInfo.project.name);
    const expectedKind = vp === 'mobile-375' ? 'drawer' : 'popover';

    let actualKind = 'unknown';
    if (opened) {
      // Flowbite Drawer 의 outer wrapper 가 [data-testid="flowbite-drawer"] 또는
      // class 에 'drawer' 포함. 단순히 #notification-popover 의 부모 체인을 본다.
      const isDrawer = await page
        .locator('[role="dialog"][aria-modal="true"]#notification-popover')
        .first()
        .isVisible({ timeout: 1000 })
        .catch(() => false);
      const isPopover = await page
        .locator('[role="dialog"][aria-modal="false"]#notification-popover')
        .first()
        .isVisible({ timeout: 1000 })
        .catch(() => false);
      if (isDrawer) actualKind = 'drawer';
      else if (isPopover) actualKind = 'popover';
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-mobile-drawer-or-popover',
    );

    test.info().annotations.push({
      type: 'panel-kind',
      description: `branch=${branch} opened=${opened} viewport=${vp} expected=${expectedKind} actual=${actualKind}`,
    });
  });

  // ============================================================
  // step-07: 알림 종류별 아이콘 (FRIEND/PAPER/POST_FEATURED/COMMENT/ADMIN)
  //  - F-1103 핵심: NotificationItem.getNotificationIcon switch 의 FRIEND case 추가
  //  - 시드 데이터가 없으면 빈 상태 fallback 으로 캡처.
  //  - 아이콘 5종 동시 노출 검증은 시드 모드에서만 가능 (e2e-result.md 에 기록).
  // ============================================================
  test('step-07-icon-types', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);
    const opened = await tryOpenBellPanel(page);

    let itemCount = 0;
    if (opened) {
      // NotificationItem 은 list 본문에서 div role 등으로 렌더 — 일단 알림 카드 수 측정.
      // 시드 부족 시 0건이며 빈 상태 캡처가 step-03 와 일관.
      itemCount = await page
        .locator('#notification-popover [role="button"]')
        .count()
        .catch(() => 0);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-icon-types',
    );

    test.info().annotations.push({
      type: 'icon-types',
      description: `branch=${branch} opened=${opened} itemCount=${itemCount}`,
    });
  });

  // ============================================================
  // step-08: 전체 삭제 confirm modal 시뮬 (F-1102)
  //  - notifications.length === 0 이면 "전체 삭제" 버튼 자체가 미렌더 (NotificationList line 107).
  //  - 시드 부족 시 trigger 부재만 캡처 (변경된 ConfirmModal 카피 회귀는 시드 모드에서 검증).
  // ============================================================
  test('step-08-delete-all-confirm', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page, 1500);

    const branch = await detectBellBranch(page);
    const opened = await tryOpenBellPanel(page);

    let triggerVisible = false;
    let modalVisible = false;
    if (opened) {
      const deleteAllBtn = page
        .getByRole('button', { name: /전체\s*삭제/ })
        .first();
      triggerVisible = await deleteAllBtn.isVisible({ timeout: 1500 }).catch(() => false);

      if (triggerVisible) {
        await deleteAllBtn.click().catch(() => undefined);
        await page.waitForTimeout(500);

        // ConfirmModal 의 헤딩 — "받은 편지함을 비울까요?"
        const modalHeading = page
          .getByText(/받은 편지함을 비울까요/)
          .first();
        modalVisible = await modalHeading.isVisible({ timeout: 2000 }).catch(() => false);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-delete-all-confirm',
    );

    test.info().annotations.push({
      type: 'delete-all-confirm',
      description: `branch=${branch} opened=${opened} triggerVisible=${triggerVisible} modalVisible=${modalVisible}`,
    });
  });
});
