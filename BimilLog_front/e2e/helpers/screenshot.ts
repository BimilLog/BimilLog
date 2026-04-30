import path from 'node:path';
import type { Page, TestInfo } from '@playwright/test';
import { themeFromProjectName, viewportFromProjectName } from './theme';

const ROOT = process.env.SCREENSHOT_OUT_DIR ||
  path.resolve(process.cwd(), '..', 'docs', 'superpowers', 'multiagent', '__last_run__');

export interface CaptureContext {
  roundDir: string;
  iter: number;
}

/**
 * 캡처 매트릭스: light/dark × mobile-375/desktop-1280
 * 경로: {ROOT}/{roundDir}/iterations/iter-{i}/screenshots/{theme}/{vp}/{stepName}/viewport.png
 */
export function screenshotPath(
  testInfo: TestInfo,
  ctx: CaptureContext,
  stepName: string,
): string {
  const theme = themeFromProjectName(testInfo.project.name);
  const vp = viewportFromProjectName(testInfo.project.name);
  return path.join(
    ROOT,
    ctx.roundDir,
    'iterations',
    `iter-${ctx.iter}`,
    'screenshots',
    theme,
    vp,
    stepName,
    'viewport.png',
  );
}

export async function captureFullPage(
  page: Page,
  testInfo: TestInfo,
  ctx: CaptureContext,
  stepName: string,
): Promise<string> {
  const out = screenshotPath(testInfo, ctx, stepName);
  await page.screenshot({ path: out, fullPage: true });
  return out;
}
