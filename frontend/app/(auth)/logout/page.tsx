"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks";
import { AuthLoadingScreen } from "@/components";
import { fcmManager } from "@/lib/auth/fcm";

export default function LogoutPage() {
  const { logout } = useAuth();
  const router = useRouter();
  const isProcessingRef = useRef(false);
  const isMountedRef = useRef(true);

  useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (isProcessingRef.current) {
      return;
    }
    isProcessingRef.current = true;

    const performLogout = async () => {
      try {
        await logout();
      } catch (error) {
        if (process.env.NODE_ENV === 'development') {
          console.error("Logout failed:", error);
        }
      } finally {
        // sessionManager.clear(); // 더 이상 필요 없음
        fcmManager.clearCache();
        
        if (isMountedRef.current) {
          router.replace("/");
        }
      }
    };

    const timeoutId = setTimeout(() => {
      if (isMountedRef.current) {
        router.replace("/");
      }
    }, 5000);

    performLogout();

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
