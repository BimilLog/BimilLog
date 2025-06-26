"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { LogOut, Users } from "lucide-react";
import { authApi } from "@/lib/api";

export default function LogoutPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const isProcessing = useRef(false);

  useEffect(() => {
    if (isProcessing.current) {
      return;
    }
    isProcessing.current = true;

    const handleLogout = async () => {
      const isForConsent = searchParams.get("consent") === "true";

      try {
        // 로그아웃 API 호출
        await authApi.logout();

        if (isForConsent) {
          // 카카오 동의를 위한 로그아웃인 경우
          const kakaoConsentUrl = sessionStorage.getItem("kakaoConsentUrl");

          if (kakaoConsentUrl) {
            // 약간의 딜레이 후 카카오 동의 페이지로 이동
            setTimeout(() => {
              window.location.href = kakaoConsentUrl;
            }, 1000);
          } else {
            console.error("저장된 카카오 동의 URL을 찾을 수 없습니다.");
            alert("동의 URL을 찾을 수 없습니다. 다시 시도해주세요.");
            router.push("/");
          }
        } else {
          // 일반 로그아웃인 경우
          router.push("/");
        }
      } catch (error) {
        console.error("로그아웃 실패:", error);
        // 로그아웃 실패해도 메인페이지로 이동
        router.push("/");
      }
    };

    handleLogout();
  }, [router, searchParams]);

  const isForConsent = searchParams.get("consent") === "true";

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex items-center justify-center">
      <div className="text-center">
        <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center mx-auto mb-4">
          {isForConsent ? (
            <Users className="w-7 h-7 text-white animate-pulse" />
          ) : (
            <LogOut className="w-7 h-7 text-white animate-pulse" />
          )}
        </div>
        <p className="text-gray-600 mb-2">
          {isForConsent
            ? "카카오 친구 동의를 위해 로그아웃 중..."
            : "로그아웃 중..."}
        </p>
        <p className="text-sm text-gray-500">
          {isForConsent
            ? "곧 카카오 동의 페이지로 이동합니다"
            : "잠시만 기다려주세요"}
        </p>
      </div>
    </div>
  );
}
