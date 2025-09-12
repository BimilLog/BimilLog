"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { useToast } from "@/hooks/useToast";
import type { AuthError } from "@/types/auth";
import { AUTH_ERROR_MESSAGES } from "@/types/auth";

export function useAuthError() {
  const router = useRouter();
  const { showError, showWarning, showInfo } = useToast();

  const handleAuthError = useCallback((error: AuthError, redirect?: string) => {
    const message = AUTH_ERROR_MESSAGES[error.code] || error.message;
    
    switch (error.code) {
      case "SESSION_EXPIRED":
        showWarning("세션 만료", message);
        if (redirect) {
          router.push(redirect);
        }
        break;
        
      case "NETWORK_ERROR":
        showError("네트워크 오류", message);
        break;
        
      case "KAKAO_ERROR":
        showError("카카오 로그인 오류", message);
        if (redirect) {
          router.push(`${redirect}?error=${encodeURIComponent(message)}`);
        }
        break;
        
      default:
        showError("인증 오류", message);
        if (redirect) {
          router.push(redirect);
        }
    }
    
    if (process.env.NODE_ENV === 'development' && error.details) {
      console.error("Auth Error Details:", error.details);
    }
  }, [router, showError, showWarning]);

  const parseApiError = useCallback((error: unknown): AuthError => {
    const apiError = error as { response?: { status?: number; data?: { message?: string } }; code?: string; message?: string };
    
    if (apiError?.response?.status === 401) {
      return {
        code: "SESSION_EXPIRED",
        message: apiError.response.data?.message || "세션이 만료되었습니다",
        details: error
      };
    }
    
    if (apiError?.code === "NETWORK_ERROR" || !navigator.onLine) {
      return {
        code: "NETWORK_ERROR",
        message: "네트워크 연결을 확인해주세요",
        details: error
      };
    }
    
    if (apiError?.message?.includes("카카오")) {
      return {
        code: "KAKAO_ERROR",
        message: apiError.message,
        details: error
      };
    }
    
    if (apiError?.response?.status && apiError.response.status >= 500) {
      return {
        code: "SERVER_ERROR",
        message: "서버 오류가 발생했습니다",
        details: error
      };
    }
    
    return {
      code: "UNKNOWN",
      message: apiError?.message || "알 수 없는 오류가 발생했습니다",
      details: error
    };
  }, []);

  const clearAuthError = useCallback(() => {
    const searchParams = new URLSearchParams(window.location.search);
    searchParams.delete("error");
    const newUrl = searchParams.toString() 
      ? `${window.location.pathname}?${searchParams.toString()}`
      : window.location.pathname;
    window.history.replaceState({}, "", newUrl);
  }, []);

  return {
    handleAuthError,
    parseApiError,
    clearAuthError
  };
}