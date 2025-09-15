"use client";

import React from "react";
import Link from "next/link";
import Image from "next/image";
import dynamic from "next/dynamic";
import { useAuth } from "@/hooks";
import { useTheme } from "@/hooks/features/useTheme";
import { Button } from "@/components";
import { Avatar, AvatarFallback, AvatarImage } from "@/components";
import { Settings, Moon, Sun, Monitor } from "lucide-react";
import { Navbar, NavbarBrand, NavbarCollapse, NavbarLink, NavbarToggle } from "flowbite-react";

const NotificationBell = dynamic(
  () => import("@/components/organisms/common/notification-bell").then(mod => ({ default: mod.NotificationBell })),
  {
    ssr: false,
    loading: () => <div className="w-6 h-6 animate-pulse bg-gray-200 rounded-full" />
  }
);

export const AuthHeader = React.memo(() => {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const { theme, isDark, toggleTheme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  const getInitials = (name?: string) => {
    if (!name) return "U";
    return name.substring(0, 1).toUpperCase();
  };

  const getThemeIcon = () => {
    if (!mounted) return <Monitor className="w-5 h-5" />;
    if (theme === 'dark') return <Moon className="w-5 h-5" />;
    if (theme === 'light') return <Sun className="w-5 h-5" />;
    return <Monitor className="w-5 h-5" />;
  };

  return (
    <Navbar
      fluid
      className="border-b bg-white/80 backdrop-blur-sm sticky top-0 z-50"
      theme={{
        root: {
          base: "px-4 sm:px-6 lg:px-8 py-3 sm:py-4",
          rounded: {
            on: "rounded",
            off: ""
          },
          bordered: {
            on: "border",
            off: ""
          },
          inner: {
            base: "mx-auto flex flex-wrap items-center justify-between",
            fluid: {
              on: "",
              off: "container"
            }
          }
        },
        brand: {
          base: "flex items-center"
        },
        collapse: {
          base: "w-full md:block md:w-auto",
          list: "mt-4 flex flex-col p-4 md:mt-0 md:flex-row md:space-x-8 md:p-0 md:text-sm md:font-medium",
          hidden: {
            on: "hidden",
            off: ""
          }
        },
        link: {
          base: "block py-2 pr-4 pl-3 md:p-0",
          active: {
            on: "bg-brand-primary text-white dark:text-white md:bg-transparent md:text-brand-primary",
            off: "border-b border-gray-100 text-brand-muted hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white md:border-0 md:hover:bg-transparent md:hover:text-brand-primary"
          },
          disabled: {
            on: "text-gray-400 hover:cursor-not-allowed dark:text-gray-600",
            off: ""
          }
        },
        toggle: {
          base: "inline-flex items-center p-2 ml-3 text-sm text-gray-500 rounded-lg md:hidden hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-200 dark:text-gray-400 dark:hover:bg-gray-700 dark:focus:ring-gray-600",
          icon: "w-6 h-6"
        }
      }}
    >
      <NavbarBrand as={Link} href="/">
        <Image
          src="/log.png"
          alt="비밀로그"
          width={48}
          height={48}
          className="h-10 sm:h-12 w-auto object-contain mr-3"
          priority
          placeholder="blur"
          blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDgiIGhlaWdodD0iNDgiIHZpZXdCb3g9IjAgMCA0OCA0OCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjQ4IiBmaWxsPSIjRjNGNEY2Ii8+Cjwvc3ZnPgo="
        />
      </NavbarBrand>

      <div className="flex md:order-2 items-center gap-2 sm:gap-3">
        {/* 테마 토글 버튼 - 모든 사용자에게 표시 */}
        <button
          onClick={toggleTheme}
          className="p-2 text-brand-muted hover:text-brand-primary hover:bg-gray-100 rounded-lg transition-colors min-h-[44px] min-w-[44px] touch-manipulation"
          title={mounted ? `테마 변경 (현재: ${theme === 'dark' ? '다크' : theme === 'light' ? '라이트' : '시스템'})` : '테마 변경'}
        >
          {getThemeIcon()}
        </button>

        {isLoading ? (
          <div className="w-8 h-8 bg-gray-200 rounded-full animate-pulse" />
        ) : isAuthenticated && user ? (
          <>
            <NotificationBell />
            <div className="flex items-center gap-2 sm:gap-3">
              <Link
                href="/settings"
                className="hidden lg:flex p-2 text-brand-muted hover:text-brand-primary hover:bg-gray-100 rounded-lg transition-colors min-h-[44px] min-w-[44px] touch-manipulation"
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
              <span className="hidden sm:inline lg:hidden xl:inline font-semibold text-sm text-brand-primary max-w-24 lg:max-w-none truncate">
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

        <NavbarToggle className="md:hidden" />
      </div>

      <NavbarCollapse>
        <NavbarLink as={Link} href="/board" className="text-sm lg:text-base">
          게시판
        </NavbarLink>
        <NavbarLink as={Link} href="/visit" className="text-sm lg:text-base">
          다른 롤링페이퍼 방문
        </NavbarLink>
        {isAuthenticated && (
          <NavbarLink as={Link} href="/rolling-paper" className="text-sm lg:text-base">
            내 롤링페이퍼
          </NavbarLink>
        )}
        <NavbarLink as={Link} href="/suggest" className="text-sm lg:text-base">
          건의하기
        </NavbarLink>
        {isAuthenticated && (
          <NavbarLink as={Link} href="/mypage" className="text-sm lg:text-base">
            마이페이지
          </NavbarLink>
        )}
        {isAuthenticated && user?.role === "ADMIN" && (
          <NavbarLink as={Link} href="/admin" className="text-red-600 hover:text-red-700 transition-colors font-semibold text-sm lg:text-base">
            관리자
          </NavbarLink>
        )}
        {!isAuthenticated && (
          <NavbarLink as={Link} href="/login" className="text-sm lg:text-base">
            로그인
          </NavbarLink>
        )}
      </NavbarCollapse>
    </Navbar>
  );
});

AuthHeader.displayName = "AuthHeader";
