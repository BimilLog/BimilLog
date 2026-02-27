"use client";

import { useEffect } from "react";
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, AuthLoadingScreen } from "@/components";
import Link from "next/link";
import { useAuth } from "@/hooks";
import { useRouter } from "next/navigation";
import { AuthLayout } from "@/components/organisms/auth";

/**
 * 회원가입 페이지
 *
 * 카카오 OAuth 버튼을 제공합니다.
 * 소셜 로그인 완료 시 신규/기존 회원 모두 즉시 JWT 토큰이 발급되어 홈으로 이동합니다.
 */
export default function SignUpPage() {
  const { login, isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return <AuthLoadingScreen message="로딩 중..." />;
  }

  return (
    <AuthLayout>
      <Card variant="elevated">
        <CardHeader className="text-center pb-2">
          <CardTitle className="text-2xl font-bold text-brand-primary">
            회원가입
          </CardTitle>
          <CardDescription className="text-brand-muted">
            나만의 롤링페이퍼를 만들어보세요
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6 pt-6">
          <Button
            className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-brand-primary font-semibold text-base"
            onClick={() => login()}
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

          <div className="text-center">
            <p className="text-sm text-brand-muted">
              이미 계정이 있으신가요?{" "}
              <Link
                href="/login"
                className="text-purple-600 hover:text-purple-700 font-medium"
              >
                로그인
              </Link>
            </p>
          </div>
        </CardContent>
      </Card>
    </AuthLayout>
  );
}
