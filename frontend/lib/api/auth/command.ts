import { apiClient } from '../client'
import { AuthResponse, SocialLoginRequest, SignUpRequest } from '@/types/domains/auth'

export const authCommand = {
  kakaoLogin: (code: string, fcmToken?: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'KAKAO',
      code,
      fcmToken
    }
    return apiClient.post<AuthResponse>("/api/auth/command/login", requestBody)
  },
  
  signUp: (userName: string, uuid: string) => {
    const requestBody: SignUpRequest = {
      userName,
      uuid
    }
    return apiClient.post<AuthResponse>("/api/auth/command/signup", requestBody)
  },
  
  logout: () => apiClient.post("/api/auth/command/logout"),
  
  withdraw: () => apiClient.delete("/api/auth/command/withdraw"),
}