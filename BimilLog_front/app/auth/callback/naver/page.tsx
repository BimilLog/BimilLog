"use client";

import { useSocialCallback } from "@/hooks";
import { AuthLoadingScreen } from "@/components";
import { Suspense } from "react";

/**
 * 네이버 OAuth Callback 페이지
 * 네이버 로그인 후 리다이렉트되는 페이지로, authorization code를 받아 백엔드로 전송
 */
function NaverCallbackContent() {
  const { loadingStep, isRecovering } = useSocialCallback('NAVER');
  return (
    <AuthLoadingScreen
      message={loadingStep}
      variant={isRecovering ? "recovery" : "primary"}
    />
  );
}

export default function NaverCallbackPage() {
  return (
    <div className="min-h-screen bg-paper-50 dark:bg-paper-50">
      <Suspense fallback={<AuthLoadingScreen message="네이버 인증 처리 중..." screen="suspense-fallback" />}>
        <NaverCallbackContent />
      </Suspense>
    </div>
  );
}
