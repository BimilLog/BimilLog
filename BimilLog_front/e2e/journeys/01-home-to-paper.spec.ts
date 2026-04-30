import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-1-home-to-paper';
const ITER = Number(process.env.UIUX_ITER || '1');

const SEED_NICKNAME = process.env.UIUX_SEED_NICKNAME || 'seedUser';
const HAS_SEED = process.env.UIUX_HAS_SEED === '1';

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 백그라운드 polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

test.describe('round-1-home-to-paper', () => {
  test.beforeEach(async ({ page }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
  });

  // -------- step-01: 홈 진입 (비인증) --------
  test('step-01-home-base', async ({ page }, testInfo) => {
    await page.goto('/');
    await waitSettled(page);

    // h1 확인 (비인증 사용자가 보는 메인 카피)
    await expect(page.getByRole('heading', { level: 1 })).toContainText('익명으로');

    // 인기 페이퍼 섹션은 데스크톱에서만 노출 (`lg:w-[400px]`)
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp === 'desktop-1280') {
      await expect(page.locator('[data-testid="popular-papers-section"]')).toBeVisible();
    }

    await captureFullPage(page, testInfo, { roundDir: ROUND_DIR, iter: ITER }, 'step-01-home-base');

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'home-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // -------- step-01-int-01: 데스크톱 — 인기 페이퍼 카드 hover --------
  test('step-01-int-01-popular-card-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, '인기 페이퍼 섹션은 데스크톱(lg) 이상에서만 노출');
      return;
    }

    await page.goto('/');
    await waitSettled(page);

    const firstCard = page.locator('[data-testid="popular-paper-row"]').first();
    const cardCount = await firstCard.count();
    if (cardCount === 0) {
      test.skip(true, '인기 페이퍼 카드 없음 (시드 데이터 부재)');
      return;
    }

    await firstCard.hover();
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-01-popular-card-hover',
    );
  });

  // -------- step-02: 페이퍼 페이지 (시드 미구축 시 not-found 화면) --------
  test('step-02-paper-empty', async ({ page }, testInfo) => {
    await page.goto(`/rolling-paper/${encodeURIComponent(SEED_NICKNAME)}`);
    await waitSettled(page);

    // SSR initialPaperData 미존재 시 클라이언트 fetch 가 진행되며 잠시 로딩이 노출된다.
    // paper-empty-state(빈 상태) 또는 not-found 텍스트("롤링페이퍼를 찾을 수 없습니다") 중
    // 먼저 도착하는 것을 기다린다. 둘 다 안 떠도 5초 후 현재 상태 그대로 캡처한다 (로딩 화면도 가치 있음).
    await Promise.race([
      page.locator('[data-testid="paper-empty-state"]').waitFor({ timeout: 5000 }).catch(() => null),
      page.getByText('롤링페이퍼를 찾을 수 없습니다').waitFor({ timeout: 5000 }).catch(() => null),
      page.getByText('총 ', { exact: false }).waitFor({ timeout: 5000 }).catch(() => null),
    ]);
    await page.waitForTimeout(400);

    // 시드 미구축이면 "롤링페이퍼를 찾을 수 없습니다" not-found 화면이 노출됨
    // 시드가 있으면 paper-empty-state 가 노출됨
    // 두 케이스 모두 캡처 (실 사용자가 보는 화면)

    const stepName = HAS_SEED ? 'step-02-paper-empty' : 'step-02-paper-not-found';

    await captureFullPage(page, testInfo, { roundDir: ROUND_DIR, iter: ITER }, stepName);

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'paper-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // -------- step-02-int-01: 페이퍼 빈 상태 CTA — "첫 메시지 남기기" 클릭 후 모달 --------
  test('step-02-int-01-empty-cta-modal', async ({ page }, testInfo) => {
    if (!HAS_SEED) {
      test.skip(true, 'baseline 시드 미구축 — paper-empty-state 미노출 (not-found 화면)');
      return;
    }

    await page.goto(`/rolling-paper/${encodeURIComponent(SEED_NICKNAME)}`);
    await waitSettled(page);

    const cta = page.locator('[data-testid="paper-empty-state"] button:has-text("첫 메시지 남기기")');
    if ((await cta.count()) === 0) {
      test.skip(true, '비-소유자 빈 상태 CTA 미노출 (소유자 흐름이거나 메시지 존재)');
      return;
    }

    await cta.click();
    // Flowbite Modal 오픈 대기 — F-001 변경 후 role=dialog 보장
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5000 });

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-message-form-open',
    );
  });

  // -------- step-03/04: 시드 의존 (인증 + 본인 페이퍼 + 메시지 작성/제출) --------
  test('step-03-message-form-open-authenticated', async () => {
    test.skip(true, 'baseline 시드 미구축 (인증 + 본인 페이퍼 필요)');
  });

  test('step-04-message-submitted-toast', async () => {
    test.skip(true, 'baseline 시드 미구축 (메시지 작성 + 토스트 검증 필요)');
  });

  // -------- step-Header-01: 알림 드롭다운 (인증 사용자 전용 — 시드 미구축이므로 skip) --------
  test('step-header-01-notification-dropdown', async () => {
    test.skip(true, 'baseline 시드 미구축 — 비인증 사용자에게는 알림 종이 노출되지 않음');
  });

  // -------- step-Header-02: 사용자 메뉴 드롭다운 (인증 사용자 전용 — skip) --------
  test('step-header-02-user-menu-dropdown', async () => {
    test.skip(true, 'baseline 시드 미구축 — 비인증 사용자에게는 Avatar 가 노출되지 않음');
  });
});
