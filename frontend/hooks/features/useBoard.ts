"use client";

// ============ POST QUERY HOOKS ============
// 단순 API hooks - 직접 사용 시에만 import
export {
  usePostList as usePostListQuery,
  useRealtimePosts as useRealtimePostsQuery,
  useWeeklyPosts as useWeeklyPostsQuery,
} from '@/hooks/api/usePostQueries';

// ============ POST MUTATION HOOKS ============
export {
  useCreatePost,
  useUpdatePost,
  useDeletePost,
} from '@/hooks/api/usePostMutations';

// Note: useWriteForm and useEditForm are now exported from hooks/features/post