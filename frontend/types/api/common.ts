export interface ApiResponse<T = any> {
  success: boolean
  data?: T | null
  message?: string
  error?: string
  needsRelogin?: boolean
}

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

export interface ErrorResponse {
  error: string
  message: string
  timestamp: string
  path: string
}