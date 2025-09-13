"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authQuery } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { useNotifications } from "@/hooks/useNotifications";
import { getFCMToken, isMobileOrTablet } from "@/lib/utils";
import { useToast } from "@/hooks/useToast";
import { useSession } from "@/hooks/useSession";
import type { AuthResponse, KakaoCallbackState } from "@/types/auth";

export function useKakaoCallback() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const { fetchNotifications } = useNotifications();
  const { showSuccess, showError } = useToast();
  const session = useSession();
  
  const [state, setState] = useState<KakaoCallbackState>({
    isProcessing: false,
    error: null
  });
  
  const isProcessingRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  const tryGetFCMToken = useCallback(async (): Promise<string | null> => {
    if (!isMobileOrTablet()) return null;
    
    try {
      return await getFCMToken();
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error("FCM 토큰 가져오기 중 오류:", error);
      }
      return null;
    }
  }, []);

  const handleError = useCallback((message: string, urlError: string) => {
    setState({ isProcessing: false, error: message });
    showError("로그인 오류", message);
    router.push(`/login?error=${encodeURIComponent(urlError)}`);
  }, [showError, router]);

  const handleApiError = useCallback((error: unknown) => {
    if (process.env.NODE_ENV === 'development') {
      console.error("API call failed during Kakao login:", error);
    }
    handleError("서버와 통신 중 오류가 발생했습니다.", "API 호출 실패");
  }, [handleError]);

  const handleNewUser = useCallback((uuid?: string) => {
    if (uuid) {
      session.setTempUserUuid(uuid);
    }
    router.push("/signup?required=true");
  }, [session, router]);

  const determineRedirectUrl = useCallback((stateParam: string | null): string => {
    if (session.isKakaoFriendConsentFlow()) {
      return session.handleKakaoFriendConsentComplete();
    }
    
    if (stateParam) {
      return decodeURIComponent(stateParam);
    }
    
    return "/";
  }, [session]);

  const showKakaoFriendConsentSuccess = useCallback(() => {
    setTimeout(() => {
      showSuccess(
        "카카오 친구 목록 동의 완료",
        "이제 친구 목록을 확인할 수 있습니다."
      );
    }, 1000);
  }, [showSuccess]);

  const handleExistingUser = useCallback(async (stateParam: string | null) => {
    await refreshUser();
    await fetchNotifications();
    
    const finalRedirect = determineRedirectUrl(stateParam);
    
    if (session.isKakaoFriendConsentFlow()) {
      showKakaoFriendConsentSuccess();
    }
    
    router.push(finalRedirect);
  }, [refreshUser, fetchNotifications, determineRedirectUrl, session, showKakaoFriendConsentSuccess, router]);

  const handleLoginSuccess = useCallback(async (authResponse: AuthResponse, stateParam: string | null) => {
    switch (authResponse.status) {
      case "NEW_USER":
        handleNewUser(authResponse.uuid);
        break;
        
      case "EXISTING_USER":
        await handleExistingUser(stateParam);
        break;
        
      default:
        handleError("예상치 못한 로그인 응답이 발생했습니다.", "예상치 못한 로그인 응답");
    }
  }, [handleNewUser, handleExistingUser, handleError]);

  const processCallback = useCallback(async () => {
    const code = searchParams.get("code");
    const error = searchParams.get("error");
    const stateParam = searchParams.get("state");

    if (error) {
      handleError(`카카오 인증 중 오류가 발생했습니다: ${error}`, error);
      return;
    }

    if (!code) {
      handleError("카카오 로그인이 취소되었습니다.", "카카오 로그인 취소 또는 오류");
      return;
    }

    try {
      const fcmToken = await tryGetFCMToken();
      const loginResponse = await authQuery.kakaoLogin(code, fcmToken || undefined);

      if (!loginResponse.success || !loginResponse.data) {
        handleError(loginResponse.error || "로그인 실패", loginResponse.error || "로그인 실패");
        return;
      }

      await handleLoginSuccess(loginResponse.data as AuthResponse, stateParam);
    } catch (error) {
      handleApiError(error);
    }
  }, [searchParams, handleError, tryGetFCMToken, handleLoginSuccess, handleApiError]);

  useEffect(() => {
    if (isProcessingRef.current) {
      return;
    }
    
    isProcessingRef.current = true;
    setState({ isProcessing: true, error: null });
    abortControllerRef.current = new AbortController();

    processCallback();

    return () => {
      isProcessingRef.current = false;
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, [processCallback]);

  return {
    isProcessing: state.isProcessing,
    error: state.error
  };
}