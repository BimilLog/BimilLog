"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/atoms/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/atoms/avatar";
import { Settings } from "lucide-react";
import { MobileNav } from "@/components/organisms/mobile-nav";
import { NotificationBell } from "./notification-bell";

export function AuthHeader() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.substring(0, 1).toUpperCase();
  };

  return (
    <header className="border-b bg-white/80 backdrop-blur-sm sticky top-0 z-50">
      <div className="container mx-auto px-4 py-4 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link href="/" className="flex items-center">
            <img
              src="/log.png"
              alt="비밀로그"
              className="h-12 object-contain"
            />
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-end">
          <nav className="hidden md:flex items-center gap-6">
            <Link
              href="/board"
              className="text-gray-600 hover:text-gray-900 transition-colors"
            >
              게시판
            </Link>
            {isAuthenticated && (
              <Link
                href="/rolling-paper"
                className="text-gray-600 hover:text-gray-900 transition-colors flex items-center space-x-1"
              >
                <span>내 롤링페이퍼</span>
              </Link>
            )}
            <Link
              href="/suggest"
              className="text-gray-600 hover:text-gray-900 transition-colors"
            >
              건의하기
            </Link>
            {isAuthenticated && (
              <Link
                href="/mypage"
                className="text-gray-600 hover:text-gray-900 transition-colors"
              >
                마이페이지
              </Link>
            )}
            {isAuthenticated && user?.role === "ADMIN" && (
              <Link
                href="/admin"
                className="text-red-600 hover:text-red-700 transition-colors font-semibold"
              >
                관리자
              </Link>
            )}
            {!isAuthenticated && (
              <Link
                href="/login"
                className="text-gray-600 hover:text-gray-900 transition-colors"
              >
                로그인
              </Link>
            )}
          </nav>
        </div>

        <div className="flex items-center justify-end">
          {isLoading ? (
            <div className="w-8 h-8 bg-gray-200 rounded-full animate-pulse" />
          ) : isAuthenticated && user ? (
            <>
              <NotificationBell />
              <div className="flex items-center space-x-2 md:space-x-4">
                <Link
                  href="/settings"
                  className="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                  title="설정"
                >
                  <Settings className="w-5 h-5" />
                </Link>
                <Avatar className="h-9 w-9">
                  <AvatarImage src={user.thumbnailImage} alt={user.userName} />
                  <AvatarFallback>{getInitials(user.userName)}</AvatarFallback>
                </Avatar>
                <span className="hidden sm:inline font-semibold text-sm text-gray-700">
                  {user.userName}님
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={logout}
                  className="hidden md:flex bg-white text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700"
                >
                  로그아웃
                </Button>
              </div>
            </>
          ) : null}
          <MobileNav />
        </div>
      </div>
    </header>
  );
}
