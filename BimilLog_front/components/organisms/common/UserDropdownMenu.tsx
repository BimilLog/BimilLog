"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Settings,
  LogOut,
  Shield,
  ScrollText,
  UserCircle,
  UserX
} from "lucide-react";
import {
  Dropdown,
  DropdownDivider,
  DropdownHeader,
  DropdownItem,
  Avatar
} from "flowbite-react";
import type { Member } from "@/types/domains/user";

interface UserDropdownMenuProps {
  user: Member;
}

const DROPDOWN_THEME = {
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
} as const;

export const UserDropdownMenu = React.memo(({ user }: UserDropdownMenuProps) => {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const handleLogout = () => {
    const currentPath = pathname || "/";
    const queryString = searchParams?.toString();
    const redirectTarget = queryString ? `${currentPath}?${queryString}` : currentPath;
    const encodedRedirect = encodeURIComponent(redirectTarget);
    router.push(`/logout?redirect=${encodedRedirect}`);
  };

  return (
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
      theme={DROPDOWN_THEME}
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
      <DropdownItem as={Link} href="/blacklist">
        <UserX className="mr-2 h-4 w-4 stroke-slate-600 fill-slate-100" />
        블랙리스트
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
      <DropdownItem
        onClick={handleLogout}
        className="text-red-600"
      >
        <LogOut className="mr-2 h-4 w-4 stroke-red-600 fill-red-100" />
        로그아웃
      </DropdownItem>
    </Dropdown>
  );
});

UserDropdownMenu.displayName = "UserDropdownMenu";
