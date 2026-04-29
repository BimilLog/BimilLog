import { test, expect, type Page, type Route } from '@playwright/test';

// =============================================================================
// /rolling-paper/{nickname} 페이지 TDD 빨강 단계 시나리오
//
// work-queue.md /rolling-paper 섹션 A + B 항목 검증
//
// 작성 규칙:
//   - data-testid 우선 (예: grid-cell, paper-header-action-{name},
//     anonymous-write-helper, paper-empty-state, paper-grid-container 등)
//   - waitForLoadState('networkidle') 사용, time-based wait 금지
//   - 백엔드는 띄워져 있지 않다고 가정 → /api/** 응답 모킹 필수
//   - 비로그인 상태 시나리오 위주 (방문자 시점)
//
// 현재 구현 기준 대부분 시나리오가 실패해야 정상이다 (TDD red).
// =============================================================================

interface VisitMessageDTO {
  decoType: string;
  x: number;
  y: number;
}

interface VisitPaperResultDTO {
  ownerId: number;
  visitMessageDTOList: VisitMessageDTO[];
}

const TARGET_NICKNAME = 'test-user';

const FULL_MESSAGES_FIXTURE: VisitPaperResultDTO = {
  ownerId: 1,
  visitMessageDTOList: [
    { decoType: 'POTATO', x: 0, y: 0 },
    { decoType: 'CARROT', x: 1, y: 0 },
    { decoType: 'CABBAGE', x: 2, y: 0 },
    { decoType: 'TOMATO', x: 3, y: 1 },
    { decoType: 'STRAWBERRY', x: 0, y: 2 },
    { decoType: 'CAT', x: 2, y: 3 },
  ],
};

const EMPTY_PAPER_FIXTURE: VisitPaperResultDTO = {
  ownerId: 1,
  visitMessageDTOList: [],
};

// 방문자 시점 롤링페이퍼 fetch (SSR 직접 백엔드 호출 + 클라이언트 Route Handler 양쪽)
async function mockPaperEndpoints(
  page: Page,
  options: { paper?: VisitPaperResultDTO; status?: number } = {}
) {
  const status = options.status ?? 200;
  const paper = options.paper ?? FULL_MESSAGES_FIXTURE;

  await page.route('**/api/paper/**', async (route: Route) => {
    const url = route.request().url();
    // popular 는 별도 핸들러
    if (url.includes('/api/paper/popular')) {
      await route.fallback();
      return;
    }

    if (status !== 200) {
      await route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify({ success: false, error: '롤링페이퍼 조회 실패' }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: paper }),
    });
  });

  // popular fallback (홈에서만 쓰이지만 안전하게 모킹)
  await page.route('**/api/paper/popular**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: { content: [], nextCursor: null },
      }),
    });
  });
}

// 인증/알림 (비로그인 상태)
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
  await page.route('**/api/friend/**', async (route: Route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({ success: false, error: 'Unauthorized' }),
    });
  });
}

// =============================================================================
// A 항목 시나리오
// =============================================================================

test.describe('/rolling-paper - GridCell 접근성 (A-1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('A-1: 그리드 셀이 <button> 또는 role="button" 이며 aria-label 을 가진다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    const firstCell = page.getByTestId('grid-cell').first();
    await expect(firstCell).toBeVisible();

    // 셀이 button 으로 렌더링되는지 (role="button" 또는 tagName button)
    const isButton = await firstCell.evaluate((el) => {
      const tag = el.tagName.toLowerCase();
      const role = el.getAttribute('role');
      return tag === 'button' || role === 'button';
    });
    expect(isButton).toBe(true);

    const ariaLabel = await firstCell.getAttribute('aria-label');
    expect(ariaLabel).not.toBeNull();
    expect(ariaLabel!.length).toBeGreaterThan(0);
    // 좌표 또는 의미 정보 (예: "1행 3열", "메시지 보기/작성")
    expect(ariaLabel!).toMatch(/(행|열|메시지|위치|페이지)/);
  });

  test('A-1: 그리드 셀이 키보드 Enter 로 클릭 가능하다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 메시지가 있는 셀을 찾기 위해 첫번째 grid-cell 사용
    // 좌표별 testid 가 있다면 (0,0)은 메시지가 있는 셀
    const messageCell = page
      .getByTestId('grid-cell-0-0')
      .or(page.getByTestId('grid-cell').first());

    await expect(messageCell).toBeVisible();

    await messageCell.focus();
    await messageCell.press('Enter');

    // Enter 시 모달이 열려야 한다 (메시지 보기 또는 작성)
    const modal = page
      .getByRole('dialog')
      .or(page.getByText(/메시지\s*(보기|작성)/));
    await expect(modal.first()).toBeVisible({ timeout: 5000 });
  });
});

