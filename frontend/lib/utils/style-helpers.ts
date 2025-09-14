/**
 * 공통 스타일 패턴 헬퍼
 */

import { cn } from '@/lib/utils';

/**
 * 자주 사용되는 그라데이션 패턴들
 */
export const gradients = {
  // 페이지 배경용
  pageBackground: 'bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50',
  pageBackgroundAlt: 'bg-gradient-to-br from-pink-100 via-purple-50 to-indigo-100',

  // 카드 배경용
  cardBackground: 'bg-gradient-to-br from-white via-pink-50/50 to-purple-50/50',
  glassCard: 'bg-white/80 backdrop-blur-sm',

  // 텍스트용
  primaryText: 'bg-gradient-to-r from-pink-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent',
  heroText: 'bg-gradient-to-r from-pink-500 to-purple-600 bg-clip-text text-transparent',

  // 버튼용
  primaryButton: 'bg-gradient-to-r from-pink-500 to-purple-600',
  secondaryButton: 'bg-gradient-to-r from-indigo-500 to-purple-600',
  dangerButton: 'bg-gradient-to-r from-red-500 to-pink-500',
  successButton: 'bg-gradient-to-r from-green-500 to-emerald-600',

  // 로딩/애니메이션용
  shimmer: 'bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200',
  loading: 'bg-gradient-to-r from-purple-400 to-pink-400',
} as const;

/**
 * 반응형 컨테이너 클래스
 */
export const containers = {
  page: 'container mx-auto px-4 py-8',
  pageNoPadding: 'container mx-auto px-4',
  section: 'max-w-4xl mx-auto px-4',
  narrow: 'max-w-2xl mx-auto px-4',
  wide: 'max-w-7xl mx-auto px-4',
  full: 'w-full px-4',
} as const;

/**
 * 카드 스타일 패턴
 */
export const cardStyles = {
  default: 'bg-white rounded-lg border border-gray-200 shadow-sm',
  elevated: 'bg-white rounded-lg shadow-lg hover:shadow-xl transition-shadow',
  glass: 'bg-white/80 backdrop-blur-sm border-0 shadow-lg rounded-lg',
  flat: 'bg-white border-0 rounded-lg',
  outlined: 'bg-transparent border-2 border-gray-200 rounded-lg',

  // 특화된 카드들
  post: 'bg-white/80 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-shadow rounded-lg',
  comment: 'bg-gray-50/80 backdrop-blur-sm rounded-lg border border-gray-100',
  paper: 'bg-white/90 backdrop-blur-sm shadow-md hover:shadow-lg transition-shadow rounded-lg',
} as const;

/**
 * 버튼 스타일 패턴
 */
export const buttonStyles = {
  // 크기
  sizes: {
    xs: 'px-2 py-1 text-xs',
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base',
    xl: 'px-8 py-4 text-lg',
  },

  // 기본 스타일
  base: 'inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed',

  // 변형들
  variants: {
    primary: cn(gradients.primaryButton, 'text-white hover:shadow-lg active:scale-[0.98]'),
    secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200 active:scale-[0.98]',
    outline: 'border-2 border-gray-300 bg-transparent hover:bg-gray-50 active:scale-[0.98]',
    ghost: 'bg-transparent hover:bg-gray-100 active:scale-[0.98]',
    danger: cn(gradients.dangerButton, 'text-white hover:shadow-lg active:scale-[0.98]'),
    success: cn(gradients.successButton, 'text-white hover:shadow-lg active:scale-[0.98]'),
  }
} as const;

/**
 * 입력 필드 스타일 패턴
 */
export const inputStyles = {
  base: 'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder-gray-500 focus:border-purple-500 focus:outline-none focus:ring-2 focus:ring-purple-500/20 disabled:bg-gray-50 disabled:cursor-not-allowed',
  error: 'border-red-500 focus:border-red-500 focus:ring-red-500/20',
  success: 'border-green-500 focus:border-green-500 focus:ring-green-500/20',
  large: 'px-4 py-3 text-base',
  small: 'px-2 py-1.5 text-xs',
} as const;

/**
 * 텍스트 스타일 패턴
 */
export const textStyles = {
  // 제목들
  h1: 'text-3xl md:text-4xl lg:text-5xl font-bold',
  h2: 'text-2xl md:text-3xl lg:text-4xl font-bold',
  h3: 'text-xl md:text-2xl lg:text-3xl font-semibold',
  h4: 'text-lg md:text-xl lg:text-2xl font-semibold',

  // 본문
  body: 'text-base leading-relaxed',
  bodyLarge: 'text-lg leading-relaxed',
  bodySmall: 'text-sm leading-relaxed',

  // 특수 텍스트
  caption: 'text-xs text-gray-600',
  muted: 'text-sm text-gray-500',
  emphasis: 'font-medium text-gray-900',

  // 상태별
  error: 'text-red-600',
  success: 'text-green-600',
  warning: 'text-yellow-600',
  info: 'text-blue-600',
} as const;

/**
 * 애니메이션 클래스들
 */
export const animations = {
  // 페이드 효과
  fadeIn: 'animate-fade-in',
  fadeOut: 'animate-fade-out',

  // 스케일 효과
  scaleIn: 'animate-scale-in',
  scaleOut: 'animate-scale-out',

  // 슬라이드 효과
  slideInUp: 'animate-slide-in-up',
  slideInDown: 'animate-slide-in-down',
  slideInLeft: 'animate-slide-in-left',
  slideInRight: 'animate-slide-in-right',

  // 회전 효과
  spin: 'animate-spin',
  pulse: 'animate-pulse',
  bounce: 'animate-bounce',

  // 인터랙션
  hover: 'hover:scale-105 transition-transform',
  press: 'active:scale-95 transition-transform',
  focus: 'focus:ring-2 focus:ring-purple-500/20 focus:outline-none',
} as const;

