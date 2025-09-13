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
export * from './validation'

// Clipboard utilities
export * from './clipboard'

// Storage utilities
export * from './storage'

// Removed cookies export - deprecated file deleted

// Sanitize utilities
export * from './sanitize'