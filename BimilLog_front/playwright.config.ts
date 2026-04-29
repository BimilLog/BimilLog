import { defineConfig, devices } from '@playwright/test';

const baseMobile = {
  ...devices['Desktop Chrome'],
  viewport: { width: 375, height: 667 },
  userAgent:
    'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1',
  hasTouch: true,
  isMobile: true,
  deviceScaleFactor: 2,
};

const baseDesktop = {
  ...devices['Desktop Chrome'],
  viewport: { width: 1280, height: 800 },
};

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:3000',
    actionTimeout: 10000,
    navigationTimeout: 30000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'mobile-375-light',   use: { ...baseMobile,  colorScheme: 'light' } },
    { name: 'mobile-375-dark',    use: { ...baseMobile,  colorScheme: 'dark'  } },
    { name: 'desktop-1280-light', use: { ...baseDesktop, colorScheme: 'light' } },
    { name: 'desktop-1280-dark',  use: { ...baseDesktop, colorScheme: 'dark'  } },
  ],
});
