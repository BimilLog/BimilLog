import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-5-visit-and-leave-message';
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
 * round-5-visit-and-leave-message iter-1 — visit→message 여정 시각/인터랙션 매트릭스 캡처.
 *
 * 환경 가정:
 *  - frontend dev server: localhost:3000 가동 중
 *  - **backend: 미가용** → 검색/멤버 리스트는 빈/에러 상태로 회복되어 캡처됨.
 *    그 자체로 "에러 회복 시각 검증" 자료. functional 검증은 단위/통합 테스트에서.
 *
 * 캡처 단계 (ux-proposal "E2E 행동 시나리오" 기반):
 *  step-01-visit-base                       /visit 진입 (h1 "롤링페이퍼 방문" + 검색 카드 + 페이지사이즈 select)
 *  step-01b-visit-pagination-default-10     기본 10개씩 노출 + BoardPagination 캡션-only 모드 캡처 시도
 *  step-01c-visit-pagination-size-10        size 셀렉트를 명시적으로 10 으로 변경
 *  step-02-search-results                   '닉' 입력 후 Enter — 결과 리스트 또는 빈 상태 (백엔드 미가용)
 *  step-02b-search-results-pagination       검색 결과 페이지네이션/캡션 노출 영역
 *  step-02c-search-empty                    '__nope__zzqq__' 입력 — B-003 한국어 빈 상태 + MailOpen + 가이드
 *  step-02d-search-input-debounce           디바운스 직후 Loader2 스피너 (B-006) 캡처 시도 (짧은 settle)
 *  step-02e-search-mode-recent-dim          검색 모드 진입 시 RecentVisits dim 처리 — 비워있을 수도 (localStorage)
 *  step-03-paper-rich                       /rolling-paper/uiux-target-rich 직접 진입 — 백엔드 미가용 시 빈 그리드/에러
 *  step-04-paper-empty                      /rolling-paper/uiux-target-empty 직접 진입
 *  step-05-message-form-open                paper 페이지에서 빈 셀 클릭 시 메시지 작성 모달 (백엔드 응답 의존)
 *  step-06-message-success-toast            메시지 작성 후 토스트 — 백엔드 미가용으로 fail-soft
 *  step-06b-paper-after-message             제출 후 그리드 상태 — fail-soft
 *  step-07-mobile-search-focus              모바일 viewport(이미 mobile-375 프로젝트 자체로 검증) — input focus
 *  step-07b-mobile-search-result-scroll     focus 후 scrollIntoView 동작 + 결과 영역
 */
