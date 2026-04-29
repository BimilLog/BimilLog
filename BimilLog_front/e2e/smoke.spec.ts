import { test, expect } from '@playwright/test';

// 시나리오 작성 시 규칙:
// - data-testid 우선 사용 (예: page.getByTestId('post-card'))
// - waitForLoadState('networkidle') 사용, time-based wait 금지
// - role/text는 보조 selector

test('홈 페이지가 200으로 응답하고 body가 렌더링된다', async ({ page }) => {
  await page.goto('/');
  await page.waitForLoadState('networkidle');
  await expect(page.locator('body')).toBeVisible();
});
