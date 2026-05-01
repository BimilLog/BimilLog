"use client";

import { useEffect } from "react";
import { ErrorView } from "@/components/molecules/feedback";
import { logger } from "@/lib/utils/logger";

export default function AdminError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    logger.error("Admin page error:", error);
  }, [error]);

  return (
    <ErrorView
      stampCode="ERR"
      stampLabel="관리자 영역"
      title="관리자 화면을 불러올 수 없어요"
      description={
        <>
          관리자 페이지를 불러오는 중 문제가 발생했어요.
          <br />
          잠시 후 다시 시도해 주세요.
        </>
      }
      onRetry={reset}
      digest={error.digest}
    />
  );
}
