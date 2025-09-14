"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";

/**
 * 인증 에러 처리 훅
 * URL 파라미터에서 에러 메시지를 추출하여 관리
 */
export const useAuthError = () => {
  const [authError, setAuthError] = useState<string | null>(null);
  const searchParams = useSearchParams();

  useEffect(() => {
    const error = searchParams.get("error");
    if (error) {
      setAuthError(error);
    }
  }, [searchParams]);

  const clearAuthError = () => {
    setAuthError(null);
  };

  return { authError, clearAuthError };
};