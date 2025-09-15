"use client";

import { useEffect } from 'react';
import { useThemeStore } from '@/stores/theme.store';

export function useTheme() {
  const {
    theme,
    resolvedTheme,
    colorScheme,
    fontSize,
    reduceMotion,
    setTheme,
    setColorScheme,
    setFontSize,
    setReduceMotion,
    toggleTheme,
    initialize,
  } = useThemeStore();

  // 컴포넌트 마운트 시 테마 초기화
  useEffect(() => {
    initialize();
  }, [initialize]);

  return {
    // 상태
    theme,
    resolvedTheme,
    colorScheme,
    fontSize,
    reduceMotion,
    isDark: resolvedTheme === 'dark',

    // 액션
    setTheme,
    setColorScheme,
    setFontSize,
    setReduceMotion,
    toggleTheme,
  };
}