"use client";

import { useAuth } from "@/hooks";

/**
 * 관리자 권한 체크 훅.
 *
 * 라운드 16 F-16-001: 라운드 13 P2 토스트(F-13-BUG-1)는 AdminClient 의 useEffect 에서
 * 단일 SSOT 로 처리한다. 여기서는 인증/권한 상태만 노출하고 redirect/router.push 는
 * 호출하지 않는다 (race condition 방지 — useAdminAuth 가 AdminClient 보다 먼저 push 하면
 * 토스트가 누락되었음).
 */
export function useAdminAuth() {
  const { user, isAuthenticated, isLoading } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  return {
    user,
    isAdmin,
    isAuthenticated,
    isLoading,
  };
}
