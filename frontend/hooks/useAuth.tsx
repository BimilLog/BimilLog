"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/auth.store";
import { useToastStore } from "@/stores/toast.store";

export function useAuth() {
  const authStore = useAuthStore();
  const { showInfo } = useToastStore();

  // 초기 로드 시 사용자 정보 새로고침
  useEffect(() => {
    authStore.refreshUser();
  }, []);

  // needsRelogin 이벤트 리스너
  useEffect(() => {
    const handleNeedsRelogin = (event: Event) => {
      const customEvent = event as CustomEvent;
      const { title, message } = customEvent.detail;

      // 토스트 알림 표시
      showInfo(title, message, 5000);
      
      // 로그인 필요 처리
      authStore.handleNeedsRelogin(title, message);
    };

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
    deleteAccount: authStore.deleteAccount,
    refreshUser: authStore.refreshUser,
  };
}

// Legacy AuthProvider for backward compatibility
import type React from "react";
import type { AuthProviderProps } from "@/types/auth";

export function AuthProvider({ children }: AuthProviderProps) {
  // Zustand store는 전역 상태이므로 Provider가 필요없지만
  // 기존 코드 호환성을 위해 유지
  return <>{children}</>;
}