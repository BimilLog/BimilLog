"use client";

import React from "react";
import { Moon, Sun, Monitor } from "lucide-react";
import { useTheme } from "next-themes";

interface ThemeToggleButtonProps {
  mounted: boolean;
}

export const ThemeToggleButton = React.memo(({ mounted }: ThemeToggleButtonProps) => {
  const { theme, setTheme, resolvedTheme } = useTheme();

  const getThemeIcon = () => {
    if (!mounted) return <Monitor className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
    if (resolvedTheme === 'dark') return <Moon className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
    if (resolvedTheme === 'light') return <Sun className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
    return <Monitor className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
  };

  return (
    <button
      onClick={() => {
        if (theme === 'light') setTheme('dark');
        else if (theme === 'dark') setTheme('system');
        else setTheme('light');
      }}
      className="min-h-[44px] min-w-[44px] touch-manipulation rounded-lg p-2 text-muted-foreground transition-colors hover:bg-accent hover:text-primary"
      title={mounted ? `테마 변경 (현재: ${theme === 'dark' ? '다크' : theme === 'light' ? '라이트' : '시스템'})` : '테마 변경'}
    >
      {getThemeIcon()}
    </button>
  );
});

ThemeToggleButton.displayName = "ThemeToggleButton";
