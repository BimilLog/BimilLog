"use client";

// ============ POST MUTATION HOOKS ============
export {
  useCreatePost,
  useUpdatePost,
  useDeletePost,
} from '@/hooks/api/usePostMutations';

// Note: useWriteForm and useEditForm are now exported from hooks/features/post
// Note: Post query hooks are now in hooks/features/post/usePostList.ts