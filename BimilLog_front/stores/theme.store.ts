import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import {
  getThemeConfig,
  setThemeConfig,
  initializeTheme,
  getResolvedTheme,
  subscribeToSystemThemeChanges,
  applyTheme,
  type Theme,
  type ColorScheme,
  type ThemeConfig,
} from '@/lib/utils/theme';

interface ThemeState extends ThemeConfig {
  resolvedTheme: 'light' | 'dark';

  setTheme: (theme: Theme) => void;
  setColorScheme: (colorScheme: ColorScheme) => void;
  setFontSize: (fontSize: 'small' | 'medium' | 'large') => void;
  setReduceMotion: (reduceMotion: boolean) => void;
  toggleTheme: () => void;
  initialize: () => void;
}

let systemThemeCleanup: (() => void) | null = null;
let hasThemeInitialized = false;

const detachSystemThemeListener = () => {
  if (systemThemeCleanup) {
    systemThemeCleanup();
    systemThemeCleanup = null;
  }
};

export const useThemeStore = create<ThemeState>()(
  devtools(
    persist(
      (set, get) => {
        const config = getThemeConfig();
        const resolvedTheme = getResolvedTheme(config.theme);

        const attachSystemThemeListener = () => {
          if (systemThemeCleanup || typeof window === 'undefined') return;

          systemThemeCleanup = subscribeToSystemThemeChanges(() => {
            if (get().theme !== 'system') {
              detachSystemThemeListener();
              return;
            }

            const updatedConfig = getThemeConfig();
            const nextResolved = getResolvedTheme('system');

            set({ resolvedTheme: nextResolved });
            applyTheme({ ...updatedConfig, theme: 'system' });
          });
        };

        const ensureSystemListenerState = (theme: Theme) => {
          if (theme === 'system') {
            attachSystemThemeListener();
          } else {
            detachSystemThemeListener();
          }
        };

        return {
          ...config,
          resolvedTheme,

          setTheme: (theme) => {
            setThemeConfig({ theme });
            ensureSystemListenerState(theme);
            set({
              theme,
              resolvedTheme: getResolvedTheme(theme),
            });
          },

          setColorScheme: (colorScheme) => {
            setThemeConfig({ colorScheme });
            set({ colorScheme });
          },

          setFontSize: (fontSize) => {
            setThemeConfig({ fontSize });
            set({ fontSize });
          },

          setReduceMotion: (reduceMotion) => {
            setThemeConfig({ reduceMotion });
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
              newTheme = get().resolvedTheme === 'light' ? 'dark' : 'light';
            }

            get().setTheme(newTheme);
          },

          initialize: () => {
            if (typeof window === 'undefined') return;

            if (!hasThemeInitialized) {
              initializeTheme();
              hasThemeInitialized = true;
            } else {
              applyTheme(getThemeConfig());
            }

            const currentTheme = get().theme;
            ensureSystemListenerState(currentTheme);

            set({
              resolvedTheme: getResolvedTheme(currentTheme),
            });
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
