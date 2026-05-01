"use client";

import { AlertTriangle, RefreshCw, Home } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/atoms/actions/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from "@/components/molecules/cards/card";
import type { ErrorFallbackProps } from "./error-boundary";

interface ErrorFallbackComponentProps extends ErrorFallbackProps {
  /** 재시도 횟수 초과로 더 이상 시도 불가 (ErrorBoundary 가 주입) */
  retryDisabled?: boolean;
  /** 재시도 잔여 시도 (1~N) — 표시용 */
  resetCount?: number;
}

/**
 * Error Boundary 의 기본 카드형 fallback UI.
 *
 * 라운드 13 — paper 토큰 통일 (`bg-card` / `bg-paper-aged` / `border-stamp-red/30`).
 * 카피는 메타포 절제 톤 ("편지가 잠깐 흔들렸어요").
 * `retryDisabled` 시 재시도 비활성화 + 새로고침/홈 안내 (라운드 13 무한 루프 가드).
 */
export function ErrorFallback({
  error,
  resetErrorBoundary,
  retryDisabled = false,
  resetCount,
}: ErrorFallbackComponentProps) {
  const isDev = process.env.NODE_ENV === "development";

  return (
    <div
      className="flex items-center justify-center p-6 min-h-[200px]"
      role="alert"
      aria-live="assertive"
    >
      <Card
        className="w-full max-w-md border-stamp-red/30 dark:border-stamp-red/40 bg-card shadow-brand-md"
        variant="elevated"
      >
        <CardHeader className="text-center pb-2">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-paper-aged dark:bg-stamp-red/15 border border-stamp-red/30">
            <AlertTriangle
              className="h-6 w-6 text-stamp-red"
              aria-hidden="true"
            />
          </div>
          <h3 className="font-display text-lg font-semibold text-ink dark:text-foreground break-keep">
            편지가 잠깐 흔들렸어요
          </h3>
          <p className="font-body text-sm text-ink-soft dark:text-muted-foreground mt-1 break-keep">
            {retryDisabled
              ? "여러 번 다시 시도했지만 같은 문제가 계속돼요. 새로고침하거나 홈으로 돌아가 주세요."
              : "잠깐의 문제로 화면을 그릴 수 없었어요. 다시 시도해 주세요."}
          </p>
        </CardHeader>

        <CardContent className="pt-0">
          {isDev && (
            <details className="mt-3 rounded-md bg-paper-aged dark:bg-stamp-red/10 p-3 text-xs border border-stamp-red/20">
              <summary className="cursor-pointer font-medium text-stamp-red">
                에러 상세 (개발 환경)
              </summary>
              <div className="mt-2 space-y-2">
                <p className="font-mono text-stamp-red break-all">{error.message}</p>
                {error.stack && (
                  <pre className="overflow-x-auto whitespace-pre-wrap text-stamp-red/80 max-h-40 overflow-y-auto">
                    {error.stack}
                  </pre>
                )}
                {typeof resetCount === "number" && (
                  <p className="font-mono text-ink-soft">
                    resetCount: {resetCount}
                  </p>
                )}
              </div>
            </details>
          )}
        </CardContent>

        <CardFooter className="flex flex-col gap-2 pt-2">
          <Button
            onClick={resetErrorBoundary}
            className="w-full bg-stamp-red hover:bg-stamp-red/90 disabled:opacity-50 disabled:cursor-not-allowed"
            size="sm"
            disabled={retryDisabled}
          >
            <RefreshCw className="mr-2 h-4 w-4" aria-hidden="true" />
            {retryDisabled ? "다시 시도 불가" : "다시 시도"}
          </Button>
          {retryDisabled && (
            <Button
              onClick={() => {
                if (typeof window !== "undefined") {
                  window.location.reload();
                }
              }}
              variant="outline"
              size="sm"
              className="w-full"
            >
              <RefreshCw className="mr-2 h-4 w-4" aria-hidden="true" />
              페이지 새로고침
            </Button>
          )}
          <Button asChild variant="outline" size="sm" className="w-full">
            <Link href="/">
              <Home className="mr-2 h-4 w-4" aria-hidden="true" />
              홈으로 돌아가기
            </Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
