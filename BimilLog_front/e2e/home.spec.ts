import { test, expect, type Page, type Route } from '@playwright/test';

// =============================================================================
// /home 페이지 TDD 빨강 단계 시나리오
//
// 작성 규칙:
//   - data-testid 우선 (예: home-hero-cta-primary, popular-paper-row, home-footer-grid)
//   - role/getByText 보조
//   - waitForLoadState('networkidle') 사용, time-based wait 금지
//   - 백엔드는 띄워져 있지 않다고 가정 → /api/** 응답 모킹 필수
//   - 비로그인 상태 시나리오만 다룸
//
// 현재 구현 기준 대부분의 시나리오가 실패해야 정상이다 (TDD red).
// =============================================================================

interface PopularPaperInfo {
  memberId: number;
  memberName: string;
  rank: number;
  popularityScore: number;
  recentMessageCount: number;
}

const POPULAR_PAPERS_FIXTURE: PopularPaperInfo[] = [
  { memberId: 1, memberName: '재익', rank: 1, popularityScore: 12.5, recentMessageCount: 8 },
  { memberId: 2, memberName: '민수', rank: 2, popularityScore: 9.0, recentMessageCount: 5 },
  { memberId: 3, memberName: '지현', rank: 3, popularityScore: 7.3, recentMessageCount: 3 },
  { memberId: 4, memberName: '서연', rank: 4, popularityScore: 5.1, recentMessageCount: 2 },
];

// /api/paper/popular 응답을 200/500 으로 모킹.
async function mockPopularPapers(
  page: Page,
  options: { status?: number; papers?: PopularPaperInfo[] } = {}
) {
  const status = options.status ?? 200;
  const papers = options.papers ?? POPULAR_PAPERS_FIXTURE;

  await page.route('**/api/paper/popular**', async (route: Route) => {
    if (status !== 200) {
      await route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, error: '인기 롤링페이퍼 조회 실패' }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { content: papers, nextCursor: null },
      }),
    });
  });
}

// 인증 관련 API들은 비로그인 상태 (401/빈 응답) 으로 모킹.
async function mockAuthBoundaryEndpoints(page: Page) {
  await page.route('**/api/notification/**', async (route: Route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, error: 'Unauthorized' }),
    });
  });
  await page.route('**/api/member/**', async (route: Route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, error: 'Unauthorized' }),
    });
  });
  await page.route('**/api/auth/**', async (route: Route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, error: 'Unauthorized' }),
    });
  });
}

test.describe('/home - HomeHero (A-1, B-1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page);
  });

  test('A-1: "롤링페이퍼 만들기" CTA 클릭 시 SPA 라우팅으로 /login 이동 (reload 없음)', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // SPA 검증을 위해 페이지에 마커를 심는다. SPA 라우팅이면 마커는 살아있다.
    await page.evaluate(() => {
      (window as unknown as { __spaMarker?: boolean }).__spaMarker = true;
    });

    const primaryCta = page.getByTestId('home-hero-cta-primary');
    await expect(primaryCta).toBeVisible();

    const historyBefore = await page.evaluate(() => window.history.length);
    await primaryCta.click();

    await expect(page).toHaveURL(/\/login/);

    const marker = await page.evaluate(
      () => (window as unknown as { __spaMarker?: boolean }).__spaMarker === true
    );
    expect(marker).toBe(true); // SPA 라우팅이면 새로 fetch 한 게 아니므로 살아있음

    const historyAfter = await page.evaluate(() => window.history.length);
    expect(historyAfter).toBeGreaterThanOrEqual(historyBefore);
  });

  test('A-1: "롤링페이퍼 둘러보기" CTA 클릭 시 SPA 라우팅으로 /visit 이동 (reload 없음)', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    await page.evaluate(() => {
      (window as unknown as { __spaMarker?: boolean }).__spaMarker = true;
    });

    const secondaryCta = page.getByTestId('home-hero-cta-secondary');
    await expect(secondaryCta).toBeVisible();

    await secondaryCta.click();
    await expect(page).toHaveURL(/\/visit/);

    const marker = await page.evaluate(
      () => (window as unknown as { __spaMarker?: boolean }).__spaMarker === true
    );
    expect(marker).toBe(true);
  });

  test('B-1: Hero 영역에 primary CTA 가 시각적으로 1개로 강조된다', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const primaryCta = page.getByTestId('home-hero-cta-primary');
    const secondaryCta = page.getByTestId('home-hero-cta-secondary');

    await expect(primaryCta).toBeVisible();
    await expect(secondaryCta).toBeVisible();

    const primaryVariant = await primaryCta.getAttribute('data-variant');
    const secondaryVariant = await secondaryCta.getAttribute('data-variant');

    expect(primaryVariant).toBe('primary');
    expect(['outline', 'secondary']).toContain(secondaryVariant ?? '');
  });
});

