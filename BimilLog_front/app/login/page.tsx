"use client";

import { useEffect, useState } from "react";
import { ErrorAlert, InfoAlert } from "@/components";
import { BenefitsList, type BenefitItem } from "@/components";
import { Card } from "flowbite-react";
import Image from "next/image";
import { useAuth } from "@/hooks";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthLayout } from "@/components/organisms/auth";
import { AuthLoadingScreen } from "@/components";
import { useAuthError } from "@/hooks";
import { kakaoAuthManager } from "@/lib/auth/kakao";
import { naverAuthManager } from "@/lib/auth/naver";
import { googleAuthManager } from "@/lib/auth/google";

type Provider = "kakao" | "naver" | "google";

const LOGIN_BENEFITS: BenefitItem[] = [
  { text: "롤링페이퍼 개설하기" },
  { text: "친구로부터 비밀메시지를 받기" },
  { text: "마이페이지에서 활동점수를 확인" },
  { text: "실시간 알림 받기" },
  { text: "글과 댓글에 추천" },
];

export default function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
  const router = useRouter();
  const searchParams = useSearchParams();
  const { clearAuthError } = useAuthError();

  // URL 파라미터에서 에러 메시지를 추출 (카카오 OAuth 콜백에서 전달됨)
  const errorCode = searchParams.get("error");

  // 에러 코드별 사용자 친화적 메시지
  const getErrorMessage = (code: string | null): string => {
    if (!code) return "";

    const errorMessages: Record<string, string> = {
      "no_code": "카카오 로그인에 실패했습니다. 다시 시도해주세요.",
      "callback_failed": "로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
      "login_failed": "로그인에 실패했습니다. 다시 시도해주세요.",
      "access_denied": "카카오 로그인이 취소되었습니다.",
    };

    return errorMessages[code] || decodeURIComponent(code);
  };

  const errorMessage = getErrorMessage(errorCode);

  // 이미 로그인된 사용자는 홈페이지로 자동 리다이렉션
  useEffect(() => {
    if (isAuthenticated) {
      router.push("/");
    }
  }, [isAuthenticated, router]);

  // 에러가 있을 경우 10초 후 자동으로 에러 상태 초기화 (사용자 경험 개선)
  useEffect(() => {
    if (errorCode) {
      const timer = setTimeout(() => {
        clearAuthError();
      }, 10000);
      return () => clearTimeout(timer);
    }
  }, [errorCode, clearAuthError]);

  const [redirectingProvider, setRedirectingProvider] = useState<Provider | null>(null);
  const isAnyRedirecting = redirectingProvider !== null;

  const startOAuth = (provider: Provider) => {
    if (redirectingProvider) return; // 중복 클릭 방지
    setRedirectingProvider(provider);

    requestAnimationFrame(() => {
      try {
        if (provider === "kakao") {
          kakaoAuthManager.redirectToKakaoAuth();
        } else if (provider === "naver") {
          naverAuthManager.redirectToNaverAuth();
        } else {
          googleAuthManager.redirectToGoogleAuth();
        }
      } catch {
        setRedirectingProvider(null);
      }
    });
  };

  const handleLogin = () => startOAuth("kakao");
  const handleNaverLogin = () => startOAuth("naver");
  const handleGoogleLogin = () => startOAuth("google");

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
      <Card className="max-w-sm mx-auto bg-paper-card dark:bg-gray-900 border-2 border-ink-soft dark:border-gray-700 shadow-brand-lg">
        {/* URL 파라미터로 전달된 에러가 있을 경우에만 에러 메시지 표시 */}
        {errorMessage && (
          <div className="mb-4">
            <ErrorAlert>
              <div className="space-y-2">
                <p className="font-semibold">로그인 오류</p>
                <p className="text-sm">{errorMessage}</p>
                <button
                  onClick={handleLogin}
                  className="text-sm font-medium text-postal-navy hover:text-stamp-red underline underline-offset-2"
                >
                  다시 로그인하기
                </button>
              </div>
            </ErrorAlert>
          </div>
        )}

        <div className="mb-6 text-center washi-tape pt-4">
          {/* D-5 일관: 다크에서도 stamp-red 강조 유지 */}
          <h2 className="font-display text-2xl font-bold text-stamp-red dark:text-stamp-red tracking-tight">
            비밀로그 시작하기
          </h2>
          <p className="mt-3 font-body text-ink-soft dark:text-foreground/80">나만의 롤링페이퍼를 만들어 보세요</p>
        </div>

        <BenefitsList items={LOGIN_BENEFITS} variant="check" className="my-7" />

        <div className="space-y-3">
          <button
            type="button"
            onClick={handleLogin}
            disabled={isAnyRedirecting}
            aria-label="카카오 로그인"
            data-testid="login-oauth-kakao"
            className="flex w-full items-center justify-center gap-3 rounded-lg border border-[#FAE100] dark:border-[#E5CD00] bg-[#FAE100] dark:bg-[#E5CD00] px-4 py-3 text-sm font-semibold text-[#381e1f] shadow-sm transition-all duration-150 hover:bg-[#FFD200] dark:hover:bg-[#D5BE00] hover:shadow-md hover:-translate-y-px active:scale-[0.99] active:translate-y-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#c6a400] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 disabled:hover:shadow-sm"
          >
            {redirectingProvider === "kakao" ? (
              <>
                <span
                  className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-[#381e1f]/40 border-t-[#381e1f]"
                  aria-hidden="true"
                />
                <span>카카오로 이동 중…</span>
              </>
            ) : (
              <>
                <span className="flex items-center justify-center rounded-md bg-white">
                  <Image src="/icons/kakao_lcon.png" alt="카카오톡 로고" width={18} height={18} priority />
                </span>
                <span className="tracking-tight">카카오 로그인</span>
              </>
            )}
          </button>

          {/* 네이버 로그인 버튼 — L-8 hover affordance */}
          <button
            type="button"
            onClick={handleNaverLogin}
            disabled={isAnyRedirecting}
            aria-label="네이버 로그인"
            data-testid="login-oauth-naver"
            className="flex w-full items-center justify-center gap-3 rounded-lg border border-[#03c75a] bg-[#03c75a] px-4 py-3 text-sm font-semibold text-white shadow-sm transition-all duration-150 hover:bg-[#02b150] hover:shadow-md hover:-translate-y-px active:scale-[0.99] active:translate-y-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#03c75a] disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 disabled:hover:shadow-sm"
          >
            {redirectingProvider === "naver" ? (
              <>
                <span
                  className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white"
                  aria-hidden="true"
                />
                <span>네이버로 이동 중…</span>
              </>
            ) : (
              <>
                <span className="flex items-center justify-center rounded-md bg-white">
                  <Image src="/icons/naver-n.png" alt="네이버 N 로고" width={20} height={20} priority />
                </span>
                <span className="tracking-tight">네이버 로그인</span>
              </>
            )}
          </button>

          {/* 구글 로그인 버튼 — L-3 shadow 보강, L-8 hover affordance */}
          <button
            type="button"
            onClick={handleGoogleLogin}
            disabled={isAnyRedirecting}
            aria-label="구글 계정으로 로그인"
            data-testid="login-oauth-google"
            className="flex w-full items-center justify-center gap-3 rounded-lg border border-[#dadce0] bg-white px-4 py-3 text-sm font-semibold text-[#3c4043] shadow-[0_1px_2px_rgba(0,0,0,0.06)] transition-all duration-150 hover:bg-gray-50 hover:shadow-md hover:-translate-y-px active:scale-[0.99] active:translate-y-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#1a73e8] dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:hover:bg-gray-700 dark:shadow-none dark:hover:shadow-md disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 disabled:hover:shadow-[0_1px_2px_rgba(0,0,0,0.06)]"
          >
            {redirectingProvider === "google" ? (
              <>
                <span
                  className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-[#3c4043]/40 border-t-[#3c4043] dark:border-white/40 dark:border-t-white"
                  aria-hidden="true"
                />
                <span>구글로 이동 중…</span>
              </>
            ) : (
              <>
                <span className="flex h-6 w-6 items-center justify-center rounded-md bg-transparent">
                  <Image
                    src="/icons/google-g.svg"
                    alt="Google G 로고"
                    width={20}
                    height={20}
                    priority
                  />
                </span>
                <span className="tracking-tight">구글 계정으로 로그인</span>
              </>
            )}
          </button>
        </div>

        <div className="mt-6">
          <InfoAlert icon={false}>
            <div>
              <p className="font-semibold mb-2">로그인 없이도 이용 가능!</p>
              <p className="text-sm mb-1">
                로그인 없이도 다른 사람의 롤링페이퍼에 메시지를 남길 수 있고 게시판 이용이 가능합니다.
              </p>
            </div>
          </InfoAlert>
        </div>
      </Card>
    </AuthLayout>
  );
}
