"use client";

import React from "react";
import Link from "next/link";
import { Home, RefreshCw, ArrowLeft } from "lucide-react";
import { Button } from "@/components/atoms/actions/button";
import { BackButton } from "@/components/atoms/actions/back-button";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";

interface ErrorViewProps {
  /** 도장 안에 보일 코드 (500, ERR 등) */
  stampCode?: string;
  /** 도장 보조 라벨 (기본: "배달 실패 FAILED") */
  stampLabel?: string;
  /** 헤딩 카피 */
  title?: string;
  /** 설명 */
  description?: React.ReactNode;
  /** 다시 시도 콜백. 없으면 버튼 자체 미노출 */
  onRetry?: () => void;
  /** 다시 시도 버튼 비활성화 (재시도 횟수 초과 등) */
  retryDisabled?: boolean;
  /** Next.js error.digest */
  digest?: string;
  /** AuthHeader / HomeFooter 노출 (기본 true). global-error 처럼 false 가능 */
  withChrome?: boolean;
  /** 하단 도움말 영역 노출 (기본 true) */
  showHelpLinks?: boolean;
  /** 뒤로가기 버튼 (기본 true) */
  showBackButton?: boolean;
  /** 홈 버튼 (기본 true) */
  showHomeButton?: boolean;
}

/**
 * ErrorView — 라운드 8 RETURN 도장 메타포의 에러 표준 컴포넌트.
 *
 * `app/error.tsx` / `app/(protected)/admin/error.tsx` / `app/global-error.tsx`
 * 가 동일하게 사용. NotFoundView 와 시각적으로 일관 (봉투 + DELIVERY FAILED 도장).
 */
export function ErrorView({
  stampCode = "500",
  stampLabel = "배달 실패 FAILED",
  title = "편지가 도착하지 못했어요",
  description = (
    <>
      예상치 못한 문제가 발생했어요.
      <br />
      잠시 후 다시 시도하거나 페이지를 새로고침 해주세요.
    </>
  ),
  onRetry,
  retryDisabled = false,
  digest,
  withChrome = true,
  showHelpLinks = true,
  showBackButton = true,
  showHomeButton = true,
}: ErrorViewProps) {
  const content = (
    <div
      className="flex items-center justify-center p-4 py-16"
      role="alert"
      aria-live="assertive"
    >
      <div className="text-center max-w-md mx-auto">
        {/* 봉투 + DELIVERY FAILED 도장 SVG */}
        <div className="mb-8">
          <div className="relative">
            <div className="w-32 h-32 mx-auto mb-6 flex items-center justify-center relative text-ink dark:text-foreground">
              <svg viewBox="0 0 120 120" className="w-full h-full" aria-hidden="true">
                {/* 봉투 — 살짝 찢긴 느낌 (rotate 더 큼) */}
                <g transform="rotate(-12 60 60)">
                  <rect
                    x="14"
                    y="36"
                    width="92"
                    height="60"
                    rx="3"
                    className="fill-paper-50 dark:fill-paper-card stroke-current"
                    strokeWidth="2"
                  />
                  <polyline
                    points="14,36 60,76 106,36"
                    fill="none"
                    className="stroke-current"
                    strokeWidth="2"
                  />
                  {/* 우표 자리 — 살짝 찢긴 듯 dashed 더 굵게 */}
                  <rect
                    x="78"
                    y="42"
                    width="22"
                    height="22"
                    fill="none"
                    className="stroke-stamp-red"
                    strokeWidth="1.5"
                    strokeDasharray="3 3"
                  />
                  <text
                    x="89"
                    y="56"
                    textAnchor="middle"
                    fontSize="9"
                    fontFamily="serif"
                    className="fill-stamp-red"
                    fontWeight="bold"
                  >
                    {stampCode}
                  </text>
                </g>
                {/* DELIVERY FAILED 도장 */}
                <g transform="rotate(15 60 100)">
                  <rect
                    x="14"
                    y="92"
                    width="92"
                    height="14"
                    fill="none"
                    className="stroke-stamp-red"
                    strokeWidth="1"
                    opacity="0.7"
                  />
                  <text
                    x="60"
                    y="102"
                    textAnchor="middle"
                    fontSize="7"
                    fontFamily="serif"
                    className="fill-stamp-red"
                    fontWeight="bold"
                    letterSpacing="1"
                  >
                    {stampLabel}
                  </text>
                </g>
              </svg>
            </div>

            {/* 코드 도장 */}
            <div className="font-display text-5xl sm:text-6xl md:text-7xl font-bold text-stamp-red mb-4 inline-block transform -rotate-3 tracking-tight break-keep">
              {stampCode}
            </div>

            {/* 데코 도트 */}
            <div
              className="absolute -top-4 -left-4 w-3 h-3 bg-stamp-red/30 rounded-full opacity-60 animate-pulse"
              style={{ animationDelay: "0s" }}
              aria-hidden="true"
            />
            <div
              className="absolute -bottom-4 left-8 w-2 h-2 bg-postal-navy/30 rounded-full opacity-60 animate-pulse"
              style={{ animationDelay: "1s" }}
              aria-hidden="true"
            />
          </div>
        </div>

        {/* 텍스트 영역 */}
        <div className="mb-8">
          <h1 className="font-display text-2xl md:text-3xl font-bold text-ink dark:text-foreground mb-4 tracking-tight break-keep">
            {title}
          </h1>
          <p className="font-body text-ink-soft dark:text-muted-foreground leading-relaxed break-keep">
            {description}
          </p>
          {digest && (
            <p className="text-xs font-mono text-ink-soft/80 dark:text-muted-foreground/80 mt-4 break-all">
              오류 코드: {digest}
            </p>
          )}
          {retryDisabled && (
            <p className="text-xs font-body text-stamp-red mt-3 break-keep">
              여러 번 다시 시도했지만 같은 문제가 계속 발생하고 있어요. 페이지를 새로고침하거나 잠시 뒤에 다시 방문해 주세요.
            </p>
          )}
        </div>

        {/* 액션 버튼들 */}
        <div className="space-y-3">
          {onRetry && (
            <Button
              onClick={onRetry}
              size="lg"
              className="w-full bg-stamp-red hover:bg-stamp-red/90 disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={retryDisabled}
            >
              <RefreshCw className="w-5 h-5 mr-2" aria-hidden="true" />
              다시 시도하기
            </Button>
          )}

          {showHomeButton && (
            <Button asChild variant="outline" size="lg" className="w-full">
              <Link href="/">
                <Home className="w-5 h-5 mr-2" aria-hidden="true" />
                홈으로 돌아가기
              </Link>
            </Button>
          )}

          {showBackButton && (
            <BackButton variant="ghost" size="lg" className="w-full">
              <ArrowLeft className="w-4 h-4 mr-2" aria-hidden="true" />
              이전 페이지로
            </BackButton>
          )}
        </div>

        {/* 하단 도움말 */}
        {showHelpLinks && (
          <div className="mt-12 pt-8 border-t border-ink-soft">
            <p className="text-sm font-body text-ink-soft mb-4">문제가 계속되나요?</p>
            <div className="flex justify-center space-x-6 text-sm">
              <Link
                href="/suggest"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 rounded-sm"
              >
                문제 신고하기
              </Link>
              <Link
                href="/help"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2 rounded-sm"
              >
                도움말
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );

  if (!withChrome) {
    return <div className="min-h-screen bg-paper">{content}</div>;
  }

  return (
    <div className="min-h-screen bg-paper">
      <AuthHeader />
      {content}
      <HomeFooter />
    </div>
  );
}
