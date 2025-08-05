"use client";

import type React from "react";

import { useState, useEffect, createContext, useContext } from "react";
import { authApi, userApi, type User } from "@/lib/api";
import { useToastContext } from "@/hooks/useToast";

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (postAuthRedirectUrl?: string) => void;
  logout: () => Promise<void>;
  updateUserName: (userName: string) => Promise<boolean>;
  deleteAccount: () => Promise<boolean>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const toast = useToastContext();

  const isAuthenticated = !!user;

  // 현재 사용자 정보 조회
  const refreshUser = async () => {
    try {
      const response = await authApi.getCurrentUser();

      if (response.success && response.data) {
        setUser(response.data);
      } else {
        setUser(null);
        // needsRelogin인 경우 API 클라이언트에서 자동으로 이벤트가 발생함
      }
    } catch (error) {
      console.error("Failed to fetch user:", error);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  };

  // needsRelogin 이벤트 리스너
  useEffect(() => {
    const handleNeedsRelogin = async (event: Event) => {
      const customEvent = event as CustomEvent;
      const { title, message } = customEvent.detail;

      // 사용자 상태 초기화
      setUser(null);

      // 토스트 알림 표시
      toast.showInfo(title, message, 5000);

      // 바로 로그아웃 API 호출
      try {
        await authApi.logout();
      } catch (error) {
        console.error("Logout API failed:", error);
      }

      // 바로 로그인 페이지로 리다이렉트
      window.location.href = "/login";
    };

    window.addEventListener("needsRelogin", handleNeedsRelogin);

    return () => {
      window.removeEventListener("needsRelogin", handleNeedsRelogin);
    };
  }, [toast]);

  // 카카오 로그인
  const login = (postAuthRedirectUrl?: string) => {
    const kakaoAuthUrl = process.env.NEXT_PUBLIC_KAKAO_AUTH_URL;
    const kakaoClientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
    const kakaoRedirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;
    const responseType = "code";

    let url = `${kakaoAuthUrl}?response_type=${responseType}&client_id=${kakaoClientId}&redirect_uri=${kakaoRedirectUri}`;

    if (postAuthRedirectUrl) {
      url += `&state=${encodeURIComponent(postAuthRedirectUrl)}`; // 최종 리다이렉트 URL을 state 파라미터로 전달
    }
    window.location.href = url;
  };

  // 로그아웃
  const logout = async (): Promise<void> => {
    try {
          if (process.env.NODE_ENV === 'development') {
      console.log("로그아웃 시작...");
    }
    const response = await authApi.logout();
    if (process.env.NODE_ENV === 'development') {
      console.log("로그아웃 API 응답:", response);
    }
      setUser(null);
      window.location.href = "/";
    } catch (error) {
      console.error("Logout failed:", error);
      // 에러가 발생해도 클라이언트 상태는 초기화
      setUser(null);
      window.location.href = "/";
    }
  };

  // 닉네임 변경
  const updateUserName = async (userName: string): Promise<boolean> => {
    if (!user) {
      console.error("User not available for username update");
      return false;
    }

    try {
      const response = await userApi.updateUserName(user.userId, userName);
      if (response.success) {
        await refreshUser();
        return true;
      }
      return false;
    } catch (error) {
      console.error("Update username failed:", error);
      return false;
    }
  };

  // 회원 탈퇴
  const deleteAccount = async (): Promise<boolean> => {
    try {
      const response = await authApi.deleteAccount();
      if (response.success) {
        setUser(null);
        window.location.href = "/";
        return true;
      }
      return false;
    } catch (error) {
      console.error("Delete account failed:", error);
      return false;
    }
  };

  useEffect(() => {
    refreshUser();
  }, []);

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated,
    login,
    logout,
    updateUserName,
    deleteAccount,
    refreshUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
