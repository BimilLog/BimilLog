import { test, expect, type Page, type Route } from '@playwright/test';

// =============================================================================
// /board 페이지 TDD 빨강 단계 시나리오
//
// work-queue.md /board 섹션 A + B 항목 검증
//
// 작성 규칙:
//   - data-testid 우선 (board-pagination, search-result-count, board-mobile-card,
//     anonymous-write-info-modal, write-post-button, board-empty-state 등)
//   - waitForLoadState('networkidle') 사용, time-based wait 금지
//   - 백엔드는 띄워져 있지 않다고 가정 → /api/** 응답 모킹 필수
//   - 비로그인 상태 시나리오만 다룸
//
// 현재 구현 기준 대부분 시나리오가 실패해야 정상이다 (TDD red).
// =============================================================================

interface SimplePost {
  id: number;
  memberId: number;
  memberName: string;
  title: string;
  commentCount: number;
  likeCount: number;
  viewCount: number;
  createdAt: string;
  weekly: boolean;
  legend: boolean;
  notice: boolean;
}

// 일반 게시글 fixture - 페이지네이션 검증을 위해 충분한 수량을 만든다 (검색 케이스에서 multi-page 가능하도록)
function makePost(id: number, overrides: Partial<SimplePost> = {}): SimplePost {
  return {
    id,
    memberId: 100 + id,
    memberName: `작성자${id}`,
    title: `테스트 게시글 ${id}`,
    commentCount: id % 5,
    likeCount: id * 2,
    viewCount: id * 10,
    createdAt: '2026-04-20T10:00:00Z',
    weekly: false,
    legend: false,
    notice: false,
    ...overrides,
  };
}

const POSTS_FIXTURE: SimplePost[] = Array.from({ length: 20 }, (_, i) => makePost(i + 1));

const NOTICE_FIXTURE: SimplePost[] = [
  makePost(1001, { title: '공지사항 1', memberName: '관리자', notice: true }),
];

// CursorPageResponse (일반 목록용)
function cursorPage<T>(content: T[]) {
  return {
    content,
    nextCursor: null,
    hasNext: false,
  };
}

// PageResponse (인기글, 검색용)
function offsetPage<T>(
  content: T[],
  options: { totalElements?: number; totalPages?: number; number?: number; size?: number } = {}
) {
  const totalElements = options.totalElements ?? content.length;
  const size = options.size ?? 20;
  const totalPages = options.totalPages ?? Math.max(1, Math.ceil(totalElements / size));
  return {
    content,
    totalElements,
    totalPages,
    number: options.number ?? 0,
    size,
    first: (options.number ?? 0) === 0,
    last: (options.number ?? 0) >= totalPages - 1,
    numberOfElements: content.length,
    empty: content.length === 0,
  };
}