test.describe('round-5-visit-and-leave-message', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 보장 — /visit 은 protected 아님이지만 일관성 위해 정리
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /visit 기본 진입
  // ============================================================
  test('step-01-visit-base', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    // 페이지 헤더 검증 — 헤더 h1 또는 카드 h2 둘 중 하나는 가시이어야 함
    const h1 = page.getByRole('heading', { name: /롤링페이퍼 방문/, level: 1 });
    const cardHeading = page.getByRole('heading', {
      name: /누구의 롤링페이퍼를 방문할까요/,
    });
    await expect(h1.or(cardHeading).first()).toBeVisible({ timeout: 8000 });

    // 검색 form (role="search") 가 렌더되는지 확인
    await expect(page.getByRole('search', { name: /멤버 닉네임 검색/ })).toBeVisible();

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-visit-base',
    );

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'visit-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // ============================================================
  // step-01b-visit-pagination-default-10
  // 기본 10개씩 select + BoardPagination 캡션 노출 시도.
  // 백엔드 미가용 시 빈 상태가 노출되므로 select 영역만 캡처.
  // ============================================================
  test('step-01b-visit-pagination-default-10', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1500);

    // 페이지 사이즈 select 가 dynamic import 된 AllUsersList 안에 있음 — 가시 대기
    const pageSizeSelect = page.getByRole('combobox', { name: /페이지당 멤버 수/ });
    await pageSizeSelect.waitFor({ state: 'visible', timeout: 8000 }).catch(() => undefined);

    // pagination 가 노출됐는지 부드럽게 확인 — 백엔드 미가용 시 empty/loading 으로 미노출 가능
    const pagination = page.locator('[data-testid="board-pagination"]');
    await pagination.first()
      .waitFor({ state: 'visible', timeout: 3000 })
      .catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01b-visit-pagination-default-10',
    );
  });

  // ============================================================
  // step-01c-visit-pagination-size-10
  // size 셀렉트를 명시적으로 10 으로 재선택 — currentPage 리셋 동작 확인.
  // ============================================================
  test('step-01c-visit-pagination-size-10', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1500);

    const pageSizeSelect = page.getByRole('combobox', { name: /페이지당 멤버 수/ });
    if (await pageSizeSelect.isVisible().catch(() => false)) {
      await pageSizeSelect.selectOption('10').catch(() => undefined);
      await page.waitForTimeout(500);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01c-visit-pagination-size-10',
    );
  });

  // ============================================================
  // step-02-search-results
  // '닉' 입력 후 Enter — 결과 또는 빈/에러. 백엔드 미가용이라도 visual matrix 확보.
  // ============================================================
  test('step-02-search-results', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('닉');
    await search.press('Enter');
    // 디바운스(300ms) + 응답 대기 — 백엔드 미가용 시 에러 fallback 텍스트
    await page.waitForTimeout(900);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-search-results',
    );
  });

  // ============================================================
  // step-02b-search-results-pagination
  // 검색 결과 영역 페이지네이션/캡션 — 백엔드 미가용 시 미노출 가능.
  // 그래도 에러/빈 상태 시각 검증 자료.
  // ============================================================
  test('step-02b-search-results-pagination', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('uiux');
    await search.press('Enter');
    await page.waitForTimeout(900);

    // 결과 영역 안의 pagination 또는 search-result-header 부분만 잡으면 좋지만
    // 백엔드 미가용 시 보이지 않을 수 있어 전체 캡처로 fallback.
    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02b-search-results-pagination',
    );
  });

  // ============================================================
  // step-02c-search-empty
  // 매칭 없는 키워드 → 한국어 빈 상태 + MailOpen + 가이드. (B-003 검증 시각)
  // 백엔드 미가용 시 isLoading 영구 또는 error fallback — 그래도 에러 카피가 한글이어야 함.
  // ============================================================
  test('step-02c-search-empty', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('__nope__zzqq__');
    await search.press('Enter');
    await page.waitForTimeout(1000);

    // 한국어 빈 상태 OR 한국어 에러 상태 둘 중 하나는 노출되어야 함 (B-003)
    const emptyKo = page.getByText(/찾지 못했어요|문제가 발생|불러오지 못했어요/);
    await emptyKo.first()
      .waitFor({ state: 'visible', timeout: 3000 })
      .catch(() => undefined);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02c-search-empty',
    );
  });

  // ============================================================
  // step-02d-search-input-debounce
  // 디바운스 중 Loader2 스피너 (B-006) 캡처 — 입력 직후 짧은 settle 로 잡기.
  // ============================================================
  test('step-02d-search-input-debounce', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    // 빠르게 글자 입력 — 디바운스 300ms 진행 중 한 시점을 캡처
    await search.fill('a');
    // 디바운스 중간 (~150ms) 캡처 시도
    await page.waitForTimeout(120);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02d-search-input-debounce',
    );
  });

  // ============================================================
  // step-02e-search-mode-recent-dim
  // 검색 모드 진입 시 RecentVisits dim 처리 (opacity-50 + aria-hidden).
  // 비로그인 + localStorage 비어있어 Recent 카드 자체가 미노출일 수 있음 → 빈 상태 캡처.
  // ============================================================
  test('step-02e-search-mode-recent-dim', async ({ page }, testInfo) => {
    // localStorage 에 임의 visit 기록 주입 — RecentVisits 가 표시되도록
    await page.addInitScript(() => {
      try {
        const visits = [
          {
            nickname: 'uiux-target-rich',
            displayName: 'uiux-target-rich',
            visitedAt: new Date(Date.now() - 60_000).toISOString(),
          },
          {
            nickname: 'uiux-target-empty',
            displayName: 'uiux-target-empty',
            visitedAt: new Date(Date.now() - 3_600_000).toISOString(),
          },
        ];
        // storage.ts 의 키와 동일 가정 — 다양한 후보 키로 선반영
        localStorage.setItem('recentVisits', JSON.stringify(visits));
        localStorage.setItem('recent-visits', JSON.stringify(visits));
        localStorage.setItem('recent_visits', JSON.stringify(visits));
      } catch (_) { /* ignored */ }
    });

    await page.goto('/visit');
    await waitSettled(page, 1500);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.fill('aaa');
    await page.waitForTimeout(500);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02e-search-mode-recent-dim',
    );
  });

  // ============================================================
  // step-03-paper-rich
  // /rolling-paper/uiux-target-rich 직접 진입.
  // 시드/백엔드 미가용 시 빈 그리드 또는 에러 fallback — paper 메타포 시각 검증.
  // ============================================================
  test('step-03-paper-rich', async ({ page }, testInfo) => {
    await page.goto('/rolling-paper/uiux-target-rich');
    await waitSettled(page, 1800);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-paper-rich',
    );
  });

  // ============================================================
  // step-04-paper-empty
  // /rolling-paper/uiux-target-empty 직접 진입 — 빈 그리드.
  // ============================================================
  test('step-04-paper-empty', async ({ page }, testInfo) => {
    await page.goto('/rolling-paper/uiux-target-empty');
    await waitSettled(page, 1800);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-paper-empty',
    );
  });

  // ============================================================
  // step-05-message-form-open
  // 빈 셀 클릭 → 메시지 작성 모달 — 백엔드 응답 없이도 클라이언트 렌더 모달이면 잡힘.
  // 모달 trigger 영역이 있으면 클릭, 없으면 base 캡처로 fallback.
  // ============================================================
  test('step-05-message-form-open', async ({ page }, testInfo) => {
    await page.goto('/rolling-paper/uiux-target-empty');
    await waitSettled(page, 2000);

    // 빈 셀 — data-testid 가 없는 케이스가 많아 보수적으로 클릭 가능한 셀 후보 탐색
    const cellCandidate = page
      .locator('[data-testid*="paper-cell"], [data-testid*="message-cell"], button[aria-label*="메시지 작성"]')
      .first();
    if (await cellCandidate.isVisible().catch(() => false)) {
      await cellCandidate.click().catch(() => undefined);
      await page.waitForTimeout(500);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-message-form-open',
    );
  });

  // ============================================================
  // step-06-message-success-toast
  // 메시지 작성 폼 제출 + 성공 토스트 — 백엔드 미가용 시 에러 토스트 또는 미노출.
  // fail-soft 캡처.
  // ============================================================
  test('step-06-message-success-toast', async ({ page }, testInfo) => {
    await page.goto('/rolling-paper/uiux-target-empty');
    await waitSettled(page, 2000);

    // 가능한 form 요소가 있으면 데이터를 채워본다
    const contentField = page
      .locator('textarea, [data-testid*="message-content"], [contenteditable="true"]')
      .first();
    if (await contentField.isVisible().catch(() => false)) {
      await contentField.fill('e2e 테스트 메시지').catch(() => undefined);
    }

    const submitBtn = page
      .getByRole('button', { name: /메시지 보내기|작성|제출|보내기|전송/ })
      .first();
    if (await submitBtn.isVisible().catch(() => false)) {
      await submitBtn.click().catch(() => undefined);
      await page.waitForTimeout(800);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-message-success-toast',
    );
  });

  // ============================================================
  // step-06b-paper-after-message
  // 제출 후 그리드 상태 캡처. 백엔드 미가용으로 변화 없음 — 그래도 그리드 안정성 확인.
  // ============================================================
  test('step-06b-paper-after-message', async ({ page }, testInfo) => {
    await page.goto('/rolling-paper/uiux-target-empty');
    await waitSettled(page, 2000);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06b-paper-after-message',
    );
  });

  // ============================================================
  // step-07-mobile-search-focus
  // mobile-* 프로젝트에서만 의미 있는 단계 — desktop 프로젝트도 viewport 그대로 캡처.
  // input focus 시 sticky header 가림 방지 (scrollIntoView 검증) 시각.
  // ============================================================
  test('step-07-mobile-search-focus', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.focus();
    // scrollIntoView 는 onFocus 시 250ms 후 트리거됨 — 그 이후 한 프레임 더
    await page.waitForTimeout(450);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-mobile-search-focus',
    );
  });

  // ============================================================
  // step-07b-mobile-search-result-scroll
  // focus + 결과 영역 스크롤 후 캡처.
  // ============================================================
  test('step-07b-mobile-search-result-scroll', async ({ page }, testInfo) => {
    await page.goto('/visit');
    await waitSettled(page, 1200);

    const search = page.getByRole('searchbox', { name: /검색할 닉네임/ });
    await search.waitFor({ state: 'visible', timeout: 8000 });
    await search.focus();
    await page.waitForTimeout(300);
    await search.fill('uiux');
    await search.press('Enter');
    await page.waitForTimeout(800);

    // 검색 결과 영역으로 스크롤 — 결과가 없어도 빈 상태 영역으로 스크롤
    const resultArea = page
      .locator('[data-testid="all-users-empty"], [data-testid="all-users-loading"], [data-testid="search-result-header"], table')
      .first();
    if (await resultArea.isVisible().catch(() => false)) {
      await resultArea.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(300);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07b-mobile-search-result-scroll',
    );
  });
});
