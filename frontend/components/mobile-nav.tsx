"use client";

import { useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import {
  Menu,
  Home,
  MessageSquare,
  User as UserIcon,
  TrendingUp,
  Shield,
} from "lucide-react";
import Link from "next/link";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";

export function MobileNav() {
  const { user, isAuthenticated, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.charAt(0).toUpperCase();
  };

  return (
    <div className="md:hidden">
      <Sheet open={isOpen} onOpenChange={setIsOpen}>
        <SheetTrigger asChild>
          <Button variant="ghost" size="icon">
            <Menu className="h-6 w-6" />
            <span className="sr-only">메뉴 열기</span>
          </Button>
        </SheetTrigger>
        <SheetContent side="left" className="w-64 flex flex-col">
          <SheetHeader className="text-left">
            <SheetTitle className="sr-only">메인 메뉴</SheetTitle>
            <SheetDescription className="sr-only">
              사이트의 주요 페이지로 이동할 수 있는 링크 목록입니다.
            </SheetDescription>
            {isAuthenticated && user ? (
              <div className="flex items-center space-x-2 pt-4">
                <Avatar>
                  <AvatarImage
                    src={user.thumbnailImage || ""}
                    alt={user.userName || ""}
                  />
                  <AvatarFallback>{getInitials(user.userName)}</AvatarFallback>
                </Avatar>
                <span className="font-semibold">{user.userName}</span>
              </div>
            ) : (
              <h2 className="text-lg font-semibold pt-4">비밀로그</h2>
            )}
          </SheetHeader>

          <div className="flex-grow">
            <nav className="flex flex-col space-y-2 mt-4">
              <Link
                href="/"
                onClick={() => setIsOpen(false)}
                className="flex items-center space-x-2 p-2 rounded-md hover:bg-gray-100"
              >
                <Home className="w-5 h-5" />
                <span>홈</span>
              </Link>
              <Link
                href="/board"
                onClick={() => setIsOpen(false)}
                className="flex items-center space-x-2 p-2 rounded-md hover:bg-gray-100"
              >
                <MessageSquare className="w-5 h-5" />
                <span>게시판</span>
              </Link>
              <Link
                href="/visit"
                onClick={() => setIsOpen(false)}
                className="flex items-center space-x-2 p-2 rounded-md hover:bg-gray-100"
              >
                <TrendingUp className="w-5 h-5" />
                <span>롤링페이퍼 방문</span>
              </Link>
              {isAuthenticated && (
                <Link
                  href="/mypage"
                  onClick={() => setIsOpen(false)}
                  className="flex items-center space-x-2 p-2 rounded-md hover:bg-gray-100"
                >
                  <UserIcon className="w-5 h-5" />
                  <span>마이페이지</span>
                </Link>
              )}
              {isAuthenticated && user?.role === "ADMIN" && (
                <Link
                  href="/admin"
                  onClick={() => setIsOpen(false)}
                  className="flex items-center space-x-2 p-2 rounded-md hover:bg-red-100 text-red-600"
                >
                  <Shield className="w-5 h-5" />
                  <span>관리자</span>
                </Link>
              )}
            </nav>
          </div>

          <div className="pb-4">
            {isAuthenticated ? (
              <Button
                onClick={() => {
                  logout();
                  setIsOpen(false);
                }}
                className="w-full"
                variant="outline"
              >
                로그아웃
              </Button>
            ) : (
              <div className="flex flex-col space-y-2">
                <Button
                  asChild
                  onClick={() => setIsOpen(false)}
                  className="w-full"
                >
                  <Link href="/login">로그인</Link>
                </Button>
                <Button
                  asChild
                  onClick={() => setIsOpen(false)}
                  variant="secondary"
                  className="w-full"
                >
                  <Link href="/signup">회원가입</Link>
                </Button>
              </div>
            )}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
