"use client";

import { useEffect } from "react";
import { KakaoButton, Card, CardContent, CardDescription, CardHeader, CardTitle, ErrorAlert, InfoAlert } from "@/components";
import { MessageSquare } from "lucide-react";
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
      <Card variant="elevated">
        <CardHeader className="text-center pb-2">
          <CardTitle className="text-2xl font-bold">
            로그인
          </CardTitle>
          <CardDescription>
            나만의 롤링페이퍼를 만들어 보세요
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6 pt-6">
          {/* URL 파라미터로 전달된 에러가 있을 경우에만 에러 메시지 표시 */}
          {error && (
            <ErrorAlert>
              <div>
                <p className="font-semibold">로그인 오류</p>
                <p className="text-sm mt-1">{decodeURIComponent(error)}</p>
              </div>
            </ErrorAlert>
          )}
          
          <KakaoButton
            size="full"
            onClick={handleLogin}
          >
            <svg
              className="w-5 h-5"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M12 3c5.799 0 10.5 3.664 10.5 8.185 0 4.52-4.701 8.184-10.5 8.184a13.5 13.5 0 0 1-1.727-.11l-4.408 2.883c-.501.265-.678.236-.472-.413l.892-3.678c-2.88-1.46-4.785-3.99-4.785-6.866C1.5 6.665 6.201 3 12 3z" />
            </svg>
            카카오로 시작하기
          </KakaoButton>
          <InfoAlert>
            <div>
              <p className="font-semibold mb-2">로그인 없이도 이용 가능!</p>
              <p className="text-sm mb-1">
                로그인 없이도 다른 사람의 롤링페이퍼에 메시지를 남길 수 있어요.
              </p>
              <p className="text-sm">게시판 이용도 할 수 있어요.</p>
            </div>
          </InfoAlert>
        </CardContent>
      </Card>
    </AuthLayout>
  );
}
