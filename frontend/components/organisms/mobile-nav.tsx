"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components";
import { cn } from "@/lib/utils";
import {
  Menu,
  X,
  Home,
  MessageSquare,
  Users,
  User,
  Settings,
  Heart,
  Edit,
  Sparkles,
  Shield,
} from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/atoms/avatar";

interface MobileNavProps {
  className?: string;
}

export function MobileNav({ className }: MobileNavProps) {
  const [isOpen, setIsOpen] = useState(false);
  const pathname = usePathname();
  const { isAuthenticated, user, logout } = useAuth();

  const toggleNav = () => setIsOpen(!isOpen);
  const closeNav = () => setIsOpen(false);

  const navItems = [
    { href: "/", label: "홈", icon: Home },
    ...(isAuthenticated
      ? [{ href: "/rolling-paper", label: "내 롤링페이퍼", icon: Heart }]
      : []),
    { href: "/visit", label: "롤링페이퍼 방문", icon: Users },
    ...(isAuthenticated
      ? [{ href: "/mypage", label: "마이페이지", icon: User }]
      : []),
    { href: "/board", label: "게시판", icon: MessageSquare },
    { href: "/suggest", label: "건의하기", icon: Sparkles },
    ...(isAuthenticated
      ? [
          { href: "/settings", label: "설정", icon: Settings },
          ...(user?.role === "ADMIN"
            ? [{ href: "/admin", label: "관리자", icon: Shield }]
            : []),
        ]
      : [{ href: "/login", label: "로그인", icon: User }]),
  ];

  return (
    <>
      {/* 햄버거 메뉴 버튼 */}
      <Button
        variant="ghost"
        size="icon"
        onClick={toggleNav}
        className={cn(
          "md:hidden relative z-50 p-2",
          isOpen && "bg-white/90 text-purple-600",
          className
        )}
        aria-label="메뉴 열기"
      >
        {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
      </Button>

      {/* 모바일 네비게이션 오버레이 */}
      {isOpen && (
        <>
          {/* 배경 오버레이 */}
          <div
            className="fixed inset-0 bg-black/20 backdrop-blur-sm z-40 md:hidden"
            onClick={closeNav}
            aria-hidden="true"
          />

          {/* 네비게이션 메뉴 */}
          <div className="fixed inset-y-0 right-0 z-50 w-full max-w-sm bg-white shadow-2xl md:hidden">
            <div className="flex h-full flex-col">
              {/* 헤더 */}
              <div className="flex items-center justify-between p-6 bg-gradient-to-r from-pink-50 to-purple-50">
                <div className="flex items-center space-x-3">
                  {isAuthenticated && user ? (
                    <>
                      <Avatar className="h-9 w-9">
                        <AvatarImage
                          src={user.thumbnailImage || undefined}
                          alt={user.userName}
                          onError={(e) => {
                            // 이미지 로딩 실패 시 fallback으로 전환
                            e.currentTarget.style.display = "none";
                          }}
                        />
                        <AvatarFallback className="bg-gradient-to-r from-pink-500 to-purple-600 text-white text-sm font-medium">
                          {user.userName?.charAt(0)?.toUpperCase() || "?"}
                        </AvatarFallback>
                      </Avatar>
                      <p className="text-lg font-semibold text-gray-700">
                        {user.userName}님
                      </p>
                    </>
                  ) : (
                    <div className="flex items-center space-x-2">
                      <div className="w-9 h-9 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
                        <Heart className="w-5 h-5 text-white" />
                      </div>
                      <p className="text-lg font-semibold text-gray-700">
                        비밀로그
                      </p>
                    </div>
                  )}
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={closeNav}
                  className="text-gray-500 hover:text-gray-700"
                  aria-label="메뉴 닫기"
                >
                  <X className="h-6 w-6" />
                </Button>
              </div>

              {/* 네비게이션 링크들 */}
              <nav className="flex-1 px-6 py-4 space-y-2 bg-white">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  const isActive = pathname === item.href;
                  const isAdmin = item.href === "/admin";

                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={closeNav}
                      className={cn(
                        "flex items-center space-x-3 px-4 py-4 rounded-lg text-lg font-medium transition-all duration-200",
                        "min-h-[48px] touch-manipulation", // 터치 타겟 최적화 - 더 큰 높이
                        isActive
                          ? isAdmin
                            ? "bg-gradient-to-r from-red-500 to-red-600 text-white shadow-lg"
                            : "bg-gradient-to-r from-pink-500 to-purple-600 text-white shadow-lg"
                          : isAdmin
                          ? "text-red-600 hover:bg-red-50 hover:text-red-700 active:scale-[0.98] font-semibold"
                          : "text-gray-700 hover:bg-purple-50 hover:text-purple-600 active:scale-[0.98]"
                      )}
                    >
                      <Icon className="w-5 h-5" />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </nav>

              {/* 하단 액션 */}
              <div className="p-6 border-t border-gray-200 bg-gray-50">
                {isAuthenticated ? (
                  <div className="space-y-3">
                    <Button
                      asChild
                      variant="outline"
                      size="full"
                      className="justify-start"
                    >
                      <Link href="/board/write" onClick={closeNav}>
                        <Edit className="w-4 h-4 mr-2" />
                        글쓰기
                      </Link>
                    </Button>
                    <Button
                      variant="secondary"
                      size="full"
                      onClick={() => {
                        logout();
                        closeNav();
                      }}
                      className="justify-start"
                    >
                      로그아웃
                    </Button>
                  </div>
                ) : (
                  <Button asChild size="full" onClick={closeNav}>
                    <Link href="/login">
                      <User className="w-4 h-4 mr-2" />
                      로그인
                    </Link>
                  </Button>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
