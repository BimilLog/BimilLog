"use client"

import { useState } from "react"
import { useAuth } from "@/hooks/useAuth"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { Menu, Home, MessageSquare, User, TrendingUp } from "lucide-react"
import Link from "next/link"

export function MobileNav() {
  const [open, setOpen] = useState(false)
  const { user, isAuthenticated, login, logout } = useAuth()

  const navItems = [
    { href: "/", label: "홈", icon: Home },
    ...(isAuthenticated && user
      ? [{ href: `/rolling-paper/${user.nickname}`, label: "내 롤링페이퍼", icon: MessageSquare }]
      : []),
    { href: "/board", label: "게시판", icon: MessageSquare },
    { href: "/popular", label: "인기글", icon: TrendingUp },
    ...(isAuthenticated ? [{ href: "/mypage", label: "마이페이지", icon: User }] : []),
  ]

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="sm" className="md:hidden">
          <Menu className="w-5 h-5" />
        </Button>
      </SheetTrigger>
      <SheetContent side="left" className="w-64">
        <div className="flex flex-col space-y-4 mt-8">
          <div className="flex items-center space-x-2 px-4">
            <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-600 rounded-lg flex items-center justify-center">
              <MessageSquare className="w-5 h-5 text-white" />
            </div>
            <h2 className="text-xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
              비밀로그
            </h2>
          </div>

          <nav className="flex flex-col space-y-2 px-2">
            {navItems.map((item) => {
              const Icon = item.icon
              return (
                <Link key={item.href} href={item.href} onClick={() => setOpen(false)}>
                  <Button variant="ghost" className="w-full justify-start">
                    <Icon className="w-5 h-5 mr-3" />
                    {item.label}
                  </Button>
                </Link>
              )
            })}
          </nav>

          <div className="px-4 pt-4 border-t">
            {isAuthenticated && user ? (
              <div className="space-y-2">
                <p className="text-sm text-gray-600">{user.nickname}님</p>
                <Button
                  variant="outline"
                  className="w-full text-red-600 border-red-200 hover:bg-red-50"
                  onClick={() => {
                    logout()
                    setOpen(false)
                  }}
                >
                  로그아웃
                </Button>
              </div>
            ) : (
              <Button
                className="w-full bg-gradient-to-r from-pink-500 to-purple-600"
                onClick={() => {
                  login()
                  setOpen(false)
                }}
              >
                카카오 로그인
              </Button>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
}
