"use client";

import { useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import { Button } from "@/components";
import { Home, RefreshCw, AlertTriangle, ArrowLeft } from "lucide-react";
import { AuthHeader } from "@/components/organisms/common";
import { HomeFooter } from "@/components/organisms/home";
import { BackButton } from "@/components/atoms/actions/back-button";
import { errorLogger } from "@/lib/error-logger";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // 에러 로깅: 백엔드로 전송
    console.error("Error occurred:", error);
    errorLogger.logError(error, { digest: error.digest });
  }, [error]);

  return (
    <div className="min-h-screen bg-paper">
      <AuthHeader />

      <div className="flex items-center justify-center p-4 py-16">
        <div className="text-center max-w-md mx-auto">
          {/* 500 일러스트레이션 */}
          <div className="mb-8">
            <div className="relative">
              {/* 메인 아이콘 */}
              <div className="w-24 h-24 mx-auto mb-6 flex items-center justify-center">
                <div className="relative">
                  <Image
                    src="/log.png"
                    alt="비밀로그"
                    width={96}
                    height={96}
                    className="h-24 w-auto object-contain opacity-50"
                    priority
                    placeholder="blur"
                    blurDataURL="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iOTYiIGhlaWdodD0iOTYiIHZpZXdCb3g9IjAgMCA5NiA5NiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9Ijk2IiBoZWlnaHQ9Ijk2IiBmaWxsPSIjRjNGNEY2IiBvcGFjaXR5PSIwLjUiLz4KPC9zdmc+Cg=="
                  />
                  <div className="absolute inset-0 flex items-center justify-center">
                    <AlertTriangle className="w-12 h-12 text-stamp-red" />
                  </div>
                </div>
              </div>

              {/* 500 텍스트 — 도장처럼 살짝 기울어진 stamp-red */}
              <div className="font-display text-6xl md:text-7xl font-bold text-stamp-red mb-4 inline-block transform -rotate-3 tracking-tight">
                500
              </div>

              {/* 종이 데코 — 매우 절제 */}
              <div
                className="absolute -top-4 -left-4 w-3 h-3 bg-stamp-red/30 rounded-full opacity-60 animate-pulse"
                style={{ animationDelay: "0s" }}
              ></div>
              <div
                className="absolute -bottom-4 left-8 w-2 h-2 bg-postal-navy/30 rounded-full opacity-60 animate-pulse"
                style={{ animationDelay: "1s" }}
              ></div>
            </div>
          </div>

          {/* 텍스트 영역 */}
          <div className="mb-8">
            <h1 className="font-display text-2xl md:text-3xl font-bold text-ink dark:text-foreground mb-4 tracking-tight">
              일시적인 오류가 발생했어요
            </h1>
            <p className="font-body text-ink-soft dark:text-muted-foreground leading-relaxed">
              예상치 못한 문제가 발생했어요.
              <br />
              잠시 후 다시 시도하거나 페이지를 새로고침 해주세요.
            </p>
            {error.digest && (
              <p className="text-xs text-ink-soft mt-4">
                오류 코드: {error.digest}
              </p>
            )}
          </div>

          {/* 액션 버튼들 */}
          <div className="space-y-3">
            <Button
              onClick={() => reset()}
              size="lg"
              className="w-full bg-stamp-red hover:bg-stamp-red/90"
            >
              <RefreshCw className="w-5 h-5 mr-2" />
              다시 시도하기
            </Button>

            <Button asChild variant="outline" size="lg" className="w-full">
              <Link href="/">
                <Home className="w-5 h-5 mr-2" />
                홈으로 돌아가기
              </Link>
            </Button>

            <BackButton
              variant="ghost"
              size="lg"
              className="w-full"
            >
              <ArrowLeft className="w-4 h-4 mr-2" />
              이전 페이지로
            </BackButton>
          </div>

          {/* 하단 링크들 */}
          <div className="mt-12 pt-8 border-t border-ink-soft">
            <p className="text-sm font-body text-ink-soft mb-4">
              문제가 계속되나요?
            </p>
            <div className="flex justify-center space-x-6 text-sm">
              <Link
                href="/suggest"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors"
              >
                문제 신고하기
              </Link>
              <Link
                href="/help"
                className="text-postal-navy hover:text-stamp-red hover:underline underline-offset-2 transition-colors"
              >
                도움말
              </Link>
            </div>
          </div>
        </div>
      </div>

      <HomeFooter />
    </div>
  );
}