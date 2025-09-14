import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

// Core utility functions
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * User-Agent 기반 모바일/태블릿 감지
 */
export function isMobileOrTablet(): boolean {
  if (typeof window === 'undefined') return false

  const userAgent = navigator.userAgent.toLowerCase()
  const mobileKeywords = [
    'mobile', 'android', 'iphone', 'ipad', 'ipod',
    'blackberry', 'windows phone', 'opera mini'
  ]

  return mobileKeywords.some(keyword => userAgent.includes(keyword))
}

// Date utilities
export * from './date'

// Format utilities
export * from './format'

// Validation utilities
export * from './validation-helpers'

// Clipboard utilities
export * from './clipboard'

// Storage utilities
export * from './storage'

// Removed cookies export - deprecated file deleted

// Sanitize utilities
export * from './sanitize'

// Logger utilities
export * from './logger'

// Lazy components
export * from './lazy-components'

// API helpers
export * from './api-helpers'

// Validation helpers
export * from './validation-helpers'

// Style helpers
export * from './style-helpers'

/**
 * Type Guards - 타입 안정성을 위한 타입 가드 함수들
 */

// API Response 타입 가드
export function isApiResponse<T>(value: unknown): value is { success: boolean; data?: T; error?: string } {
  return typeof value === 'object' && value !== null && 'success' in value;
}

// 에러 객체 타입 가드
export function isError(value: unknown): value is Error {
  return value instanceof Error;
}

// 문자열 타입 가드
export function isString(value: unknown): value is string {
  return typeof value === 'string';
}

// 숫자 타입 가드
export function isNumber(value: unknown): value is number {
  return typeof value === 'number' && !isNaN(value);
}

// 배열 타입 가드
export function isArray<T>(value: unknown): value is T[] {
  return Array.isArray(value);
}

// null/undefined 체크
export function isDefined<T>(value: T | null | undefined): value is T {
  return value !== null && value !== undefined;
}

// 비어있지 않은 문자열 체크
export function isNonEmptyString(value: unknown): value is string {
  return isString(value) && value.length > 0;
}

// Post 타입 가드
export function hasPostProperties(value: unknown): value is { id: number; title: string } {
  return typeof value === 'object' &&
         value !== null &&
         'id' in value &&
         'title' in value &&
         typeof (value as Record<string, unknown>).id === 'number' &&
         typeof (value as Record<string, unknown>).title === 'string';
}