// 게시판 데이터 모킹 (일반 목록 + 인기글 + 공지)
async function mockBoardEndpoints(
  page: Page,
  options: {
    posts?: SimplePost[];
    noticePosts?: SimplePost[];
    realtimePosts?: SimplePost[];
    weeklyPosts?: SimplePost[];
    legendPosts?: SimplePost[];
  } = {}
) {
  const posts = options.posts ?? POSTS_FIXTURE;
  const noticePosts = options.noticePosts ?? NOTICE_FIXTURE;
  const realtimePosts = options.realtimePosts ?? POSTS_FIXTURE.slice(0, 5);
  const weeklyPosts = options.weeklyPosts ?? POSTS_FIXTURE.slice(0, 5);
  const legendPosts = options.legendPosts ?? POSTS_FIXTURE.slice(0, 5);

  // 검색은 별도 핸들러에서 처리
  await page.route('**/api/post/search**', async (route: Route) => {
    // 기본은 빈 결과 - 각 시나리오에서 override 가능
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: offsetPage<SimplePost>([], { totalElements: 0 }),
      }),
    });
  });

  await page.route('**/api/post/notice**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: offsetPage(noticePosts, { totalElements: noticePosts.length }),
      }),
    });
  });

  await page.route('**/api/post/realtime**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: offsetPage(realtimePosts, { totalElements: realtimePosts.length }),
      }),
    });
  });

  await page.route('**/api/post/weekly**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: offsetPage(weeklyPosts, { totalElements: weeklyPosts.length }),
      }),
    });
  });

  await page.route('**/api/post/legend**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: offsetPage(legendPosts, { totalElements: legendPosts.length }),
      }),
    });
  });

  // 일반 목록 (search/notice/realtime/weekly/legend 보다 뒤에 등록되어야 specific match 우선)
  await page.route('**/api/post**', async (route: Route) => {
    const url = route.request().url();
    // 위 specific endpoint들은 위에서 잡혔지만 안전을 위해 한번 더 분기
    if (
      url.includes('/api/post/search') ||
      url.includes('/api/post/notice') ||
      url.includes('/api/post/realtime') ||
      url.includes('/api/post/weekly') ||
      url.includes('/api/post/legend')
    ) {
      // fallthrough - 이미 잡힌 경우 이쪽으로 안 옴
      await route.fallback();
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: cursorPage(posts),
      }),
    });
  });
}

// 검색 결과 모킹 (totalElements 포함)
async function mockSearchEndpoint(
  page: Page,
  options: {
    content?: SimplePost[];
    totalElements?: number;
    totalPages?: number;
  } = {}
) {
  const content = options.content ?? POSTS_FIXTURE.slice(0, 7);
  const totalElements = options.totalElements ?? content.length;
  const totalPages = options.totalPages ?? Math.max(1, Math.ceil(totalElements / 20));

  await page.route('**/api/post/search**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: offsetPage(content, { totalElements, totalPages, size: 20 }),
      }),
    });
  });
}

// 인증 관련 (비로그인 상태)
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

// =============================================================================
// A 항목 시나리오
// =============================================================================

test.describe('/board - 페이지네이션 터치 타겟 (A-1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('A-1: 모바일에서 검색 페이지네이션 버튼은 44x44 이상이다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    // 페이지네이션이 노출되려면 검색 모드 + 다중 페이지여야 함
    await mockSearchEndpoint(page, {
      content: POSTS_FIXTURE.slice(0, 20),
      totalElements: 75,
      totalPages: 4,
    });

    await page.goto('/board?q=test');
    await page.waitForLoadState('networkidle');

    const pagination = page.getByTestId('board-pagination');
    await expect(pagination).toBeVisible();

    // 페이지 버튼들 (이전/다음/숫자) 모두 44x44 이상
    const buttons = pagination.locator('button, a');
    const count = await buttons.count();
    expect(count).toBeGreaterThan(0);

    for (let i = 0; i < count; i++) {
      const btn = buttons.nth(i);
      if (!(await btn.isVisible())) continue;
      const box = await btn.boundingBox();
      expect(box, `버튼 ${i}의 boundingBox가 존재해야 함`).not.toBeNull();
      if (box) {
        expect(box.width, `버튼 ${i} 너비`).toBeGreaterThanOrEqual(44);
        expect(box.height, `버튼 ${i} 높이`).toBeGreaterThanOrEqual(44);
      }
    }
  });
});

test.describe('/board - 검색 결과 카운트 (A-2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('A-2: /board?q=test 진입 시 결과 카운트가 노출된다', async ({ page }) => {
    await mockSearchEndpoint(page, {
      content: POSTS_FIXTURE.slice(0, 7),
      totalElements: 42,
      totalPages: 3,
    });

    await page.goto('/board?q=test');
    await page.waitForLoadState('networkidle');

    const countEl = page.getByTestId('search-result-count');
    await expect(countEl).toBeVisible();

    const text = await countEl.innerText();
    // "42건의 결과", "총 42건", "검색 결과 42건" 등 허용
    expect(text).toMatch(/(42).*(건|개)|총\s?42/);
  });
});

