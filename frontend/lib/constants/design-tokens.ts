// Design Token System
// 메인페이지 기준 Pink-Purple-Indigo 그라디언트 테마
// 모바일 퍼스트 접근법과 Brad Frost의 아토믹 디자인 원칙 적용

export const designTokens = {
  // Color Palette - 메인페이지 실제 색상 기준
  colors: {
    // Primary Colors (Pink-Purple Gradient)
    primary: {
      50: "#fdf2f8",   // pink-50
      100: "#fce7f3",  // pink-100
      200: "#fbcfe8",  // pink-200
      300: "#f9a8d4",  // pink-300
      400: "#f472b6",  // pink-400
      500: "#ec4899",  // pink-500 - 기본
      600: "#db2777",  // pink-600
      700: "#be185d",  // pink-700
      800: "#9d174d",  // pink-800
      900: "#831843",  // pink-900
    },
    
    // Secondary Colors (Purple)
    secondary: {
      50: "#faf5ff",   // purple-50
      100: "#f3e8ff",  // purple-100
      200: "#e9d5ff",  // purple-200
      300: "#d8b4fe",  // purple-300
      400: "#c084fc",  // purple-400
      500: "#a855f7",  // purple-500 - 기본
      600: "#9333ea",  // purple-600
      700: "#7c3aed",  // purple-700
      800: "#6b21a8",  // purple-800
      900: "#581c87",  // purple-900
    },

    // Accent Colors (Indigo)
    accent: {
      50: "#eef2ff",   // indigo-50
      100: "#e0e7ff",  // indigo-100
      200: "#c7d2fe",  // indigo-200
      300: "#a5b4fc",  // indigo-300
      400: "#818cf8",  // indigo-400
      500: "#6366f1",  // indigo-500 - 기본
      600: "#4f46e5",  // indigo-600
      700: "#4338ca",  // indigo-700
      800: "#3730a3",  // indigo-800
      900: "#312e81",  // indigo-900
    },

    // Semantic Colors
    success: {
      50: "#f0fdf4",
      100: "#dcfce7",
      500: "#22c55e",
      600: "#16a34a",
      700: "#15803d",
    },
    warning: {
      50: "#fffbeb", 
      100: "#fef3c7",
      500: "#f59e0b",
      600: "#d97706",
      700: "#b45309",
    },
    error: {
      50: "#fef2f2",
      100: "#fee2e2",
      500: "#ef4444",
      600: "#dc2626",
      700: "#b91c1c",
    },
    info: {
      50: "#eff6ff",
      100: "#dbeafe",
      500: "#3b82f6",
      600: "#2563eb",
      700: "#1d4ed8",
    },

    // Neutral Colors (메인페이지와 일치)
    gray: {
      50: "#f9fafb",
      100: "#f3f4f6",
      200: "#e5e7eb",
      300: "#d1d5db",
      400: "#9ca3af",
      500: "#6b7280",
      600: "#4b5563",
      700: "#374151",
      800: "#1f2937",
      900: "#111827",
    },

    // 메인페이지 실제 사용 그라디언트 색상
    gradients: {
      background: "from-pink-50 via-purple-50 to-indigo-50",
      primary: "from-pink-500 to-purple-600",
      primaryHover: "from-pink-600 to-purple-700",
      title: "from-pink-600 via-purple-600 to-indigo-600",
      feature1: "from-pink-500 to-red-500",
      feature2: "from-purple-500 to-indigo-500", 
      feature3: "from-green-500 to-teal-500",
      feature4: "from-orange-500 to-yellow-500",
      cta: "from-pink-500 via-purple-600 to-indigo-600",
      footer: "bg-gray-900",
    },
  },

  // Typography (모바일 최적화)
  typography: {
    fontFamily: {
      sans: ["Inter", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "sans-serif"],
      mono: ["JetBrains Mono", "SF Mono", "Consolas", "Monaco", "monospace"],
    },
    fontSize: {
      xs: "0.75rem",      // 12px - 모바일 캡션
      sm: "0.875rem",     // 14px - 모바일 본문
      base: "1rem",       // 16px - 모바일 기본 (최소 크기)
      lg: "1.125rem",     // 18px - 모바일 부제목
      xl: "1.25rem",      // 20px - 모바일 소제목
      "2xl": "1.5rem",    // 24px - 모바일 제목
      "3xl": "1.875rem",  // 30px - 모바일 대제목
      "4xl": "2.25rem",   // 36px - 태블릿 이상
      "5xl": "3rem",      // 48px - 데스크톱
      "6xl": "3.75rem",   // 60px - 대형 화면
    },
    fontWeight: {
      light: "300",
      normal: "400",
      medium: "500",
      semibold: "600",
      bold: "700",
      extrabold: "800",
    },
    lineHeight: {
      tight: "1.25",      // 제목용
      snug: "1.375",      // 부제목용
      normal: "1.5",      // 본문용 (모바일 최적)
      relaxed: "1.625",   // 긴 텍스트용
      loose: "2",         // 특별한 경우
    },
  },

  // Spacing (모바일 퍼스트 터치 친화적 크기)
  spacing: {
    px: "1px",
    0: "0",
    0.5: "0.125rem",    // 2px
    1: "0.25rem",       // 4px
    1.5: "0.375rem",    // 6px
    2: "0.5rem",        // 8px
    2.5: "0.625rem",    // 10px
    3: "0.75rem",       // 12px
    3.5: "0.875rem",    // 14px
    4: "1rem",          // 16px
    5: "1.25rem",       // 20px
    6: "1.5rem",        // 24px
    7: "1.75rem",       // 28px
    8: "2rem",          // 32px
    9: "2.25rem",       // 36px
    10: "2.5rem",       // 40px
    11: "2.75rem",      // 44px - 최소 터치 타겟
    12: "3rem",         // 48px - 권장 터치 타겟
    14: "3.5rem",       // 56px - 큰 터치 타겟
    16: "4rem",         // 64px
    20: "5rem",         // 80px
    24: "6rem",         // 96px
    32: "8rem",         // 128px
    40: "10rem",        // 160px
    48: "12rem",        // 192px
    56: "14rem",        // 224px
    64: "16rem",        // 256px
  },

  // Touch Targets (모바일 최적화)
  touchTarget: {
    minimum: "44px",     // iOS/Android 최소 권장
    recommended: "48px", // 일반적 권장
    comfortable: "56px", // 편안한 크기
    large: "64px",       // 큰 버튼
  },

  // Border Radius
  borderRadius: {
    none: "0",
    sm: "0.125rem",      // 2px
    default: "0.25rem",  // 4px
    md: "0.375rem",      // 6px
    lg: "0.5rem",        // 8px - 모바일 카드
    xl: "0.75rem",       // 12px - 큰 카드
    "2xl": "1rem",       // 16px
    "3xl": "1.5rem",     // 24px
    full: "9999px",      // 원형
  },

  // Shadows (모바일 최적화)
  boxShadow: {
    sm: "0 1px 2px 0 rgb(0 0 0 / 0.05)",
    default: "0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)",
    md: "0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)",
    lg: "0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)", // 메인페이지 카드
    xl: "0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)", // 메인페이지 CTA
    "2xl": "0 25px 50px -12px rgb(0 0 0 / 0.25)", // 특별한 경우
    // 모바일 특화 그림자
    card: "0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)",
    modal: "0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)",
  },

  // Animation (모바일 최적화)
  animation: {
    duration: {
      immediate: "50ms",   // 즉시 피드백
      fast: "150ms",       // 터치 피드백
      normal: "300ms",     // 일반 트랜지션
      slow: "500ms",       // 큰 변화
      slower: "700ms",     // 페이지 트랜지션
    },
    easing: {
      ease: "ease",
      easeIn: "ease-in",
      easeOut: "ease-out",
      easeInOut: "ease-in-out",
      // 모바일 특화 이징
      spring: "cubic-bezier(0.68, -0.55, 0.265, 1.55)", // 바운스 효과
      smooth: "cubic-bezier(0.4, 0, 0.2, 1)",          // 부드러운 효과
    },
  },

  // Breakpoints (모바일 퍼스트)
  breakpoints: {
    xs: "320px",         // 작은 모바일
    sm: "640px",         // 큰 모바일/작은 태블릿
    md: "768px",         // 태블릿
    lg: "1024px",        // 작은 데스크톱
    xl: "1280px",        // 큰 데스크톱
    "2xl": "1536px",     // 대형 화면
  },

  // 모바일 특화 속성
  mobile: {
    // Safe Area (노치/홈바 대응)
    safeArea: {
      top: "env(safe-area-inset-top)",
      bottom: "env(safe-area-inset-bottom)",
      left: "env(safe-area-inset-left)",
      right: "env(safe-area-inset-right)",
    },
    
    // 모바일 뷰포트
    viewport: {
      width: "100vw",
      height: "100vh",
      dynamicHeight: "100dvh", // 모바일 주소창 고려
    },

    // 터치 특성
    touch: {
      callout: "none",           // 길게 누르기 메뉴 비활성화
      userSelect: "none",        // 텍스트 선택 비활성화
      tapHighlight: "transparent", // 터치 하이라이트 제거
    },
  },
} as const;

