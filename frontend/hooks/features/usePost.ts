"use client";

// ============ POST QUERY HOOKS (TanStack Query) ============
// 단순 API hooks - 직접 사용 시에만 import
export {
  usePostList as usePostListQuery,
  useRealtimePosts as useRealtimePostsQuery,
  useWeeklyPosts as useWeeklyPostsQuery,
} from '@/hooks/api/usePostQueries';

// ============ POST MUTATION HOOKS (TanStack Query) ============
export {
  useCreatePost,
  useUpdatePost,
  useDeletePost,
} from '@/hooks/api/usePostMutations';

// ============ POST HOOKS (통합 인터페이스) ============
// 기존 컴포넌트와의 호환성을 위한 통합 훅
export * from './post/usePostList';
export * from './post/usePostDetail';

// Re-export types for backward compatibility
export type { CommentWithReplies } from './post/usePostDetail';