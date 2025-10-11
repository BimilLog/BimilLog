import { apiClient } from '../client'
import { AuthResponse, SocialLoginRequest, SignUpRequest } from '@/types/domains/auth'

export const authCommand = {
  kakaoLogin: (code: string, fcmToken?: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'KAKAO',
      code,
      fcmToken
    }
    return apiClient.post<AuthResponse>("/api/auth/login", requestBody)
  },
  
  signUp: (memberName: string) => {
    const requestBody: SignUpRequest = {
      memberName
    }
    return apiClient.post<AuthResponse>("/api/member/signup", requestBody)
  },
  
  logout: () => apiClient.post("/api/auth/logout"),
}