import { apiClient } from '../client'
import { AuthResponse, SocialLoginRequest, SignUpRequest } from '@/types/domains/auth'

export const authCommand = {
  kakaoLogin: (code: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'KAKAO',
      code,
    }
    return apiClient.post<AuthResponse>("/api/auth/login", requestBody)
  },

  naverLogin: (code: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'NAVER',
      code,
    }
    return apiClient.post<AuthResponse>("/api/auth/login", requestBody)
  },

  googleLogin: (code: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'GOOGLE',
      code,
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
