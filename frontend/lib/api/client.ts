import { ApiResponse } from '@/types/common'
import { logger } from '@/lib/utils'
import { isValidApiResponse } from './helpers'

function getCookie(name: string): string | null {
  if (typeof window === 'undefined') return null
  
  const value = `; ${document.cookie}`
  const parts = value.split(`; ${name}=`)
  
  if (parts.length === 2) {
    const cookieValue = parts.pop()?.split(';').shift()
    return cookieValue || null
  }
  
  return null
}

function logCsrfTokenUpdate(method: string, endpoint: string): void {
  if (process.env.NODE_ENV !== 'development') return
  
  const modifyingMethods = ['POST', 'PUT', 'DELETE', 'PATCH']
  if (!modifyingMethods.includes(method)) return
  
  const currentToken = getCookie("XSRF-TOKEN")
  if (!currentToken) return
  
  logger.log(`[${method}] ${endpoint} - Token after request:`, currentToken.substring(0, 8) + '...')

  setTimeout(() => {
    const newToken = getCookie("XSRF-TOKEN")
    if (newToken && newToken !== currentToken) {
      logger.log(`[TOKEN UPDATE] New CSRF token detected:`, newToken.substring(0, 8) + '...')
    }
  }, 100)
}

export class ApiClient {
  private baseURL: string

  constructor(baseURL: string) {
    this.baseURL = baseURL
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`
    const csrfToken = getCookie("XSRF-TOKEN")
    
    const defaultHeaders: Record<string, string> = {
      "Content-Type": "application/json",
    }
    
    if (csrfToken) {
      defaultHeaders["X-XSRF-TOKEN"] = csrfToken
      logger.log(`[${options.method || 'GET'}] ${endpoint} - Using CSRF token:`, csrfToken.substring(0, 8) + '...')
    }

    const config: RequestInit = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers,
      },
      credentials: "include",
    }

    try {
      const response = await fetch(url, config)
      
      const requiredAuthEndpoints = [
        '/user',
        '/api/user',
        '/api/member',
        '/paper',
        '/api/admin',
        '/post/manage/like',
        '/comment/like',
        '/notification',
        '/api/auth/logout',
        '/api/user/withdraw'
      ]

      const requiresAuth = requiredAuthEndpoints.some(requiredUrl => {
        if (requiredUrl === '/paper' || requiredUrl.endsWith('/like')) {
          return endpoint === requiredUrl
        }
        return endpoint.startsWith(requiredUrl)
      })

      logCsrfTokenUpdate(options.method || 'GET', endpoint)

      if (!response.ok) {
        if (!requiresAuth && response.status === 401) {
          return { success: true, data: null }
        }

        // response body 중복 읽기 방지를 위해 clone
        const clonedResponse = response.clone()
        let errorMessage = `HTTP error! status: ${response.status}`

        try {
          const errorData = await response.json()

          // 에러 응답 구조 검증 및 메시지 추출
          let extractedMessage = errorMessage

          if (typeof errorData === 'object' && errorData !== null) {
            // 백엔드 UserErrorResponse 구조: { errorCode, errorMessage }
            if (typeof errorData.errorMessage === 'string') {
              extractedMessage = errorData.errorMessage
            } else if (typeof errorData.message === 'string') {
              extractedMessage = errorData.message
            } else if (typeof errorData.error === 'string') {
              extractedMessage = errorData.error
            }
          }

          const needsRelogin = extractedMessage.includes("다른기기에서 로그아웃 하셨습니다")

          if (needsRelogin && typeof window !== 'undefined') {
            const event = new CustomEvent('needsRelogin', {
              detail: {
                title: '로그인이 필요합니다',
                message: '다른기기에서 로그아웃 하셨습니다 다시 로그인해주세요'
              }
            })
            window.dispatchEvent(event)
          }

          // Promise를 reject하여 TanStack Query가 에러로 인식하도록 함
          throw new Error(extractedMessage)
        } catch (jsonError) {
          logger.warn('Failed to parse error response as JSON:', jsonError)

          try {
            // clone된 response 사용
            const errorText = await clonedResponse.text()
            if (errorText) {
              errorMessage = errorText
            }
          } catch (textError) {
            logger.error('Failed to parse error response as text:', textError)
          }

          // Promise를 reject하여 TanStack Query가 에러로 인식하도록 함
          throw new Error(errorMessage)
        }
      }

      // Content-Length 체크로 빈 응답 먼저 처리
      const contentLength = response.headers.get('content-length')
      if (contentLength === '0') {
        // 201 Created + Location 헤더 처리 (게시글 작성 등)
        if (response.status === 201) {
          const location = response.headers.get('location')
          logger.log('[apiClient] 201 응답 - Location 헤더:', location);
          if (location) {
            // Location: /post/123 → id: 123 추출
            const match = location.match(/\/post\/(\d+)$/)
            logger.log('[apiClient] Location 매칭 결과:', match);
            if (match) {
              const extractedId = parseInt(match[1]);
              logger.log('[apiClient] 추출된 ID:', extractedId);
              return {
                success: true,
                data: { id: extractedId } as T,
              }
            }
          }
        }

        return {
          success: true,
          data: null as T,
        }
      }

      // response body 중복 읽기 방지를 위해 clone
      const clonedResponse = response.clone()

      try {
        const rawData = await response.json()

        // API 응답 구조 검증
        if (isValidApiResponse<T>(rawData)) {
          return rawData
        }

        // 원시 데이터를 ApiResponse 구조로 래핑
        return {
          success: true,
          data: rawData as T,
        }
      } catch (parseError) {
        // JSON 파싱 실패 시 cloned response로 텍스트 재시도
        try {
          const text = await clonedResponse.text()
          if (text === "OK" || text === "") {
            return {
              success: true,
              data: null as T,
            }
          }

          // 텍스트 응답을 데이터로 처리
          return {
            success: true,
            data: text as T,
          }
        } catch (textError) {
          logger.error('Response parsing failed:', {
            parseError,
            textError,
            url: `${this.baseURL}${endpoint}`
          })

          return {
            success: false,
            error: "Failed to parse response",
          }
        }
      }
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : "Network error",
      }
    }
  }

  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: "GET" })
  }

  async post<T>(endpoint: string, body?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    })
  }

  async put<T>(endpoint: string, body?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
    })
  }

  async delete<T>(endpoint: string, body?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "DELETE",
      body: body ? JSON.stringify(body) : undefined,
    })
  }

  async patch<T>(endpoint: string, body?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: body ? JSON.stringify(body) : undefined,
    })
  }
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
export const apiClient = new ApiClient(API_BASE_URL)

export const csrfDebugUtils = {
  getCurrentToken: () => getCookie("XSRF-TOKEN"),
  logCurrentToken: () => {
    const token = getCookie("XSRF-TOKEN")
    logger.log("Current CSRF Token:", token ? token.substring(0, 8) + '...' : 'Not found')
  }
}