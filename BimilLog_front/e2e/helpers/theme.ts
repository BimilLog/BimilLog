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
      // next-themes 는 storageKey 에 단순 string 을 저장한다 (providers/theme-provider.tsx).
      // 한편 호환을 위해 일반 'theme' 키도 함께 갱신한다.
      localStorage.setItem('theme-storage', t);
      localStorage.setItem('theme', t);
    } catch (_) { /* ignored */ }

    const apply = () => {
      if (!document.documentElement) return;
      if (t === 'dark') {
        document.documentElement.classList.add('dark');
        document.documentElement.style.colorScheme = 'dark';
      } else {
        document.documentElement.classList.remove('dark');
        document.documentElement.style.colorScheme = 'light';
      }
    };

    // documentElement 가 아직 없을 수 있으므로 즉시 + DOMContentLoaded 양쪽에서 적용
    apply();
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', apply, { once: true });
    }

    // next-themes 가 system 으로 fallback 하려고 다시 class 를 제거할 수 있으므로
    // MutationObserver 로 한 번 더 보정한다 (페이지 진입 직후 ~2s 동안).
    let attempts = 0;
    const obs = new MutationObserver(() => {
      const hasDark = document.documentElement.classList.contains('dark');
      if (t === 'dark' && !hasDark) document.documentElement.classList.add('dark');
      if (t === 'light' && hasDark) document.documentElement.classList.remove('dark');
      attempts += 1;
      if (attempts > 50) obs.disconnect();
    });
    obs.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] });
    setTimeout(() => obs.disconnect(), 2000);
  }, theme);
}

export function themeFromProjectName(projectName: string): Theme {
  return projectName.endsWith('-dark') ? 'dark' : 'light';
}

export function viewportFromProjectName(projectName: string): 'mobile-375' | 'desktop-1280' {
  return projectName.startsWith('mobile-375') ? 'mobile-375' : 'desktop-1280';
}
