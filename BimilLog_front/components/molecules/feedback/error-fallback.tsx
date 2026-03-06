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

/**
 * Error Boundary의 기본 에러 폴백 UI
 *
 * - "문제가 발생했습니다" 메시지 표시
 * - "다시 시도" 버튼으로 에러 복구
 * - 개발 환경에서만 에러 상세 정보 표시
 * - 다크모드 지원
 */
export function ErrorFallback({ error, resetErrorBoundary }: ErrorFallbackProps) {
  const isDev = process.env.NODE_ENV === "development";

  return (
    <div className="flex items-center justify-center p-6 min-h-[200px]">
      <Card className="w-full max-w-md border-red-200 dark:border-red-800 bg-white dark:bg-gray-900">
        <CardHeader className="text-center pb-2">
          <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/30">
            <AlertTriangle className="h-6 w-6 text-red-600 dark:text-red-400" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            문제가 발생했습니다
          </h3>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            일시적인 오류가 발생했어요. 다시 시도해주세요.
          </p>
        </CardHeader>

        <CardContent className="pt-0">
          {isDev && (
            <details className="mt-3 rounded-md bg-red-50 dark:bg-red-950/30 p-3 text-xs">
              <summary className="cursor-pointer font-medium text-red-700 dark:text-red-400">
                에러 상세 (개발 환경)
              </summary>
              <div className="mt-2 space-y-2">
                <p className="font-mono text-red-600 dark:text-red-300 break-all">
                  {error.message}
                </p>
                {error.stack && (
                  <pre className="overflow-x-auto whitespace-pre-wrap text-red-500 dark:text-red-400 max-h-40 overflow-y-auto">
                    {error.stack}
                  </pre>
                )}
              </div>
            </details>
          )}
        </CardContent>

        <CardFooter className="flex flex-col gap-2 pt-2">
          <Button
            onClick={resetErrorBoundary}
            className="w-full"
            size="sm"
          >
            <RefreshCw className="mr-2 h-4 w-4" />
            다시 시도
          </Button>
          <Button asChild variant="outline" size="sm" className="w-full">
            <Link href="/">
              <Home className="mr-2 h-4 w-4" />
              홈으로 돌아가기
            </Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
