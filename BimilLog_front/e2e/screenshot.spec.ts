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

  // 추가 페이지 (라운드 4: 전수 점검)
  const SIMPLE_ROUTES: { name: string; url: string; wait?: number }[] = [
    { name: 'login', url: '/login' },
    { name: 'signup', url: '/signup' },
    { name: 'visit', url: '/visit' },
    { name: 'suggest', url: '/suggest' },
    { name: 'privacy', url: '/privacy' },
    { name: 'terms', url: '/terms' },
    { name: 'install', url: '/install' },
    { name: 'not-found', url: '/__nonexistent_route_for_screenshot__' },
    { name: 'board-write', url: '/board/write' },
  ];

  for (const route of SIMPLE_ROUTES) {
    test(`${route.name} 페이지`, async ({ page }, testInfo) => {
      await page.goto(route.url, { waitUntil: 'networkidle' });
      await page.waitForTimeout(route.wait ?? 1500);
      await page.screenshot({
        path: path.join(OUTPUT_DIR, `${route.name}-${testInfo.project.name}.png`),
        fullPage: true,
      });
    });
  }

  // 게시글 상세 (post-detail) — mock 으로 안정 캡처
  test('board-post-detail', async ({ page }, testInfo) => {
    const fixturePost = {
      success: true,
      data: {
        id: 999,
        title: '디자인 평가용 mock 게시글입니다',
        content:
          '<p>이것은 디자인 캡처를 위한 임시 게시글입니다.</p><p>본문 단락 가독성과 카드 톤, 댓글 영역 디자인을 평가합니다.</p>',
        memberId: 1,
        memberName: '시드유저',
        viewCount: 123,
        likeCount: 7,
        commentCount: 3,
        createdAt: '2026-04-28T10:00:00Z',
        updatedAt: '2026-04-28T10:00:00Z',
        weekly: false,
        legend: false,
        notice: false,
        liked: false,
      },
    };
    const fixtureComments = {
      success: true,
      data: [
        {
          id: 1,
          postId: 999,
          memberId: 2,
          memberName: '밝은하늘',
          content: '디자인 정말 좋아졌네요!',
          likeCount: 2,
          createdAt: '2026-04-28T10:30:00Z',
          parentId: null,
          deleted: false,
        },
        {
          id: 2,
          postId: 999,
          memberId: 3,
          memberName: '감자도리',
          content: '편지 컨셉 마음에 듭니다.',
          likeCount: 0,
          createdAt: '2026-04-28T11:00:00Z',
          parentId: null,
          deleted: false,
        },
      ],
    };
    await page.route('**/api/post/999**', async (route: Route) => {
      const url = route.request().url();
      if (url.includes('/comment')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(fixtureComments),
        });
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(fixturePost),
      });
    });
    await page.route('**/api/comment**', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(fixtureComments),
      });
    });
    await page.goto('/board/post/999', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    await page.screenshot({
      path: path.join(OUTPUT_DIR, `board-post-detail-${testInfo.project.name}.png`),
      fullPage: true,
    });
  });
});
