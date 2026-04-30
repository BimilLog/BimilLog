"use client";

import { Suspense } from "react";
import { useKakaoCallback } from "@/hooks/features/auth";
import { AuthLoadingScreen } from "@/components";

/**
 * 카카오 OAuth 콜백 본 화면.
 *
 * `useSearchParams()` 가 내부에서 호출되므로 Next.js 15 권고대로
 * `<Suspense>` boundary 로 감싸야 prerender 단계에서 dynamic CSR boundary 가
 * 정확히 잡혀 첫 페인트 깜박임/빌드 경고가 생기지 않는다 (B-401).
 */
function KakaoCallbackContent() {
  // 카카오 OAuth 콜백 처리 - 인증 코드를 받아 로그인 처리 수행
  // FCM 토큰은 로그인 완료 후 메인 페이지에서 별도로 등록
  const { loadingStep, isRecovering } = useKakaoCallback();

  return (
    <AuthLoadingScreen
      message={loadingStep}
      variant={isRecovering ? "recovery" : "primary"}
    />
  );
}

export default function AuthCallbackPage() {
  return (
    <div className="min-h-screen bg-paper-50 dark:bg-paper-50">
      <Suspense fallback={<AuthLoadingScreen message="카카오 인증 처리 중..." screen="suspense-fallback" />}>
        <KakaoCallbackContent />
      </Suspense>
    </div>
  );
}
