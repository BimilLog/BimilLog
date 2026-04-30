import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-4-oauth-flow';
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
 * round-4-oauth-flow iter-1 — 카카오/네이버/구글 OAuth 콜백 흐름 캡처.
 *
 * baseline 시드 미구축 + 자격증명 노출 위험으로 실제 OAuth 인증 흐름은
 * 본 iter 에서 다루지 않는다. 대신 다음 항목들을 캡처한다:
 *
 *  1) /login 진입 (OAuth 버튼 3종 + 동적 카피 진입점)
 *  2) `/auth/callback` 직접 진입 (no code) — Suspense fallback / 회복 흐름
 *  3) `/auth/callback?code=invalid_test_code` — 백엔드가 거부 → callback_failed 로 redirect.
 *     중간 로딩 화면 캡처 시도.
 *  4) `/auth/callback/naver?code=invalid&state=invalid` — 동일 패턴 (Suspense fallback 포함)
 *  5) `/auth/callback/google?code=invalid&state=invalid` — 동일 패턴
 *  6) `/login?error={code}&provider={KAKAO|NAVER|GOOGLE}` — B-403 동적 카피 검증
 *
 * iter-1 에서 권고된 frontend-changes (B-401 Suspense, B-402 state.redirect, B-403 provider 카피,
 * B-404 recovered=1 토스트, B-405 250→80ms, friendsConsentFlow KAKAO 한정) 의 시각적 회귀를
 * 가장 가벼운 방식으로 보장한다.
 */
