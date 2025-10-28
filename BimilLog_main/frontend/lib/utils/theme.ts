/**
 * 테마 관리 유틸리티
 * 다크모드 및 테마 설정 관리
 */

export type Theme = 'light' | 'dark' | 'system';
export type ColorScheme = 'default' | 'pink' | 'purple' | 'blue' | 'green' | 'orange';

export interface ThemeConfig {
  theme: Theme;
  colorScheme: ColorScheme;
  fontSize: 'small' | 'medium' | 'large';
  reduceMotion: boolean;
}

const THEME_KEY = 'bimillog_theme';
const DEFAULT_CONFIG: ThemeConfig = {
  theme: 'system',
  colorScheme: 'default',
  fontSize: 'medium',
  reduceMotion: false,
};

/**
 * 시스템 다크모드 설정 감지
 */
export function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light';

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * 실제 적용될 테마 계산
 */
export function getResolvedTheme(theme: Theme): 'light' | 'dark' {
  if (theme === 'system') {
    return getSystemTheme();
  }
  return theme;
}

/**
 * 테마 설정 가져오기
 */
export function getThemeConfig(): ThemeConfig {
  if (typeof window === 'undefined') return DEFAULT_CONFIG;

  try {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored) {
      return { ...DEFAULT_CONFIG, ...JSON.parse(stored) };
    }
  } catch (error) {
    console.error('Failed to get theme config:', error);
  }

  return DEFAULT_CONFIG;
}

/**
 * 테마 설정 저장
 */
export function setThemeConfig(config: Partial<ThemeConfig>): void {
  if (typeof window === 'undefined') return;

  try {
    const current = getThemeConfig();
    const updated = { ...current, ...config };
    localStorage.setItem(THEME_KEY, JSON.stringify(updated));
    applyTheme(updated);
  } catch (error) {
    console.error('Failed to set theme config:', error);
  }
}

/**
 * 테마 적용
 */
export function applyTheme(config: ThemeConfig): void {
  if (typeof window === 'undefined') return;

  const root = document.documentElement;
  const resolvedTheme = getResolvedTheme(config.theme);

  // 다크모드 적용
  root.classList.toggle('dark', resolvedTheme === 'dark');
  root.classList.toggle('light', resolvedTheme === 'light');
  root.setAttribute('data-mode', resolvedTheme);
  root.setAttribute('data-theme', resolvedTheme);
  root.style.colorScheme = resolvedTheme;

  // 색상 스킴 적용
  root.setAttribute('data-color-scheme', config.colorScheme);

  // 폰트 크기 적용
  root.setAttribute('data-font-size', config.fontSize);

  // 애니메이션 감소 적용
  if (config.reduceMotion) {
    root.classList.add('reduce-motion');
  } else {
    root.classList.remove('reduce-motion');
  }

  // 메타 테마 색상 업데이트
  updateMetaThemeColor(resolvedTheme);
}

/**
 * 메타 테마 색상 업데이트
 */
function updateMetaThemeColor(theme: 'light' | 'dark'): void {
  const metaThemeColor = document.querySelector('meta[name="theme-color"]');
  if (metaThemeColor) {
    metaThemeColor.setAttribute('content', theme === 'dark' ? '#1a1a1a' : '#ffffff');
  }
}

/**
 * 테마 토글
 */
export function toggleTheme(): void {
  const config = getThemeConfig();
  const newTheme = config.theme === 'light' ? 'dark' : 'light';
  setThemeConfig({ theme: newTheme });
}

/**
 * 초기 테마 설정
 */
export function initializeTheme(): void {
  if (typeof window === 'undefined') return;

  const config = getThemeConfig();
  applyTheme(config);

  // 시스템 테마 변경 감지
  if (config.theme === 'system') {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleChange = () => {
      applyTheme(config);
    };

    // 이벤트 리스너 추가 (최신 브라우저)
    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleChange);
    } else {
      // 구형 브라우저 대응
      mediaQuery.addListener(handleChange);
    }
  }
}

/**
 * 색상 스킴별 CSS 변수 정의
 */
export const COLOR_SCHEMES = {
  default: {
    primary: 'rgb(236 72 153)', // pink-500
    secondary: 'rgb(168 85 247)', // purple-500
    accent: 'rgb(99 102 241)', // indigo-500
  },
  pink: {
    primary: 'rgb(236 72 153)',
    secondary: 'rgb(244 114 182)',
    accent: 'rgb(251 207 232)',
  },
  purple: {
    primary: 'rgb(168 85 247)',
    secondary: 'rgb(147 51 234)',
    accent: 'rgb(196 181 253)',
  },
  blue: {
    primary: 'rgb(59 130 246)',
    secondary: 'rgb(37 99 235)',
    accent: 'rgb(147 197 253)',
  },
  green: {
    primary: 'rgb(34 197 94)',
    secondary: 'rgb(22 163 74)',
    accent: 'rgb(134 239 172)',
  },
  orange: {
    primary: 'rgb(251 146 60)',
    secondary: 'rgb(249 115 22)',
    accent: 'rgb(254 215 170)',
  },
};

/**
 * 폰트 크기 설정
 */
export const FONT_SIZES = {
  small: {
    base: '14px',
    scale: 0.875,
  },
  medium: {
    base: '16px',
    scale: 1,
  },
  large: {
    base: '18px',
    scale: 1.125,
  },
};
