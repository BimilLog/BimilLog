"use client";

import { useSocialCallback } from "@/hooks";
import { AuthLoadingScreen } from "@/components";
import { Suspense } from "react";

/**
 * 네이버 OAuth Callback 페이지
 * 네이버 로그인 후 리다이렉트되는 페이지로, authorization code를 받아 백엔드로 전송
 */
function NaverCallbackContent() {
  const { loadingStep } = useSocialCallback('NAVER');
  return <AuthLoadingScreen message={loadingStep} />;
}

export default function NaverCallbackPage() {
  return (
    <Suspense fallback={<AuthLoadingScreen message="네이버 인증 처리 중..." />}>
      <NaverCallbackContent />
    </Suspense>
  );
}
