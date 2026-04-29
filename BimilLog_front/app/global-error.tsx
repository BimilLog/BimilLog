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

  return (
    <html>
      <body>
        <div className="min-h-screen flex items-center justify-center bg-paper">
          <div className="text-center max-w-md mx-auto p-4">
            <h1 className="font-display text-4xl font-bold text-ink mb-4 tracking-tight">
              앱에 문제가 발생했어요
            </h1>
            <p className="font-body text-ink-soft mb-8">
              죄송합니다. 예상치 못한 오류가 발생했습니다.
              <br />
              페이지를 새로고침해주세요.
            </p>
            <button
              onClick={() => reset()}
              className="px-6 py-3 bg-stamp-red text-white rounded-lg hover:bg-stamp-red/90 transition-colors font-medium shadow-brand-md"
            >
              다시 시도하기
            </button>
          </div>
        </div>
      </body>
    </html>
  );
}