/**
 * 반응형 그리드 패턴
 */
export const gridLayouts = {
  // 카드 그리드
  cards: 'grid gap-6 grid-cols-1 md:grid-cols-2 lg:grid-cols-3',
  cardsLarge: 'grid gap-8 grid-cols-1 md:grid-cols-2',
  cardsFull: 'grid gap-4 grid-cols-1',

  // 리스트
  list: 'space-y-4',
  listCompact: 'space-y-2',
  listSpaced: 'space-y-6',

  // 자동 크기 조정
  autoFit: 'grid gap-6 grid-cols-[repeat(auto-fit,minmax(300px,1fr))]',
  autoFitSmall: 'grid gap-4 grid-cols-[repeat(auto-fit,minmax(200px,1fr))]',

  // 롤링페이퍼용 특수 그리드
  rollingPaper: {
    mobile: 'grid grid-cols-4 gap-1',
    desktop: 'grid grid-cols-6 gap-2',
  }
} as const;

/**
 * 모바일 최적화 클래스들
 */
export const mobileOptimized = {
  // 터치 타겟 (최소 44px)
  touchTarget: 'min-h-[44px] min-w-[44px]',
  touchTargetLarge: 'min-h-[48px] min-w-[48px]',

  // 터치 최적화
  touchFriendly: 'touch-manipulation select-none',

  // 모바일 전용 숨김/표시
  mobileOnly: 'md:hidden',
  desktopOnly: 'hidden md:block',

  // 안전 영역 (iPhone notch 등)
  safeArea: {
    top: 'pt-safe-top',
    bottom: 'pb-safe-bottom',
    left: 'pl-safe-left',
    right: 'pr-safe-right',
  }
} as const;

/**
 * 스타일 유틸리티 함수들
 */
export const styleUtils = {
  // 조건부 스타일 적용
  conditional: (condition: boolean, trueStyles: string, falseStyles: string = '') => {
    return condition ? trueStyles : falseStyles;
  },

  // 상태별 스타일 적용
  state: (state: 'default' | 'loading' | 'error' | 'success', styles: Record<string, string>) => {
    return styles[state] || styles.default || '';
  },

  // 반응형 클래스 생성
  responsive: (base: string, md?: string, lg?: string) => {
    return cn(base, md && `md:${md}`, lg && `lg:${lg}`);
  },

  // 그라데이션 텍스트 생성
  gradientText: (from: string, to: string) => {
    return `bg-gradient-to-r from-${from} to-${to} bg-clip-text text-transparent`;
  }
} as const;

/**
 * 프리셋 컴포넌트 스타일
 */
export const componentPresets = {
  // 페이지 레이아웃
  page: cn(gradients.pageBackground, 'min-h-screen'),
  pageContent: cn(containers.page),

  // 섹션
  section: cn(containers.section, 'py-12'),
  sectionTitle: cn(textStyles.h2, gradients.primaryText, 'text-center mb-8'),

  // 카드 컨테이너
  cardContainer: cn(cardStyles.glass, 'p-6'),
  cardHeader: 'border-b border-gray-100 pb-4 mb-4',
  cardBody: 'space-y-4',
  cardFooter: 'border-t border-gray-100 pt-4 mt-4',

  // 폼
  formContainer: cn(cardStyles.glass, 'p-6 max-w-md mx-auto'),
  formGroup: 'space-y-2',
  formLabel: 'block text-sm font-medium text-gray-700',
  formInput: inputStyles.base,
  formButton: cn(buttonStyles.base, buttonStyles.variants.primary, buttonStyles.sizes.lg, 'w-full'),
  formError: cn(textStyles.error, 'text-xs mt-1'),

  // 리스트 아이템
  listItem: cn(cardStyles.default, 'p-4 hover:shadow-md transition-shadow'),
  listItemActive: 'ring-2 ring-purple-500/20 border-purple-300',

  // 버튼들
  primaryAction: cn(buttonStyles.base, buttonStyles.variants.primary, buttonStyles.sizes.lg),
  secondaryAction: cn(buttonStyles.base, buttonStyles.variants.secondary, buttonStyles.sizes.md),
  dangerAction: cn(buttonStyles.base, buttonStyles.variants.danger, buttonStyles.sizes.md),
} as const;

/**
 * 테마 관련 유틸리티 (다크 모드 대비)
 */
export const themeUtils = {
  // 라이트/다크 조건부 적용
  light: (lightClass: string) => lightClass,
  dark: (darkClass: string) => `dark:${darkClass}`,
  theme: (lightClass: string, darkClass: string) => `${lightClass} dark:${darkClass}`,

  // 테마별 색상
  colors: {
    background: {
      light: 'bg-white',
      dark: 'dark:bg-gray-900',
      both: 'bg-white dark:bg-gray-900'
    },
    text: {
      light: 'text-gray-900',
      dark: 'dark:text-gray-100',
      both: 'text-gray-900 dark:text-gray-100'
    },
    border: {
      light: 'border-gray-200',
      dark: 'dark:border-gray-700',
      both: 'border-gray-200 dark:border-gray-700'
    }
  }
} as const;