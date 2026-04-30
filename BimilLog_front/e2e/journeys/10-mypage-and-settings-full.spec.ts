import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-10-mypage-and-settings-full';
const ITER = Number(process.env.UIUX_ITER || '1');

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 background polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

type ProtectedBranch = 'mypage' | 'settings' | 'login' | 'unknown';

/**
 * fail-soft 진입 헬퍼.
 *
 *  /mypage, /settings 는 (protected) 라우트 그룹 — 비인증 시 미들웨어/RSC 가
 *  /login 으로 리다이렉트할 수 있다. 둘 중 어느 분기든 캡처 자체는 의미 있다:
 *   - mypage 분기: ProfileBadges/UserActivitySection/ProfileCard 의 토큰/탭/ARIA 회귀 검증 가능
 *   - settings 분기: NotificationSettings/AccountSettings 회귀 검증 가능
 *   - login 분기: 로그인 화면 토큰/카피 회귀 검증 가능
 *
 *  분기 종류는 annotation 에 남겨 디자인 리뷰 시 참고.
 */
async function gotoProtected(
  page: Page,
  pathname: '/mypage' | '/settings',
  search = '',
): Promise<ProtectedBranch> {
  const url = `${pathname}${search}`;
  await page.goto(url);
  await waitSettled(page, 1500);

  const finalUrl = page.url();
  if (/\/login/.test(finalUrl)) return 'login';
  if (/\/mypage/.test(finalUrl)) return 'mypage';
  if (/\/settings/.test(finalUrl)) return 'settings';
  return 'unknown';
}

/**
 * 활동 탭 클릭 헬퍼 — role="tab" + 라벨 regex (full or short).
 *  - desktop: "작성글" / "작성댓글" / "추천글" / "추천댓글"
 *  - mobile (sm:hidden): "작성" / "댓글" / "추천" / "추천♡"
 */
async function clickActivityTab(page: Page, idLabelRegex: RegExp): Promise<boolean> {
  const tab = page.getByRole('tab', { name: idLabelRegex }).first();
  if (await tab.isVisible({ timeout: 2500 }).catch(() => false)) {
    await tab.click().catch(() => undefined);
    await page.waitForTimeout(500);
    return true;
  }
  return false;
}

/**
 * round-10-mypage-and-settings-full iter-1
 *  - /mypage 진입 → 활동 탭 4종 (작성글/작성댓글/추천글/추천댓글) 순회
 *    + ProfileBadges 영역 + 빈 상태 메타포 카피 회귀
 *  - /settings 진입 → NotificationSettings (B-304 indeterminate)
 *    + WithdrawConfirmModal (B-305 paper 토큰 + 메타포)
 *  - 모바일 sticky 헤더 + 단축 라벨 회귀
 *
 * 환경 가정:
 *  - frontend dev server: localhost:3000 가동 중
 *  - **backend: 미가용** → /mypage, /settings 가 protected 라
 *    비인증 시 /login 리다이렉트 가능. fail-soft 로 진행한다.
 *
 * 캡처 단계 (10 step):
 *  step-01-mypage-base                 /mypage 진입 base — ProfileCard 또는 /login fallback
 *  step-02-activity-tab-posts          ?activeTab=my-posts (작성글)
 *  step-03-activity-tab-comments       ?activeTab=my-comments (작성댓글)
 *  step-04-activity-tab-likes          ?activeTab=liked-posts (추천글)
 *  step-05-profile-badges              ProfileBadges (B-302 다크 토큰)
 *  step-06-settings-base               /settings 진입 base — NotificationSettings
 *  step-07-notification-indeterminate  B-304 indeterminate 토글 시각 + aria-checked="mixed"
 *  step-08-withdraw-modal              B-305 WithdrawConfirmModal 메타포 ("이 편지함을 영원히 닫을까요?")
 *  step-09-empty-state-copy            빈 상태 4종 메타포 (편지/답장/마음)
 *  step-10-mobile-sticky-short-label   모바일 sticky 헤더 + 단축 라벨 (라운드 9 friend 탭 일관)
 */
