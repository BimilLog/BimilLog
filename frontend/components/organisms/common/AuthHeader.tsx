"use client";

import React from "react";
import Link from "next/link";
import Image from "next/image";
import dynamic from "next/dynamic";
import { useAuth } from "@/hooks";
import { Button } from "@/components";
import { Avatar, AvatarFallback, AvatarImage } from "@/components";
import { Settings } from "lucide-react";
import { MobileNav } from "@/components/organisms/common/MobileNav";

const NotificationBell = dynamic(
  () => import("@/components/organisms/common/notification-bell").then(mod => ({ default: mod.NotificationBell })),
  {
    ssr: false,
    loading: () => <div className="w-6 h-6 animate-pulse bg-gray-200 rounded-full" />
  }
);

export const AuthHeader = React.memo(() => {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.substring(0, 1).toUpperCase();
  };

  return (
    <header className="border-b bg-white/80 backdrop-blur-sm sticky top-0 z-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-3 sm:py-4 flex items-center justify-between">
        <div className="flex items-center">
          <Link href="/" className="flex items-center">
            <Image
              src="/log.png"
              alt="비밀로그"
              width={48}
              height={48}
              className="h-10 sm:h-12 w-auto object-contain"
              priority
              placeholder="blur"
              blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDgiIGhlaWdodD0iNDgiIHZpZXdCb3g9IjAgMCA0OCA0OCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRjNGNEY2Ii8+Cjwvc3ZnPgo="
            />
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-end">
          {/* 데스크톱 네비게이션 */}
          <nav className="hidden lg:flex items-center gap-6">
            <Link
              href="/board"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm lg:text-base"
            >
              게시판
            </Link>
            <Link
              href="/visit"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm lg:text-base"
            >
              다른 롤링페이퍼 방문
            </Link>
            {isAuthenticated && (
              <Link
                href="/rolling-paper"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm lg:text-base"
              >
                내 롤링페이퍼
              </Link>
            )}
            <Link
              href="/suggest"
              className="text-gray-600 hover:text-gray-900 transition-colors text-sm lg:text-base"
            >
              건의하기
            </Link>
            {isAuthenticated && (
              <Link
                href="/mypage"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm lg:text-base"
              >
                마이페이지
              </Link>
            )}
            {isAuthenticated && user?.role === "ADMIN" && (
              <Link
                href="/admin"
                className="text-red-600 hover:text-red-700 transition-colors font-semibold text-sm lg:text-base"
              >
                관리자
              </Link>
            )}
            {!isAuthenticated && (
              <Link
                href="/login"
                className="text-gray-600 hover:text-gray-900 transition-colors text-sm lg:text-base"
              >
                로그인
              </Link>
            )}
          </nav>

          {/* 태블릿 네비게이션 */}
          <nav className="hidden sm:flex lg:hidden items-center gap-4">
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

        <div className="flex items-center justify-end gap-2 sm:gap-3">
          {isLoading ? (
            <div className="w-8 h-8 bg-gray-200 rounded-full animate-pulse" />
          ) : isAuthenticated && user ? (
            <>
              <NotificationBell />
              <div className="flex items-center gap-2 sm:gap-3">
                <Link
                  href="/settings"
                  className="hidden lg:flex p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors min-h-[44px] min-w-[44px] touch-manipulation"
                  title="설정"
                >
                  <Settings className="w-5 h-5" />
                </Link>
                <Link href="/rolling-paper" title="내 롤링페이퍼로 이동" className="min-h-[44px] min-w-[44px] flex items-center justify-center touch-manipulation">
                  <Avatar className="h-8 w-8 sm:h-9 sm:w-9 hover:ring-2 hover:ring-purple-200 transition-all cursor-pointer">
                    <AvatarImage
                      src={user.thumbnailImage}
                      alt={user.userName}
                    />
                    <AvatarFallback className="text-sm">
                      {getInitials(user.userName)}
                    </AvatarFallback>
                  </Avatar>
                </Link>
                <span className="hidden sm:inline lg:hidden xl:inline font-semibold text-sm text-gray-700 max-w-24 lg:max-w-none truncate">
                  {user.userName}님
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={logout}
                  className="hidden lg:flex bg-white text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700 text-sm px-3 min-h-[44px] touch-manipulation"
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
});

AuthHeader.displayName = "AuthHeader";
