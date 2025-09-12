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

export interface LoginStatus {
  isLoggedIn: boolean
  userName?: string
}