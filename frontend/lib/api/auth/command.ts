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
  
  signUp: (memberName: string, uuid: string) => {
    const requestBody: SignUpRequest = {
      memberName,
      uuid
    }
    return apiClient.post<AuthResponse>("/api/auth/signup", requestBody)
  },
  
  logout: () => apiClient.post("/api/auth/logout"),
}