test.describe('/home - PopularPapers (A-2, A-3, A-4, B-4)', () => {
  test('A-2: PopularPapers 행에 "최근 24시간" 또는 "메시지 N개" 라벨이 노출', async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page);

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const firstRow = page.getByTestId('popular-paper-row').first();
    await expect(firstRow).toBeVisible();

    const labelText = await firstRow.innerText();
    expect(labelText).toMatch(/최근 24시간|메시지\s?\d+\s?개/);
  });

  test('A-3: PopularPapers 행 전체 클릭 시 /rolling-paper/{name} 또는 /visit?nickname=...로 이동', async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page);

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const firstRow = page.getByTestId('popular-paper-row').first();
    await expect(firstRow).toBeVisible();

    // 우측에 별도 버튼이 없어야 한다 - 행 자체가 단일 클릭 타겟
    const buttonsInRow = firstRow.locator('button');
    expect(await buttonsInRow.count()).toBe(0);

    await firstRow.click();

    await expect(page).toHaveURL(/\/(rolling-paper\/|visit\?nickname=)/);
  });

  test('A-4: /api/paper/popular 500 응답 시 "다시 불러오기" 또는 "재시도" 버튼 노출', async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page, { status: 500 });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const retryButton = page.getByRole('button', { name: /다시 불러오기|재시도/ });
    await expect(retryButton).toBeVisible();
  });

  test('B-4: 로딩 중 Skeleton 표시 (animate-pulse 또는 skeleton testid)', async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);

    // 응답을 의도적으로 지연시켜 로딩 상태를 노출
    await page.route('**/api/paper/popular**', async (route: Route) => {
      await new Promise((resolve) => setTimeout(resolve, 1500));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { content: POPULAR_PAPERS_FIXTURE, nextCursor: null },
        }),
      });
    });

    await page.goto('/');
    // networkidle 대기 X — 로딩 상태가 보여야 하므로 DOM 로드 직후 검증
    await page.waitForLoadState('domcontentloaded');

    const skeleton = page
      .getByTestId('popular-papers-skeleton')
      .or(page.locator('[data-testid="popular-papers-section"] .animate-pulse').first());

    await expect(skeleton).toBeVisible({ timeout: 5000 });
  });
});

test.describe('/home - HomeFooter (A-5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page);
  });

  test('A-5: 모바일(375)에서 footer 그리드가 grid-cols-2 (또는 sm:grid-cols-3) 클래스를 가지며 5열 강제로 깨지지 않음', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const footerGrid = page.getByTestId('home-footer-grid');
    await expect(footerGrid).toBeVisible();

    const className = (await footerGrid.getAttribute('class')) ?? '';

    // 5열 강제 X
    expect(className).not.toMatch(/(^|\s)grid-cols-5(\s|$)/);
    // 모바일 우선 grid 적용
    expect(className).toMatch(/grid-cols-2/);
    expect(className).toMatch(/sm:grid-cols-3/);
    expect(className).toMatch(/lg:grid-cols-5/);
  });

  test('A-5: footer 헤더 텍스트("개인정보처리방침")가 줄바꿈으로 깨지지 않는다 (모바일)', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const privacyLink = page.getByRole('link', { name: '개인정보처리방침' });
    await expect(privacyLink).toBeVisible();

    // 단일 라인으로 렌더링되는지 검증 (높이가 line-height 1.x 수준)
    const box = await privacyLink.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      // 한 줄짜리 링크는 일반적으로 30px 이하 — 줄바꿈 시 40px 이상이 되어 fail
      expect(box.height).toBeLessThan(36);
    }
  });
});

test.describe('/home - 헤더 토글 (A-6)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page);
  });

  test('A-6: 모바일에서 햄버거 메뉴 버튼은 44x44 이상이고 한국어 aria-label("메뉴")을 가진다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const toggle = page.getByRole('button', { name: '메뉴' });
    await expect(toggle).toBeVisible();

    const box = await toggle.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      expect(box.width).toBeGreaterThanOrEqual(44);
      expect(box.height).toBeGreaterThanOrEqual(44);
    }
  });
});

test.describe('/home - HomeFeatures (B-2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPopularPapers(page);
  });

  test('B-2: HomeFeatures 가 step 형태(1, 2, 3 또는 "1단계, 2단계, 3단계") 텍스트를 노출한다', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    const features = page.getByTestId('home-features');
    await expect(features).toBeVisible();

    const text = await features.innerText();

    // 숫자 step 또는 "N단계" 형태 모두 허용
    const hasStepNumbers = /(^|[\s가-힣])1(단계|\.|\)|\s)/.test(text)
      && /(^|[\s가-힣])2(단계|\.|\)|\s)/.test(text)
      && /(^|[\s가-힣])3(단계|\.|\)|\s)/.test(text);

    expect(hasStepNumbers).toBe(true);
  });
});
