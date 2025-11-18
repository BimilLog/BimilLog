import { apiClient } from '../client'

export interface ClientErrorLog {
  platform: 'web' | 'android' | 'ios'
  errorMessage: string
  stackTrace?: string
  url?: string
  userAgent?: string
  additionalInfo?: string
}

export const logCommand = {
  /**
   * 클라이언트 에러를 백엔드로 전송
   */
  logClientError: (errorLog: ClientErrorLog) => {
    return apiClient.post<void>("/api/global/client-error", errorLog)
  },
}
