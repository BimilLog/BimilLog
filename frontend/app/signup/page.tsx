"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Heart, Check, AlertCircle } from "lucide-react"
import Link from "next/link"
import { useState, useEffect } from "react"
import { useAuth } from "@/hooks/useAuth"
import { useRouter, useSearchParams } from "next/navigation"

export default function SignupPage() {
  const { login, isAuthenticated, isLoading, updateUserName, refreshUser } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()
  const needsNickname = searchParams.get("nickname") === "required"

  const [nickname, setNicknameInput] = useState("")
  const [isNicknameValid, setIsNicknameValid] = useState<boolean | null>(null)
  const [isChecking, setIsChecking] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (isAuthenticated && !needsNickname) {
      router.push("/")
    }
  }, [isAuthenticated, needsNickname, router])

  // 닉네임 중복 확인 (서버 API가 없으므로 임시 로직)
  const handleCheckNickname = async () => {
    if (!nickname.trim()) return
    setIsChecking(true)
    // 실제 서버 API가 없으므로 임시로 항상 사용 가능하다고 가정
    // TODO: 서버에 닉네임 중복 확인 API가 있다면 userApi.checkNickname(nickname) 호출
    await new Promise((resolve) => setTimeout(resolve, 500)) // Simulate API call
    setIsNicknameValid(nickname.trim() !== "testuser") // 예시: "testuser"는 사용 불가
    setIsChecking(false)
  }

  const handleSetNickname = async () => {
    if (!isNicknameValid || !nickname.trim()) return

    setIsSubmitting(true)
    try {
      const success = await updateUserName(nickname) // 닉네임 변경 API 호출
      if (success) {
        await refreshUser() // 닉네임 변경 후 사용자 정보 새로고침
        router.push("/")
      } else {
        alert("닉네임 설정에 실패했습니다. 다시 시도해주세요.")
      }
    } catch (error) {
      console.error("Set nickname failed:", error)
      alert("닉네임 설정에 실패했습니다. 다시 시도해주세요.")
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <Heart className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600">로딩 중...</p>
        </div>
      </div>
    )
  }

  // 닉네임 설정이 필요한 경우
  if (needsNickname) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center p-4">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <Link href="/" className="inline-flex items-center space-x-2">
              <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center">
                <Heart className="w-7 h-7 text-white" />
              </div>
              <span className="text-2xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
                비밀로그
              </span>
            </Link>
          </div>

          <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
            <CardHeader className="text-center pb-2">
              <CardTitle className="text-2xl font-bold text-gray-800">닉네임 설정</CardTitle>
              <CardDescription className="text-gray-600">사용할 닉네임을 설정해주세요</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6 pt-6">
              <div className="space-y-2">
                <Label htmlFor="nickname" className="text-sm font-medium text-gray-700">
                  닉네임
                </Label>
                <div className="flex space-x-2">
                  <div className="flex-1 relative">
                    <Input
                      id="nickname"
                      type="text"
                      placeholder="사용할 닉네임을 입력하세요"
                      value={nickname}
                      onChange={(e) => {
                        setNicknameInput(e.target.value)
                        setIsNicknameValid(null)
                      }}
                      className={`pr-10 ${
                        isNicknameValid === true
                          ? "border-green-500"
                          : isNicknameValid === false
                            ? "border-red-500"
                            : ""
                      }`}
                    />
                    {isNicknameValid === true && (
                      <Check className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-green-500" />
                    )}
                    {isNicknameValid === false && (
                      <AlertCircle className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-red-500" />
                    )}
                  </div>
                  <Button
                    variant="outline"
                    onClick={handleCheckNickname}
                    disabled={!nickname.trim() || isChecking}
                    className="px-4"
                  >
                    {isChecking ? "확인중..." : "중복확인"}
                  </Button>
                </div>
                {isNicknameValid === false && <p className="text-sm text-red-600">이미 사용중인 닉네임입니다.</p>}
                {isNicknameValid === true && <p className="text-sm text-green-600">사용 가능한 닉네임입니다.</p>}
              </div>

              <Button
                className="w-full h-12 bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700 font-semibold"
                disabled={!isNicknameValid || isSubmitting}
                onClick={handleSetNickname}
              >
                {isSubmitting ? "설정 중..." : "닉네임 설정 완료"}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    )
  }

  // 일반 회원가입 페이지
  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center space-x-2">
            <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center">
              <Heart className="w-7 h-7 text-white" />
            </div>
            <span className="text-2xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              비밀로그
            </span>
          </Link>
        </div>

        <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
          <CardHeader className="text-center pb-2">
            <CardTitle className="text-2xl font-bold text-gray-800">회원가입</CardTitle>
            <CardDescription className="text-gray-600">나만의 롤링페이퍼를 만들어보세요</CardDescription>
          </CardHeader>
          <CardContent className="space-y-6 pt-6">
            {/* 카카오 로그인 */}
            <Button
              className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-gray-800 font-semibold text-base"
              onClick={() => login("/signup?nickname=required")}
            >
              <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3z" />
              </svg>
              카카오로 시작하기
            </Button>

            <div className="text-center">
              <p className="text-sm text-gray-600">
                이미 계정이 있으신가요?{" "}
                <Link href="/login" className="text-purple-600 hover:text-purple-700 font-medium">
                  로그인
                </Link>
              </p>
            </div>

            <div className="bg-purple-50 border border-purple-200 rounded-lg p-4">
              <div className="text-sm text-purple-800">
                <p className="font-medium mb-2">🎉 회원가입 혜택</p>
                <ul className="space-y-1 text-sm">
                  <li>• 나만의 롤링페이퍼 생성</li>
                  <li>• 메시지 관리 및 삭제</li>
                  <li>• 커뮤니티 참여 및 추천</li>
                  <li>• 실시간 알림 수신</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="text-center mt-6">
          <Link href="/" className="text-gray-500 hover:text-gray-700 transition-colors">
            ← 홈으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  )
}