test.describe('/rolling-paper - 헤더 액션 버튼 44x44 (A-2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('A-2: RollingPaperHeader 액션 버튼들이 모두 44x44 이상이다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 비로그인 시점이라도 KakaoShare/링크 공유는 노출된다.
    // 헤더 액션 testid 명명: paper-header-action-{name}
    const actionTestIds = [
      'paper-header-action-list',
      'paper-header-action-friend',
      'paper-header-action-kakao',
      'paper-header-action-link',
    ];

    let visibleCount = 0;
    for (const testId of actionTestIds) {
      const btn = page.getByTestId(testId).first();
      if (!(await btn.count())) continue;
      if (!(await btn.isVisible().catch(() => false))) continue;
      visibleCount += 1;
      const box = await btn.boundingBox();
      expect(box, `${testId} 의 boundingBox`).not.toBeNull();
      if (box) {
        expect(box.width, `${testId} 너비`).toBeGreaterThanOrEqual(44);
        expect(box.height, `${testId} 높이`).toBeGreaterThanOrEqual(44);
      }
    }

    // 최소 1개 이상 액션 버튼이 노출되어야 (KakaoShare/링크)
    expect(visibleCount).toBeGreaterThan(0);
  });

  test('A-2: 헤더 닉네임 영역의 max-w 가 합리적 (44px 이상)이다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 닉네임 텍스트 (모바일/데스크톱 양쪽 노출되는 h1)
    const titleHeading = page
      .getByRole('heading', { level: 1 })
      .filter({ hasText: new RegExp(`${TARGET_NICKNAME}.*롤링페이퍼`) })
      .first();
    await expect(titleHeading).toBeVisible();

    const box = await titleHeading.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      // truncate 되더라도 최소 44px 이상은 확보되어야 가독성 ok
      expect(box.width).toBeGreaterThanOrEqual(44);
    }
  });
});

test.describe('/rolling-paper - MessageForm 헬퍼 텍스트 (A-3)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('A-3: 메시지 작성 모달 진입 시 "로그인 없이 작성 가능" 헬퍼 텍스트가 노출된다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 빈 셀 클릭 → 메시지 작성 폼 열림
    // (3, 3) 좌표는 모킹 fixture 상 비어있는 셀
    const emptyCell = page
      .getByTestId('grid-cell-3-3')
      .or(page.getByTestId('grid-cell').nth(10));

    await expect(emptyCell.first()).toBeVisible();
    await emptyCell.first().click();

    // 작성 폼 모달이 열리고 헬퍼 텍스트가 보임
    const helper = page.getByTestId('anonymous-write-helper');
    await expect(helper).toBeVisible({ timeout: 5000 });

    const text = await helper.innerText();
    expect(text).toMatch(/로그인\s*없이|익명으로|회원가입\s*없이/);
  });
});

test.describe('/rolling-paper - 빈 상태 CTA (A-4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page, { paper: EMPTY_PAPER_FIXTURE });
  });

  test('A-4: 메시지가 0개일 때 paper-empty-state 빈 상태 안내가 노출된다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    const emptyState = page.getByTestId('paper-empty-state');
    await expect(emptyState).toBeVisible();

    const text = await emptyState.innerText();
    // 큰 안내 + CTA 류 메시지
    expect(text).toMatch(/(첫\s*메시지|아직\s*메시지|비어\s*있어요|메시지를\s*남겨)/);
  });
});

