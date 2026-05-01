"use client";

import React, { useCallback, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Settings,
  LogOut,
  Shield,
  ScrollText,
  UserCircle,
  UserX,
  Mail
} from "lucide-react";
import {
  Dropdown,
  DropdownDivider,
  DropdownHeader,
  DropdownItem,
  Avatar
} from "flowbite-react";
import { ConfirmModal } from "@/components/molecules/modals/confirm-modal";
import type { Member } from "@/types/domains/user";

interface UserDropdownMenuProps {
  user: Member;
}

/**
 * DROPDOWN_THEME — Flowbite Dropdown 의 폐기 토큰을 paper/ink/postal-navy 로 일괄 치환.
 * 라운드 17 F-17-BUG-5. 모든 vanilla gray + brand-* 폐기 토큰 → paper 메타포 일관.
 *
 * z-50 → z-auth-header (드롭다운은 헤더와 같은 stacking, AuthHeader Navbar 위에 노출).
 */
const DROPDOWN_THEME = {
  arrowIcon: "ml-2 h-4 w-4",
  content: "py-1 focus:outline-none",
  floating: {
    animation: "transition-opacity",
    arrow: {
      base: "absolute z-10 h-2 w-2 rotate-45",
      style: {
        dark: "bg-paper-card dark:bg-paper-200",
        light: "bg-paper-card",
        auto: "bg-paper-card dark:bg-paper-200"
      },
      placement: "-4px"
    },
    base: "z-auth-header w-fit rounded-lg divide-y divide-postal-navy/15 dark:divide-postal-navy/40 shadow-lg focus:outline-none",
    content: "py-1 text-sm text-ink dark:text-ink-900",
    divider: "my-1 h-px bg-postal-navy/15 dark:bg-postal-navy/40",
    header: "block py-2 px-4 text-sm text-ink dark:text-ink-900",
    hidden: "invisible opacity-0",
    item: {
      container: "",
      base: "flex items-center justify-start py-2 px-4 text-sm text-ink cursor-pointer w-full hover:bg-paper-soft focus:bg-paper-soft dark:text-ink-900 dark:hover:bg-paper-300 focus:outline-none dark:focus:bg-paper-300",
      icon: "mr-2 h-4 w-4"
    },
    style: {
      dark: "bg-paper-card text-ink dark:bg-paper-200 dark:text-ink-900",
      light: "border border-postal-navy/15 bg-paper-card text-ink",
      auto: "border border-postal-navy/15 bg-paper-card text-ink dark:border-postal-navy/40 dark:bg-paper-200 dark:text-ink-900"
    },
    target: "w-fit"
  },
  inlineWrapper: "flex items-center"
} as const;