test.describe('round-10-mypage-and-settings-full', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 — 라운드 10 도 인증 시드 없는 fail-soft 검증.
    // /mypage, /settings 가 (protected) 라 /login 으로 리다이렉트 될 수 있음.
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /mypage 진입 base (ProfileCard 또는 /login redirect)
  // ============================================================
  test('step-01-mypage-base', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage');

    if (branch === 'mypage') {
      // ProfileCard 또는 활동 탭리스트가 마운트 되었는지 확인
      await page
        .getByRole('tablist', { name: /활동 내역 탭/ })
        .first()
        .waitFor({ state: 'visible', timeout: 4000 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-mypage-base',
    );

    test.info().annotations.push({
      type: 'mypage-branch',
      description: `branch=${branch} url=${page.url()}`,
    });

    // 인터랙션 트리거 스캔 — mypage 분기일 때만 의미 있음
    if (branch === 'mypage') {
      const triggers = await scanInteractionTriggers(page);
      test.info().annotations.push({
        type: 'mypage-triggers',
        description: JSON.stringify(triggers, null, 2),
      });
    }

    // sanity: 어느 분기든 페이지 자체는 마운트
    expect(page.url()).toMatch(/\/(mypage|login)/);
  });

  // ============================================================
  // step-02: ?activeTab=my-posts (작성글) — URL SSOT 검증
  //  B-303 핵심: 새로고침/공유 진입 시 URL → 내부 상태 동기화
  // ============================================================
  test('step-02-activity-tab-posts', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage', '?activeTab=my-posts');

    if (branch === 'mypage') {
      // 작성글 탭이 active (aria-selected="true")
      await page
        .getByRole('tab', { selected: true, name: /작성/ })
        .first()
        .waitFor({ state: 'visible', timeout: 3000 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-activity-tab-posts',
    );

    test.info().annotations.push({
      type: 'b-303-activity-posts',
      description: `branch=${branch} url=${page.url()}`,
    });
  });

  // ============================================================
  // step-03: ?activeTab=my-comments (작성댓글)
  // ============================================================
  test('step-03-activity-tab-comments', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage', '?activeTab=my-comments');

    let urlMatched = false;
    if (branch === 'mypage') {
      // URL 동기화 검증 — ?activeTab=my-comments 가 살아있는지
      urlMatched = await page
        .waitForFunction(
          () => /[?&]activeTab=my-comments\b/.test(window.location.search),
          undefined,
          { timeout: 3000 },
        )
        .then(() => true)
        .catch(() => false);

      // active 탭의 aria-selected 검증
      await page
        .getByRole('tab', { selected: true, name: /작성댓글|댓글/ })
        .first()
        .waitFor({ state: 'visible', timeout: 2000 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-activity-tab-comments',
    );

    test.info().annotations.push({
      type: 'b-303-activity-comments',
      description: `branch=${branch} urlMatched=${urlMatched} url=${page.url()}`,
    });
  });

  // ============================================================
  // step-04: ?activeTab=liked-posts (추천글)
  // ============================================================
  test('step-04-activity-tab-likes', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage', '?activeTab=liked-posts');

    if (branch === 'mypage') {
      await page
        .waitForFunction(
          () => /[?&]activeTab=liked-posts\b/.test(window.location.search),
          undefined,
          { timeout: 3000 },
        )
        .catch(() => undefined);

      // 키보드 화살표 네비게이션 (B-303) — focus-visible:ring 시각 회귀
      const tab = page.getByRole('tab', { name: /추천(글)?/ }).first();
      if (await tab.isVisible({ timeout: 2500 }).catch(() => false)) {
        await tab.focus().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-activity-tab-likes',
    );
  });

  // ============================================================
  // step-05: ProfileBadges 영역 (B-302 다크 토큰)
  //  paper-50/900 + postal-navy/seal-gold/stamp-red 다크 회귀.
  //  카테고리 필터 role="tablist" + aria-selected + min-h-[44px].
  // ============================================================
  test('step-05-profile-badges', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage');

    if (branch === 'mypage') {
      // ProfileBadges 영역으로 스크롤 — 페이지 중간/하단 위치
      const badgeTitle = page
        .getByText(/배지|뱃지|곧 달성 가능한/)
        .first();
      if (await badgeTitle.isVisible({ timeout: 2500 }).catch(() => false)) {
        await badgeTitle.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(300);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-profile-badges',
    );
  });

  // ============================================================
  // step-06: /settings 진입 base — NotificationSettings + AccountSettings
  // ============================================================
  test('step-06-settings-base', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/settings');

    if (branch === 'settings') {
      // NotificationSettings 의 "푸시 알림 설정" 헤더 또는
      // AccountSettings 의 "계정 관리" 등이 마운트 되었는지 확인
      await page
        .getByText(/푸시 알림 설정|알림 설정과 계정/)
        .first()
        .waitFor({ state: 'visible', timeout: 4000 })
        .catch(() => undefined);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-06-settings-base',
    );

    test.info().annotations.push({
      type: 'settings-branch',
      description: `branch=${branch} url=${page.url()}`,
    });

    if (branch === 'settings') {
      const triggers = await scanInteractionTriggers(page);
      test.info().annotations.push({
        type: 'settings-triggers',
        description: JSON.stringify(triggers, null, 2),
      });
    }

    expect(page.url()).toMatch(/\/(settings|login)/);
  });

  // ============================================================
  // step-07: NotificationSettings indeterminate (B-304)
  //  - 일부 알림만 켠 상태 (3/4) 시 aria-checked="mixed" + dash 마커
  //  - 보조 텍스트 "일부 알림만 켜져 있어요 (3/4)" 노출
  //  - settings 분기 도달 못해도 fail-soft 로 캡처 자체는 의미 있음
  // ============================================================
  test('step-07-notification-indeterminate', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/settings');

    if (branch === 'settings') {
      // 보조 텍스트 또는 "일부 알림만 켜져 있어요" 안내가 보이는지 확인
      const indeterminateHint = page
        .getByText(/일부 알림만 켜져 있어요|일부.*알림/)
        .first();
      if (await indeterminateHint.isVisible({ timeout: 2500 }).catch(() => false)) {
        await indeterminateHint.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      } else {
        // 보조 텍스트 미표시 분기 — Switch 가 mounted 인지만 확인
        const allSwitch = page
          .getByRole('switch')
          .first();
        if (await allSwitch.isVisible({ timeout: 2500 }).catch(() => false)) {
          await allSwitch.scrollIntoViewIfNeeded().catch(() => undefined);
          await page.waitForTimeout(200);
        }
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-07-notification-indeterminate',
    );
  });

  // ============================================================
  // step-08: WithdrawConfirmModal (B-305)
  //  - 헤딩 "이 편지함을 영원히 닫을까요?"
  //  - paper-50/900 + stamp-red 토큰
  //  - settings 분기에서 "회원 탈퇴" 버튼 클릭 시도, 모달 오픈 확인
  // ============================================================
  test('step-08-withdraw-modal', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/settings');

    if (branch === 'settings') {
      // "회원 탈퇴" 버튼 클릭 시도 — AccountSettings 영역
      const withdrawBtn = page
        .getByRole('button', { name: /회원\s*탈퇴|탈퇴하기/ })
        .first();
      if (await withdrawBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
        await withdrawBtn.scrollIntoViewIfNeeded().catch(() => undefined);
        await withdrawBtn.click().catch(() => undefined);
        await page.waitForTimeout(700);

        // 모달 헤딩 확인 — "이 편지함을 영원히 닫을까요?"
        await page
          .getByText(/이 편지함을 영원히 닫을까요/)
          .first()
          .waitFor({ state: 'visible', timeout: 3000 })
          .catch(() => undefined);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-08-withdraw-modal',
    );

    test.info().annotations.push({
      type: 'b-305-withdraw',
      description: `branch=${branch} url=${page.url()}`,
    });
  });

  // ============================================================
  // step-09: 빈 상태 4종 메타포 카피
  //  비인증 + backend 미가용 → 데이터 0건 분기 가능 → 빈 상태 노출 케이스.
  //  4개 카피 (편지/답장/마음) 중 어느 하나라도 매칭되면 시각 회귀.
  //  fail-soft: login 분기면 그대로 캡처.
  // ============================================================
  test('step-09-empty-state-copy', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage', '?activeTab=liked-comments');

    if (branch === 'mypage') {
      const empty = page
        .getByText(
          /아직 작성한 편지가 없어요|아직 남긴 답장이 없어요|아직 좋아요한 편지가 없어요|아직 좋아요한 답장이 없어요/,
        )
        .first();
      if (await empty.isVisible({ timeout: 3000 }).catch(() => false)) {
        await empty.scrollIntoViewIfNeeded().catch(() => undefined);
        await page.waitForTimeout(200);
      }
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-09-empty-state-copy',
    );
  });

  // ============================================================
  // step-10: 모바일 sticky 헤더 + 단축 라벨
  //  - mobile-375 프로젝트에서 "작성/댓글/추천/추천♡" 단축 라벨 노출
  //  - desktop-1280 프로젝트에서는 풀 라벨
  //  - sticky top-0 + bg-paper-50/95 + backdrop-blur-sm 시각 회귀
  // ============================================================
  test('step-10-mobile-sticky-short-label', async ({ page }, testInfo) => {
    const branch = await gotoProtected(page, '/mypage');

    if (branch === 'mypage') {
      // 페이지 최상단으로
      await page.evaluate(() => window.scrollTo(0, 0));
      await page.waitForTimeout(200);

      // 탭리스트 visibility 확인
      await page
        .getByRole('tablist', { name: /활동 내역 탭/ })
        .first()
        .waitFor({ state: 'visible', timeout: 2500 })
        .catch(() => undefined);

      // 약간 스크롤하여 sticky 효과가 보이는 위치로
      await page.evaluate(() => window.scrollTo(0, 200));
      await page.waitForTimeout(300);
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-10-mobile-sticky-short-label',
    );

    // sanity — 페이지 자체는 mypage/login 어느 분기든 mount 완료
    expect(page.url()).toMatch(/\/(mypage|login)/);
  });
});
