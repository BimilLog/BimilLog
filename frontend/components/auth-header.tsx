"use client";

import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Heart } from "lucide-react";
import { MobileNav } from "@/components/mobile-nav";
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
          <Link href="/" className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
              <Heart className="w-5 h-5 text-white" />
            </div>
            <span className="text-xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              비밀로그
            </span>
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

        <div className="flex items-center justify-end space-x-2 md:space-x-4">
          {isLoading ? (
            <div className="w-8 h-8 bg-gray-200 rounded-full animate-pulse" />
          ) : isAuthenticated && user ? (
            <>
              <NotificationBell />
              <Avatar className="h-9 w-9">
                <AvatarImage src={user.thumbnailImage} alt={user.userName} />
                <AvatarFallback>{getInitials(user.userName)}</AvatarFallback>
              </Avatar>
              <span className="hidden sm:inline font-semibold text-sm text-gray-700">
                {user.userName}님
              </span>
              <Button
                variant="outline"
                onClick={logout}
                className="bg-white text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700"
              >
                로그아웃
              </Button>
            </>
          ) : null}
          <MobileNav />
        </div>
      </div>
    </header>
  );
}
