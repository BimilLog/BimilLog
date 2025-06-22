"use client"

import { Button } from "@/components/ui/button"
import { Heart } from "lucide-react"
import Link from "next/link"
import { useAuth } from "@/hooks/useAuth"
import { NotificationBell } from "./notification-bell"
import { MobileNav } from "./mobile-nav"

export function AuthHeader() {
  const { user, isAuthenticated, isLoading, logout } = useAuth()

  return (
    <header className="border-b bg-white/80 backdrop-blur-sm sticky top-0 z-50">
      <div className="container mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link href="/" className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
              <Heart className="w-5 h-5 text-white" />
            </div>
            <span className="text-xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              비밀로그
            </span>
          </Link>
          <MobileNav />
        </div>

        <nav className="hidden md:flex items-center space-x-6">
          <Link href="/board" className="text-gray-600 hover:text-gray-900 transition-colors">
            게시판
          </Link>
          <Link href="/popular" className="text-gray-600 hover:text-gray-900 transition-colors">
            인기글
          </Link>
          {isAuthenticated && user && (
            <Link
              href={`/rolling-paper/${user.nickname}`}
              className="text-gray-600 hover:text-gray-900 transition-colors"
            >
              내 롤링페이퍼
            </Link>
          )}
        </nav>

        <div className="flex items-center space-x-2">
          {isLoading ? (
            <div className="w-20 h-8 bg-gray-200 rounded animate-pulse"></div>
          ) : isAuthenticated && user ? (
            <>
              <NotificationBell />
              <Button asChild variant="ghost" className="text-gray-600 hover:text-gray-900">
                <Link href="/mypage">{user.nickname}님</Link>
              </Button>
              <Button
                variant="outline"
                onClick={logout}
                className="bg-white text-red-600 border-red-200 hover:bg-red-50"
              >
                로그아웃
              </Button>
            </>
          ) : (
            <>
              <Button asChild variant="ghost" className="text-gray-600 hover:text-gray-900">
                <Link href="/login">로그인</Link>
              </Button>
              <Button
                asChild
                className="bg-gradient-to-r from-pink-500 to-purple-600 hover:from-pink-600 hover:to-purple-700"
              >
                <Link href="/signup">시작하기</Link>
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
