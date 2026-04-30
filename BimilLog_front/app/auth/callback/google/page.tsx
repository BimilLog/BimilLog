"use client";

import { Suspense } from "react";
import { AuthLoadingScreen } from "@/components";
import { useSocialCallback } from "@/hooks";

function GoogleCallbackContent() {
  const { loadingStep, isRecovering } = useSocialCallback("GOOGLE");
  return (
    <AuthLoadingScreen
      message={loadingStep}
      variant={isRecovering ? "recovery" : "primary"}
    />
  );
}

export default function GoogleCallbackPage() {
  return (
    <Suspense fallback={<AuthLoadingScreen message="구글 인증 처리 중..." />}>
      <GoogleCallbackContent />
    </Suspense>
  );
}
