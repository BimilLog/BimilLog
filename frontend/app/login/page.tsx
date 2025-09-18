"use client";

import { useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, ErrorAlert, InfoAlert } from "@/components";
import Image from "next/image";
import { useAuth } from "@/hooks";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthLayout } from "@/components/organisms/auth";
import { AuthLoadingScreen } from "@/components";
import { useAuthError } from "@/hooks";
import { kakaoAuthManager } from "@/lib/auth/kakao";

export default function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
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
    <AuthLayout
      breadcrumbItems={[
        { title: "홈", href: "/" },
        { title: "로그인", href: "/login" },
      ]}
    >
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
          
          <button
            onClick={handleLogin}
            className="w-full transition-all duration-200 hover:opacity-90 active:scale-[0.98] touch-manipulation"
            aria-label="카카오로 로그인하기"
          >
            <Image
              src="/kakao_login_medium_narrow.png"
              alt="카카오 로그인"
              width={300}
              height={45}
              className="w-full h-auto max-w-[300px] mx-auto"
              priority
            />
          </button>
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
