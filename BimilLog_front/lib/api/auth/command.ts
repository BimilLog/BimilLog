import { apiClient } from '../client'
import { SocialLoginRequest } from '@/types/domains/auth'

export const authCommand = {
  kakaoLogin: (code: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'KAKAO',
      code,
    }
    return apiClient.post<void>("/api/auth/login", requestBody)
  },

  naverLogin: (code: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'NAVER',
      code,
    }
    return apiClient.post<void>("/api/auth/login", requestBody)
  },

  googleLogin: (code: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'GOOGLE',
      code,
    }
    return apiClient.post<void>("/api/auth/login", requestBody)
  },

  logout: () => apiClient.post("/api/auth/logout"),
}
