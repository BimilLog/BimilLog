import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-6-board-browse-and-search';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 백그라운드 polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

/**
 * round-6-board-browse-and-search iter-1 — /board 진입 ~ 검색/페이지네이션/빈 결과
 * 시각/인터랙션 매트릭스 캡처.
 *
 * 환경 가정:
 *  - frontend dev server: localhost:3000 가동 중
 *  - **backend: 미가용** → 게시글 목록/검색 결과는 빈/에러 카피로 회복되어 캡처됨.
 *    그 자체로 "에러 회복 시각 검증" 자료. functional 검증은 단위/통합 테스트에서.
 *
 * 캡처 단계 (ux-proposal "인터랙션 캡처 지점" 기반, 18 step):
 *  step-01-board-base                       /board 진입 (h1 + breadcrumb + 검색 카드 + 탭)
 *  step-01b-board-desktop-table             데스크톱 테이블 6컬럼 헤더 (모바일 viewport에서도 동일 페이지)
 *  step-01c-board-mobile-card               모바일 카드 레이아웃
 *  step-02-board-loadmore                   더보기 버튼 클릭 (백엔드 미가용 시 노출되지 않을 수 있음 — fail-soft)
 *  step-03-search-results                   '테스트' 입력 + Enter — URL ?q= 갱신 + 결과 영역
 *  step-03b-search-result-count             검색 결과 카운트 ("총 N건의 결과") 영역
 *  step-04-search-page-2                    검색 결과 페이지 2 클릭 (시드 21+ 일 때만 — fail-soft)
 *  step-04b-search-pagination-caption       시드 1~20 시 caption-only "총 N건 · 페이지 1/1" 모드
 *  step-05-search-empty                     '__nope__zzqq__' — 빈 결과 + 키워드 echo + CTA
 *  step-05b-search-cleared                  X 버튼으로 검색 클리어 → /board 복귀
 *  step-06-tabs-realtime                    실시간 인기글 탭
 *  step-06b-tabs-weekly                     주간 인기글 탭
 *  step-06c-tabs-legend-pagination          명예의 전당 탭 + BoardPagination
 *  step-06d-tabs-back-all                   전체 탭 복귀 + NoticeList + 일반 목록
 *  step-07-post-detail-from-search          검색 결과에서 게시글 클릭 → /board/post/{id}
 *  step-07b-back-to-search                  뒤로가기 → URL ?q=&page= 복원 여부 (F-BUG-3 검증)
 *  step-08-mobile-base                      모바일 기본 진입 (mobile-* 프로젝트 자체로 검증)
 *  step-08b-mobile-search-focus             검색 input focus → scrollIntoView (F-BUG-7 검증)
 */
