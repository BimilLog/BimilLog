import { test, type Route } from '@playwright/test';
import path from 'node:path';

const OUTPUT_DIR = process.env.SCREENSHOT_OUT_DIR
  ? path.resolve(process.env.SCREENSHOT_OUT_DIR)
  : path.resolve(
      process.cwd(),
      '..',
      'docs',
      'superpowers',
      'multiagent',
      'screenshots'
    );

test.describe.configure({ mode: 'serial' });

test.describe('수정된 페이지 스크린샷', () => {
  test('home 전체 페이지', async ({ page }, testInfo) => {
    await page.goto('/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await page.screenshot({
      path: path.join(OUTPUT_DIR, `home-${testInfo.project.name}.png`),
      fullPage: true,
    });
  });

  test('board 전체 페이지', async ({ page }, testInfo) => {
    await page.goto('/board', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await page.screenshot({
      path: path.join(OUTPUT_DIR, `board-${testInfo.project.name}.png`),
      fullPage: true,
    });
  });

  test('paper - 메시지 채워진 상태', async ({ page }, testInfo) => {
    const fixture = {
      success: true,
      data: {
        ownerId: 1,
        visitMessageDTOList: [
          { decoType: 'POTATO', x: 0, y: 0 },
          { decoType: 'CARROT', x: 1, y: 0 },
          { decoType: 'CABBAGE', x: 2, y: 0 },
          { decoType: 'TOMATO', x: 3, y: 1 },
          { decoType: 'STRAWBERRY', x: 0, y: 2 },
          { decoType: 'CAT', x: 2, y: 3 },
          { decoType: 'POTATO', x: 5, y: 4 },
          { decoType: 'CABBAGE', x: 7, y: 5 },
          { decoType: 'TOMATO', x: 9, y: 6 },
        ],
      },
    };
    await page.route('**/api/paper/**', async (route: Route) => {
      const url = route.request().url();
      if (url.includes('/popular')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: [], nextCursor: null } }),
        });
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(fixture),
      });
    });
    await page.goto('/rolling-paper/screenshot-user', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await page.screenshot({
      path: path.join(OUTPUT_DIR, `paper-${testInfo.project.name}.png`),
      fullPage: true,
    });
  });

  test('paper - 빈 상태', async ({ page }, testInfo) => {
    await page.route('**/api/paper/**', async (route: Route) => {
      const url = route.request().url();
      if (url.includes('/popular')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true, data: { content: [], nextCursor: null } }),
        });
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { ownerId: 2, visitMessageDTOList: [] },
        }),
      });
    });
    await page.goto('/rolling-paper/empty-user', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await page.screenshot({
      path: path.join(OUTPUT_DIR, `paper-empty-${testInfo.project.name}.png`),
      fullPage: true,
    });
  });
});
