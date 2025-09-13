/**
 * Development Logger Utility
 * 개발 환경에서만 동작하는 로그 유틸리티
 */

export const logger = {
  /**
   * 개발 환경에서만 로그 출력
   */
  log: (...args: any[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.log(...args);
    }
  },

  /**
   * 개발 환경에서만 에러 로그 출력
   */
  error: (...args: any[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.error(...args);
    }
  },

  /**
   * 개발 환경에서만 경고 로그 출력
   */
  warn: (...args: any[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.warn(...args);
    }
  },

  /**
   * 개발 환경에서만 디버그 로그 출력
   */
  debug: (...args: any[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.debug(...args);
    }
  },

  /**
   * 개발 환경에서만 정보 로그 출력
   */
  info: (...args: any[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.info(...args);
    }
  },

  /**
   * 운영 환경에서도 중요한 에러는 출력 (필요시)
   */
  critical: (...args: any[]) => {
    console.error('[CRITICAL]', ...args);
  }
};