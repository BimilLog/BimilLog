"use client";

import { useEffect, useMemo, useState } from "react";
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
import { readLastUsedProvider } from "@/hooks/features/auth";

type Provider = "kakao" | "naver" | "google";

const LOGIN_BENEFITS: BenefitItem[] = [
  { text: "롤링페이퍼 개설하기" },
  { text: "친구로부터 비밀메시지를 받기" },
  { text: "마이페이지에서 활동점수를 확인" },
  { text: "실시간 알림 받기" },
  { text: "글과 댓글에 추천" },
];

const PROVIDER_PERSIST_TO_BUTTON: Record<"KAKAO" | "NAVER" | "GOOGLE", Provider> = {
  KAKAO: "kakao",
  NAVER: "naver",
  GOOGLE: "google",
};

export default function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
  const router = useRouter();
  const searchParams = useSearchParams();
  const { clearAuthError } = useAuthError();

  // URL 파라미터에서 에러 메시지를 추출 (OAuth 콜백 → /login redirect 시 전달)
  const errorCode = searchParams.get("error");
  const errorProviderRaw = searchParams.get("provider");

  // B-403: provider 식별자를 동적 카피에 활용 (없으면 중립 카피)
  const PROVIDER_KR: Record<"KAKAO" | "NAVER" | "GOOGLE", string> = {
    KAKAO: "카카오",
    NAVER: "네이버",
    GOOGLE: "구글",
  };
  const providerKr =
    errorProviderRaw && (errorProviderRaw === "KAKAO" || errorProviderRaw === "NAVER" || errorProviderRaw === "GOOGLE")
      ? PROVIDER_KR[errorProviderRaw]
      : null;
  const providerLabel = providerKr ?? "소셜";

  /**
   * 에러 코드별 사용자 친화적 메시지.
   *
   * - app 내부에서 redirect 한 매크로 코드 (`no_code`, `callback_failed`, ...) +
   * - OAuth provider 가 직접 보낸 raw 코드 (`access_denied`, `invalid_request`, ...)
   *   를 모두 한국어로 매핑한다. 매핑 누락 시 일반 카피 + raw code 보조 표시.
   */
  const getErrorMessage = (code: string | null): { title: string; raw?: string } => {
    if (!code) return { title: "" };

    // 매크로 코드 — 콜백 hook 에서 redirect 시 사용
    const macroMessages: Record<string, string> = {
      no_code: `${providerLabel} 로그인에 실패했어요. 다시 시도해 주세요.`,
      callback_failed: "로그인 처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.",
      login_failed: `${providerLabel} 로그인에 실패했어요. 다시 시도해 주세요.`,
    };

    // OAuth provider 가 직접 redirect 에 실어 보낸 raw 에러 코드
    const oauthMessages: Record<string, string> = {
      access_denied: `${providerLabel} 로그인이 취소되었어요.`,
      invalid_request: "요청 정보가 올바르지 않아요. 다시 시도해 주세요.",
      unauthorized_client: "잠시 후 다시 시도해 주세요. 인증 환경에 문제가 있어요.",
      unsupported_response_type: "지원되지 않는 응답 방식이에요. 잠시 후 다시 시도해 주세요.",
      invalid_scope: `${providerLabel} 인증 권한 설정에 문제가 있어요.`,
      server_error: `${providerLabel} 인증 서버에서 일시적인 오류가 발생했어요.`,
      temporarily_unavailable: `${providerLabel} 인증 서버가 잠시 응답하지 않아요. 잠시 후 다시 시도해 주세요.`,
    };

    if (code in macroMessages) {
      return { title: macroMessages[code] };
    }
    if (code in oauthMessages) {
      return { title: oauthMessages[code] };
    }

    // 매핑 누락 — 일반 카피 + raw code 보조 텍스트
    return {
      title: "인증 처리 중 문제가 발생했어요. 잠시 후 다시 시도해 주세요.",
      raw: decodeURIComponent(code),
    };
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

  // B-302: 외부 OAuth 페이지에서 뒤로가기로 돌아왔을 때 redirectingProvider 가
  // 영원히 spinner 상태로 stuck 되는 것을 방지.
  // visibilitychange + pageshow + focus 이벤트로 광범위하게 reset.
  useEffect(() => {
    const reset = () => {
      setRedirectingProvider(null);
    };

    const handleVisibilityChange = () => {
      if (typeof document !== "undefined" && document.visibilityState === "visible") {
        reset();
      }
    };

    if (typeof document !== "undefined") {
      document.addEventListener("visibilitychange", handleVisibilityChange);
    }
    if (typeof window !== "undefined") {
      window.addEventListener("pageshow", reset);
      window.addEventListener("focus", reset);
    }

    return () => {
      if (typeof document !== "undefined") {
        document.removeEventListener("visibilitychange", handleVisibilityChange);
      }
      if (typeof window !== "undefined") {
        window.removeEventListener("pageshow", reset);
        window.removeEventListener("focus", reset);
      }
    };
  }, []);

  // 4-5: last-used provider 힌트 — localStorage 의 last provider 를 mount 시 읽어
  // 해당 OAuth 버튼에 "마지막 로그인" 라벨 핀.
  const [lastUsedProvider, setLastUsedProvider] = useState<Provider | null>(null);
  useEffect(() => {
    const raw = readLastUsedProvider();
    if (raw) {
      setLastUsedProvider(PROVIDER_PERSIST_TO_BUTTON[raw]);
    }
  }, []);

  // 4-8: last-used provider 가 있는 단말이면 InfoAlert 축소
  const hasReturningHint = lastUsedProvider !== null;

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

  // 마지막 로그인 뱃지 (sr-only 안내 + 시각적 ribbon)
  const LastUsedBadge = useMemo(
    () => (
      <span
        className="absolute -top-2 right-3 inline-flex items-center gap-1 rounded-full bg-stamp-red px-2 py-0.5 text-[10px] font-semibold text-white shadow-sm"
        aria-hidden="true"
      >
        <span aria-hidden="true">●</span>
        마지막 로그인
      </span>
    ),
    []
  );

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
        {errorMessage.title && (
          <div className="mb-4">
            <ErrorAlert>
              <div className="space-y-2">
                <p className="font-semibold">로그인 오류</p>
                <p className="text-sm">{errorMessage.title}</p>
                {errorMessage.raw && (
                  <p className="text-[11px] text-ink-soft/80 dark:text-foreground/60">
                    오류 코드: {errorMessage.raw}
                  </p>
                )}
                <button
                  onClick={handleLogin}
                  className="text-sm font-medium text-postal-navy hover:text-stamp-red dark:text-ink-900 dark:hover:text-stamp-red underline underline-offset-2"
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
          <div className="relative">
            {lastUsedProvider === "kakao" && LastUsedBadge}
            <button
              type="button"
              onClick={handleLogin}
              disabled={isAnyRedirecting}
              aria-label={
                lastUsedProvider === "kakao"
                  ? "카카오 로그인 (마지막으로 사용한 로그인)"
                  : "카카오 로그인"
              }
              data-testid="login-oauth-kakao"
              data-recent={lastUsedProvider === "kakao" ? "true" : undefined}
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
          </div>

          {/* 네이버 로그인 버튼 — L-8 hover affordance */}
          <div className="relative">
            {lastUsedProvider === "naver" && LastUsedBadge}
            <button
              type="button"
              onClick={handleNaverLogin}
              disabled={isAnyRedirecting}
              aria-label={
                lastUsedProvider === "naver"
                  ? "네이버 로그인 (마지막으로 사용한 로그인)"
                  : "네이버 로그인"
              }
              data-testid="login-oauth-naver"
              data-recent={lastUsedProvider === "naver" ? "true" : undefined}
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
          </div>

          {/* 구글 로그인 버튼 — L-3 shadow 보강, L-8 hover affordance */}
          <div className="relative">
            {lastUsedProvider === "google" && LastUsedBadge}
            <button
              type="button"
              onClick={handleGoogleLogin}
              disabled={isAnyRedirecting}
              aria-label={
                lastUsedProvider === "google"
                  ? "구글 계정으로 로그인 (마지막으로 사용한 로그인)"
                  : "구글 계정으로 로그인"
              }
              data-testid="login-oauth-google"
              data-recent={lastUsedProvider === "google" ? "true" : undefined}
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
        </div>

        {/* sr-only: 스크린리더 사용자에게 마지막 로그인 안내 */}
        {lastUsedProvider && (
          <p className="sr-only" aria-live="polite">
            지난번에는{" "}
            {lastUsedProvider === "kakao"
              ? "카카오"
              : lastUsedProvider === "naver"
                ? "네이버"
                : "구글"}
            로 로그인하셨어요.
          </p>
        )}

        {/* 4-8: 복귀 사용자에게는 InfoAlert 축소 */}
        <div className="mt-6">
          {hasReturningHint ? (
            <p className="text-xs text-center text-ink-soft dark:text-foreground/70">
              로그인 없이도 일부 기능 이용 가능
            </p>
          ) : (
            <InfoAlert icon={false}>
              <div>
                <p className="font-semibold mb-2">로그인 없이도 이용 가능!</p>
                <p className="text-sm mb-1">
                  로그인 없이도 다른 사람의 롤링페이퍼에 메시지를 남길 수 있고 게시판 이용이 가능합니다.
                </p>
              </div>
            </InfoAlert>
          )}
        </div>
      </Card>
    </AuthLayout>
  );
}
