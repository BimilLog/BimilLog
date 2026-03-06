"use client";

import { ErrorBoundary } from "@/components/molecules/feedback/error-boundary";
import BoardClient from "./BoardClient";
import type { BoardInitialData } from "./BoardClient";

interface Props {
  initialData: BoardInitialData | null | undefined;
}

/**
 * BoardClient를 ErrorBoundary로 감싼 래퍼 컴포넌트
 * 서버 컴포넌트(page.tsx)에서 사용합니다.
 */
export default function BoardClientWithErrorBoundary({ initialData }: Props) {
  return (
    <ErrorBoundary context="board">
      <BoardClient initialData={initialData ?? undefined} />
    </ErrorBoundary>
  );
}