test.describe('/board - 검색 입력 (A-3)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('A-3: 검색 input은 onKeyDown 으로 Enter 처리한다 (onKeyPress deprecated)', async ({ page }) => {
    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    // 페이지의 input/dropdown 영역에서 input 직접 평가
    const onKeyDownAttached = await page.evaluate(() => {
      const inputs = Array.from(document.querySelectorAll('input[type="text"]'));
      const target = inputs.find((el) => {
        const placeholder = (el as HTMLInputElement).placeholder ?? '';
        return /입력하세요/.test(placeholder);
      }) as HTMLInputElement | undefined;
      if (!target) return { found: false };

      // React가 부착하는 props는 DOM attribute로 노출되지 않으므로
      // React fiber 또는 react props key 추적은 어렵다.
      // 대신 onkeypress vs onkeydown DOM 핸들러 존재 여부로 우회 검증한다.
      const reactPropsKey = Object.keys(target).find((k) => k.startsWith('__reactProps$'));
      if (!reactPropsKey) return { found: true, hasOnKeyDown: false, hasOnKeyPress: false };
      const props = (target as unknown as Record<string, unknown>)[reactPropsKey] as Record<string, unknown>;
      return {
        found: true,
        hasOnKeyDown: typeof props.onKeyDown === 'function',
        hasOnKeyPress: typeof props.onKeyPress === 'function',
      };
    });

    expect(onKeyDownAttached.found).toBe(true);
    expect(onKeyDownAttached.hasOnKeyDown).toBe(true);
    expect(onKeyDownAttached.hasOnKeyPress).toBe(false);
  });

  test('A-3: 검색 중 spinner 가 표시된다', async ({ page }) => {
    // 검색 응답을 지연시켜 로딩 상태 노출
    await page.route('**/api/post/search**', async (route: Route) => {
      await new Promise((resolve) => setTimeout(resolve, 1500));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: offsetPage(POSTS_FIXTURE.slice(0, 5), { totalElements: 5 }),
        }),
      });
    });

    await page.goto('/board?q=loading');
    await page.waitForLoadState('domcontentloaded');

    const spinner = page.getByTestId('board-search-spinner');
    await expect(spinner).toBeVisible({ timeout: 5000 });
  });

  test('A-3: 빈 입력으로 Enter 시에도 executeSearch 가 호출되어 일반 목록으로 복귀한다', async ({ page }) => {
    await page.goto('/board?q=test');
    await page.waitForLoadState('networkidle');

    // URL에 q=test 가 있는 검색 모드 → 검색창 비우고 Enter → /board 로 복귀해야 함
    const input = page.locator('input[type="text"]').filter({
      hasText: '',
    }).first();

    // placeholder 기반으로 더 정확히
    const searchInput = page.locator('input[placeholder*="입력하세요"]').first();
    await expect(searchInput).toBeVisible();

    await searchInput.fill('');
    await searchInput.press('Enter');

    await page.waitForLoadState('networkidle');
    // q 파라미터가 사라진 일반 목록 URL로 이동
    await expect(page).toHaveURL(/\/board(\?.*)?$/);
    const url = page.url();
    expect(url).not.toMatch(/[?&]q=/);
  });
});

