"use client";

import { useEffect, useState } from "react";
import useAuthStore from "@/util/authStore";

/**
 * 인증 상태를 확인하는 컴포넌트
 * 초기 앱 로드 시 한 번만 인증 상태를 확인합니다.
 */
const AuthCheck = ({ children }: { children: React.ReactNode }) => {
  const { checkAuth } = useAuthStore();
  const [isInitialized, setIsInitialized] = useState(false);

  // 컴포넌트 마운트 시 한 번만 인증 상태 확인
  useEffect(() => {
    const initialize = async () => {
      if (!isInitialized) {
        await checkAuth();
        setIsInitialized(true);
      }
    };

    initialize();
    // checkAuth와 isInitialized를 의존성으로 사용
  }, [checkAuth, isInitialized]);

  return <>{children}</>;
};

export default AuthCheck;
