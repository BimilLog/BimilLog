"use client";

import { useEffect } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { useAuth } from "./useAuth";

/**
 * useAuthGuard — 인증 가드 표준 훅 (라운드 17, F-17-BUG-1/13)
 *
 * 목적: 비로그인 → /login 리다이렉트 시 redirect 쿼리를 일관되게 부착.
 *
 * 변경 전:
 *   - (protected)/layout.tsx 는 router.push("/login") (redirect 누락)
 *   - HomeFooter.handleFriendClick 는 router.push("/login?redirect=/friends")
 *   - 산재한 router.push 가 정책 불일치 → 사용자가 의도한 페이지 복귀 못 함
 *
 * 변경 후:
 *   - 어디서나 useAuthGuard() 한 줄로 통일
 *   - 현재 pathname + search 를 redirect 로 자동 부착
 *   - explicit redirect 인자 도 허용 (HomeFooter 처럼 가상의 진입을 가드 할 때)
 *
 * @param explicitRedirect 명시적 redirect 경로 (없으면 현재 pathname + search)
 * @returns isAuthenticated, isLoading, user, shouldShowGuardedContent
 */
export function useAuthGuard(explicitRedirect?: string) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const fallbackPath = pathname || "/";
      const queryString = searchParams?.toString();
      const currentWithQuery = queryString
        ? `${fallbackPath}?${queryString}`
        : fallbackPath;
      const redirect = explicitRedirect || currentWithQuery;
      router.push(`/login?redirect=${encodeURIComponent(redirect)}`);
    }
  }, [isAuthenticated, isLoading, router, pathname, searchParams, explicitRedirect]);

  return {
    isAuthenticated,
    isLoading,
    user,
    shouldShowGuardedContent: !isLoading && isAuthenticated,
    shouldShowLoading: isLoading,
  };
}
