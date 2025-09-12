"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components";
import { Avatar, AvatarFallback, AvatarImage } from "@/components";
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
      <div className="container mx-auto px-3 sm:px-4 py-3 sm:py-4 flex items-center justify-between">
        <div className="flex items-center space-x-2 sm:space-x-4">
          <Link href="/" className="flex items-center">
            <img
              src="/log.png"
              alt="비밀로그"
              className="h-10 sm:h-12 object-contain"
            />
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-end">
          <nav className="hidden lg:flex items-center gap-4 xl:gap-6">
            <Link
              href="/board"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm xl:text-base"
            >
              게시판
            </Link>
            <Link
              href="/visit"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm xl:text-base"
            >
              다른 롤링페이퍼 방문
            </Link>
            {isAuthenticated && (
              <Link
                href="/rolling-paper"
                className="text-gray-600 hover:text-gray-900 transition-colors flex items-center space-x-1 text-sm xl:text-base"
              >
                <span className="hidden xl:inline">내 </span>
                <span>롤링페이퍼</span>
              </Link>
            )}
            <Link
              href="/suggest"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm xl:text-base"
            >
              건의하기
            </Link>
            {isAuthenticated && (
              <Link
                href="/mypage"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm xl:text-base"
              >
                마이페이지
              </Link>
            )}
            {isAuthenticated && user?.role === "ADMIN" && (
              <Link
                href="/admin"
                className="text-red-600 hover:text-red-700 transition-colors font-semibold text-sm xl:text-base"
              >
                관리자
              </Link>
            )}
            {!isAuthenticated && (
              <Link
                href="/login"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm xl:text-base"
              >
                로그인
              </Link>
            )}
          </nav>

          {/* 중간 크기 화면용 간소화된 네비게이션 */}
          <nav className="hidden md:flex lg:hidden items-center gap-3">
            <Link
              href="/board"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm"
            >
              게시판
            </Link>
            <Link
              href="/visit"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm"
            >
              방문하기
            </Link>
            {isAuthenticated && (
              <Link
                href="/rolling-paper"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm"
              >
                롤링페이퍼
              </Link>
            )}
            {isAuthenticated && user?.role === "ADMIN" && (
              <Link
                href="/admin"
                className="text-red-600 hover:text-red-700 transition-colors font-semibold text-sm"
              >
                관리자
              </Link>
            )}
            {!isAuthenticated && (
              <Link
                href="/login"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm"
              >
                로그인
              </Link>
            )}
          </nav>
        </div>

        <div className="flex items-center justify-end space-x-1 sm:space-x-2">
          {isLoading ? (
            <div className="w-7 h-7 sm:w-8 sm:h-8 bg-gray-200 rounded-full animate-pulse" />
          ) : isAuthenticated && user ? (
            <>
              <NotificationBell />
              <div className="flex items-center space-x-1 sm:space-x-2 lg:space-x-3">
                <Link
                  href="/settings"
                  className="hidden lg:flex p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                  title="설정"
                >
                  <Settings className="w-4 h-4 xl:w-5 xl:h-5" />
                </Link>
                <Link href="/rolling-paper" title="내 롤링페이퍼로 이동">
                  <Avatar className="h-7 w-7 sm:h-8 sm:w-8 lg:h-9 lg:w-9 hover:ring-2 hover:ring-purple-200 transition-all cursor-pointer">
                    <AvatarImage
                      src={user.thumbnailImage}
                      alt={user.userName}
                    />
                    <AvatarFallback className="text-xs sm:text-sm">
                      {getInitials(user.userName)}
                    </AvatarFallback>
                  </Avatar>
                </Link>
                <span className="hidden sm:inline lg:hidden xl:inline font-semibold text-xs sm:text-sm text-gray-700 max-w-20 sm:max-w-24 lg:max-w-none truncate">
                  {user.userName}님
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={logout}
                  className="hidden lg:flex bg-white text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700 text-xs xl:text-sm px-2 xl:px-3"
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