test.describe('/rolling-paper - 모바일 그리드 패딩 (A-5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('A-5: 모바일에서 paper-grid-container 의 좌우 패딩이 작다 (px-4 이하)', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    const gridContainer = page.getByTestId('paper-grid-container');
    await expect(gridContainer).toBeVisible();

    // boundingBox 검증: viewport 너비 375 기준 좌우 합산 패딩이 32px(=px-4×2) 이하
    const box = await gridContainer.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      // 컨테이너의 가로폭이 viewport의 91% 이상이면 패딩이 작은 것 (375 * 0.91 ≈ 341)
      expect(box.width).toBeGreaterThanOrEqual(341);
    }
  });
});

// =============================================================================
// B 항목 시나리오 (선택)
// =============================================================================

test.describe('/rolling-paper - VisitMessage 잠금 뱃지 (B-M1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M1: 방문자 시점의 메시지 셀에 잠금 뱃지/아이콘이 노출된다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 메시지가 있는 셀 (0,0) 에 잠금 표시
    const lockedCell = page
      .getByTestId('grid-cell-locked')
      .or(page.getByTestId('grid-cell-0-0').locator('[data-locked="true"]'))
      .or(page.getByTestId('grid-cell-0-0').locator('svg[aria-label*="잠"]'));

    await expect(lockedCell.first()).toBeVisible();
  });
});

test.describe('/rolling-paper - 카카오 공유 토스트 (B-M2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M2: 카카오 공유 버튼 클릭 후 토스트가 노출된다 (약식)', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // window.Kakao SDK 모킹 (성공 시나리오)
    await page.evaluate(() => {
      (window as unknown as Record<string, unknown>).Kakao = {
        isInitialized: () => true,
        Share: {
          sendDefault: () => Promise.resolve({ success: true }),
        },
      };
    });

    const kakaoBtn = page.getByTestId('paper-header-action-kakao');
    await expect(kakaoBtn).toBeVisible();
    await kakaoBtn.click();

    // 토스트 컨테이너 또는 alert role 에 새 토스트 추가
    const toast = page
      .getByRole('alert')
      .or(page.locator('[data-testid="toast"]'))
      .or(page.getByText(/공유/));

    await expect(toast.first()).toBeVisible({ timeout: 5000 });
  });
});

test.describe('/rolling-paper - 비로그인 친구요청 진입점 (B-M3)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M3: 비로그인 시 헤더에 로그인 안내 진입점이 노출된다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    const loginEntry = page
      .getByTestId('paper-header-login-cta')
      .or(page.getByRole('link', { name: /로그인/ }))
      .or(page.getByText(/친구\s*요청.*로그인|로그인\s*하면\s*친구|로그인하고/));

    await expect(loginEntry.first()).toBeVisible();
  });
});

test.describe('/rolling-paper - 이중 sticky 단일화 (B-M4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M4: AuthHeader 와 RollingPaperHeader 의 sticky 영역이 단일화되어 합산 높이가 100px 이하', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // sticky 또는 fixed position 인 모든 상단 영역의 합산 높이
    const totalStickyHeight = await page.evaluate(() => {
      const all = Array.from(document.querySelectorAll('header, [class*="sticky"]')) as HTMLElement[];
      let total = 0;
      for (const el of all) {
        const cs = window.getComputedStyle(el);
        if (cs.position !== 'sticky' && cs.position !== 'fixed') continue;
        const rect = el.getBoundingClientRect();
        if (rect.top > 200) continue; // 화면 상단(200px 이내)에 위치한 sticky 만
        if (rect.height === 0) continue;
        total += rect.height;
      }
      return total;
    });

    // 단일화 후에는 합산이 100px 이하여야 함 (현재는 두 헤더가 겹쳐 100px 초과)
    expect(totalStickyHeight).toBeLessThanOrEqual(100);
  });
});

