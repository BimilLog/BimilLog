/**
 * Theme utilities: handles dark mode, accent colors, and related preferences.
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

type SystemThemeListener = (theme: 'light' | 'dark') => void;

/**
 * Detects the OS-level color scheme.
 */
export function getSystemTheme(): 'light' | 'dark' {
  if (typeof window === 'undefined') return 'light';
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * Resolves the runtime theme, expanding "system" to the actual value.
 */
export function getResolvedTheme(theme: Theme): 'light' | 'dark' {
  return theme === 'system' ? getSystemTheme() : theme;
}

/**
 * Reads the saved theme configuration.
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
 * Persists theme configuration and applies it immediately.
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
 * Applies a config to the DOM (classes, attributes, meta theme-color).
 */
export function applyTheme(config: ThemeConfig): void {
  if (typeof window === 'undefined') return;

  const root = document.documentElement;
  const resolvedTheme = getResolvedTheme(config.theme);

  root.classList.toggle('dark', resolvedTheme === 'dark');
  root.classList.toggle('light', resolvedTheme === 'light');
  root.setAttribute('data-mode', resolvedTheme);
  root.setAttribute('data-theme', resolvedTheme);
  root.style.colorScheme = resolvedTheme;

  root.setAttribute('data-color-scheme', config.colorScheme);
  root.setAttribute('data-font-size', config.fontSize);

  if (config.reduceMotion) {
    root.classList.add('reduce-motion');
  } else {
    root.classList.remove('reduce-motion');
  }

  updateMetaThemeColor(resolvedTheme);
}

/**
 * Updates browser chrome color.
 */
function updateMetaThemeColor(theme: 'light' | 'dark'): void {
  const metaThemeColor = document.querySelector('meta[name="theme-color"]');
  if (metaThemeColor) {
    metaThemeColor.setAttribute('content', theme === 'dark' ? '#1a1a1a' : '#ffffff');
  }
}

/**
 * Convenience toggler between light/dark (ignores system preference).
 */
export function toggleTheme(): void {
  const config = getThemeConfig();
  const newTheme = config.theme === 'light' ? 'dark' : 'light';
  setThemeConfig({ theme: newTheme });
}

/**
 * Applies the persisted theme once the client hydrates.
 */
export function initializeTheme(): void {
  if (typeof window === 'undefined') return;
  const config = getThemeConfig();
  applyTheme(config);
}

/**
 * Subscribes to system theme changes and returns an unsubscribe function.
 */
export function subscribeToSystemThemeChanges(listener: SystemThemeListener): () => void {
  if (typeof window === 'undefined') return () => {};

  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  const handler = () => listener(getSystemTheme());

  if (mediaQuery.addEventListener) {
    mediaQuery.addEventListener('change', handler);
  } else {
    mediaQuery.addListener(handler);
  }

  return () => {
    if (mediaQuery.addEventListener) {
      mediaQuery.removeEventListener('change', handler);
    } else {
      mediaQuery.removeListener(handler);
    }
  };
}

/**
 * Accent color presets.
 */
export const COLOR_SCHEMES = {
  default: {
    primary: 'rgb(236 72 153)',
    secondary: 'rgb(168 85 247)',
    accent: 'rgb(99 102 241)',
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
 * Font-size presets so we can keep CSS tokens simple.
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
