import type { BrowserContext, Page } from '@playwright/test';

/**
 * baseline 시드가 발급한 JWT 액세스 토큰을 쿠키로 주입하여
 * OAuth 흐름을 거치지 않고 인증 상태를 만든다.
 * 토큰은 baseline 시드 결과 파일(BACKEND가 작성)에서 읽어온다.
 */
export async function loginWithSeedToken(
  context: BrowserContext,
  jwt: string,
  refreshToken: string,
): Promise<void> {
  await context.addCookies([
    {
      name: 'jwt_access_token',
      value: jwt,
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
    {
      name: 'jwt_refresh_token',
      value: refreshToken,
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);
}

/**
 * 실제 카카오 OAuth 흐름을 검증해야 하는 여정에서만 사용.
 * 자격증명은 .env.uiux-test-accounts 에서만 읽고 어떤 산출물에도 기록 금지.
 */
export async function loginViaKakao(page: Page): Promise<void> {
  const email = process.env.KAKAO_TEST_EMAIL;
  const password = process.env.KAKAO_TEST_PASSWORD;
  if (!email || !password) {
    throw new Error(
      'KAKAO_TEST_EMAIL / KAKAO_TEST_PASSWORD 환경변수가 없습니다. ' +
      '.env.uiux-test-accounts 파일을 만들고 dotenv 로 로드하세요.',
    );
  }

  await page.goto('/login');
  await page.getByRole('button', { name: /카카오/i }).click();
  await page.fill('input[name="loginId"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL(/\/(?!login|signup).*/, { timeout: 30000 });
}
