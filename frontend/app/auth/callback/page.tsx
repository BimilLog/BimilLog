"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Heart } from "lucide-react";
import { authApi } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import { useNotifications } from "@/hooks/useNotifications";
import { getFCMToken, isMobileOrTablet } from "@/lib/utils";

export default function AuthCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const { connectSSE, fetchNotifications } = useNotifications();
  const isProcessing = useRef(false);

  useEffect(() => {
    if (isProcessing.current) {
      return;
    }
    isProcessing.current = true;

    const handleCallback = async () => {
      const code = searchParams.get("code");
      const error = searchParams.get("error");
      const state = searchParams.get("state"); // 최종 리다이렉트 URL

      if (error) {
        console.error("Kakao Auth error:", error);
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
              console.error("FCM 토큰 가져오기 중 오류:", fcmError);
            }
          }

          // 카카오 로그인 (FCM 토큰 포함)
          const loginResponse = await authApi.kakaoLogin(
            code,
            fcmToken || undefined
          );

          if (loginResponse.success) {
            // 쿠키가 정상적으로 세팅되었는지 확인하기 위해 /auth/me API 호출
            const userResponse = await authApi.getCurrentUser();

            if (userResponse.success && userResponse.data?.userName) {
              // 정식 회원: 유저 정보가 있고, userName이 존재함
              await refreshUser(); // 전역 상태 업데이트

              // 기존회원 로그인 성공 후 SSE 연결 및 알림 목록 조회
              await fetchNotifications(); // 알림 목록 조회
              connectSSE(); // SSE 연결 시작

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
                  alert(
                    "카카오 친구 목록 동의가 완료되었습니다! 이제 친구 목록을 확인할 수 있습니다."
                  );
                }, 1000);
              } else if (state) {
                // 일반 로그인 state 파라미터가 있는 경우
                finalRedirect = decodeURIComponent(state);
              }

              router.push(finalRedirect);
            } else {
              // 임시 회원: 유저 정보가 없거나, userName이 없음
              router.push("/signup?required=true");
            }
          } else {
            console.error("Server login failed:", loginResponse.error);
            router.push(
              "/login?error=" +
                encodeURIComponent(loginResponse.error || "로그인 실패")
            );
          }
        } catch (apiError) {
          console.error("API call failed during Kakao login:", apiError);
          router.push("/login?error=" + encodeURIComponent("API 호출 실패"));
        }
      } else {
        // code가 없는 경우 (예: 사용자가 로그인 취소)
        router.push(
          "/login?error=" + encodeURIComponent("카카오 로그인 취소 또는 오류")
        );
      }
    };

    handleCallback();
  }, [router, searchParams, refreshUser, connectSSE, fetchNotifications]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 flex items-center justify-center">
      <div className="text-center">
        <img
          src="/log.png"
          alt="비밀로그"
          className="h-12 object-contain mx-auto mb-4 animate-pulse"
        />
        <p className="text-gray-600">로그인 처리 중...</p>
        {isMobileOrTablet() && (
          <p className="text-sm text-gray-500 mt-2">모바일 알림 설정 중...</p>
        )}
      </div>
    </div>
  );
}
