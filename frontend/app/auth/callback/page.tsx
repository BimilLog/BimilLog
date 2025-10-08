"use client";

import { useKakaoCallback } from "@/hooks/features/auth";
import { AuthLoadingScreen } from "@/components";

export default function AuthCallbackPage() {
  // 카카오 OAuth 콜백 처리 - 인증 코드를 받아 로그인 처리 수행
  // FCM 토큰은 로그인 완료 후 메인 페이지에서 별도로 등록
  const { loadingStep } = useKakaoCallback();

  return (
    <>
      <AuthLoadingScreen
        message={loadingStep}
      />
    </>
  );
}
