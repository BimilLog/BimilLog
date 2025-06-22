"use client"

import { useEffect } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Heart } from "lucide-react"
import { authApi } from "@/lib/api"
import { useAuth } from "@/hooks/useAuth"

export default function AuthCallbackPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { refreshUser } = useAuth()

  useEffect(() => {
    const handleCallback = async () => {
      const code = searchParams.get("code")
      const error = searchParams.get("error")
      const state = searchParams.get("state") // 최종 리다이렉트 URL

      if (error) {
        console.error("Kakao Auth error:", error)
        router.push("/login?error=" + encodeURIComponent(error))
        return
      }

      if (code) {
        try {
          // 인가 코드를 서버로 전송하여 로그인 처리
          const response = await authApi.kakaoLogin(code)
          if (response.success) {
            await refreshUser() // 사용자 정보 새로고침
            // 서버 응답에 따라 닉네임 설정이 필요한지 확인
            // Swagger 문서에 따르면 /auth/login 응답에 닉네임 필요 여부가 명시되어 있지 않으므로,
            // 일단 `refreshUser` 후 사용자 객체에 닉네임이 없으면 설정 페이지로 리다이렉트하는 로직을 추가합니다.
            const user = await authApi.getCurrentUser()
            if (user.success && user.data && !user.data.userName) {
              router.push("/signup?nickname=required")
            } else {
              const finalRedirect = state ? decodeURIComponent(state) : "/"
              router.push(finalRedirect)
            }
          } else {
            console.error("Server login failed:", response.error)
            router.push("/login?error=" + encodeURIComponent(response.error || "로그인 실패"))
          }
        } catch (apiError) {
          console.error("API call failed during Kakao login:", apiError)
          router.push("/login?error=" + encodeURIComponent("API 호출 실패"))
        }
      } else {
        // code가 없는 경우 (예: 사용자가 로그인 취소)
        router.push("/login?error=" + encodeURIComponent("카카오 로그인 취소 또는 오류"))
      }
    }

    handleCallback()
  }, [router, searchParams, refreshUser])

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
      <div className="text-center">
        <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
          <Heart className="w-7 h-7 text-white animate-pulse" />
        </div>
        <p className="text-gray-600">로그인 처리 중...</p>
      </div>
    </div>
  )
}
