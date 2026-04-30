"use client";

import { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components";
import { BenefitsList, type BenefitItem } from "@/components";
import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/hooks";
import { useRouter } from "next/navigation";
import { AuthLayout } from "@/components/organisms/auth";
import { kakaoAuthManager } from "@/lib/auth/kakao";
import { naverAuthManager } from "@/lib/auth/naver";
import { googleAuthManager } from "@/lib/auth/google";

type Provider = "kakao" | "naver" | "google";

const SIGNUP_BENEFITS: BenefitItem[] = [
  // 카피 단축 — iter-1 캡처에서 첫 항목 끝의 "받아 / 요." 위도우 줄바꿈 회피.
  // (NEEDS_FIX 5 / L-1)
  { emphasis: "비밀 롤링페이퍼", text: "따뜻한 편지를 안전하게 받아요." },
  { emphasis: "친구의 마음", text: "익명이지만 진심 담긴 메시지를 모아요." },
  { emphasis: "활동 알림", text: "새 메시지·반응을 바로 받아봐요." },
];

/**
 * 회원가입 페이지
 *
 * - 카카오/네이버/구글 OAuth 3종 모두 노출 (F-201)
 * - 약관 / 개인정보 처리방침 1줄 안내 + 링크 (F-204)
 * - 버튼 클릭 즉시 시각 피드백 — 외부 리다이렉트 전 disabled + spinner (F-207 / 4-B)
 * - signup 진입 시 AuthLoadingScreen 풀스크린 깜박임 제거 — 인라인 fallback 으로 대체 (F-202 / 5-1)
 */
export default function SignUpPage() {
  const { isAuthenticated, isLoading } = useAuth({ skipRefresh: true });
  const router = useRouter();
  const [redirectingProvider, setRedirectingProvider] = useState<Provider | null>(null);

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push("/");
    }
  }, [isLoading, isAuthenticated, router]);

  const startOAuth = (provider: Provider) => {
    if (redirectingProvider) return; // 중복 클릭 방지
    setRedirectingProvider(provider);

    // 외부 리다이렉트 직전 — UI 상태가 paint 될 시간을 확보 (next frame)
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

  const isAnyRedirecting = redirectingProvider !== null;

  return (
    <AuthLayout>
      <Card
        variant="elevated"
        className="bg-paper-card border-2 border-ink-soft shadow-brand-lg max-w-md mx-auto"
      >
        <CardHeader className="text-center pb-2 washi-tape pt-6">
          {/* D-5: 다크에서도 stamp-red 강조 유지 (라이트와 동일 톤 정책) */}
          <CardTitle className="font-display text-2xl md:text-3xl font-bold text-stamp-red dark:text-stamp-red tracking-tight">
            회원가입
          </CardTitle>
          <CardDescription className="font-body text-ink-soft dark:text-foreground/80">
            나만의 롤링페이퍼를 만들어보세요
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6 pt-6">
          {/* 가입 베네핏 — login 페이지와 동일 컴포넌트 (5-10) */}
          <BenefitsList items={SIGNUP_BENEFITS} variant="dot" label="가입하면 이런 일이" />

          {/* 인증 상태 확인 중 — 풀스크린 대신 인라인 표시 (F-202) */}
          {isLoading && (
            <div
              role="status"
              aria-live="polite"
              className="flex items-center justify-center gap-2 rounded-md border border-dashed border-ink-soft/40 bg-paper-soft/30 px-4 py-3 text-sm font-body text-ink-soft dark:text-muted-foreground"
            >
              <span className="inline-block h-2 w-2 animate-pulse rounded-full bg-stamp-red" />
              <span>인증 상태를 확인하는 중…</span>
            </div>
          )}

          <div className="space-y-3">
            {/* 카카오 — 공식 컬러 #FAE100 (5-5) + aria-label (5-6)
                D-2: 다크에서 톤다운 (#E5CD00). L-5: 흰 박스 불투명도 100%.
                L-8: hover 시 명도 Δ 5%↑ (#FFD200) + shadow + translate */}
            <button
              type="button"
              onClick={() => startOAuth("kakao")}
              disabled={isAnyRedirecting || isLoading}
              aria-label="카카오로 회원가입 및 로그인"
              data-testid="signup-oauth-kakao"
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
                    <Image
                      src="/icons/kakao_lcon.png"
                      alt="카카오톡 로고"
                      width={18}
                      height={18}
                      priority
                    />
                  </span>
                  <span className="tracking-tight">카카오로 시작하기</span>
                </>
              )}
            </button>

            {/* 네이버 (F-201) — L-8: hover 시 shadow + 1px lift */}
            <button
              type="button"
              onClick={() => startOAuth("naver")}
              disabled={isAnyRedirecting || isLoading}
              aria-label="네이버로 회원가입 및 로그인"
              data-testid="signup-oauth-naver"
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
                    <Image
                      src="/icons/naver-n.png"
                      alt="네이버 N 로고"
                      width={20}
                      height={20}
                    />
                  </span>
                  <span className="tracking-tight">네이버로 시작하기</span>
                </>
              )}
            </button>

            {/* 구글 (F-201) — L-3: 카드 배경(paper)과 분리되도록 shadow 보강.
                L-8: hover affordance 강화 (shadow-md + 1px lift) */}
            <button
              type="button"
              onClick={() => startOAuth("google")}
              disabled={isAnyRedirecting || isLoading}
              aria-label="구글 계정으로 회원가입 및 로그인"
              data-testid="signup-oauth-google"
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
                    />
                  </span>
                  <span className="tracking-tight">구글 계정으로 시작하기</span>
                </>
              )}
            </button>
          </div>

          {/* 약관/개인정보 안내 (F-204 / 4-D) — 고지형 (implicit consent)
              D-4: 라이트/다크 모두 postal-navy 톤(차분) — 라이트와 동일 등급.
              다크에서 postal-navy 가독성 보강을 위해 dark:text-postal-navy/95 로 미세 조정.
              L-8: hover 시 underline 두께/오프셋 강화. */}
          <p className="text-center text-xs font-body leading-relaxed text-ink-soft dark:text-foreground/80">
            가입 시{" "}
            <Link
              href="/terms"
              className="text-postal-navy dark:text-postal-navy/95 underline underline-offset-2 transition-colors duration-150 hover:text-stamp-red hover:decoration-2 hover:decoration-stamp-red/70 hover:underline-offset-4"
            >
              서비스 이용약관
            </Link>
            {" 및 "}
            <Link
              href="/privacy"
              className="text-postal-navy dark:text-postal-navy/95 underline underline-offset-2 transition-colors duration-150 hover:text-stamp-red hover:decoration-2 hover:decoration-stamp-red/70 hover:underline-offset-4"
            >
              개인정보 처리방침
            </Link>
            에 동의한 것으로 간주됩니다.
          </p>

          <div className="text-center">
            <p className="text-sm font-body text-ink-soft dark:text-foreground/80">
              이미 계정이 있으신가요?{" "}
              <Link
                href="/login"
                className="text-postal-navy dark:text-postal-navy/95 font-medium underline underline-offset-2 transition-colors duration-150 hover:text-stamp-red hover:decoration-2 hover:decoration-stamp-red/70 hover:underline-offset-4"
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
