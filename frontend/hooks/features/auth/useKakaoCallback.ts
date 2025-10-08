"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authCommand } from "@/lib/api";
import { logger } from "@/lib/utils/logger";

/**
 * Kakao OAuth callback 처리 훅
 * 카카오 로그인 후 리다이렉트된 인가 코드를 백엔드로 전송
 *
 * FCM 토큰은 로그인 성공 후 별도로 등록하도록 변경됨 (UX 개선)
 */
export const useKakaoCallback = () => {
  const [isProcessing, setIsProcessing] = useState(true);
  const [loadingStep, setLoadingStep] = useState<string>("카카오 인증 처리 중...");
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");

      // 카카오에서 에러가 발생한 경우 (사용자 취소 등)
      if (error) {
        router.push(`/login?error=${encodeURIComponent(error)}`);
        return;
      }

      // Authorization Code가 없는 경우
      if (!code) {
        router.push("/login?error=no_code");
        return;
      }

      try {
        setLoadingStep("사용자 정보 확인 중...");

        // localStorage에서 이전에 저장된 FCM 토큰 확인
        const savedFcmToken = typeof window !== "undefined" ? localStorage.getItem("fcm_token") : null;

        // Authorization Code를 백엔드로 전송하여 JWT 토큰 받기
        // 저장된 FCM 토큰이 있으면 함께 전달
        const response = await authCommand.kakaoLogin(code, savedFcmToken || undefined);

        if (response.success && response.data) {
          setLoadingStep("로그인 완료!");

          // 신규 사용자인 경우 회원가입 페이지로 이동
          // UUID는 HttpOnly 쿠키로 전달되어 프론트엔드에서 직접 접근 불가
          if (response.data === "NEW_USER") {
            router.push("/signup?required=true");
          } else {
            // 기존 사용자인 경우 메인 페이지로 이동
            router.push("/");
          }
        } else {
          router.push(`/login?error=${response.error || "login_failed"}`);
        }
      } catch (error) {
        logger.error("Callback processing error:", error);
        router.push("/login?error=callback_failed");
      } finally {
        setIsProcessing(false);
      }
    };

    processCallback();
  }, [searchParams, router]);

  return { isProcessing, loadingStep };
};