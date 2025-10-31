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
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
          <div className="text-center max-w-md mx-auto p-4">
            <h1 className="text-4xl font-bold text-brand-primary mb-4">
              앱에 문제가 발생했어요
            </h1>
            <p className="text-brand-muted mb-8">
              죄송합니다. 예상치 못한 오류가 발생했습니다.
              <br />
              페이지를 새로고침해주세요.
            </p>
            <button
              onClick={() => reset()}
              className="px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
            >
              다시 시도하기
            </button>
          </div>
        </div>
      </body>
    </html>
  );
}
