"use client";

// ============ COMMENT QUERY HOOKS ============
export {
  usePopularComments,
} from '@/hooks/api/useCommentQueries';

// ============ COMMENT MUTATION HOOKS ============
export {
  useCreateComment,
  useUpdateComment,
  useDeleteComment,
  useLikeComment,
  useLikeCommentOptimized,
} from '@/hooks/api/useCommentMutations';