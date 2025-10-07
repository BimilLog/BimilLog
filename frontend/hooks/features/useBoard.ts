"use client";

// ============ BOARD QUERY HOOKS ============
export {
  useBoardPosts,
  useBoardSearch,
  useBoardRealtimePosts,
  useBoardWeeklyPosts,
} from '@/hooks/api/useBoardQueries';

// ============ BOARD MUTATION HOOKS ============
export {
  useCreateBoardPost,
  useUpdateBoardPost,
  useDeleteBoardPost,
} from '@/hooks/api/useBoardMutations';

// ============ BOARD FORM HOOKS ============
// Re-export from post folder for backward compatibility
export { useWriteForm } from './post/useWriteForm';
export { useEditForm } from './post/useEditForm';