"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { AuthLoadingScreen } from "@/components";
import { logger } from '@/lib/utils/logger';

export default function LogoutPage() {
  const { logout } = useAuth({ skipRefresh: true });
  const router = useRouter();
  const searchParams = useSearchParams();
  const { showError } = useToast();
  const consentParam = searchParams?.get('consent');
  const rawRedirectParam = searchParams?.get('redirect');

  const sanitizeRedirect = (value: string | null): string | null => {
    if (!value) return null;
    try {
      const decoded = decodeURIComponent(value);
      if (decoded.startsWith('/') && !decoded.startsWith('//')) {
        return decoded;
      }
      return null;
    } catch {
      return null;
    }
  };

  const redirectTarget = sanitizeRedirect(rawRedirectParam);

  const logoutRef = useRef(logout);
  const routerRef = useRef(router);
  const showErrorRef = useRef(showError);
  const consentParamRef = useRef(consentParam);
  const redirectTargetRef = useRef<string | null>(redirectTarget);

  useEffect(() => {
    logoutRef.current = logout;
  }, [logout]);

  useEffect(() => {
    routerRef.current = router;
  }, [router]);

  useEffect(() => {
    showErrorRef.current = showError;
  }, [showError]);

  useEffect(() => {
    consentParamRef.current = consentParam;
  }, [consentParam]);

  useEffect(() => {
    redirectTargetRef.current = sanitizeRedirect(rawRedirectParam);
  }, [rawRedirectParam]);

  // 중복 실행 방지를 위한 플래그 (logout 로직이 여러 번 실행되는 것을 막음)
  const isProcessingRef = useRef(false);

  useEffect(() => {
    // 이미 로그아웃 처리 중인 경우 중복 실행 방지
    if (isProcessingRef.current) {
      return;
    }
    isProcessingRef.current = true;

    const performLogout = async () => {
      try {
        logger.log("로그아웃 API 호출 시작");
        // 서버에 로그아웃 요청 전송 (auth.store.ts에서 모든 정리 작업 수행)
        await logoutRef.current?.();
        logger.log("로그아웃 API 호출 완료");
      } catch (error) {
        logger.error("Logout API failed:", error);
        showErrorRef.current?.("로그아웃 중 오류가 발생했습니다. 홈페이지로 이동합니다.");
      } finally {
        logger.log("LogoutPage finally 블록 진입");

        // 카카오 친구 동의 플로우 확인
        const isConsentFlow = consentParamRef.current === 'true';
        logger.log("Consent flow:", isConsentFlow);

        if (isConsentFlow) {
          // sessionStorage에서 동의 URL 가져오기
          const consentUrl = sessionStorage.getItem('kakaoConsentUrl');
          if (consentUrl) {
            logger.log("카카오 동의 URL로 리다이렉트:", consentUrl);
            window.location.href = consentUrl;
            return;
          }
        }

        // 일반 로그아웃의 경우 홈페이지로 이동
        const redirectPath = redirectTargetRef.current || "/";
        logger.log("페이지 리다이렉트 시작:", redirectPath);

        // window.location.replace를 우선적으로 사용 (더 확실한 페이지 이동)
        try {
          logger.log("window.location.replace 실행");
          window.location.replace(redirectPath);
        } catch (error) {
          logger.error("window.location.replace 실패, router.replace 시도:", error);
          try {
            routerRef.current.replace(redirectPath);
          } catch (routerError) {
            logger.error("router.replace도 실패:", routerError);
          }
        }
      }
    };

    // 로그아웃이 5초 이상 걸릴 경우 강제로 이동 (fallback 처리)
    const timeoutId = setTimeout(() => {
      logger.warn("로그아웃 타임아웃 (5초) - 강제 리다이렉트 시작");

      const isConsentFlow = consentParamRef.current === 'true';
      logger.log("Timeout - Consent flow:", isConsentFlow);

      if (isConsentFlow) {
        const consentUrl = sessionStorage.getItem('kakaoConsentUrl');
        if (consentUrl) {
          logger.log("Timeout - 카카오 동의 URL로 강제 리다이렉트:", consentUrl);
          window.location.href = consentUrl;
          return;
        }
      }

      const redirectPath = redirectTargetRef.current || "/";
      logger.log("Timeout - 홈으로 강제 리다이렉트:", redirectPath);

      try {
        window.location.replace(redirectPath);
      } catch (error) {
        logger.error("Timeout - window.location.replace 실패:", error);
        try {
          routerRef.current.replace(redirectPath);
        } catch (routerError) {
          logger.error("Timeout - router.replace도 실패:", routerError);
        }
      }
    }, 5000);

    performLogout();

    // cleanup 함수: 타임아웃 정리 및 처리 플래그 초기화
    return () => {
      clearTimeout(timeoutId);
    };
  }, []);

  return (
    <AuthLoadingScreen 
      message="로그아웃 중..."
      subMessage="안전하게 로그아웃 처리 중입니다."
    />
  );
}
