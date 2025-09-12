"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { useNotifications } from "@/hooks/useNotifications";
import { getFCMToken, isMobileOrTablet } from "@/lib/utils";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/toast";
import { AuthLoadingScreen } from "@/components/atoms/AuthLoadingScreen";

export default function AuthCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const { fetchNotifications } = useNotifications();
  const { showSuccess, showError, toasts, removeToast } = useToast();
  const isProcessingRef = useRef(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (isProcessingRef.current) {
      return;
    }
    isProcessingRef.current = true;
    abortControllerRef.current = new AbortController();

    const handleCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");
      const state = searchParams.get("state");

      if (error) {
        showError("로그인 오류", `카카오 인증 중 오류가 발생했습니다: ${error}`);
        router.push("/login?error=" + encodeURIComponent(error));
        return;
      }

      if (code) {
        try {
          // FCM 토큰 가져오기 (모바일/태블릿에서만)
          let fcmToken: string | null = null;

          if (isMobileOrTablet()) {
            try {
              fcmToken = await getFCMToken();
            } catch (fcmError) {
              if (process.env.NODE_ENV === 'development') {
                console.error("FCM 토큰 가져오기 중 오류:", fcmError);
              }
            }
          }

          // 카카오 로그인 (FCM 토큰 포함)
          const loginResponse = await authApi.kakaoLogin(
            code,
            fcmToken || undefined
          );

          if (loginResponse.success && loginResponse.data) {
            const authResponse = loginResponse.data;
            
            if (authResponse.status === "NEW_USER") {
              // 신규 사용자: uuid를 세션스토리지에 저장하고 회원가입 페이지로 이동
              if (authResponse.uuid) {
                sessionStorage.setItem("tempUserUuid", authResponse.uuid);
              }
              router.push("/signup?required=true");
            } else if (authResponse.status === "EXISTING_USER") {
              // 기존 사용자: 사용자 정보 갱신 후 홈으로 이동
              await refreshUser();
              await fetchNotifications();

              // 카카오 친구 동의 완료 후 돌아온 경우 확인
              const returnUrl = sessionStorage.getItem("returnUrl");
              const kakaoConsentUrl = sessionStorage.getItem("kakaoConsentUrl");

              let finalRedirect = "/";

              if (returnUrl && kakaoConsentUrl) {
                // 카카오 친구 동의 완료 후 돌아온 경우
                finalRedirect = returnUrl;

                // 세션 스토리지 정리
                sessionStorage.removeItem("returnUrl");
                sessionStorage.removeItem("kakaoConsentUrl");

                // 성공 알림 표시
                setTimeout(() => {
                  showSuccess(
                    "카카오 친구 목록 동의 완료",
                    "이제 친구 목록을 확인할 수 있습니다."
                  );
                }, 1000);
              } else if (state) {
                // 일반 로그인 state 파라미터가 있는 경우
                finalRedirect = decodeURIComponent(state);
              }

              router.push(finalRedirect);
            } else {
              showError("로그인 오류", "예상치 못한 로그인 응답이 발생했습니다.");
              router.push("/login?error=" + encodeURIComponent("예상치 못한 로그인 응답"));
            }
          } else {
            const errorMessage = loginResponse.error || "로그인 실패";
            showError("로그인 실패", errorMessage);
            router.push("/login?error=" + encodeURIComponent(errorMessage));
          }
        } catch (apiError) {
          if (process.env.NODE_ENV === 'development') {
            console.error("API call failed during Kakao login:", apiError);
          }
          showError("API 호출 실패", "서버와 통신 중 오류가 발생했습니다.");
          router.push("/login?error=" + encodeURIComponent("API 호출 실패"));
        }
      } else {
        showError("로그인 취소", "카카오 로그인이 취소되었습니다.");
        router.push("/login?error=" + encodeURIComponent("카카오 로그인 취소 또는 오류"));
      }
    };

    handleCallback();

    // cleanup: 컴포넌트 언마운트 시 처리
    return () => {
      isProcessingRef.current = false;
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, [
    router,
    searchParams,
    refreshUser,
    fetchNotifications,
    showSuccess,
    showError,
  ]);

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
