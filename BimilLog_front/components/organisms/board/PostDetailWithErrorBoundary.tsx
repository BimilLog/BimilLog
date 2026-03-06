"use client";

import { ErrorBoundary } from "@/components/molecules/feedback/error-boundary";
import PostDetailClient from "./PostDetailClient";
import type { Post } from "@/lib/api";

interface Props {
  initialPost: Post;
  postId: string;
}

/**
 * PostDetailClient를 ErrorBoundary로 감싼 래퍼 컴포넌트
 * 서버 컴포넌트(page.tsx)에서 사용합니다.
 */
export default function PostDetailWithErrorBoundary({
  initialPost,
  postId,
}: Props) {
  return (
    <ErrorBoundary context="post-detail">
      <PostDetailClient initialPost={initialPost} postId={postId} />
    </ErrorBoundary>
  );
}
