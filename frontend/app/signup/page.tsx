"use client";

import { useEffect } from "react";
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, AuthLoadingScreen } from "@/components";
import Link from "next/link";
import { useAuth, useToast } from "@/hooks";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthLayout, NicknameSetupForm } from "@/components/organisms/auth";

/**
 * 회원가입 페이지
 *
 * 두 가지 상태를 처리합니다:
 * 1. 초기 회원가입 페이지 (/signup) - 카카오 로그인 버튼만 표시
 * 2. 닉네임 설정 페이지 (/signup?required=true) - 카카오 OAuth 완료 후 닉네임 입력 폼 표시
 *
 * URL 파라미터:
 * - required=true: 닉네임 설정이 필요한 단계임을 표시
 * - uuid: 임시 UUID (useSignupUuid 훅에서 자동으로 URL에서 추출)
 */
export default function SignUpPage() {
  const { login, isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
  const router = useRouter();
  const searchParams = useSearchParams();
  // URL 쿼리에서 required=true인 경우 닉네임 설정이 필요한 회원가입 단계
  const needsNickname = searchParams.get("required") === "true";
  const { showSuccess, showError } = useToast();
  // UUID는 HttpOnly 쿠키로 전달되어 프론트엔드에서 직접 접근 불가

  // 이미 로그인된 사용자가 일반 회원가입 페이지에 접근한 경우 홈으로 리다이렉트
  // needsNickname이 false인 경우는 일반적인 /signup 접근을 의미
  useEffect(() => {
    if (!isLoading && isAuthenticated && !needsNickname) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, router, needsNickname]);

  // 로딩 상태
  if (isLoading) {
    return <AuthLoadingScreen message="로딩 중..." />;
  }

  // 카카오 로그인 후 닉네임 설정 단계 (required=true)
  // 회원가입 플로우: 카카오 로그인 → OAuth 콜백 → 임시 UUID 발급 (HttpOnly 쿠키) → 닉네임 설정 → 회원가입 완료
  // UUID는 HttpOnly 쿠키로 전달되어 프론트엔드에서 접근 불가, 회원가입 API 호출 시 자동 전송
  if (needsNickname) {
    return (
      <AuthLayout>
        <NicknameSetupForm
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

  // 최초 회원가입 페이지 (아직 카카오 로그인을 하지 않은 상태)
  // 카카오 버튼 클릭 시 login("/signup?required=true")로 OAuth 시작
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
          {/* 카카오 OAuth 시작 버튼 - redirectTo에 required=true로 닉네임 설정 필요함을 표시 */}
          <Button
            className="w-full h-12 bg-yellow-400 hover:bg-yellow-500 text-brand-primary font-semibold text-base"
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
