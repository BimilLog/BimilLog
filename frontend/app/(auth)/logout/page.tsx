"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { AuthLoadingScreen } from "@/components";
import { fcmManager } from "@/lib/auth/fcm";
import { logger } from '@/lib/utils/logger';

export default function LogoutPage() {
  const { logout } = useAuth();
  const router = useRouter();
  // 중복 실행 방지를 위한 플래그 (logout 로직이 여러 번 실행되는 것을 막음)
  const isProcessingRef = useRef(false);
  // 컴포넌트가 언마운트된 후 setState 호출을 방지하는 플래그
  const isMountedRef = useRef(true);

  // 컴포넌트 언마운트 시 상태 플래그 업데이트
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    // 이미 로그아웃 처리 중인 경우 중복 실행 방지
    if (isProcessingRef.current) {
      return;
    }
    isProcessingRef.current = true;

    const performLogout = async () => {
      try {
        // 서버에 로그아웃 요청 전송
        await logout();
      } catch (error) {
        if (process.env.NODE_ENV === 'development') {
          logger.error("Logout failed:", error);
        }
      } finally {
        // sessionManager.clear(); // 더 이상 필요 없음
        // FCM 토큰 캐시 정리
        fcmManager.clearCache();

        // 컴포넌트가 아직 마운트되어 있을 때만 라우팅 실행 (메모리 누수 방지)
        if (isMountedRef.current) {
          router.replace("/");
        }
      }
    };

    // 로그아웃이 5초 이상 걸릴 경우 강제로 홈페이지로 이동 (fallback 처리)
    const timeoutId = setTimeout(() => {
      if (isMountedRef.current) {
        router.replace("/");
      }
    }, 5000);

    performLogout();

    // cleanup 함수: 타임아웃 정리 및 처리 플래그 초기화
    return () => {
      clearTimeout(timeoutId);
      isProcessingRef.current = false;
    };
  }, [logout, router]);

  return (
    <AuthLoadingScreen 
      message="로그아웃 중..."
      subMessage="안전하게 로그아웃 처리 중입니다."
    />
  );
}
