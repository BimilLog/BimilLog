"use client";

import { Suspense } from "react";
import { AuthLoadingScreen } from "@/components";
import { useSocialCallback } from "@/hooks";

function GoogleCallbackContent() {
  const { loadingStep } = useSocialCallback("GOOGLE");
  return <AuthLoadingScreen message={loadingStep} />;
}

export default function GoogleCallbackPage() {
  return (
    <Suspense fallback={<AuthLoadingScreen message="구글 인증 처리 중..." />}>
      <GoogleCallbackContent />
    </Suspense>
  );
}
