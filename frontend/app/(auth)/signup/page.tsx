"use client";

import { useEffect } from "react";
import { Button } from "@/components";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { useRouter, useSearchParams } from "next/navigation";
import { useSignupUuid } from "@/hooks/useSignupUuid";
import { AuthLayout } from "@/components/features/auth";
import { AuthLoadingScreen } from "@/components/atoms/AuthLoadingScreen";
import { NicknameSetupForm } from "@/components/features/auth";
import { useToast } from "@/hooks/useToast";

export default function SignUpPage() {
  const { login, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const needsNickname = searchParams.get("required") === "true";
  const { showSuccess, showError } = useToast();
  const { tempUuid, isValidating, error } = useSignupUuid();

  // 로그인된 상태에서 닉네임 설정이 필요하지 않은 경우 홈으로 리다이렉트
  useEffect(() => {
    if (!isLoading && isAuthenticated && !needsNickname) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, router, needsNickname]);

  // 로딩 중
  if (isLoading || (needsNickname && isValidating)) {
    return <AuthLoadingScreen message="로딩 중..." />;
  }

  // UUID 검증 오류
  if (needsNickname && error) {
    return <AuthLoadingScreen message="회원가입 오류" subMessage={error} />;
  }

  // 닉네임 설정이 필요한 경우
  if (needsNickname && tempUuid) {
    return (
      <AuthLayout>
        <NicknameSetupForm
          tempUuid={tempUuid}
          onSuccess={() => {
            showSuccess("회원가입 완료", "비밀로그에 오신 것을 환영합니다!");
          }}
          onError={(errorMessage: string) => {
            showError("회원가입 실패", errorMessage);
          }}
        />
      </AuthLayout>
    );
  }

  // 일반 회원가입 페이지
  return (
    <AuthLayout>
      <Card className="border-0 shadow-2xl bg-white/80 backdrop-blur-sm">
        <CardHeader className="text-center pb-2">
          <CardTitle className="text-2xl font-bold text-gray-800">
            회원가입
          </CardTitle>
          <CardDescription className="text-gray-600">
            나만의 롤링페이퍼를 만들어보세요
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6 pt-6">
          <Button
            className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-gray-800 font-semibold text-base"
            onClick={() => login("/signup?required=true")}
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
            <p className="text-sm text-gray-600">
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
