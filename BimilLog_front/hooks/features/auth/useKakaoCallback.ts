"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authCommand, authQuery } from "@/lib/api";
import { logger } from "@/lib/utils/logger";
import { useAuthStore } from "@/stores/auth.store";
import { registerFcmTokenAction } from "@/lib/actions/notification";
import { markPendingWelcome } from "./useWelcomeOnboarding";
import { rememberLastUsedProvider } from "./lastUsedProvider";

/**
 * Kakao OAuth callback 처리 훅
 * 카카오 로그인 후 리다이렉트된 인가 코드를 백엔드로 전송
 * 신규/기존 회원 모두 즉시 JWT 토큰이 발급되어 동일하게 처리됨
 *
 * B-307: useEffect 의존성 변경(searchParams, router) 시에도 단일 실행 보장.
 */
export const useKakaoCallback = () => {
  const [isProcessing, setIsProcessing] = useState(true);
  const [loadingStep, setLoadingStep] = useState<string>("카카오 인증 처리 중...");
  // 회복 흐름인지 — UI 가 spinner 톤을 차분(navy)하게 다르게 처리할 때 사용
  const [isRecovering, setIsRecovering] = useState(false);
  const router = useRouter();
  const searchParams = useSearchParams();
  const setProvider = useAuthStore((state) => state.setProvider);

  // B-307: 단일 실행 가드 ref
  const hasProcessedRef = useRef(false);

  useEffect(() => {
    if (hasProcessedRef.current) return;
    hasProcessedRef.current = true;

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
      // F-203: 새로고침으로 code 가 사라진 경우, 이미 로그인된 상태인지 한 번 확인 후 회복
      if (!code) {
        if (isFriendsConsentFlow && typeof window !== 'undefined') {
          sessionStorage.removeItem('friendsConsentFlow');
          sessionStorage.removeItem('returnUrl');
        }

        try {
          setIsRecovering(true);
          setLoadingStep("이미 처리된 인증을 확인하는 중...");
          const recovery = await authQuery.getCurrentUser();
          if (recovery.success && recovery.data) {
            // 이미 토큰이 발급되어 인증된 상태 → 홈으로 정중하게 회복
            setProvider('KAKAO');
            rememberLastUsedProvider('KAKAO');
            setLoadingStep("로그인 완료!");
            router.push("/?recovered=1");
            return;
          }
        } catch {
          /* fall-through to login */
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
          rememberLastUsedProvider('KAKAO');

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

          // 환영 토스트 트리거 마커 (신규/기존은 클라이언트 휴리스틱으로 판별)
          markPendingWelcome('KAKAO');

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
  }, [searchParams, router, setProvider]);

  return { isProcessing, loadingStep, isRecovering };
};
