"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authCommand, type SocialProvider } from "@/lib/api";
import { useAuthStore } from "@/stores/auth.store";
import { logger } from "@/lib/utils/logger";
import { registerFcmTokenAction } from "@/lib/actions/notification";

/**
 * 소셜 OAuth callback 처리 통합 훅
 * 신규/기존 회원 모두 즉시 JWT 토큰이 발급되어 동일하게 처리됨
 *
 * @param provider - 소셜 로그인 제공자 (KAKAO, NAVER, GOOGLE)
 */
const providerDisplayName: Record<SocialProvider, string> = {
  KAKAO: "카카오",
  NAVER: "네이버",
  GOOGLE: "구글",
};

export const useSocialCallback = (provider: SocialProvider) => {
  const [isProcessing, setIsProcessing] = useState(true);
  const [loadingStep, setLoadingStep] = useState<string>(`${providerDisplayName[provider]} 인증 처리 중...`);
  const router = useRouter();
  const searchParams = useSearchParams();
  const setProvider = useAuthStore((state) => state.setProvider);

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");
      const state = searchParams.get("state");

      // 친구 동의 플로우인지 확인 (카카오 전용)
      const isFriendsConsentFlow = typeof window !== 'undefined'
        ? sessionStorage.getItem('friendsConsentFlow') === 'true'
        : false;

      if (error) {
        if (isFriendsConsentFlow && typeof window !== 'undefined') {
          sessionStorage.removeItem('friendsConsentFlow');
          sessionStorage.removeItem('returnUrl');
        }
        router.push(`/login?error=${encodeURIComponent(error)}`);
        return;
      }

      if (!code) {
        if (isFriendsConsentFlow && typeof window !== 'undefined') {
          sessionStorage.removeItem('friendsConsentFlow');
          sessionStorage.removeItem('returnUrl');
        }
        router.push("/login?error=no_code");
        return;
      }

      try {
        setLoadingStep(isFriendsConsentFlow ? "친구 목록 권한 업데이트 중..." : "사용자 정보 확인 중...");

        const savedFcmToken = typeof window !== "undefined" ? localStorage.getItem("fcm_token") : null;

        let response;
        if (provider === 'KAKAO') {
          response = await authCommand.kakaoLogin(code);
        } else if (provider === 'NAVER') {
          response = await authCommand.naverLogin(code);
        } else if (provider === 'GOOGLE') {
          response = await authCommand.googleLogin(code);
        } else {
          throw new Error(`Unsupported provider: ${provider}`);
        }

        if (response.success) {
          setProvider(provider);

          if (savedFcmToken) {
            try {
              const registerResult = await registerFcmTokenAction(savedFcmToken);
              if (!registerResult.success) {
                logger.warn("FCM 토큰 등록 실패:", registerResult.error);
              }
            } catch (registerError) {
              logger.warn("FCM 토큰 등록 중 오류:", registerError);
            }
          }
          setLoadingStep(isFriendsConsentFlow ? "권한 업데이트 완료!" : "로그인 완료!");

          // 친구 동의 플로우인 경우 (카카오 전용)
          if (isFriendsConsentFlow && typeof window !== 'undefined') {
            const returnUrl = sessionStorage.getItem('returnUrl') || '/';
            sessionStorage.removeItem('friendsConsentFlow');
            sessionStorage.removeItem('returnUrl');
            router.push(returnUrl);
            return;
          }

          // state 파라미터에 저장된 리다이렉트 URL 확인 후 이동
          let redirectUrl = '/';
          if (state) {
            try {
              const decodedState = decodeURIComponent(state);
              const stateData = JSON.parse(decodedState);
              redirectUrl = stateData.redirect || '/';
            } catch {
              redirectUrl = '/';
            }
          }

          router.push(redirectUrl);
        } else {
          if (isFriendsConsentFlow && typeof window !== 'undefined') {
            sessionStorage.removeItem('friendsConsentFlow');
            sessionStorage.removeItem('returnUrl');
          }
          router.push(`/login?error=${response.error || "login_failed"}`);
        }
      } catch (error) {
        logger.error("Callback processing error:", error);
        if (isFriendsConsentFlow && typeof window !== 'undefined') {
          sessionStorage.removeItem('friendsConsentFlow');
          sessionStorage.removeItem('returnUrl');
        }
        router.push("/login?error=callback_failed");
      } finally {
        setIsProcessing(false);
      }
    };

    processCallback();
  }, [searchParams, router, provider, setProvider]);

  return { isProcessing, loadingStep };
};
