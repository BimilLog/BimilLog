"use client";

import { useEffect, useState } from "react";
import { useKakaoCallback } from "@/hooks/features/auth";
import { AuthLoadingScreen } from "@/components";
import { isMobileOrTablet } from "@/lib/utils";

export default function AuthCallbackPage() {
  // 카카오 OAuth 콜백 처리 - 인증 코드를 받아 로그인 처리 수행
  // FCM 토큰 처리는 useKakaoCallback 내부에서 수행
  useKakaoCallback();

  const [subMessage, setSubMessage] = useState<string | undefined>(undefined);

  useEffect(() => {
    setSubMessage(isMobileOrTablet() ? "모바일 알림 설정 중..." : undefined);
  }, []);

  return (
    <>
      <AuthLoadingScreen
        message="로그인 처리 중..."
        // 모바일/태블릿인 경우 FCM 토큰 등록 안내 메시지 표시
        subMessage={subMessage}
      />
    </>
  );
}
