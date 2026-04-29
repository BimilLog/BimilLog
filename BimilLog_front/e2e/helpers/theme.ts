import type { Page } from '@playwright/test';

export type Theme = 'light' | 'dark';

/**
 * E2E 라운드 시작 시 페이지의 테마를 강제로 설정한다.
 * - localStorage 의 next-themes 키 ("theme") 와 zustand persist 키 모두 시도
 * - HTML class("dark") 도 직접 토글하여 hydration 직후 깜박임 방지
 */
export async function setTheme(page: Page, theme: Theme): Promise<void> {
  await page.addInitScript((t: Theme) => {
    try {
      localStorage.setItem('theme', t);
      localStorage.setItem('theme-storage', JSON.stringify({ state: { theme: t }, version: 0 }));
    } catch (_) { /* ignored */ }

    const apply = () => {
      if (!document.documentElement) return;
      if (t === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    };

    // documentElement 가 아직 없을 수 있으므로 즉시 + DOMContentLoaded 양쪽에서 적용
    apply();
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', apply, { once: true });
    }
  }, theme);
}

export function themeFromProjectName(projectName: string): Theme {
  return projectName.endsWith('-dark') ? 'dark' : 'light';
}

export function viewportFromProjectName(projectName: string): 'mobile-375' | 'desktop-1280' {
  return projectName.startsWith('mobile-375') ? 'mobile-375' : 'desktop-1280';
}
