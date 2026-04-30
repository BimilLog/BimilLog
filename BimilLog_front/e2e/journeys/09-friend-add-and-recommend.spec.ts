import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-9-friend-add-and-recommend';
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

type Branch = 'friends' | 'login' | 'unknown';

/**
 * fail-soft 진입 헬퍼.
 *
 *  /friends 는 (protected) 라우트 그룹 — 비인증 시 미들웨어/RSC 가
 *  /login 으로 리다이렉트할 수 있다. 둘 중 어느 분기든 캡처 자체는 의미 있다:
 *   - friends 분기: FriendTabs 의 토큰/탭/ARIA 회귀 검증 가능
 *   - login 분기: 로그인 화면 토큰/카피 회귀 검증 가능
 *
 *  분기 종류는 annotation 에 남겨 디자인 리뷰 시 참고.
 */
async function gotoFriends(page: Page, search = ''): Promise<Branch> {
  const url = `/friends${search}`;
  await page.goto(url);
  await waitSettled(page, 1500);

  const finalUrl = page.url();
  if (/\/login/.test(finalUrl)) return 'login';
  if (/\/friends/.test(finalUrl)) return 'friends';
  return 'unknown';
}

/**
 * 친구 탭 클릭 헬퍼 — role="tab" 의 정확한 이름으로 클릭.
 * 실패해도 throw 하지 않음 (fail-soft).
 *
 *  - desktop: "내 친구" / "추천 친구" / "받은 요청" / "보낸 요청"
 *  - mobile (sm:hidden): "친구" / "추천" / "받은" / "보낸"
 *  탭 버튼에는 둘 다 span 으로 들어가 있어 accessible name 은 두 라벨이 합쳐진 형태.
 *  → role=tab + name regex 로 한쪽만 매칭해도 OK.
 */
async function clickTab(page: Page, idLabelRegex: RegExp): Promise<boolean> {
  const tab = page.getByRole('tab', { name: idLabelRegex }).first();
  if (await tab.isVisible({ timeout: 2500 }).catch(() => false)) {
    await tab.click().catch(() => undefined);
    await page.waitForTimeout(500);
    return true;
  }
  return false;
}

/**
 * round-9-friend-add-and-recommend iter-1
 *  - /friends 페이지 진입 ~ 4개 탭 (내 친구 / 추천 / 받은 / 보낸) 순회
 *  - B-002 핵심 검증: 탭 클릭 시 URL ?tab= 동기화 (router.replace)
 *  - 다크 토큰 일관, ARIA tablist, 빈 상태 메타포 카피, 모바일 단축 라벨 회귀.
 *
 * 환경 가정:
 *  - frontend dev server: localhost:3000 가동 중
 *  - **backend: 미가용** → /friends 가 protected 라 비인증 시 /login 리다이렉트 가능.
 *    이 경우에도 캡처 자체는 의미 있으며 fail-soft 로 진행한다.
 *
 * 캡처 단계 (8 step):
 *  step-01-friends-base                /friends 진입 base — 탭 헤더/패널 또는 /login fallback
 *  step-02-tab-received                받은 요청 탭 (?tab=received) — URL 동기화 시각 + 카운트 뱃지
 *  step-03-tab-sent                    보낸 요청 탭 (?tab=sent) — B-002 핵심: URL 동기화 시각
 *  step-04-tab-recommended             추천 친구 탭 (?tab=recommended) — 신뢰 시그널 노출
 *  step-05-empty-state-copy            빈 상태 카피 메타포 (편지/종이) — 4개 탭 중 데이터 없는 케이스
 *  step-06-friend-card-aria            친구 카드 + ARIA tablist 적용 (focus visible)
 *  step-07-mobile-tab-short-label      모바일 단축 라벨 (sm:hidden 분기) + sticky 헤더
 *  step-08-dark-token-consistency      다크 토큰 일관 (paper/ink/postal-navy 회귀)
 */
