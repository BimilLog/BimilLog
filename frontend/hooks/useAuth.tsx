"use client";

import type React from "react";

import { useState, useEffect, createContext, useContext, useMemo, useCallback } from "react";
import { authApi, userApi, sseManager, type User } from "@/lib/api";
import { useToastContext } from "@/hooks/useToast";
import type { AuthContextType, AuthProviderProps } from "@/types/auth";

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const toast = useToastContext();

  const isAuthenticated = useMemo(() => !!user, [user]);

  // 현재 사용자 정보 조회
  const refreshUser = useCallback(async () => {
    try {
      const response = await authApi.getCurrentUser();

      if (response.success && response.data) {
        setUser(response.data);
        
        // 기존회원(로그인) 또는 신규회원(회원가입 완료) 시 SSE 연결
        if (response.data.userName?.trim()) {
          if (process.env.NODE_ENV === 'development') {
            console.log(`사용자 인증 완료 (${response.data.userName}) - SSE 연결 시작`);
          }
          sseManager.connect();
        }
      } else {
        setUser(null);
        sseManager.disconnect();
        // needsRelogin인 경우 API 클라이언트에서 자동으로 이벤트가 발생함
      }
    } catch (error) {
      console.error("Failed to fetch user:", error);
      setUser(null);
      sseManager.disconnect();
    } finally {
      setIsLoading(false);
    }
  }, []);

  // needsRelogin 이벤트 리스너
  useEffect(() => {
    const handleNeedsRelogin = (event: Event) => {
      const customEvent = event as CustomEvent;
      const { title, message } = customEvent.detail;

      // SSE 연결 해제하고 사용자 상태 초기화
      sseManager.disconnect();
      setUser(null);

      // 토스트 알림 표시
      toast.showInfo(title, message, 5000);

      // 바로 로그인 페이지로 리다이렉트 (logout API 호출 불필요 - 이미 인증 실패 상태)
      window.location.href = "/login";
    };

    window.addEventListener("needsRelogin", handleNeedsRelogin);

    return () => {
      window.removeEventListener("needsRelogin", handleNeedsRelogin);
    };
  }, [toast]);

  // 카카오 로그인
  const login = useCallback((postAuthRedirectUrl?: string) => {
    const kakaoAuthUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
    const kakaoClientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
    const kakaoRedirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;
    const responseType = "code";

    let url = `${kakaoAuthUrl}?response_type=${responseType}&client_id=${kakaoClientId}&redirect_uri=${kakaoRedirectUri}`;

    if (postAuthRedirectUrl) {
      url += `&state=${encodeURIComponent(postAuthRedirectUrl)}`; // 최종 리다이렉트 URL을 state 파라미터로 전달
    }
    window.location.href = url;
  }, []);

  // 로그아웃
  const logout = useCallback(async (): Promise<void> => {
    try {
      if (process.env.NODE_ENV === 'development') {
        console.log("로그아웃 시작...");
      }
      
      const response = await authApi.logout();
      
      if (process.env.NODE_ENV === 'development') {
        console.log("로그아웃 API 응답:", response);
      }
    } catch (error) {
      console.error("Logout failed:", error);
    } finally {
      // 항상 SSE 연결 해제하고 상태 초기화
      sseManager.disconnect();
      setUser(null);
    }
  }, []);

  // 닉네임 변경
  const updateUserName = useCallback(async (userName: string): Promise<boolean> => {
    if (!user) {
      console.error("User not available for username update");
      return false;
    }

    try {
      const response = await userApi.updateUserName(userName);
      if (response.success) {
        await refreshUser(); // 사용자 정보 새로고침 시 SSE 연결도 함께 처리됨
        return true;
      }
      return false;
    } catch (error) {
      console.error("Update username failed:", error);
      return false;
    }
  }, [user, refreshUser]);

  // 회원가입
  const signUp = useCallback(async (userName: string, uuid: string): Promise<{ success: boolean; error?: string }> => {
    try {
      const response = await authApi.signUp(userName, uuid);
      if (response.success) {
        await refreshUser(); // 회원가입 성공 후 사용자 정보 갱신
        return { success: true };
      }
      return { success: false, error: response.error || "회원가입에 실패했습니다." };
    } catch (error) {
      console.error("SignUp failed:", error);
      return { success: false, error: "회원가입 중 오류가 발생했습니다." };
    }
  }, [refreshUser]);

  // 회원 탈퇴
  const deleteAccount = useCallback(async (): Promise<boolean> => {
    try {
      const response = await authApi.deleteAccount();
      if (response.success) {
        sseManager.disconnect();
        setUser(null);
        window.location.href = "/";
        return true;
      }
      return false;
    } catch (error) {
      console.error("Delete account failed:", error);
      return false;
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const value = useMemo<AuthContextType>(() => ({
    user,
    isLoading,
    isAuthenticated,
    login,
    logout,
    signUp,
    updateUserName,
    deleteAccount,
    refreshUser,
  }), [user, isLoading, isAuthenticated, login, logout, signUp, updateUserName, deleteAccount, refreshUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
