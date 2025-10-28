import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import {
  getThemeConfig,
  setThemeConfig,
  initializeTheme,
  getResolvedTheme,
  type Theme,
  type ColorScheme,
  type ThemeConfig,
} from '@/lib/utils/theme';

interface ThemeState extends ThemeConfig {
  // 실제 적용된 테마 (system일 경우 resolved된 값)
  resolvedTheme: 'light' | 'dark';

  // Actions
  setTheme: (theme: Theme) => void;
  setColorScheme: (colorScheme: ColorScheme) => void;
  setFontSize: (fontSize: 'small' | 'medium' | 'large') => void;
  setReduceMotion: (reduceMotion: boolean) => void;
  toggleTheme: () => void;
  initialize: () => void;
}

export const useThemeStore = create<ThemeState>()(
  devtools(
    persist(
      (set, get) => {
        const config = getThemeConfig();

        return {
          ...config,
          resolvedTheme: getResolvedTheme(config.theme),

          setTheme: (theme) => {
            const newConfig = { theme };
            setThemeConfig(newConfig);
            set({
              theme,
              resolvedTheme: getResolvedTheme(theme),
            });
          },

          setColorScheme: (colorScheme) => {
            const newConfig = { colorScheme };
            setThemeConfig(newConfig);
            set({ colorScheme });
          },

          setFontSize: (fontSize) => {
            const newConfig = { fontSize };
            setThemeConfig(newConfig);
            set({ fontSize });
          },

          setReduceMotion: (reduceMotion) => {
            const newConfig = { reduceMotion };
            setThemeConfig(newConfig);
            set({ reduceMotion });
          },

          toggleTheme: () => {
            const currentTheme = get().theme;
            let newTheme: Theme;

            if (currentTheme === 'light') {
              newTheme = 'dark';
            } else if (currentTheme === 'dark') {
              newTheme = 'light';
            } else {
              // system인 경우 현재 resolved 테마의 반대로
              newTheme = get().resolvedTheme === 'light' ? 'dark' : 'light';
            }

            get().setTheme(newTheme);
          },

          initialize: () => {
            initializeTheme();

            // 시스템 테마 변경 감지
            if (typeof window !== 'undefined' && get().theme === 'system') {
              const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

              const handleChange = () => {
                set({
                  resolvedTheme: getResolvedTheme('system'),
                });
              };

              if (mediaQuery.addEventListener) {
                mediaQuery.addEventListener('change', handleChange);
              } else {
                mediaQuery.addListener(handleChange);
              }
            }
          },
        };
      },
      {
        name: 'theme-storage',
        partialize: (state) => ({
          theme: state.theme,
          colorScheme: state.colorScheme,
          fontSize: state.fontSize,
          reduceMotion: state.reduceMotion,
        }),
      }
    )
  )
);