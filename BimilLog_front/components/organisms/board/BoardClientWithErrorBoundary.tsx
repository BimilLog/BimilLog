"use client";

import { Suspense } from "react";
import { ErrorBoundary } from "@/components/molecules/feedback/error-boundary";
import BoardClient from "./BoardClient";
import { BoardTableSkeleton } from "./BoardTableSkeleton";
import type { BoardInitialData } from "./BoardClient";

interface Props {
  initialData: BoardInitialData | null | undefined;
}

/**
 * BoardClient를 ErrorBoundary + Suspense 로 감싼 래퍼 컴포넌트
 * F-BUG-6 (round-6): BoardClient 내부에서 useSearchParams() 사용 →
 *   Next.js 15 에서 useSearchParams 는 Suspense boundary 안에 있어야 SSR/CSR 정합 보장 (build 경고 방지)
 * 서버 컴포넌트(page.tsx)에서 사용합니다.
 */
export default function BoardClientWithErrorBoundary({ initialData }: Props) {
  return (
    <ErrorBoundary context="board">
      <Suspense fallback={<BoardTableSkeleton rows={6} />}>
        <BoardClient initialData={initialData ?? undefined} />
      </Suspense>
    </ErrorBoundary>
  );
}
