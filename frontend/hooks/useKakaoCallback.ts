"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { useNotifications } from "@/hooks/useNotifications";
import { getFCMToken, isMobileOrTablet } from "@/lib/utils";
import { AuthResponse } from "@/types/domains/auth";
import { useToast } from "@/hooks/useToast";

interface KakaoCallbackState {
  isProcessing: boolean;
  error: string | null;
}

export function useKakaoCallback() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const { fetchNotifications } = useNotifications();
  const { showSuccess, showError } = useToast();
  
  const [state, setState] = useState<KakaoCallbackState>({
    isProcessing: false,
    error: null
  });
  
  const isProcessingRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (isProcessingRef.current) {
      return;
    }
    
    isProcessingRef.current = true;
    setState({ isProcessing: true, error: null });
    abortControllerRef.current = new AbortController();

    const processCallback = async () => {
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
        const loginResponse = await authApi.kakaoLogin(code, fcmToken || undefined);

        if (!loginResponse.success || !loginResponse.data) {
          handleError(loginResponse.error || "로그인 실패", loginResponse.error || "로그인 실패");
          return;
        }

        await handleLoginSuccess(loginResponse.data as AuthResponse, stateParam);
      } catch (error) {
        handleApiError(error);
      }
    };

    const tryGetFCMToken = async (): Promise<string | null> => {
      if (!isMobileOrTablet()) return null;
      
      try {
        return await getFCMToken();
      } catch (error) {
        if (process.env.NODE_ENV === 'development') {
          console.error("FCM 토큰 가져오기 중 오류:", error);
        }
        return null;
      }
    };

    const handleLoginSuccess = async (authResponse: AuthResponse, stateParam: string | null) => {
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
    };

    const handleNewUser = (uuid?: string) => {
      if (uuid) {
        sessionStorage.setItem("tempUserUuid", uuid);
      }
      router.push("/signup?required=true");
    };

    const handleExistingUser = async (stateParam: string | null) => {
      await refreshUser();
      await fetchNotifications();
      
      const finalRedirect = determineRedirectUrl(stateParam);
      
      if (isKakaoFriendConsent()) {
        showKakaoFriendConsentSuccess();
      }
      
      router.push(finalRedirect);
    };

    const determineRedirectUrl = (stateParam: string | null): string => {
      const returnUrl = sessionStorage.getItem("returnUrl");
      const kakaoConsentUrl = sessionStorage.getItem("kakaoConsentUrl");
      
      if (returnUrl && kakaoConsentUrl) {
        sessionStorage.removeItem("returnUrl");
        sessionStorage.removeItem("kakaoConsentUrl");
        return returnUrl;
      }
      
      if (stateParam) {
        return decodeURIComponent(stateParam);
      }
      
      return "/";
    };

    const isKakaoFriendConsent = (): boolean => {
      return !!(sessionStorage.getItem("returnUrl") && sessionStorage.getItem("kakaoConsentUrl"));
    };

    const showKakaoFriendConsentSuccess = () => {
      setTimeout(() => {
        showSuccess(
          "카카오 친구 목록 동의 완료",
          "이제 친구 목록을 확인할 수 있습니다."
        );
      }, 1000);
    };

    const handleError = (message: string, urlError: string) => {
      setState({ isProcessing: false, error: message });
      showError("로그인 오류", message);
      router.push(`/login?error=${encodeURIComponent(urlError)}`);
    };

    const handleApiError = (error: any) => {
      if (process.env.NODE_ENV === 'development') {
        console.error("API call failed during Kakao login:", error);
      }
      handleError("서버와 통신 중 오류가 발생했습니다.", "API 호출 실패");
    };

    processCallback();

    return () => {
      isProcessingRef.current = false;
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, [router, searchParams, refreshUser, fetchNotifications, showSuccess, showError]);

  return {
    isProcessing: state.isProcessing,
    error: state.error
  };
}