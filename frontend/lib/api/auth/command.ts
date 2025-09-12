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
  
  signUp: (userName: string, uuid: string) => {
    const requestBody: SignUpRequest = {
      userName,
      uuid
    }
    return apiClient.post<AuthResponse>("/api/auth/signup", requestBody)
  },
  
  logout: () => apiClient.post("/api/auth/logout"),
  
  withdraw: () => apiClient.delete("/api/user/withdraw"),
}