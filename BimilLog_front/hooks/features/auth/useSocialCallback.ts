"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authCommand, type SocialProvider } from "@/lib/api";
import { useAuthStore } from "@/stores/auth.store";
import { logger } from "@/lib/utils/logger";
import { registerFcmTokenAction } from "@/lib/actions/notification";

/**
 * 소셜 OAuth callback 처리 통합 훅
 * 카카오, 네이버 등 여러 소셜 로그인의 콜백을 처리
 *
 * @param provider - 소셜 로그인 제공자 (KAKAO 또는 NAVER)
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

      // 소셜 로그인에서 에러가 발생한 경우 (사용자 취소 등)
      if (error) {
        // 친구 동의 플로우 중이었다면 sessionStorage 정리
        if (isFriendsConsentFlow && typeof window !== 'undefined') {
          sessionStorage.removeItem('friendsConsentFlow');
          sessionStorage.removeItem('returnUrl');
        }
        router.push(`/login?error=${encodeURIComponent(error)}`);
        return;
      }

      // Authorization Code가 없는 경우
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

        // localStorage에서 이전에 저장된 FCM 토큰 확인
        const savedFcmToken = typeof window !== "undefined" ? localStorage.getItem("fcm_token") : null;

        // Authorization Code를 백엔드로 전송하여 JWT 토큰 받기
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

        if (response.success && response.data) {
          // 로그인 성공 시 provider 저장
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

            // sessionStorage 정리
            sessionStorage.removeItem('friendsConsentFlow');
            sessionStorage.removeItem('returnUrl');

            // 원래 페이지로 복귀
            router.push(returnUrl);
            return;
          }

          // 일반 로그인 플로우
          // 신규 사용자인 경우 회원가입 페이지로 이동
          // UUID는 HttpOnly 쿠키로 전달되어 프론트엔드에서 직접 접근 불가
          if (response.data === "NEW_USER") {
            router.push("/signup?required=true");
          } else {
            // state 파라미터에 저장된 리다이렉트 URL 확인
            let redirectUrl = '/';
            if (state) {
              try {
                const decodedState = decodeURIComponent(state);
                const stateData = JSON.parse(decodedState);
                redirectUrl = stateData.redirect || '/';
              } catch {
                // JSON 파싱 실패 시 state를 그대로 URL로 사용
                redirectUrl = '/';
              }
            }

            // 기존 사용자인 경우 지정된 페이지 또는 메인 페이지로 이동
            router.push(redirectUrl);
          }
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
