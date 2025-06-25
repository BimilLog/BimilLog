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
            console.log("모바일/태블릿 환경 감지 - FCM 토큰 가져오기 시도");
            try {
              fcmToken = await getFCMToken();
              if (fcmToken) {
                console.log("FCM 토큰 획득 성공 - 로그인 시 전송합니다");
              } else {
                console.log("FCM 토큰 획득 실패 - FCM 토큰 없이 로그인 진행");
              }
            } catch (fcmError) {
              console.error("FCM 토큰 가져오기 중 오류:", fcmError);
              console.log("FCM 토큰 오류 무시하고 로그인 진행");
            }
          } else {
            console.log("데스크톱 환경 - FCM 토큰 건너뛰기");
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
              console.log("기존회원 로그인 성공 - SSE 연결을 시작합니다.");
              await fetchNotifications(); // 알림 목록 조회
              connectSSE(); // SSE 연결 시작

              const finalRedirect = state ? decodeURIComponent(state) : "/";
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
        <div className="w-12 h-12 bg-gradient-to-r from-pink-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-4">
          <Heart className="w-7 h-7 text-white animate-pulse" />
        </div>
        <p className="text-gray-600">로그인 처리 중...</p>
        {isMobileOrTablet() && (
          <p className="text-sm text-gray-500 mt-2">모바일 알림 설정 중...</p>
        )}
      </div>
    </div>
  );
}
