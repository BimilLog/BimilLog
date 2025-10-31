import { logCommand, ClientErrorLog } from './api/log/command'

/**
 * 클라이언트 에러를 백엔드로 전송하는 유틸리티
 */
export const errorLogger = {
  /**
   * 에러를 백엔드로 전송
   */
  logError: async (
    error: Error | string,
    additionalInfo?: Record<string, unknown>
  ): Promise<void> => {
    try {
      const errorMessage = typeof error === 'string' ? error : error.message
      const stackTrace = typeof error === 'string' ? undefined : error.stack

      const errorLog: ClientErrorLog = {
        platform: 'web',
        errorMessage,
        stackTrace,
        url: typeof window !== 'undefined' ? window.location.href : undefined,
        userAgent: typeof window !== 'undefined' ? navigator.userAgent : undefined,
        additionalInfo: additionalInfo ? JSON.stringify(additionalInfo) : undefined,
      }

      await logCommand.logClientError(errorLog)
    } catch (loggingError) {
      // 로깅 실패는 무시 (무한 루프 방지)
      console.error('Failed to log error to backend:', loggingError)
    }
  },

  /**
   * 전역 에러 핸들러 설정
   */
  setupGlobalErrorHandlers: () => {
    if (typeof window === 'undefined') return

    // window.onerror 핸들러
    window.onerror = (message, source, lineno, colno, error) => {
      errorLogger.logError(
        error || String(message),
        {
          source,
          lineno,
          colno,
          type: 'window.onerror',
        }
      )
      return false // 기본 에러 처리도 실행
    }

    // unhandledrejection 핸들러
    window.addEventListener('unhandledrejection', (event) => {
      errorLogger.logError(
        event.reason instanceof Error
          ? event.reason
          : String(event.reason),
        {
          type: 'unhandledrejection',
        }
      )
    })

    console.log('[ErrorLogger] Global error handlers initialized')
  },
}
