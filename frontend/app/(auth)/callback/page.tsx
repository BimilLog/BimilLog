"use client";

import { useKakaoCallback } from "@/hooks/api";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/feedback/toast";
import { AuthLoadingScreen } from "@/components/atoms/AuthLoadingScreen";
import { isMobileOrTablet } from "@/lib/utils";

export default function AuthCallbackPage() {
  const { isProcessing } = useKakaoCallback();
  const { toasts, removeToast } = useToast();

  return (
    <>
      <AuthLoadingScreen 
        message="로그인 처리 중..."
        subMessage={isMobileOrTablet() ? "모바일 알림 설정 중..." : undefined}
      />
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </>
  );
}
