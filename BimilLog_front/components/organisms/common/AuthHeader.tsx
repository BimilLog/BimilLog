"use client";

import React from "react";
import Link from "next/link";
import dynamic from "next/dynamic";
import { useAuth } from "@/hooks";
import {
  Navbar,
  NavbarBrand,
  NavbarCollapse,
  NavbarLink,
  NavbarToggle,
  Spinner as FlowbiteSpinner
} from "flowbite-react";
import { ThemeToggleButton } from "./ThemeToggleButton";
import { UserDropdownMenu } from "./UserDropdownMenu";
import { KillingTimeDropdown } from "./KillingTimeDropdown";

const NotificationBell = dynamic(
  () => import("@/components/organisms/common/notification-bell").then(mod => ({ default: mod.NotificationBell })),
  {
    ssr: false,
    loading: () => <div className="w-6 h-6 animate-pulse bg-gray-200 rounded-full" />
  }
);

const NAVBAR_THEME = {
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
    base: "inline-flex items-center justify-center min-h-touch min-w-touch p-3 ml-3 text-sm text-gray-500 rounded-lg md:hidden hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-200 dark:text-gray-400 dark:hover:bg-gray-700 dark:focus:ring-gray-600",
    icon: "w-6 h-6"
  }
} as const;

interface AuthHeaderProps {
  // sticky 비활성화 플래그: 같은 페이지 내 다른 sticky 헤더가 있을 때 합산 높이를 줄이기 위해 사용
  disableSticky?: boolean;
}

export const AuthHeader = React.memo<AuthHeaderProps>(({ disableSticky = false }) => {
  const { user, isAuthenticated, isLoading } = useAuth();
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <Navbar
      data-toast-anchor
      fluid
      className={`${disableSticky ? "relative" : "sticky top-0"} z-50 border-b border-ink-soft dark:border-border bg-paper-soft/85 dark:bg-background/80 backdrop-blur-sm transition-colors duration-300`}
      theme={NAVBAR_THEME}
    >
      {/* 단순 워드마크 — 종이 톤 + display 폰트 + ink 단색.
          기존 핑크 박스/로고 이미지 제거로 좌상단 가독성 확보 */}
      <NavbarBrand as={Link} href="/" aria-label="비밀로그 홈">
        <span className="flex items-center gap-2 mr-3">
          <span
            aria-hidden="true"
            className="inline-block w-2.5 h-2.5 rounded-full bg-stamp-red shadow-[0_0_0_3px_rgba(199,62,62,0.18)]"
          />
          <span className="font-display text-xl sm:text-2xl font-bold tracking-tight text-ink dark:text-gray-100">
            비밀로그
          </span>
        </span>
      </NavbarBrand>

      <div className="flex items-center gap-2 sm:gap-3 md:ml-auto md:order-2">
        <ThemeToggleButton mounted={mounted} />

        {isLoading ? (
          <div className="w-8 h-8 flex items-center justify-center">
            <FlowbiteSpinner color="pink" size="sm" aria-label="Loading..." />
          </div>
        ) : isAuthenticated && user ? (
          <>
            <NotificationBell />
            <UserDropdownMenu user={user} />
          </>
        ) : null}

        <NavbarToggle
          className="md:hidden"
          aria-label="메뉴"
          data-testid="header-menu-toggle"
        />
      </div>

      <NavbarCollapse className="basis-full md:basis-auto md:order-1 md:flex md:items-center md:gap-6 md:mx-auto">
        <div className="md:relative">
          <KillingTimeDropdown />
        </div>
        <NavbarLink as={Link} href="/board" className="text-sm lg:text-base">
          게시판
        </NavbarLink>
        <NavbarLink as={Link} href="/visit" className="text-sm lg:text-base">
          롤링페이퍼 둘러보기
        </NavbarLink>
        <NavbarLink as={Link} href="/suggest" className="text-sm lg:text-base">
          건의하기
        </NavbarLink>
        {isAuthenticated && (
          <NavbarLink as={Link} href="/friends" className="text-sm lg:text-base">
            친구
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
