import { test, expect, type Page } from '@playwright/test';
import { setTheme, themeFromProjectName, viewportFromProjectName } from '../helpers/theme';
import { captureFullPage } from '../helpers/screenshot';
import { scanInteractionTriggers } from '../helpers/interaction-scanner';

const ROUND_DIR = process.env.UIUX_ROUND_DIR || 'round-2-signup-to-first-action';
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

test.describe('round-2-signup-to-first-action', () => {
  test.beforeEach(async ({ page, context }, testInfo) => {
    await setTheme(page, themeFromProjectName(testInfo.project.name));
    // 비인증 흐름 보장 — 시드 미구축 환경에서 잔존 쿠키가 있다면 가입 페이지가 자동 redirect 될 수 있음
    await context.clearCookies();
  });

  // -------- step-01: /signup 진입 (비인증) — 3종 OAuth 버튼/약관 안내/베네핏 리스트 --------
  test('step-01-signup-base', async ({ page }, testInfo) => {
    await page.goto('/signup');
    await waitSettled(page);

    // 회원가입 카드 헤딩 (h1 로는 노출되지 않음 — Card 내부 Title)
    await expect(page.getByText('회원가입', { exact: true })).toBeVisible();

    // 3종 OAuth 버튼 모두 노출 (F-201)
    await expect(page.locator('[data-testid="signup-oauth-kakao"]')).toBeVisible();
    await expect(page.locator('[data-testid="signup-oauth-naver"]')).toBeVisible();
    await expect(page.locator('[data-testid="signup-oauth-google"]')).toBeVisible();

    // 약관/개인정보 처리방침 링크 (F-204)
    await expect(page.getByRole('link', { name: '서비스 이용약관' })).toBeVisible();
    await expect(page.getByRole('link', { name: '개인정보 처리방침' })).toBeVisible();

    // 베네핏 리스트 (signup 변형: dot variant) (5-10)
    // BenefitsList 의 라벨로 존재 검증 (signup 은 "가입하면 이런 일이" label)
    await expect(page.getByText('가입하면 이런 일이')).toBeVisible();
    // 베네핏 항목 핵심 키워드 (3개)
    await expect(page.getByText('비밀 롤링페이퍼')).toBeVisible();
    await expect(page.getByText('친구의 마음')).toBeVisible();
    await expect(page.getByText('활동 알림')).toBeVisible();

    await captureFullPage(page, testInfo, { roundDir: ROUND_DIR, iter: ITER }, 'step-01-signup-base');

    const triggers = await scanInteractionTriggers(page);
    test.info().annotations.push({
      type: 'signup-triggers',
      description: JSON.stringify(triggers, null, 2),
    });
  });

  // -------- step-01-int-01: 카카오 버튼 hover (데스크톱) --------
  test('step-01-int-01-kakao-button-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용 (모바일은 터치)');
      return;
    }

    await page.goto('/signup');
    await waitSettled(page);

    const btn = page.locator('[data-testid="signup-oauth-kakao"]');
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

  // -------- step-01-int-02: 네이버 버튼 hover (데스크톱) --------
  test('step-01-int-02-naver-button-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용');
      return;
    }

    await page.goto('/signup');
    await waitSettled(page);

    const btn = page.locator('[data-testid="signup-oauth-naver"]');
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

  // -------- step-01-int-03: 구글 버튼 hover (데스크톱) --------
  test('step-01-int-03-google-button-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용');
      return;
    }

    await page.goto('/signup');
    await waitSettled(page);

    const btn = page.locator('[data-testid="signup-oauth-google"]');
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

  // -------- step-01-int-04: 약관 링크 hover (데스크톱) --------
  test('step-01-int-04-terms-link-hover', async ({ page }, testInfo) => {
    const vp = viewportFromProjectName(testInfo.project.name);
    if (vp !== 'desktop-1280') {
      test.skip(true, 'hover 인터랙션은 데스크톱 전용');
      return;
    }

    await page.goto('/signup');
    await waitSettled(page);

    const link = page.getByRole('link', { name: '서비스 이용약관' });
    await expect(link).toBeVisible();
    await link.hover();
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-01-int-04-terms-link-hover',
    );
  });

  // -------- step-02: 콜백 회복 화면 (F-203 검증) --------
  // /auth/callback 에 code/error 모두 없이 진입하면:
  //  1) 인증된 상태 → setProvider + /?recovered=1 로 redirect
  //  2) 비인증 → /login?error=no_code 로 redirect
  // 비인증 baseline 에서는 (2) 케이스를 캡처. AuthLoadingScreen 의 "이미 처리된 인증을 확인하는 중..."
  // 단계가 잠깐 보였다가 login?error=no_code 로 이동되어 login 페이지의 에러 토스트/표시가 노출됨.
  test('step-02-signup-callback-recovery', async ({ page }, testInfo) => {
    // 직접 에러 시뮬: ?error=test_failure 로 진입하면
    // useKakaoCallback 이 즉시 /login?error=test_failure 로 redirect.
    // 실제 사용자가 카카오에서 거부했을 때 보는 동선 (= F-203 가 회복하려는 상황의 반대 케이스).
    await page.goto('/auth/callback?error=user_cancelled');
    await waitSettled(page, 1500);

    // login 페이지로 redirect 되어야 함 (실제 동작 검증)
    // URL 또는 본문 확인 — login 페이지에 도달했는지
    const currentUrl = page.url();
    test.info().annotations.push({
      type: 'callback-redirect-target',
      description: `error=user_cancelled → redirected to: ${currentUrl}`,
    });

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02-signup-callback-error-redirect',
    );
  });

  // -------- step-02b: 콜백 — code/error 모두 없는 회복 시도 화면 --------
  // /auth/callback (param 없음) → useKakaoCallback 이 getCurrentUser() 호출 후
  // 비인증이면 /login?error=no_code 로 redirect. 그 짧은 사이 AuthLoadingScreen 의
  // "이미 처리된 인증을 확인하는 중..." 단계가 잠깐 노출됨.
  test('step-02b-signup-callback-no-code', async ({ page }, testInfo) => {
    await page.goto('/auth/callback');
    // 0.3 초 시점 — 회복 시도 단계 캡처 시도 (race 가 있으니 best effort)
    await page.waitForTimeout(300);

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02b-signup-callback-recovery-attempt',
    );

    // 최종 settle — login 페이지로 이동했는지 기록
    await page.waitForTimeout(2500);
    const finalUrl = page.url();
    test.info().annotations.push({
      type: 'callback-no-code-final',
      description: `no params → redirected to: ${finalUrl}`,
    });

    await captureFullPage(
      page,
      testInfo,
      { roundDir: ROUND_DIR, iter: ITER },
      'step-02b-signup-callback-recovery-final',
    );
  });

  // -------- step-03: 가입 성공 토스트 (시드 의존) --------
  test('step-03-welcome-toast-after-signup', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 — OAuth 콜백 후 markPendingWelcome 까지 도달 불가 (실제 카카오/네이버/구글 인증 필요). HAS_SEED 와 테스트 전용 로그인 endpoint 가 준비되면 활성화.',
    );
  });

  // -------- step-04: 신규 사용자 홈 — 온보딩 카드 (시드 의존, 4-C 다음 iter 이월 대상) --------
  test('step-04-newcomer-home-onboarding-card', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 + 4-C 신규 온보딩 카드는 iter-1 이월 항목 (frontend-changes.md 참고).',
    );
  });

  // -------- step-05: 첫 액션 — 내 페이퍼 진입 (시드 의존) --------
  test('step-05-first-action-empty-paper', async () => {
    test.skip(
      true,
      'baseline 시드 미구축 — 인증 + 본인 페이퍼 + 빈 메시지 상태 필요.',
    );
  });

  // -------- step-Header-01: 인증 사용자 메뉴 드롭다운 (시드 의존) --------
  test('step-header-01-user-menu-dropdown', async () => {
    test.skip(true, 'baseline 시드 미구축 — 비인증 사용자에게는 user menu 미노출.');
  });
});
