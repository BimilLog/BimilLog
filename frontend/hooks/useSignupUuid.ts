"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useToast } from "@/hooks/useToast";

export function useSignupUuid() {
  const [tempUuid, setTempUuid] = useState<string | null>(null);
  const [isValidating, setIsValidating] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();
  const { showError } = useToast();

  const validateUuid = useCallback((uuid: string): boolean => {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }, []);

  const redirectToLogin = useCallback((errorMessage: string) => {
    setError(errorMessage);
    showError("회원가입 오류", errorMessage);
    setTimeout(() => {
      router.push("/login");
    }, 2000);
  }, [router, showError]);

  useEffect(() => {
    const storedUuid = sessionStorage.getItem("tempUserUuid");
    
    if (!storedUuid || storedUuid.length === 0) {
      redirectToLogin("회원가입 정보가 없습니다. 다시 로그인해주세요.");
      return;
    }

    if (!validateUuid(storedUuid)) {
      console.error("Invalid UUID format:", storedUuid);
      redirectToLogin("잘못된 회원가입 정보입니다. 다시 로그인해주세요.");
      return;
    }

    setTempUuid(storedUuid);
    setIsValidating(false);
  }, [validateUuid, redirectToLogin]);

  const clearUuid = useCallback(() => {
    sessionStorage.removeItem("tempUserUuid");
    setTempUuid(null);
  }, []);

  return {
    tempUuid,
    isValidating,
    error,
    clearUuid,
    validateUuid,
  };
}