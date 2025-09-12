// API 응답 및 요청 타입 정의

export interface ApiResponse<T = any> {
  success: boolean
  data?: T | null
  message?: string
  error?: string
  needsRelogin?: boolean // 다른 기기에서 로그아웃된 경우
}

// 페이지네이션 타입
export interface PageResponse<T> {
  totalPages: number
  totalElements: number
  size: number
  content: T[]
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

// 백엔드 v2 Auth API 타입 정의
export interface AuthResponse {
  status: "NEW_USER" | "EXISTING_USER" | "SUCCESS"
  uuid?: string
  data: Record<string, any>
}

export interface SocialLoginRequest {
  provider: string
  code: string
  fcmToken?: string
}

export interface SignUpRequest {
  userName: string
  uuid: string
}