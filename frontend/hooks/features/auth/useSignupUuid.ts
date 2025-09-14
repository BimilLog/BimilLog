"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";

/**
 * 회원가입 UUID 검증 훅
 * 백엔드에서 회원가입 시점에 UUID를 검증하므로 프론트엔드는 UUID 추출만 수행
 */
export const useSignupUuid = () => {
  const [tempUuid, setTempUuid] = useState<string | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const searchParams = useSearchParams();

  useEffect(() => {
    const uuid = searchParams.get("uuid");
    const required = searchParams.get("required");

    // 회원가입이 필요한 페이지인데 UUID가 없는 경우
    if (required === "true" && !uuid) {
      setError("회원가입을 위해 카카오 로그인이 필요합니다.");
      setIsValidating(false);
      return;
    }

    // UUID가 있으면 저장 (백엔드에서 회원가입 시점에 검증)
    if (uuid) {
      setTempUuid(uuid);
    }

    setIsValidating(false);
  }, [searchParams]);

  return { tempUuid, isValidating, error };
};