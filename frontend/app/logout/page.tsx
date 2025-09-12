"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { AuthLoadingScreen } from "@/components/atoms/AuthLoadingScreen";

export default function LogoutPage() {
  const { logout } = useAuth();
  const router = useRouter();
  const isMounted = useRef(true);

  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    const handleLogout = async () => {
      try {
        // 서버 로그아웃
        await logout();
        
        // 홈으로 리다이렉트
        if (isMounted.current) {
          router.replace("/");
        }
      } catch (error) {
        if (process.env.NODE_ENV === 'development') {
          console.error("Logout failed:", error);
        }
        // 에러가 발생해도 강제 로그아웃
        await logout();
        if (isMounted.current) {
          router.replace("/");
        }
      }
    };

    handleLogout();
  }, [logout, router]);

  return (
    <AuthLoadingScreen 
      message="로그아웃 중..."
      subMessage="안전하게 로그아웃 처리 중입니다."
    />
  );
}