export const UserDropdownMenu = React.memo(({ user }: UserDropdownMenuProps) => {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  // B-303: 로그아웃 확인 모달 상태
  const [isLogoutConfirmOpen, setIsLogoutConfirmOpen] = useState(false);
  // B-303 / 코드 패턴: focus return — 트리거(아바타) 로 포커스 복원
  const triggerWrapperRef = useRef<HTMLDivElement | null>(null);

  const handleLogoutClick = useCallback(() => {
    setIsLogoutConfirmOpen(true);
  }, []);

  const performLogoutNavigation = useCallback(() => {
    const currentPath = pathname || "/";
    const queryString = searchParams?.toString();
    const redirectTarget = queryString ? `${currentPath}?${queryString}` : currentPath;
    const encodedRedirect = encodeURIComponent(redirectTarget);
    // ?confirmed=1 — /logout 페이지에 이미 사용자 동의 받았음을 알리는 쿼리스트링.
    router.push(`/logout?confirmed=1&redirect=${encodedRedirect}`);
  }, [router, pathname, searchParams]);

  const handleConfirmLogout = useCallback(() => {
    setIsLogoutConfirmOpen(false);
    performLogoutNavigation();
  }, [performLogoutNavigation]);

  const handleCloseLogoutConfirm = useCallback(() => {
    setIsLogoutConfirmOpen(false);
    // focus 복원 — 모달 닫힘 후 트리거(아바타) 로
    if (typeof window !== "undefined") {
      window.setTimeout(() => {
        const avatar = triggerWrapperRef.current?.querySelector<HTMLElement>(
          '[data-testid="header-user-avatar"]'
        );
        avatar?.focus?.();
      }, 0);
    }
  }, []);

  return (
    <div ref={triggerWrapperRef}>
      <Dropdown
        arrowIcon={false}
        inline
        label={
          <Avatar
            alt={user.memberName}
            img={user.thumbnailImage}
            rounded
            // 라운드 17 F-17-BUG-5: hover:ring-purple-200 → hover:ring-stamp-red/30 (paper 메타포 일관)
            className="hover:ring-2 hover:ring-stamp-red/30 transition-all cursor-pointer"
            data-testid="header-user-avatar"
          />
        }
        theme={DROPDOWN_THEME}
      >
        <DropdownHeader>
          <span className="block text-sm font-semibold text-ink dark:text-ink-900">{user.memberName}</span>
          <span className="block truncate text-sm text-ink-soft dark:text-ink-500">
            @{user.socialNickname}
          </span>
          {user.role === "ADMIN" && (
            // 관리자 배지 — 라운드 17 F-17-BUG-5: bg-red-100 text-red-800 → stamp-red 토큰
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-stamp-red/10 text-stamp-red mt-1 break-keep">
              관리자
            </span>
          )}
        </DropdownHeader>
        <DropdownItem
          as={Link}
          href="/rolling-paper"
          data-testid="user-menu-papers"
          role="menuitem"
        >
          {/* 라운드 17 F-17-BUG-5: stroke-slate-600 fill-slate-100 → ink-soft 토큰 */}
          <ScrollText className="mr-2 h-4 w-4 stroke-ink-soft fill-paper-100" aria-hidden="true" />
          내 롤링페이퍼
        </DropdownItem>
        <DropdownItem
          as={Link}
          href="/mypage"
          data-testid="user-menu-mypage"
          role="menuitem"
        >
          <UserCircle className="mr-2 h-4 w-4 stroke-ink-soft fill-paper-100" aria-hidden="true" />
          마이페이지
        </DropdownItem>
        <DropdownItem
          as={Link}
          href="/settings"
          data-testid="user-menu-settings"
          role="menuitem"
        >
          <Settings className="mr-2 h-4 w-4 stroke-ink-soft fill-paper-100" aria-hidden="true" />
          설정
        </DropdownItem>
        <DropdownItem
          as={Link}
          href="/blacklist"
          data-testid="user-menu-blacklist"
          role="menuitem"
        >
          <UserX className="mr-2 h-4 w-4 stroke-ink-soft fill-paper-100" aria-hidden="true" />
          블랙리스트
        </DropdownItem>
        {user.role === "ADMIN" && (
          <>
            <DropdownDivider />
            <DropdownItem
              as={Link}
              href="/admin"
              className="text-stamp-red"
              data-testid="user-menu-admin"
              role="menuitem"
            >
              {/* stroke-purple-600 fill-purple-100 → stamp-red (관리자/destructive 일관) */}
              <Shield className="mr-2 h-4 w-4 stroke-stamp-red fill-stamp-red/10" aria-hidden="true" />
              관리자 페이지
            </DropdownItem>
          </>
        )}
        <DropdownDivider />
        <DropdownItem
          onClick={handleLogoutClick}
          className="text-stamp-red"
          data-testid="user-menu-logout"
          role="menuitem"
        >
          {/* stroke-red-600 fill-red-100 → stamp-red 토큰 */}
          <LogOut className="mr-2 h-4 w-4 stroke-stamp-red fill-stamp-red/10" aria-hidden="true" />
          로그아웃
        </DropdownItem>
      </Dropdown>

      {/* B-303: 로그아웃 확인 모달 — 비가역 액션이므로 명시적 confirm. */}
      <ConfirmModal
        isOpen={isLogoutConfirmOpen}
        onClose={handleCloseLogoutConfirm}
        onConfirm={handleConfirmLogout}
        title="이번 편지 묶음을 잠시 닫을까요?"
        message="다음에 다시 들어오면 받은 편지가 그대로 기다리고 있어요."
        confirmText="로그아웃"
        cancelText="취소"
        confirmButtonVariant="destructive"
        icon={
          <Mail
            className="h-8 w-8 stroke-stamp-red fill-paper-soft"
            aria-hidden="true"
          />
        }
      />
    </div>
  );
});

UserDropdownMenu.displayName = "UserDropdownMenu";
