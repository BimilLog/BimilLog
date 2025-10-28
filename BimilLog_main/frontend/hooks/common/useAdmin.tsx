"use client";

import { useAuth } from './useAuth';

/**
 * 관리자 권한 체크 훅
 *
 * 사용자의 관리자 권한 여부를 확인합니다.
 *
 * @returns isAdmin - 관리자 여부
 * @returns user - 현재 사용자 정보
 * @returns isLoading - 인증 정보 로딩 중 여부
 *
 * @example
 * ```tsx
 * const { isAdmin } = useAdmin();
 *
 * if (isAdmin) {
 *   return <AdminButton />;
 * }
 * ```
 *
 * @author Jaeik
 * @version 2.0.0
 */
export function useAdmin() {
  const { user, isLoading } = useAuth({ skipRefresh: true });

  return {
    isAdmin: user?.role === 'ADMIN',
    user,
    isLoading,
  };
}
