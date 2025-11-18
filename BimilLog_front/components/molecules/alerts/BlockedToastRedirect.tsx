"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useToast } from "@/hooks";

interface BlockedToastRedirectProps {
  message?: string;
  redirectTo?: string;
  fallbackText?: string;
}

/**
 * 블랙리스트/권한 차단 시 토스트를 띄우고 지정 경로로 이동하는 클라이언트 컴포넌트
 */
export function BlockedToastRedirect({
  message,
  redirectTo = "/",
  fallbackText = "이동 중입니다...",
}: BlockedToastRedirectProps) {
  const router = useRouter();
  const { showToast } = useToast();

  useEffect(() => {
    showToast({
      type: "error",
      message: message || "차단되거나 차단한 회원과는 상호작용할 수 없습니다.",
    });

    const timer = setTimeout(() => {
      router.replace(redirectTo);
    }, 200);

    return () => clearTimeout(timer);
  }, [message, redirectTo, router, showToast]);

  return (
    <div className="min-h-[50vh] flex items-center justify-center px-4">
      <p className="text-sm text-brand-secondary">{fallbackText}</p>
    </div>
  );
}