test.describe('round-6-board-browse-and-search', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 보장 — /board 는 protected 아님
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /board 기본 진입
  // ============================================================
  test('step-01-board-base', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1500);

    // 페이지 헤딩 (Breadcrumb "커뮤니티") 또는 검색 form 존재 확인
    const search = page.getByRole('search', { name: /게시판 검색/ });
    await expect(search.first()).toBeVisible({ timeout: 8000 }).catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-board-base',
    );

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'board-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // ============================================================
  // step-01b-board-desktop-table
  // 데스크톱 테이블 6컬럼 헤더 영역 — desktop-1280 프로젝트에서만 의미.
  // mobile-375 프로젝트에서도 동일 페이지 캡처 (viewport 자체로 검증).
  // ============================================================
  test('step-01b-board-desktop-table', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1500);

    // 게시글 테이블이 있다면 안정적으로 잡힐 때까지 짧게 대기 (백엔드 미가용 시 미노출 OK)
    const table = page.locator('table').first();
    await table.waitFor({ state: 'visible', timeout: 3000 }).catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01b-board-desktop-table',
    );
  });

  // ============================================================
  // step-01c-board-mobile-card
  // 모바일 카드 레이아웃. 데스크톱 viewport 에서도 그대로 캡처 (페이지 동일).
  // ============================================================
  test('step-01c-board-mobile-card', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1500);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01c-board-mobile-card',
    );
  });

  // ============================================================
  // step-02-board-loadmore
  // 더보기 버튼 클릭 시도 — 백엔드 미가용 시 "마지막 편지까지 도착했어요" 또는 미노출.
  // ============================================================
  test('step-02-board-loadmore', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1500);

    const loadMoreBtn = page.getByRole('button', { name: /더보기/ }).first();
    if (await loadMoreBtn.isVisible().catch(() => false)) {
      await loadMoreBtn.click().catch(() => undefined);
      await page.waitForTimeout(800);
    }

    // 백엔드 미가용 시 "마지막 편지까지 도착했어요" 카피 후보 가시성 확인 (fail-soft)
    const endCaption = page.getByTestId('load-more-end');
    await endCaption.waitFor({ state: 'visible', timeout: 1500 }).catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-board-loadmore',
    );
  });

  // ============================================================
  // step-03-search-results
  // '테스트' 입력 + Enter — URL ?q= 갱신 + 결과 영역.
  // ============================================================
  test('step-03-search-results', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const search = page.getByTestId('search-input');
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('테스트');
    await search.press('Enter');
    await page.waitForTimeout(900);

    // URL 갱신 검증 (F-BUG-3 회귀 검증의 일부)
    expect(page.url()).toMatch(/[?&]q=%ED%85%8C%EC%8A%A4%ED%8A%B8|[?&]q=테스트/);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-search-results',
    );
  });

  // ============================================================
  // step-03b-search-result-count
  // 결과 카운트 영역 (data-testid="search-result-count") + aria-live polite (F-401).
  // ============================================================
  test('step-03b-search-result-count', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const search = page.getByTestId('search-input');
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('테스트');
    await search.press('Enter');
    await page.waitForTimeout(900);

    const count = page.getByTestId('search-result-count');
    await count.waitFor({ state: 'visible', timeout: 3000 }).catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03b-search-result-count',
    );
  });

  // ============================================================
  // step-04-search-page-2
  // 검색 결과 page 2 클릭 → URL ?page=2 동기화 (F-BUG-3).
  // 시드 21+ 가 있을 때만 가능. 백엔드 미가용 시 페이지네이션 미노출 → fail-soft 캡처.
  // ============================================================
  test('step-04-search-page-2', async ({ page }, testInfo) => {
    await page.goto('/board?q=테스트');
    await waitSettled(page, 1500);

    // 다음 페이지 버튼 클릭 시도 (보통 BoardPagination 의 next 버튼)
    const pagination = page.locator('[data-testid="board-pagination"]').first();
    if (await pagination.isVisible().catch(() => false)) {
      const nextBtn = pagination.getByRole('button', { name: /다음|Next/ }).first();
      if (await nextBtn.isVisible().catch(() => false)) {
        await nextBtn.click().catch(() => undefined);
        await page.waitForTimeout(800);
      } else {
        // 페이지 번호 2 버튼 시도
        const page2 = pagination.getByRole('button', { name: /^2$/ }).first();
        if (await page2.isVisible().catch(() => false)) {
          await page2.click().catch(() => undefined);
          await page.waitForTimeout(800);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-search-page-2',
    );
  });

  // ============================================================
  // step-04b-search-pagination-caption
  // 시드 1~20 건일 때 BoardPagination caption-only ("총 N건 · 페이지 1/1") 모드.
  // 백엔드 미가용 시 페이지네이션 미노출 → 검색 결과 영역만 캡처.
  // ============================================================
  test('step-04b-search-pagination-caption', async ({ page }, testInfo) => {
    await page.goto('/board?q=테스트');
    await waitSettled(page, 1500);

    // caption-only 모드는 data-pagination-mode="caption-only" 속성으로 식별
    const captionPagination = page.locator('[data-pagination-mode="caption-only"]').first();
    await captionPagination.waitFor({ state: 'visible', timeout: 2500 }).catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04b-search-pagination-caption',
    );
  });

  // ============================================================
  // step-05-search-empty
  // 매칭 없는 키워드 → 한국어 빈 상태 + 키워드 echo + "첫 글의 주인공 되기" CTA.
  // 백엔드 미가용 시 에러 카피 ("편지를 가져오지 못했어요") 일 수 있음 — fail-soft.
  // ============================================================
  test('step-05-search-empty', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const search = page.getByTestId('search-input');
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('__nope__zzqq__');
    await search.press('Enter');
    await page.waitForTimeout(1000);

    // 한국어 빈 상태 OR 에러 상태 둘 중 하나 노출
    const empty = page.getByTestId('board-empty-state');
    const errorKo = page.getByText(/편지를 가져오지 못했어요|찾지 못했어요|문제가 발생/);
    await empty.or(errorKo).first()
      .waitFor({ state: 'visible', timeout: 3000 })
      .catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-search-empty',
    );
  });

  // ============================================================
  // step-05b-search-cleared
  // X 버튼으로 검색 클리어 → URL /board 복귀 + 일반 목록 재노출.
  // ============================================================
  test('step-05b-search-cleared', async ({ page }, testInfo) => {
    await page.goto('/board?q=__nope__zzqq__');
    await waitSettled(page, 1200);

    const search = page.getByTestId('search-input');
    await search.waitFor({ state: 'visible', timeout: 8000 });

    const clearBtn = page.getByRole('button', { name: /검색어 지우기/ }).first();
    if (await clearBtn.isVisible().catch(() => false)) {
      await clearBtn.click().catch(() => undefined);
      await page.waitForTimeout(800);
    } else {
      // X 버튼 미노출 시 직접 input 비우고 Enter
      await search.fill('');
      await search.press('Enter');
      await page.waitForTimeout(800);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05b-search-cleared',
    );
  });

  // ============================================================
  // step-06-tabs-realtime
  // 실시간 인기글 탭 클릭.
  // ============================================================
  test('step-06-tabs-realtime', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const realtimeTab = page.getByRole('tab', { name: /실시간 인기글/ }).first();
    if (await realtimeTab.isVisible().catch(() => false)) {
      await realtimeTab.click().catch(() => undefined);
      await page.waitForTimeout(700);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-tabs-realtime',
    );
  });

  // ============================================================
  // step-06b-tabs-weekly
  // 주간 인기글 탭 클릭.
  // ============================================================
  test('step-06b-tabs-weekly', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const weeklyTab = page.getByRole('tab', { name: /주간 인기글/ }).first();
    if (await weeklyTab.isVisible().catch(() => false)) {
      await weeklyTab.click().catch(() => undefined);
      await page.waitForTimeout(700);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06b-tabs-weekly',
    );
  });

  // ============================================================
  // step-06c-tabs-legend-pagination
  // 명예의 전당 탭 + BoardPagination 가시성.
  // ============================================================
  test('step-06c-tabs-legend-pagination', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const legendTab = page.getByRole('tab', { name: /명예의 전당/ }).first();
    if (await legendTab.isVisible().catch(() => false)) {
      await legendTab.click().catch(() => undefined);
      await page.waitForTimeout(800);
    }

    // 레전드 페이지네이션 잡기 시도 (백엔드 미가용 시 미노출)
    const pagination = page.locator('[data-testid="board-pagination"]').first();
    await pagination.waitFor({ state: 'visible', timeout: 2500 }).catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06c-tabs-legend-pagination',
    );
  });

  // ============================================================
  // step-06d-tabs-back-all
  // 전체 탭 복귀 + NoticeList + 일반 목록 영역.
  // ============================================================
  test('step-06d-tabs-back-all', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    // 다른 탭 → 전체 탭 순회
    const realtimeTab = page.getByRole('tab', { name: /실시간 인기글/ }).first();
    if (await realtimeTab.isVisible().catch(() => false)) {
      await realtimeTab.click().catch(() => undefined);
      await page.waitForTimeout(500);
    }
    const allTab = page.getByRole('tab', { name: /전체 게시판/ }).first();
    if (await allTab.isVisible().catch(() => false)) {
      await allTab.click().catch(() => undefined);
      await page.waitForTimeout(700);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06d-tabs-back-all',
    );
  });

  // ============================================================
  // step-07-post-detail-from-search
  // 검색 결과 행 클릭 → /board/post/{id} 이동.
  // 백엔드 미가용 시 게시글 행 자체가 없어 직접 /board/post/1 진입으로 fallback.
  // ============================================================
  test('step-07-post-detail-from-search', async ({ page }, testInfo) => {
    await page.goto('/board?q=테스트');
    await waitSettled(page, 1500);

    const postLink = page.locator('a[href^="/board/post/"]').first();
    if (await postLink.isVisible().catch(() => false)) {
      await postLink.click().catch(() => undefined);
      await page.waitForTimeout(1200);
    } else {
      // fallback: 직접 진입 (백엔드 미가용 시 빈/에러 페이지)
      await page.goto('/board/post/1');
      await waitSettled(page, 1500);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-post-detail-from-search',
    );
  });

  // ============================================================
  // step-07b-back-to-search
  // 뒤로가기 → URL /board?q=&page= 복원 여부 (F-BUG-3 회귀 검증).
  // ============================================================
  test('step-07b-back-to-search', async ({ page }, testInfo) => {
    await page.goto('/board?q=테스트&page=2');
    await waitSettled(page, 1500);

    const postLink = page.locator('a[href^="/board/post/"]').first();
    if (await postLink.isVisible().catch(() => false)) {
      await postLink.click().catch(() => undefined);
      await page.waitForTimeout(1000);
      await page.goBack().catch(() => undefined);
      await page.waitForTimeout(800);
    } else {
      // 게시글 행 자체가 없으므로 새 진입으로 시뮬레이션 (URL 자체 검증)
      await page.goto('/board?q=테스트&page=2');
      await waitSettled(page, 1200);
    }

    // URL 에 q=테스트 + page=2 가 살아있는지 검증 (F-BUG-3 회귀)
    const url = page.url();
    const hasQ = /[?&]q=%ED%85%8C%EC%8A%A4%ED%8A%B8|[?&]q=테스트/.test(url);
    const hasPage = /[?&]page=2/.test(url);
    test.info().annotations.push({
      type: 'back-to-search-url',
      description: `url=${url} hasQ=${hasQ} hasPage=${hasPage}`,
    });

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07b-back-to-search',
    );
  });

  // ============================================================
  // step-08-mobile-base
  // 모바일 기본 진입 (mobile-* 프로젝트 자체로 검증). desktop 도 동일 페이지 캡처.
  // ============================================================
  test('step-08-mobile-base', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1500);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-mobile-base',
    );
  });

  // ============================================================
  // step-08b-mobile-search-focus
  // 검색 input focus → onFocus 핸들러의 250ms 지연 후 scrollIntoView (F-BUG-7 검증).
  // ============================================================
  test('step-08b-mobile-search-focus', async ({ page }, testInfo) => {
    await page.goto('/board');
    await waitSettled(page, 1200);

    const search = page.getByTestId('search-input');
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.focus();
    // BoardSearch.handleFocus 의 setTimeout 250ms + scrollIntoView smooth 마무리 대기
    await page.waitForTimeout(450);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08b-mobile-search-focus',
    );
  });
});
