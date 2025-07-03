"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/hooks/useAuth";
import { authApi } from "@/lib/api";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import { useToast } from "@/hooks/useToast";
import { ToastContainer } from "@/components/molecules/toast";

export default function LogoutPage() {
  const { logout } = useAuth();
  const router = useRouter();
  const { showError, toasts, removeToast } = useToast();

  useEffect(() => {
    const handleLogout = async () => {
      try {
        // 카카오 연결 끊기 - 동의 철회 처리
        if (window.Kakao && window.Kakao.isInitialized()) {
          try {
            // 동의 철회
            const unlink = () => {
              return new Promise((resolve, reject) => {
                window.Kakao.API.request({
                  url: "/v1/user/unlink",
                  success: resolve,
                  fail: reject,
                });
              });
            };

            await unlink();
            console.log("카카오 연결 끊기 성공");
          } catch (kakaoError) {
            console.error("카카오 연결 끊기 실패:", kakaoError);
            showError(
              "카카오 연결 끊기 실패",
              "동의 URL을 찾을 수 없습니다. 다시 시도해주세요."
            );
          }
        }

        // 서버 로그아웃
        await authApi.logout();
        await logout();

        // 홈으로 리다이렉트
        router.replace("/");
      } catch (error) {
        console.error("Logout failed:", error);
        // 에러가 발생해도 강제 로그아웃
        await logout();
        router.replace("/");
      }
    };

    handleLogout();
  }, [logout, router, showError]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <div className="flex items-center justify-center p-4 py-20">
        <Card className="w-full max-w-md border-0 shadow-2xl bg-white/90 backdrop-blur-sm">
          <CardHeader className="text-center pb-6">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center mx-auto mb-4">
              <div className="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin" />
            </div>
            <CardTitle className="text-2xl bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent">
              로그아웃 중...
            </CardTitle>
          </CardHeader>
          <CardContent className="text-center">
            <p className="text-gray-600 mb-6">
              안전하게 로그아웃 처리 중입니다.
              <br />
              잠시만 기다려주세요.
            </p>
          </CardContent>
        </Card>
      </div>

      <HomeFooter />
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </div>
  );
}
