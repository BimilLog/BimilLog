/**
 * Development Logger Utility
 * 개발 환경에서만 동작하는 로그 유틸리티
 */

type LoggableValue = string | number | boolean | null | undefined | object | Error | unknown;

export const logger = {
  /**
   * 개발 환경에서만 로그 출력
   */
  log: (...args: LoggableValue[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.log(...args);
    }
  },

  /**
   * 개발 환경에서만 에러 로그 출력
   */
  error: (...args: LoggableValue[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.error(...args);
    }
  },

  /**
   * 개발 환경에서만 경고 로그 출력
   */
  warn: (...args: LoggableValue[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.warn(...args);
    }
  },

  /**
   * 개발 환경에서만 디버그 로그 출력
   */
  debug: (...args: LoggableValue[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.debug(...args);
    }
  },

  /**
   * 개발 환경에서만 정보 로그 출력
   */
  info: (...args: LoggableValue[]) => {
    if (process.env.NODE_ENV === 'development') {
      console.info(...args);
    }
  },

  /**
   * 운영 환경에서도 중요한 에러는 출력 (필요시)
   */
  critical: (...args: LoggableValue[]) => {
    console.error('[CRITICAL]', ...args);
  }
};