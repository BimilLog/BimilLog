import { test, expect } from '@playwright/test';
import { setTheme, themeFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-1-home-to-paper';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

test.describe('home-to-paper', () => {
  test.beforeEach(async ({ page }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
  });

  test('step-01-home-base', async ({ page }, testInfo) => {
    await page.goto('/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    await captureFullPage(page, testInfo, { roundDir: ROUND_DIR, iter: ITER }, 'step-01-home-base');
    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({ type: 'triggers', description: JSON.stringify(triggers) });
  });

  test('step-02-paper-empty', async ({ page }, testInfo) => {
    await page.goto('/rolling-paper/seed-empty-user', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    await captureFullPage(page, testInfo, { roundDir: ROUND_DIR, iter: ITER }, 'step-02-paper-empty');
  });

  // step-03/04 는 인증·시드 의존이므로 시운전 단계에서는 건너뛴다.
  // backend 에이전트가 baseline 시드 + JWT 발급 후 채워질 spec.
});
