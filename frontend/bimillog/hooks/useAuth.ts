"use client"

import type React from "react"

import { useState, useEffect, createContext, useContext } from "react"
import { authApi, userApi, type User } from "@/lib/api"

interface AuthContextType {
  user: User | null
  isLoading: boolean
  isAuthenticated: boolean
  login: (postAuthRedirectUrl?: string) => void
  logout: () => Promise<void>
  updateUserName: (userName: string) => Promise<boolean>
  deleteAccount: () => Promise<boolean>
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = !!user

  // 현재 사용자 정보 조회
  const refreshUser = async () => {
    try {
      const response = await authApi.getCurrentUser()
      if (response.success && response.data) {
        setUser(response.data)
      } else {
        setUser(null)
      }
    } catch (error) {
      console.error("Failed to fetch user:", error)
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }

  // 카카오 로그인
  const login = (postAuthRedirectUrl?: string) => {
    const kakaoAuthUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL
    const kakaoClientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID
    const kakaoRedirectUri = `${window.location.origin}/auth/callback` // v0 미리보기 URL로 자동 설정됩니다.
    const responseType = "code"

    let url = `${kakaoAuthUrl}?client_id=${kakaoClientId}&redirect_uri=${encodeURIComponent(kakaoRedirectUri)}&response_type=${responseType}`

    if (postAuthRedirectUrl) {
      url += `&state=${encodeURIComponent(postAuthRedirectUrl)}` // 최종 리다이렉트 URL을 state 파라미터로 전달
    }
    window.location.href = url
  }

  // 로그아웃
  const logout = async (): Promise<void> => {
    try {
      await authApi.logout()
      setUser(null)
      window.location.href = "/"
    } catch (error) {
      console.error("Logout failed:", error)
    }
  }

  // 닉네임 변경
  const updateUserName = async (userName: string): Promise<boolean> => {
    try {
      const response = await userApi.updateUserName(userName)
      if (response.success) {
        await refreshUser()
        return true
      }
      return false
    } catch (error) {
      console.error("Update username failed:", error)
      return false
    }
  }

  // 회원 탈퇴
  const deleteAccount = async (): Promise<boolean> => {
    try {
      const response = await authApi.deleteAccount()
      if (response.success) {
        setUser(null)
        window.location.href = "/"
        return true
      }
      return false
    } catch (error) {
      console.error("Delete account failed:", error)
      return false
    }
  }

  useEffect(() => {
    refreshUser()
  }, [])

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated,
    login,
    logout,
    updateUserName,
    deleteAccount,
    refreshUser,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
