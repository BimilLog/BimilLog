"use client";

import { useSearchParams } from "next/navigation";

/**
 * 회원가입 필수 여부 확인 훅
 * UUID는 HttpOnly 쿠키로 전달되어 프론트엔드에서 접근 불가
 * required 파라미터로만 회원가입 필수 여부 확인
 */
export const useSignupRequired = () => {
  const searchParams = useSearchParams();
  const isRequired = searchParams.get("required") === "true";

  return { isRequired };
};