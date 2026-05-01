"use client";

import { useEffect } from "react";
import { errorLogger } from "@/lib/error-logger";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // 글로벌 에러 로깅: 백엔드로 전송
    console.error("Global error occurred:", error);
    errorLogger.logError(error, {
      digest: error.digest,
      type: "global-error",
    });
  }, [error]);

  // global-error 는 root <html>/<body> 자체를 다시 렌더해야 하므로
  // AuthHeader / HomeFooter 등 외부 layout 의존이 큰 ErrorView 대신
  // self-contained 마크업을 유지하되 라운드 8 메타포 톤만 일치시킴.
  return (
    <html lang="ko">
      <body>
        <div
          className="min-h-screen flex items-center justify-center bg-paper"
          role="alert"
          aria-live="assertive"
        >
          <div className="text-center max-w-md mx-auto p-4">
            {/* 봉투 + DELIVERY FAILED 도장 SVG (currentColor 기반) */}
            <div className="w-32 h-32 mx-auto mb-6 flex items-center justify-center text-ink">
              <svg viewBox="0 0 120 120" className="w-full h-full" aria-hidden="true">
                <g transform="rotate(-12 60 60)">
                  <rect
                    x="14"
                    y="36"
                    width="92"
                    height="60"
                    rx="3"
                    className="fill-paper-50 stroke-current"
                    strokeWidth="2"
                  />
                  <polyline
                    points="14,36 60,76 106,36"
                    fill="none"
                    className="stroke-current"
                    strokeWidth="2"
                  />
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
                </g>
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
                    배달 실패 FAILED
                  </text>
                </g>
              </svg>
            </div>

            <h1 className="font-display text-3xl md:text-4xl font-bold text-ink mb-4 tracking-tight break-keep">
              앱에 문제가 발생했어요
            </h1>
            <p className="font-body text-ink-soft mb-2 break-keep">
              죄송합니다. 예상치 못한 오류가 발생했습니다.
              <br />
              페이지를 새로고침해주세요.
            </p>
            {error.digest && (
              <p className="text-xs font-mono text-ink-soft/80 mt-3 mb-6 break-all">
                오류 코드: {error.digest}
              </p>
            )}
            <button
              onClick={() => reset()}
              className="mt-2 px-6 py-3 bg-stamp-red text-white rounded-lg hover:bg-stamp-red/90 transition-colors font-medium shadow-brand-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stamp-red focus-visible:ring-offset-2"
            >
              다시 시도하기
            </button>
          </div>
        </div>
      </body>
    </html>
  );
}
