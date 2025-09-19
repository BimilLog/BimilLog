"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/auth.store";
import { useToastStore } from "@/stores/toast.store";

// Re-export hooks that were moved to separate files
export { usePasswordModal } from './usePasswordModal';
// Auth-related hooks are now in features/auth
export { useKakaoCallback, useAuthError, useSignupUuid } from '../features/auth';

// ===== MAIN USE AUTH HOOK =====
interface UseAuthOptions {
  skipRefresh?: boolean; // 초기 로드 시 refreshUser 호출 건너뛰기
}

export function useAuth(options: UseAuthOptions = {}) {
  const authStore = useAuthStore();
  const { showInfo } = useToastStore();
  const { skipRefresh = false } = options;

  // 초기 로드 시 사용자 정보 새로고침 (skipRefresh가 false일 때만)
  useEffect(() => {
    if (!skipRefresh) {
      authStore.refreshUser();
    } else {
      // skipRefresh가 true일 때 로딩 상태를 즉시 false로 설정
      authStore.setLoading(false);
    }
  }, [skipRefresh]);

  // needsRelogin 이벤트 리스너 - 토큰 만료나 인증 에러 시 전역에서 발생하는 커스텀 이벤트 처리
  useEffect(() => {
    const handleNeedsRelogin = (event: Event) => {
      const customEvent = event as CustomEvent;
      const { title, message } = customEvent.detail;

      // 사용자에게 재로그인이 필요함을 알림
      showInfo(title, message, 5000);

      // 인증 상태를 초기화하고 로그인 페이지로 리다이렉트
      authStore.handleNeedsRelogin(title, message);
    };

    // API 에러 핸들러에서 발생시키는 커스텀 이벤트를 전역으로 수신
    window.addEventListener("needsRelogin", handleNeedsRelogin);

    return () => {
      window.removeEventListener("needsRelogin", handleNeedsRelogin);
    };
  }, [showInfo, authStore.handleNeedsRelogin]);

  return {
    user: authStore.user,
    isLoading: authStore.isLoading,
    isAuthenticated: authStore.isAuthenticated,
    login: authStore.login,
    logout: authStore.logout,
    signUp: authStore.signUp,
    updateUserName: authStore.updateUserName,

    refreshUser: authStore.refreshUser,
  };
}