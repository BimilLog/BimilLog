"use client";

// ============ BOARD QUERY HOOKS (TanStack Query) ============
export {
  useBoardPosts,
  useBoardSearch,
  useBoardPopularPosts,
  useBoardLegendPosts,
  useBoardNoticePosts,
} from '@/hooks/api/useBoardQueries';

// ============ BOARD MUTATION HOOKS (TanStack Query) ============
export {
  useCreateBoardPost,
  useUpdateBoardPost,
  useDeleteBoardPost,
} from '@/hooks/api/useBoardMutations';

// ============ POST HOOKS (통합 인터페이스) ============
// 기존 컴포넌트와의 호환성을 위한 통합 훅
export * from './post/usePostList';
export * from './post/usePostDetail';
export * from './post/usePostActions';
export * from './post/usePostSearch';

// Re-export types for backward compatibility
export type { CommentWithReplies } from './post/usePostDetail';