test.describe('/board - 모바일 카드 어포던스 (A-4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('A-4: 모바일 카드는 board-mobile-card testid 를 가지고 전체 클릭 가능하다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    const card = page.getByTestId('board-mobile-card').first();
    await expect(card).toBeVisible();

    // 카드 자체가 <a> 또는 카드 내부에 전체 영역을 덮는 anchor 가 있어야 한다
    const isAnchor = await card.evaluate((el) => el.tagName.toLowerCase() === 'a');
    if (!isAnchor) {
      // 카드 내부 anchor 가 카드 영역을 덮는지(absolute inset-0 또는 동등) 검증
      const overlayAnchor = card.locator('a').first();
      await expect(overlayAnchor).toBeVisible();
      const cardBox = await card.boundingBox();
      const anchorBox = await overlayAnchor.boundingBox();
      expect(cardBox).not.toBeNull();
      expect(anchorBox).not.toBeNull();
      if (cardBox && anchorBox) {
        // anchor 가 카드의 80% 이상을 덮어야 "전체 클릭 가능"으로 본다
        const cardArea = cardBox.width * cardBox.height;
        const anchorArea = anchorBox.width * anchorBox.height;
        expect(anchorArea / cardArea).toBeGreaterThan(0.8);
      }
    }
  });

  test('A-4: 모바일 카드 작성자 Popover 클릭 시 stopPropagation 으로 카드 라우팅이 안 된다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    const card = page.getByTestId('board-mobile-card').first();
    await expect(card).toBeVisible();

    // 카드 안에서 작성자 버튼 (User 아이콘) 클릭
    const authorButton = card.locator('button').filter({ hasText: /작성자/ }).first();
    await expect(authorButton).toBeVisible();

    const urlBefore = page.url();
    await authorButton.click();

    // 짧게 기다리고 URL 변화 없는지 검증 (스토리상 popover 만 열려야 함)
    await page.waitForLoadState('networkidle');
    expect(page.url()).toBe(urlBefore);
  });
});

test.describe('/board - 글쓰기 비로그인 안내 (A-5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('A-5: 비로그인 상태에서 글쓰기 버튼 클릭 시 익명 안내 모달 노출', async ({ page }) => {
    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    const writeButton = page.getByTestId('write-post-button');
    await expect(writeButton).toBeVisible();

    await writeButton.click();

    const modal = page.getByTestId('anonymous-write-info-modal');
    await expect(modal).toBeVisible();

    const text = await modal.innerText();
    // 4자리 숫자 비밀번호 정책 안내
    expect(text).toMatch(/4자리|1000.*9999|숫자\s*\d+자리/);
    // 로그인 권장 메시지
    expect(text).toMatch(/로그인|회원/);
  });
});

test.describe('/board - BoardHeader 중복 제거 (A-6)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('A-6: /board 페이지에 글쓰기 버튼은 정확히 1개만 노출된다 (BoardHeader 중복 제거)', async ({ page }) => {
    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    const writeButtons = page.getByTestId('write-post-button');
    await expect(writeButtons).toHaveCount(1);
  });
});

// =============================================================================
// B 항목 시나리오 (선택)
// =============================================================================

test.describe('/board - 모바일 검색 영역 재설계 (B-M1)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('B-M1: 모바일에서 검색 type 드롭다운이 input 위 별도 행으로 분리된다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    const searchRow = page.getByTestId('board-search-row');
    await expect(searchRow).toBeVisible();

    // type 드롭다운(트리거 버튼)과 input 의 y 좌표 비교 - 드롭다운이 위에 있어야 함
    const typeTrigger = page.getByRole('button', { name: '검색 유형 선택' });
    const searchInput = page.locator('input[placeholder*="입력하세요"]').first();

    await expect(typeTrigger).toBeVisible();
    await expect(searchInput).toBeVisible();

    const typeBox = await typeTrigger.boundingBox();
    const inputBox = await searchInput.boundingBox();

    expect(typeBox).not.toBeNull();
    expect(inputBox).not.toBeNull();
    if (typeBox && inputBox) {
      // 별도 행이면 input 의 top 이 type 의 bottom 이상(약간의 오차 허용)
      expect(inputBox.y).toBeGreaterThanOrEqual(typeBox.y + typeBox.height - 4);
    }
  });
});