test.describe('round-4-oauth-flow', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 컨텍스트 보장 — 이전 라운드의 잔존 쿠키가 /login → / 로 자동 redirect 시키는 것 방지
    await context.clearCookies();
  });

  // ============================================================
  // step-01: /login 진입 (OAuth 버튼 3종 진입점)
  // ============================================================
  test('step-01-login-base', async ({ page }, testInfo) => {
    await page.goto('/login');
    await waitSettled(page);

    // round-3 step-01 과 동일하게 카드 헤딩 + 3종 OAuth 버튼 가시성 검증
    await expect(page.getByRole('heading', { name: /비밀로그 시작하기/ })).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-kakao"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-naver"]')).toBeVisible();
    await expect(page.locator('[data-testid="login-oauth-google"]')).toBeVisible();

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-login-base',
    );

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'login-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // ============================================================
  // step-02-callback-suspense-fallback-kakao
  // `/auth/callback` 직접 진입 (no code) → useKakaoCallback 회복 흐름:
  //   isRecovering=true → AuthLoadingScreen variant="recovery" (postal-navy spinner)
  //   "이미 처리된 인증을 확인하는 중..." → getCurrentUser 401 → /login?error=no_code&provider=KAKAO
  // 첫 진입 직후 한 프레임에 Suspense fallback 또는 recovery 화면이 잡힐 수 있다.
  // ============================================================
  test('step-02-callback-suspense-fallback-kakao', async ({ page }, testInfo) => {
    // navigate 후 즉시 캡처 시도 (waitForLoadState 만 짧게)
    const navigation = page.goto('/auth/callback');
    // 첫 페인트 직후 로딩 화면을 잡기 위해 너무 길게 기다리지 않는다
    await page.waitForLoadState('domcontentloaded');
    // recovery 톤 spinner 가 잡히는 한 순간 (B-401 Suspense fallback 또는 isRecovering=true)
    await page.waitForTimeout(150);

    // 캡처: 콜백 진입 직후 paper 메타포 풀스크린 (Suspense fallback 또는 recovery 화면)
    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-callback-suspense-fallback-kakao',
    );

    // 흐름 종료까지 대기 (없어도 되지만 다음 테스트로 누수되지 않도록)
    await navigation.catch(() => undefined);
    await page.waitForURL(/\/login\?error=no_code/, { timeout: 5000 }).catch(() => undefined);
  });

  // ============================================================
  // step-02-callback-loading-kakao-mock
  // `/auth/callback?code=invalid_test_code` → 백엔드가 invalid code 거부.
  // 중간 로딩 화면("카카오 인증 처리 중..." → "사용자 정보 확인 중...") 캡처 시도.
  // ============================================================
  test('step-02-callback-loading-kakao-mock', async ({ page }, testInfo) => {
    // 카카오 로그인 API 응답을 인위적으로 지연시켜 로딩 화면을 캡처한다.
    // 자격증명 노출 없이 백엔드가 자동으로 invalid code 를 거부할 것을 활용.
    await page.route('**/api/auth/kakao/login', async (route) => {
      // 1.2초 지연 후 백엔드 실제 응답을 통과시킨다 (실제 결과는 거부 expected)
      await new Promise((r) => setTimeout(r, 1200));
      await route.continue();
    });

    const navigation = page.goto('/auth/callback?code=invalid_test_code');
    await page.waitForLoadState('domcontentloaded');
    // 첫 단계 ("카카오 인증 처리 중...") 또는 두 번째 단계가 잡히도록 짧은 settle
    await page.waitForTimeout(500);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-callback-loading-kakao-mock',
    );

    // 흐름 종료 (login error 페이지로 redirect) 대기
    await navigation.catch(() => undefined);
    await page
      .waitForURL(/\/login\?error=/, { timeout: 5000 })
      .catch(() => undefined);
  });

  // ============================================================
  // step-03-callback-loading-naver-mock
  // 네이버 콜백: invalid code + invalid state → 처리 흐름 진입 화면 캡처.
  // useSocialCallback('NAVER') 가 Suspense wrap 내부에서 진입한다.
  // ============================================================
  test('step-03-callback-loading-naver-mock', async ({ page }, testInfo) => {
    await page.route('**/api/auth/naver/login', async (route) => {
      await new Promise((r) => setTimeout(r, 1200));
      await route.continue();
    });

    const naverState = encodeURIComponent('invalid');
    const navigation = page.goto(
      `/auth/callback/naver?code=invalid&state=${naverState}`,
    );
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(500);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-03-callback-loading-naver-mock',
    );

    await navigation.catch(() => undefined);
    await page
      .waitForURL(/\/login\?error=/, { timeout: 5000 })
      .catch(() => undefined);
  });

  // ============================================================
  // step-04-callback-loading-google-mock
  // 구글 콜백: invalid code + invalid state.
  // ============================================================
  test('step-04-callback-loading-google-mock', async ({ page }, testInfo) => {
    await page.route('**/api/auth/google/login', async (route) => {
      await new Promise((r) => setTimeout(r, 1200));
      await route.continue();
    });

    const googleState = encodeURIComponent('invalid');
    const navigation = page.goto(
      `/auth/callback/google?code=invalid&state=${googleState}`,
    );
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(500);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-04-callback-loading-google-mock',
    );

    await navigation.catch(() => undefined);
    await page
      .waitForURL(/\/login\?error=/, { timeout: 5000 })
      .catch(() => undefined);
  });

  // ============================================================
  // step-05-login-error-with-provider-kakao
  // `/login?error=no_code&provider=KAKAO` → B-403 동적 카피 검증.
  // 카카오 한글명이 포함된 카피 ("카카오 로그인에 실패했어요...") 노출.
  // ============================================================
  test('step-05-login-error-with-provider-kakao', async ({ page }, testInfo) => {
    await page.goto('/login?error=no_code&provider=KAKAO');
    await waitSettled(page);

    // ErrorAlert 카드 본문 검증 (B-403 동적 카피)
    await expect(page.getByText('로그인 오류')).toBeVisible();
    // "카카오 로그인에 실패했어요" 또는 "카카오 로그인에 실패" 부분 매칭
    await expect(page.getByText(/카카오 로그인에 실패/)).toBeVisible();
    await expect(page.getByText('다시 로그인하기')).toBeVisible();

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-login-error-with-provider-kakao',
    );
  });

  // ============================================================
  // step-05-login-error-with-provider-naver
  // `/login?error=callback_failed&provider=NAVER` → 일반 카피 (provider 무관) 검증.
  // callback_failed 는 macroMessages 의 일반 카피이므로 provider 한글명은 미사용.
  // ============================================================
  test('step-05-login-error-with-provider-naver', async ({ page }, testInfo) => {
    await page.goto('/login?error=callback_failed&provider=NAVER');
    await waitSettled(page);

    await expect(page.getByText('로그인 오류')).toBeVisible();
    // callback_failed 의 일반 카피 ("로그인 처리 중 문제가 발생했어요...")
    await expect(page.getByText(/로그인 처리 중 문제가 발생했어요/)).toBeVisible();
    await expect(page.getByText('다시 로그인하기')).toBeVisible();

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-login-error-with-provider-naver',
    );
  });

  // ============================================================
  // step-05-login-error-with-provider-google
  // `/login?error=server_error&provider=GOOGLE` → 구글 한글명 동적 카피.
  // server_error 는 oauthMessages 매핑이라 provider 한글명 ("구글") 이 카피에 등장.
  // ============================================================
  test('step-05-login-error-with-provider-google', async ({ page }, testInfo) => {
    await page.goto('/login?error=server_error&provider=GOOGLE');
    await waitSettled(page);

    await expect(page.getByText('로그인 오류')).toBeVisible();
    // server_error oauthMessage: "{providerLabel} 인증 서버에서 일시적인 오류가 발생했어요."
    await expect(page.getByText(/구글 인증 서버/)).toBeVisible();
    await expect(page.getByText('다시 로그인하기')).toBeVisible();

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-05-login-error-with-provider-google',
    );
  });
});
