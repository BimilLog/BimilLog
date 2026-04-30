"use client";

import { useEffect } from "react";
import { ErrorView } from "@/components/molecules/feedback";
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
    <ErrorView
      stampCode="500"
      stampLabel="배달 실패 FAILED"
      title="편지가 도착하지 못했어요"
      description={
        <>
          예상치 못한 문제가 발생했어요.
          <br />
          잠시 후 다시 시도하거나 페이지를 새로고침 해주세요.
        </>
      }
      onRetry={reset}
      digest={error.digest}
    />
  );
}