// Type definitions
export type ColorScale = keyof typeof designTokens.colors.primary;
export type FontSize = keyof typeof designTokens.typography.fontSize;
export type Spacing = keyof typeof designTokens.spacing;
export type BorderRadius = keyof typeof designTokens.borderRadius;

// Utility functions
export const getColor = (color: keyof typeof designTokens.colors, scale?: string) => {
  const colorObject = designTokens.colors[color];
  if (typeof colorObject === 'object' && scale && scale in colorObject) {
    return (colorObject as any)[scale];
  }
  return colorObject;
};

export const getSpacing = (key: Spacing) => designTokens.spacing[key];
export const getFontSize = (key: FontSize) => designTokens.typography.fontSize[key];

// 모바일 퍼스트 유틸리티
export const isTouchDevice = () => 'ontouchstart' in window || navigator.maxTouchPoints > 0;

export const getMobileBreakpoint = (size: keyof typeof designTokens.breakpoints) => 
  designTokens.breakpoints[size];

// 그라디언트 유틸리티 (메인페이지 스타일)
export const getGradient = (type: keyof typeof designTokens.colors.gradients) => 
  designTokens.colors.gradients[type];

// 터치 타겟 검증
export const validateTouchTarget = (size: string | number) => {
  const sizeInPx = typeof size === 'string' ? parseInt(size) : size;
  return sizeInPx >= 44; // 최소 44px 보장
}; 