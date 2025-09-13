"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiClient } from "@/lib/api";

/**
 * Kakao OAuth callback 처리 훅
 */
export const useKakaoCallback = () => {
  const [isProcessing, setIsProcessing] = useState(true);
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const processCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");

      if (error) {
        router.push(`/login?error=${encodeURIComponent(error)}`);
        return;
      }

      if (!code) {
        router.push("/login?error=no_code");
        return;
      }

      try {
        const response = await apiClient.post("/auth/callback", { code });

        if (response.success) {
          const data = response.data as any;
          if (data?.needsSignup) {
            router.push(`/signup?uuid=${data.tempUuid}`);
          } else {
            router.push("/");
          }
        } else {
          router.push(`/login?error=${response.error || "login_failed"}`);
        }
      } catch (error) {
        console.error("Callback processing error:", error);
        router.push("/login?error=callback_failed");
      } finally {
        setIsProcessing(false);
      }
    };

    processCallback();
  }, [searchParams, router]);

  return { isProcessing };
};

/**
 * 인증 에러 처리 훅
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

/**
 * 회원가입 UUID 검증 훅
 */
export const useSignupUuid = () => {
  const [tempUuid, setTempUuid] = useState<string | null>(null);
  const [isValidating, setIsValidating] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const searchParams = useSearchParams();
  const router = useRouter();

  useEffect(() => {
    const validateUuid = async () => {
      const uuid = searchParams.get("uuid");

      if (!uuid) {
        setError("no_uuid");
        router.push("/login");
        return;
      }

      try {
        // UUID 유효성 검증 (실제 구현 시 백엔드 API 호출)
        const response = await apiClient.get(`/auth/validate-uuid?uuid=${uuid}`);
        const data = response.data as any;

        if (response.success && data?.valid) {
          setTempUuid(uuid);
        } else {
          setError("invalid_uuid");
          router.push("/login?error=invalid_signup_session");
        }
      } catch (error) {
        console.error("UUID validation error:", error);
        setError("validation_failed");
        router.push("/login?error=signup_validation_failed");
      } finally {
        setIsValidating(false);
      }
    };

    validateUuid();
  }, [searchParams, router]);

  return { tempUuid, isValidating, error };
};