test.describe('/board - 카드 정보 위계 (B-M2)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('B-M2: 모바일 카드의 작성자 텍스트가 잘림 없이 표시된다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    // 긴 닉네임을 가진 게시글 모킹
    const longNamePosts = [
      makePost(1, { memberName: '굉장히긴닉네임테스트12345' }),
      ...POSTS_FIXTURE.slice(1, 5),
    ];

    await mockBoardEndpoints(page, { posts: longNamePosts });

    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    const firstCard = page.getByTestId('board-mobile-card').first();
    await expect(firstCard).toBeVisible();

    // 작성자 영역에 닉네임의 일부라도 보여야 함
    const authorText = await firstCard.innerText();
    expect(authorText).toMatch(/굉장히긴닉네임/);

    // 잘림(...) 가 일어났을 때도 wrapper 의 max-w 가 너무 좁지 않은지 검증
    const authorButton = firstCard.locator('button').filter({ hasText: /굉장히긴닉네임/ }).first();
    if ((await authorButton.count()) > 0) {
      const box = await authorButton.boundingBox();
      expect(box).not.toBeNull();
      if (box) {
        // 80px 이상은 확보되어야 한다 (max-w-20 = 80px 이 너무 좁다는 work-queue 제안)
        expect(box.width).toBeGreaterThanOrEqual(80);
      }
    }
  });
});

test.describe('/board - 빈 상태 안내 (B-M4)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('B-M4: 검색 결과 0건 시 board-empty-state 가이드가 노출된다', async ({ page }) => {
    await mockSearchEndpoint(page, {
      content: [],
      totalElements: 0,
      totalPages: 0,
    });

    await page.goto('/board?q=zzzzzznoresult');
    await page.waitForLoadState('networkidle');

    const empty = page.getByTestId('board-empty-state');
    await expect(empty).toBeVisible();

    const text = await empty.innerText();
    // "검색어를 변경" 류 가이드 문구
    expect(text).toMatch(/검색어를?\s?(변경|바꿔|다시)/);
  });
});

test.describe('/board - 인증 만료 액션 피드백 (B-M5)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('B-M5: 글쓰기 페이지 진입 시 401 응답 모킹 시 토스트/안내가 노출된다 (약식)', async ({ page }) => {
    // 비로그인 상태로 /board/write 직접 진입 시도
    await page.goto('/board/write');
    await page.waitForLoadState('networkidle');

    // 토스트 영역 또는 비로그인 안내 모달이 노출되어야 한다
    const toastOrModal = page
      .getByRole('alert')
      .or(page.getByTestId('anonymous-write-info-modal'))
      .or(page.getByText(/로그인이 필요|세션이 만료|로그인 후/));

    await expect(toastOrModal.first()).toBeVisible({ timeout: 5000 });
  });
});

test.describe('/board - NoticeList z-index (B-M6)', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthBoundaryEndpoints(page);
    await mockBoardEndpoints(page);
  });

  test('B-M6: 모바일에서 공지 작성자 popover 가 카드 위로 표시된다', async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== 'mobile-375', 'mobile-375 전용 시나리오');

    // 공지에 익명이 아닌 작성자를 부여
    const notices = [
      makePost(1001, { title: '공지 with author', memberName: '관리자김', notice: true }),
    ];
    await mockBoardEndpoints(page, { noticePosts: notices });

    await page.goto('/board');
    await page.waitForLoadState('networkidle');

    // 공지 카드 내 "관리자김" 버튼 찾기
    const noticeAuthorButton = page.locator('button').filter({ hasText: '관리자김' }).first();
    await expect(noticeAuthorButton).toBeVisible();

    await noticeAuthorButton.click();

    // popover 컨텐츠 ("롤링페이퍼 보기") 가 표시되고 z-index 가 양수
    const popoverContent = page.getByText(/롤링페이퍼 보기/).first();
    await expect(popoverContent).toBeVisible();

    const popoverZ = await popoverContent.evaluate((el) => {
      let cur: HTMLElement | null = el as HTMLElement;
      while (cur) {
        const z = window.getComputedStyle(cur).zIndex;
        if (z && z !== 'auto' && Number(z) > 0) return Number(z);
        cur = cur.parentElement;
      }
      return 0;
    });

    expect(popoverZ).toBeGreaterThan(0);
  });
});
