"use client";

import { useAuthGuard } from "@/hooks";
import { AuthLoadingScreen } from "@/components/atoms/feedback/auth-loading-screen";
import type { AuthLayoutProps } from "@/types/domains/auth";

/**
 * 인증 필요 라우트 그룹 레이아웃 (라운드 17 — F-17-BUG-1/3 정정)
 *
 * 변경:
 *   - useAuthGuard() 훅으로 redirect 쿼리 자동 부착 (F-17-BUG-1)
 *   - 그라데이션(pink/purple/indigo) 부활 → AuthLoadingScreen 통일 (F-17-BUG-3)
 *
 * 사용자가 /mypage 같은 보호 라우트에 비로그인 진입하면 /login?redirect=/mypage 로
 * 이동하고, 콜백 후 원위치 복귀가 보장된다.
 */
export default function AuthenticatedLayout({ children }: AuthLayoutProps) {
  const { shouldShowLoading, shouldShowGuardedContent } = useAuthGuard();

  if (shouldShowLoading) {
    return (
      <AuthLoadingScreen
        message="로그인 상태를 확인하는 중..."
        subMessage="잠시만 기다려 주세요"
        variant="primary"
      />
    );
  }

  if (!shouldShowGuardedContent) {
    return null;
  }

  return <>{children}</>;
}
