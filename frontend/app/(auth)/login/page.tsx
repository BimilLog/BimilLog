"use client";

import { useEffect } from "react";
import { Button } from "@/components";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components";
import { MessageSquare, AlertCircle } from "lucide-react";
import { useAuth } from "@/hooks";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthLayout } from "@/components/organisms/auth";
import { AuthLoadingScreen } from "@/components";
import { useAuthError } from "@/hooks";
import { kakaoAuthManager } from "@/lib/auth/kakao";

export default function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { clearAuthError } = useAuthError();

  // URL 파라미터에서 에러 메시지를 추출 (카카오 OAuth 콜백에서 전달됨)
  const error = searchParams.get("error");

  // 이미 로그인된 사용자는 홈페이지로 자동 리다이렉션
  useEffect(() => {
    if (isAuthenticated) {
      router.push("/");
    }
  }, [isAuthenticated, router]);

  // 에러가 있을 경우 5초 후 자동으로 에러 상태 초기화 (사용자 경험 개선)
  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => {
        clearAuthError();
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [error, clearAuthError]);

  const handleLogin = () => {
    kakaoAuthManager.redirectToKakaoAuth();
  };

  // 로그인 상태 확인 중일 때 로딩 스크린 표시
  if (isLoading) {
    return <AuthLoadingScreen message="로딩 중..." />;
  }

  return (
    <AuthLayout>
      <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
        <CardHeader className="text-center pb-2">
          <CardTitle className="text-2xl font-bold text-gray-800">
            로그인
          </CardTitle>
          <CardDescription className="text-gray-600">
            나만의 롤링페이퍼를 만들어 보세요
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6 pt-6">
          {/* URL 파라미터로 전달된 에러가 있을 경우에만 에러 메시지 표시 */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3">
              <div className="flex items-start space-x-2">
                <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
                <div className="text-sm text-red-800">
                  <p className="font-medium">로그인 오류</p>
                  {/* URL 인코딩된 에러 메시지를 디코딩하여 표시 */}
                  <p>{decodeURIComponent(error)}</p>
                </div>
              </div>
            </div>
          )}
          
          <Button
            className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-gray-800 font-semibold text-base transition-all active:scale-[0.98]"
            onClick={handleLogin}
          >
            <svg
              className="w-5 h-5 mr-2"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3z" />
            </svg>
            카카오로 시작하기
          </Button>
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-2">
              <MessageSquare className="w-5 h-5 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-blue-800">
                <p className="font-medium mb-1">로그인 없이도 이용 가능!</p>
                <p>
                  로그인 없이도 다른 사람의 롤링페이퍼에 메시지를 남길 수
                  있어요.
                </p>
                <p>게시판 이용도 할 수 있어요.</p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </AuthLayout>
  );
}
