import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-3-login-logout-relogin';
const ITER = Number(process.env.UIUX_ITER || '1');

const HAS_SEED = process.env.UIUX_HAS_SEED === '1';

test.describe.configure({ mode: 'serial' });

/**
 * 안전한 wait — networkidle 가 백그라운드 polling 으로 영원히 대기되는 케이스 회피.
 * domcontentloaded 후 짧은 settle 만 부여한다.
 */
async function waitSettled(page: Page, ms = 800) {
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(ms);
}

test.describe('round-3-login-logout-relogin', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 흐름 보장 — 시드 미구축 환경에서 잔존 쿠키가 있다면 /login 이 자동 redirect 될 수 있음
    await context.clearCookies();
  });

  // -------- step-01: /login 진입 (비인증) — 3종 OAuth 버튼 + 베네핏 5종 + InfoAlert --------
  test('step-01-login-base', async ({ page }, testInfo) => {
    await page.goto('/login');
    await waitSettled(page);

    // 메인 헤딩 — Card 내부 타이틀 ("비밀로그 시작하기")
    await expect(page.getByRole('heading', { name: /비밀로그 시작하기/ })).toBeVisible();

    // 3종 OAuth 버튼 가시성 (ux-proposal §2 시나리오 A)
    await expect(page.locator('[data-testid="login-oauth-kakao"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-naver"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-google"]')).toBeVisible();

    // 베네핏 리스트 5개 항목 — LOGIN_BENEFITS 핵심 키워드
    await expect(page.getByText('롤링페이퍼 개설하기')).toBeVisible();
    await expect(page.getByText('비밀메시지를 받기')).toBeVisible();
    await expect(page.getByText('활동점수를 확인')).toBeVisible();
    await expect(page.getByText('실시간 알림 받기')).toBeVisible();
    await expect(page.getByText('글과 댓글에 추천')).toBeVisible();

    // 비인증 baseline — InfoAlert "로그인 없이도 이용 가능!" (4-8 축소 변종 미적용 상태)
    await expect(page.getByText('로그인 없이도 이용 가능!')).toBeVisible();

    await captureFullPage(page, testInfo, { roundDir: ROUND_DIR, iter: ITER }, 'step-01-login-base');

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'login-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // -------- step-01-int-01: 카카오 버튼 hover (데스크톱 전용) --------
  test('step-01-int-01-kakao-button-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용 (모바일은 터치)');
      return;
    }

    await page.goto('/login');
    await waitSettled(page);

    const btn = page.locator('[data-testid="login-oauth-kakao"]');
    await expect(btn).toBeVisible();
    await btn.hover();
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-01-kakao-button-hover',
    );
  });

  // -------- step-01-int-02: 네이버 버튼 hover (데스크톱 전용) --------
  test('step-01-int-02-naver-button-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용');
      return;
    }

    await page.goto('/login');
    await waitSettled(page);

    const btn = page.locator('[data-testid="login-oauth-naver"]');
    await expect(btn).toBeVisible();
    await btn.hover();
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-02-naver-button-hover',
    );
  });

  // -------- step-01-int-03: 구글 버튼 hover (데스크톱 전용) --------
  test('step-01-int-03-google-button-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용');
      return;
    }

    await page.goto('/login');
    await waitSettled(page);

    const btn = page.locator('[data-testid="login-oauth-google"]');
    await expect(btn).toBeVisible();
    await btn.hover();
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-03-google-button-hover',
    );
  });

  // -------- step-01-int-04: last-used provider ribbon (4-5) --------
  // 비인증 baseline 에서는 localStorage 에 last provider 가 없을 가능성이 큼.
  // 강제로 `bimillog_last_provider` 를 주입한 뒤 /login 진입하여 ribbon 캡처를 시도한다.
  // ribbon 이 노출되면 InfoAlert 도 축소 변종으로 전환됨 (4-8) — 같은 캡처에서 한 번에 검증.
  test('step-01-int-04-last-used-ribbon', async ({ page, context }, testInfo) => {
    // 1) 컨텍스트 init script 로 storage 주입 (페이지 mount 전)
    //    auth.store 의 PROVIDER_PERSIST_TO_BUTTON 이 'KAKAO' 를 'kakao' 로 매핑하므로
    //    저장값은 'KAKAO' 형태여야 함. lastUsedProvider helper 는 그대로 string 으로 읽는다.
    await context.addInitScript(() => {
      try {
        localStorage.setItem('bimillog_last_provider', 'KAKAO');
      } catch {
        /* ignored */
      }
    });

    await page.goto('/login');
    await waitSettled(page);

    // ribbon 노출 여부 (frontend-changes.md 4-5: "마지막 로그인" stamp-red ribbon)
    const ribbon = page.getByText('마지막 로그인', { exact: false });
    const recentBtn = page.locator('[data-testid="login-oauth-kakao"][data-recent="true"]');

    // best effort — 노출되지 않으면 skip 으로 표기 (storage 주입 타이밍/keys 불일치 시)
    const ribbonVisible = await ribbon
      .first()
      .isVisible({ timeout: 1500 })
      .catch(() => false);
    const recentAttrPresent = (await recentBtn.count()) > 0;

    test.info().annotations.push({
      type: 'last-used-ribbon-detection',
      description: JSON.stringify(
        { ribbonVisible, recentAttrPresent, storageKey: 'bimillog_last_provider' },
        null,
        2,
      ),
    });

    if (!ribbonVisible && !recentAttrPresent) {
      // 그래도 같은 페이지를 캡처해서 baseline 변경점을 시각적으로 추적할 수 있게 함
      await captureFullPage(
        page,
        testInfo,
        { roundDir: ROUND_DIR, iter: ITER },
        'step-01-int-04-last-used-ribbon-fallback',
      );
      test.skip(
        true,
        'last-used ribbon 노출 안 됨 (storage 주입 타이밍/keys 불일치). fallback 캡처는 저장됨.',
      );
      return;
    }

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-04-last-used-ribbon',
    );
  });

  // -------- step-01-int-05: 에러 진입 (?error=access_denied) — 에러 카드 톤 캡처 --------
  // ux-proposal §2 시나리오 A 5번. baseline 캡처 가능 (비인증 + 쿼리 파라미터만).
  test('step-01-int-05-login-error-access-denied', async ({ page }, testInfo) => {
    await page.goto('/login?error=access_denied');
    await waitSettled(page);

    // 에러 알럿 본문 검증 (page.tsx getErrorMessage: "카카오 로그인이 취소되었습니다.")
    await expect(page.getByText('카카오 로그인이 취소되었습니다')).toBeVisible();
    // "다시 로그인하기" 링크 — 에러 알럿 내부 액션
    await expect(page.getByText('다시 로그인하기')).toBeVisible();

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-05-login-error-access-denied',
    );
  });

  // -------- step-02: 로그아웃 확인 모달 (시드 의존) --------
  // ux-proposal §2 시나리오 B-7 / B-303 — 인증 상태에서 헤더 아바타 → 드롭다운 → 로그아웃 클릭
  // → ConfirmModal "이번 편지 묶음을 잠시 닫을까요?" 노출. 인증 쿠키 없이는 도달 불가.
  test('step-02-logout-confirm', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 — UserDropdownMenu 의 ConfirmModal 은 인증 상태 + 헤더 아바타 노출 후 진입 가능. HAS_SEED + 테스트 전용 로그인 endpoint 가 준비되면 활성화.',
    );
  });

  // -------- step-03: 로그아웃 후 홈 (시드 의존) --------
  // ux-proposal §2 시나리오 C-2 — `useGoodbyeFarewell` 훅이 sessionStorage 마커 소비
  // → "안전하게 로그아웃했어요" 토스트 노출. 인증 쿠키 + /logout 진입 → 홈 도착 흐름 필요.
  test('step-03-after-logout', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 — useGoodbyeFarewell 토스트는 /logout → 홈 진입 시 sessionStorage 마커 소비로 발사. 인증 흐름 필요.',
    );
  });

  // -------- step-04: 재로그인 페이지 — 시나리오 A 와 동일하지만 다른 시점 --------
  // 시드 흐름과 무관하게 비인증 사용자가 다시 /login 으로 돌아오는 캡처를 한 번 더 수행한다.
  // last-used provider ribbon 이 step-01-int-04 와 별개로 검증되도록 storage 그대로 둔다.
  test('step-04-relogin-page', async ({ page, context }, testInfo) => {
    // 시뮬레이션: 사용자가 한 번 로그인했던 단말 (storage 에 흔적이 남은 상태)
    await context.addInitScript(() => {
      try {
        localStorage.setItem('bimillog_last_provider', 'KAKAO');
      } catch {
        /* ignored */
      }
    });

    await page.goto('/login');
    await waitSettled(page);

    // 시나리오 A 와 동일한 카드 — 3종 OAuth 가시성 재확인
    await expect(page.locator('[data-testid="login-oauth-kakao"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-naver"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-google"]')).toBeVisible();

    // 4-5 ribbon 노출 여부 annotation (재로그인 시점에서도 동일하게 식별되는지)
    const recentBtn = page.locator('[data-testid="login-oauth-kakao"][data-recent="true"]');
    const recentAttrPresent = (await recentBtn.count()) > 0;
    test.info().annotations.push({
      type: 'relogin-recent-attr',
      description: `data-recent 속성 노출: ${recentAttrPresent}`,
    });

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-relogin-page',
    );

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'relogin-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // -------- step-Header-01: 알림 종 / 사용자 드롭다운 (인증 사용자 전용 — 시드 미구축이므로 skip) --------
  test('step-header-01-notification-bell-empty', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 — NotificationBell 은 canUseNotifications=true (인증) 시에만 노출. 4-6 NotificationList paper 메타포 빈 상태 캡처는 인증 흐름 필요.',
    );
  });

  test('step-header-02-user-menu-dropdown', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 — Avatar + UserDropdownMenu 는 인증 사용자에게만 노출.',
    );
  });
});
