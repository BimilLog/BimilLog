"use client";

import React from "react";
import Link from "next/link";
import Image from "next/image";
import dynamic from "next/dynamic";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { useTheme } from "@/hooks/features/useTheme";
import {
  Settings,
  Moon,
  Sun,
  Monitor,
  LogOut,
  Shield,
  ScrollText,
  UserCircle
} from "lucide-react";
import {
  Navbar,
  NavbarBrand,
  NavbarCollapse,
  NavbarLink,
  NavbarToggle,
  Dropdown,
  DropdownDivider,
  DropdownHeader,
  DropdownItem,
  Avatar,
  Spinner as FlowbiteSpinner
} from "flowbite-react";

const NotificationBell = dynamic(
  () => import("@/components/organisms/common/notification-bell").then(mod => ({ default: mod.NotificationBell })),
  {
    ssr: false,
    loading: () => <div className="w-6 h-6 animate-pulse bg-gray-200 rounded-full" />
  }
);

export const AuthHeader = React.memo(() => {
  const router = useRouter();
  const { user, isAuthenticated, isLoading } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);


  const getThemeIcon = () => {
    if (!mounted) return <Monitor className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
    if (theme === 'dark') return <Moon className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
    if (theme === 'light') return <Sun className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
    return <Monitor className="w-5 h-5 stroke-slate-600 fill-slate-100" />;
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
          width={150}
          height={48}
          className="h-10 sm:h-12 w-auto object-contain mr-3"
          priority
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
          <div className="w-8 h-8 flex items-center justify-center">
            <FlowbiteSpinner color="pink" size="sm" aria-label="Loading..." />
          </div>
        ) : isAuthenticated && user ? (
          <>
            <NotificationBell />
            <Dropdown
              arrowIcon={false}
              inline
              label={
                <Avatar
                  alt={user.memberName}
                  img={user.thumbnailImage}
                  rounded
                  className="hover:ring-2 hover:ring-purple-200 transition-all cursor-pointer"
                />
              }
              theme={{
                arrowIcon: "ml-2 h-4 w-4",
                content: "py-1 focus:outline-none",
                floating: {
                  animation: "transition-opacity",
                  arrow: {
                    base: "absolute z-10 h-2 w-2 rotate-45",
                    style: {
                      dark: "bg-gray-900 dark:bg-gray-700",
                      light: "bg-white",
                      auto: "bg-white dark:bg-gray-700"
                    },
                    placement: "-4px"
                  },
                  base: "z-50 w-fit rounded-lg divide-y divide-gray-100 shadow-lg focus:outline-none",
                  content: "py-1 text-sm text-gray-700 dark:text-gray-200",
                  divider: "my-1 h-px bg-gray-100 dark:bg-gray-600",
                  header: "block py-2 px-4 text-sm text-gray-700 dark:text-gray-200",
                  hidden: "invisible opacity-0",
                  item: {
                    container: "",
                    base: "flex items-center justify-start py-2 px-4 text-sm text-gray-700 cursor-pointer w-full hover:bg-gray-100 focus:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-600 focus:outline-none dark:hover:text-white dark:focus:bg-gray-600 dark:focus:text-white",
                    icon: "mr-2 h-4 w-4"
                  },
                  style: {
                    dark: "bg-gray-900 text-white dark:bg-gray-700",
                    light: "border border-gray-200 bg-white text-gray-900",
                    auto: "border border-gray-200 bg-white text-gray-900 dark:border-none dark:bg-gray-700 dark:text-white"
                  },
                  target: "w-fit"
                },
                inlineWrapper: "flex items-center"
              }}
            >
              <DropdownHeader>
                <span className="block text-sm font-semibold">{user.memberName}</span>
                <span className="block truncate text-sm text-gray-500">
                  @{user.socialNickname}
                </span>
                {user.role === "ADMIN" && (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-800 mt-1">
                    관리자
                  </span>
                )}
              </DropdownHeader>
              <DropdownItem as={Link} href="/rolling-paper">
                <ScrollText className="mr-2 h-4 w-4 stroke-slate-600 fill-slate-100" />
                내 롤링페이퍼
              </DropdownItem>
              <DropdownItem as={Link} href="/mypage">
                <UserCircle className="mr-2 h-4 w-4 stroke-slate-600 fill-slate-100" />
                마이페이지
              </DropdownItem>
              <DropdownItem as={Link} href="/settings">
                <Settings className="mr-2 h-4 w-4 stroke-slate-600 fill-slate-100" />
                설정
              </DropdownItem>
              {user.role === "ADMIN" && (
                <>
                  <DropdownDivider />
                  <DropdownItem as={Link} href="/admin" className="text-red-600">
                    <Shield className="mr-2 h-4 w-4 stroke-purple-600 fill-purple-100" />
                    관리자 페이지
                  </DropdownItem>
                </>
              )}
              <DropdownDivider />
              <DropdownItem onClick={() => router.push('/logout')} className="text-red-600">
                <LogOut className="mr-2 h-4 w-4 stroke-red-600 fill-red-100" />
                로그아웃
              </DropdownItem>
            </Dropdown>
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
        <NavbarLink as={Link} href="/suggest" className="text-sm lg:text-base">
          건의하기
        </NavbarLink>
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