test.describe('round-9-friend-add-and-recommend', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 라운드 9 는 인증 시드 없는 fail-soft 검증.
    // /friends 가 (protected) 라 /login 으로 리다이렉트 될 수 있음.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /friends 진입 base (탭 헤더/패널 또는 /login redirect)
  // ============================================================
  test('step-01-friends-base', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page);

    if (branch === 'friends') {
      // FriendTabs 가 마운트 되었는지 확인 — role="tablist"
      await page
        .getByRole('tablist', { name: /친구 탭/ })
        .first()
        .waitFor({ state: 'visible', timeout: 4000 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-friends-base',
    );

    test.info().annotations.push({
      type: 'friends-branch',
      description: `branch=${branch} url=${page.url()}`,
    });

    // 인터랙션 트리거 스캔 — friends 분기일 때만 의미 있음
    if (branch === 'friends') {
      const triggers = await scanInteractionTriggers(page);
      test.info().annotations.push({
        type: 'friends-triggers',
        description: JSON.stringify(triggers, null, 2),
      });
    }

    // sanity: 어느 분기든 페이지 자체는 마운트
    expect(page.url()).toMatch(/\/(friends|login)/);
  });

  // ============================================================
  // step-02: 받은 요청 탭 (?tab=received) — URL 동기화 + 카운트 뱃지
  //  fail-soft: login 분기면 그대로 캡처
  // ============================================================
  test('step-02-tab-received', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page);

    if (branch === 'friends') {
      // tab 클릭 시도 — desktop 라벨 "받은 요청" 또는 mobile 단축 "받은"
      const clicked = await clickTab(page, /받은(\s*요청)?/);
      if (clicked) {
        // URL 동기화 검증 — router.replace 후 ?tab=received 가 살아있어야 함 (B-002)
        await page.waitForFunction(
          () => /[?&]tab=received\b/.test(window.location.search),
          undefined,
          { timeout: 3000 },
        ).catch(() => undefined);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-tab-received',
    );

    test.info().annotations.push({
      type: 'received-url',
      description: `branch=${branch} url=${page.url()}`,
    });
  });

  // ============================================================
  // step-03: 보낸 요청 탭 (?tab=sent) — **B-002 핵심: 탭 URL 동기화**
  //  router.replace 가 동작해 ?tab=sent 가 URL 에 반영되는지 시각 + URL 검증.
  // ============================================================
  test('step-03-tab-sent', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page);

    let urlMatched = false;
    if (branch === 'friends') {
      const clicked = await clickTab(page, /보낸(\s*요청)?/);
      if (clicked) {
        // B-002: URL SSOT — 클릭 직후 ?tab=sent 로 동기화
        urlMatched = await page
          .waitForFunction(
            () => /[?&]tab=sent\b/.test(window.location.search),
            undefined,
            { timeout: 3000 },
          )
          .then(() => true)
          .catch(() => false);

        // 활성 탭의 aria-selected="true" 도 검증 (WAI-ARIA Tabs 패턴)
        await page
          .getByRole('tab', { selected: true, name: /보낸/ })
          .first()
          .waitFor({ state: 'visible', timeout: 2000 })
          .catch(() => undefined);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-tab-sent',
    );

    test.info().annotations.push({
      type: 'b-002-sent-tab-url',
      description: `branch=${branch} urlMatched=${urlMatched} url=${page.url()}`,
    });
  });

  // ============================================================
  // step-04: 추천 친구 탭 (?tab=recommended) — 신뢰 시그널 (2촌/3촌 배지)
  // ============================================================
  test('step-04-tab-recommended', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page);

    if (branch === 'friends') {
      const clicked = await clickTab(page, /추천(\s*친구)?/);
      if (clicked) {
        await page
          .waitForFunction(
            () => /[?&]tab=recommended\b/.test(window.location.search),
            undefined,
            { timeout: 3000 },
          )
          .catch(() => undefined);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-tab-recommended',
    );
  });

  // ============================================================
  // step-05: 빈 상태 카피 메타포 (편지/종이)
  //  비인증 + backend 미가용 → 데이터 0건 분기 가능 → 빈 상태 노출 케이스.
  //  추천 탭이 가장 빈 상태 가능성 높지만 friends 분기 도달 못해도 fail-soft.
  // ============================================================
  test('step-05-empty-state-copy', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page, '?tab=sent');

    if (branch === 'friends') {
      // 보낸 요청 빈 상태 카피 — round-9 frontend-changes 에서 확정
      // "보낸 친구 요청이 없어요. 추천 탭에서 마음 가는 사람에게 먼저 손을 내밀어 보세요"
      const empty = page
        .getByText(/보낸 친구 요청이 없어요|아직 같이 편지를 주고받을|도착한 친구 요청이 없어요|지금은 추천할 친구가 없어요/)
        .first();
      if (await empty.isVisible({ timeout: 2500 }).catch(() => false)) {
        await empty.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-empty-state-copy',
    );
  });

  // ============================================================
  // step-06: 친구 카드 + ARIA tablist (focus visible)
  //  탭 버튼에 키보드 포커스 적용해서 focus-visible:ring 토큰이 잘 그려지는지 회귀.
  // ============================================================
  test('step-06-friend-card-aria', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page);

    if (branch === 'friends') {
      // 첫 활성 탭 (내 친구) 에 키보드 포커스 — focus-visible:ring 적용 시각
      const firstTab = page.getByRole('tab').first();
      if (await firstTab.isVisible({ timeout: 2500 }).catch(() => false)) {
        await firstTab.focus().catch(() => undefined);
        await page.waitForTimeout(200);
      }

      // 친구 카드 영역으로 스크롤 (있으면)
      const listOrEmpty = page
        .getByRole('tabpanel', { name: /내 친구|친구/ })
        .first();
      await listOrEmpty.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(200);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-friend-card-aria',
    );
  });

  // ============================================================
  // step-07: 모바일 단축 라벨 + sticky 헤더
  //  mobile-375 프로젝트에서 "친구/추천/받은/보낸" 단축 라벨이 보여야 함.
  //  desktop-1280 프로젝트에서는 풀 라벨 ("내 친구/추천 친구/받은 요청/보낸 요청").
  //  어느 쪽이든 4개 탭이 한 줄에 들어가는지 시각 검증.
  // ============================================================
  test('step-07-mobile-tab-short-label', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page);

    if (branch === 'friends') {
      // sticky 헤더 영역 위로 스크롤 (페이지 최상단)
      await page.evaluate(() => window.scrollTo(0, 0));
      await page.waitForTimeout(200);

      // 탭리스트 visibility 확인
      await page
        .getByRole('tablist', { name: /친구 탭/ })
        .first()
        .waitFor({ state: 'visible', timeout: 2500 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-mobile-tab-short-label',
    );
  });

  // ============================================================
  // step-08: 다크 토큰 일관 (paper/ink/postal-navy 회귀)
  //  9개 모듈 파일 (FriendTabs / FriendList / FriendListItem /
  //  ReceivedRequestList / Item / SentRequestList / Item /
  //  RecommendedFriendList / Item) 다크 토큰 회귀 캡처.
  //  dark 프로젝트일 때만 의미 있고, light 도 비교용으로 캡처.
  // ============================================================
  test('step-08-dark-token-consistency', async ({ page }, testInfo) => {
    const branch = await gotoFriends(page, '?tab=recommended');

    if (branch === 'friends') {
      await page.waitForTimeout(500);
      // 패널 영역으로 스크롤
      const panel = page
        .getByRole('tabpanel', { name: /추천/ })
        .first();
      await panel.scrollIntoViewIfNeeded().catch(() => undefined);
      await page.waitForTimeout(200);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-dark-token-consistency',
    );

    // sanity — 페이지 자체는 friends/login 어느 분기든 mount 완료
    expect(page.url()).toMatch(/\/(friends|login)/);
  });
});