test.describe('/rolling-paper - 에러 메시지 raw 좌표 노출 X (B-M5)', () => {
  test('B-M5: 좌표 충돌 에러 토스트에 raw "0~11" 등의 표현이 노출되지 않는다', async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);

    // 메시지 추가 시 충돌 에러를 응답하도록 모킹
    await page.route('**/api/paper', async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            success: false,
            error: 'x는 0~11 사이의 값이어야 합니다.',
          }),
        });
        return;
      }
      await route.fallback();
    });

    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 빈 셀 클릭 → 폼 → 제출
    const emptyCell = page
      .getByTestId('grid-cell-3-3')
      .or(page.getByTestId('grid-cell').nth(10));

    await emptyCell.first().click();

    // 폼 채우기
    await page.getByPlaceholder('익명 닉네임').fill('테스터');
    await page.getByPlaceholder(/마음을 담은/).fill('테스트 메시지입니다.');

    const submitBtn = page.getByRole('button', { name: /메시지 남기기/ });
    await submitBtn.click();

    // 토스트 출현 대기
    const toast = page.getByRole('alert').or(page.locator('[data-testid="toast"]')).first();
    await expect(toast).toBeVisible({ timeout: 5000 });

    const toastText = await toast.innerText();

    // raw 좌표 범위 표현(0~11, 0~9) 노출 X
    expect(toastText).not.toMatch(/0\s*~\s*11/);
    expect(toastText).not.toMatch(/0\s*~\s*9/);
  });
});

test.describe('/rolling-paper - 셀 애니메이션 축소 (B-M6)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M6: 한 셀에 animate-ping 과 animate-bounce 가 동시에 적용되지 않는다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 메시지가 있는 셀 (0,0)
    const messageCell = page
      .getByTestId('grid-cell-0-0')
      .or(page.getByTestId('grid-cell').first());

    await expect(messageCell.first()).toBeVisible();

    // 셀 자체 + 자식들 className 모두 확인
    const hasBothAnimations = await messageCell.first().evaluate((el) => {
      const all = [el, ...Array.from(el.querySelectorAll('*'))];
      const classes = all.map((n) => (n as HTMLElement).className).join(' ');
      const hasPing = /\banimate-ping\b/.test(classes);
      const hasBounce = /\banimate-bounce\b/.test(classes);
      return { hasPing, hasBounce };
    });

    // 둘 다 동시에 있으면 안 됨
    const both = hasBothAnimations.hasPing && hasBothAnimations.hasBounce;
    expect(both).toBe(false);
  });
});

test.describe('/rolling-paper - 토큰 통합 (B-M7)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M7: 페이지 내 그라디언트 버튼 클래스가 토큰(bg-brand-button 등)을 사용한다', async ({ page }) => {
    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    // 빈 셀 → 작성 폼 진입
    const emptyCell = page
      .getByTestId('grid-cell-3-3')
      .or(page.getByTestId('grid-cell').nth(10));
    await emptyCell.first().click();

    const submitBtn = page.getByRole('button', { name: /메시지 남기기/ });
    await expect(submitBtn).toBeVisible();

    const className = (await submitBtn.getAttribute('class')) ?? '';
    // bg-brand-* 토큰 또는 brand prefix 사용
    expect(className).toMatch(/\bbg-brand[-\w]*\b/);
  });
});

test.describe('/rolling-paper - MessageForm 모바일 컴팩트 (B-M8)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockPaperEndpoints(page);
  });

  test('B-M8: 모바일에서 MessageForm 내부 폼이 space-y-3 또는 p-4 등 컴팩트 클래스를 적용한다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto(`/rolling-paper/${TARGET_NICKNAME}`);
    await page.waitForLoadState('networkidle');

    const emptyCell = page
      .getByTestId('grid-cell-3-3')
      .or(page.getByTestId('grid-cell').nth(10));
    await emptyCell.first().click();

    const formRoot = page.getByTestId('message-form-root');
    await expect(formRoot).toBeVisible();

    const className = (await formRoot.getAttribute('class')) ?? '';
    // 모바일에서 space-y-3 또는 p-4 적용
    expect(className).toMatch(/space-y-3|p-4|gap-3/);
  });
});
