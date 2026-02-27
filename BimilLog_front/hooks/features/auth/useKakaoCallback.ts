"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authCommand } from "@/lib/api";
import { logger } from "@/lib/utils/logger";
import { useAuthStore } from "@/stores/auth.store";
import { registerFcmTokenAction } from "@/lib/actions/notification";

/**
 * Kakao OAuth callback 처리 훅
 * 카카오 로그인 후 리다이렉트된 인가 코드를 백엔드로 전송
 * 신규/기존 회원 모두 즉시 JWT 토큰이 발급되어 동일하게 처리됨
 */
export const useKakaoCallback = () => {
  const [isProcessing, setIsProcessing] = useState(true);
  const [loadingStep, setLoadingStep] = useState<string>("카카오 인증 처리 중...");
  const router = useRouter();
  const searchParams = useSearchParams();
  const setProvider = useAuthStore((state) => state.setProvider);

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");

      // 친구 동의 플로우인지 확인
      const isFriendsConsentFlow = typeof window !== 'undefined'
        ? sessionStorage.getItem('friendsConsentFlow') === 'true'
        : false;

      // 카카오에서 에러가 발생한 경우 (사용자 취소 등)
      if (error) {
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

        const savedFcmToken = typeof window !== "undefined" ? localStorage.getItem("fcm_token") : null;

        const response = await authCommand.kakaoLogin(code);

        if (response.success) {
          setProvider('KAKAO');

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

          // 친구 동의 플로우인 경우
          if (isFriendsConsentFlow && typeof window !== 'undefined') {
            const returnUrl = sessionStorage.getItem('returnUrl') || '/';
            sessionStorage.removeItem('friendsConsentFlow');
            sessionStorage.removeItem('returnUrl');
            router.push(returnUrl);
            return;
          }

          // 신규/기존 구분 없이 메인 페이지로 이동
          router.push("/");
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
  }, [searchParams, router]);

  return { isProcessing, loadingStep };
};
