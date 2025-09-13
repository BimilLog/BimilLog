"use client";

import { useCallback } from 'react';
import { commentQuery, commentCommand } from '@/lib/api';
import { useApiQuery } from '@/hooks/api/useApiQuery';
import { useApiMutation } from '@/hooks/api/useApiMutation';
import type { Comment } from '@/types/domains/comment';

// 댓글 목록 조회
export function useCommentList(postId: number) {
  return useApiQuery(
    () => commentQuery.getByPostId(postId),
    {
      enabled: postId > 0,
      refetchInterval: 30000 // 30초마다 자동 갱신
    }
  );
}

// 인기 댓글 조회
export function usePopularComments(postId: number) {
  return useApiQuery(
    () => commentQuery.getPopular(postId),
    {
      enabled: postId > 0,
      cacheTime: 5 * 60 * 1000,
      staleTime: 5 * 60 * 1000
    }
  );
}

// 댓글 작성
export function useCreateComment() {
  return useApiMutation(commentCommand.create, {
    showSuccessToast: true,
    successMessage: '댓글이 작성되었습니다.'
  });
}

// 댓글 수정
export function useUpdateComment() {
  return useApiMutation(
    ({ id, content }: { id: number; content: string }) =>
      commentCommand.update(id, { content }),
    {
      showSuccessToast: true,
      successMessage: '댓글이 수정되었습니다.'
    }
  );
}

// 댓글 삭제
export function useDeleteComment() {
  return useApiMutation(
    ({ id, password }: { id: number; password?: number }) => 
      commentCommand.delete(id, password),
    {
      showSuccessToast: true,
      successMessage: '댓글이 삭제되었습니다.'
    }
  );
}

// 댓글 좋아요
export function useLikeComment() {
  return useApiMutation(commentCommand.like, {
    showErrorToast: false // 좋아요는 에러 토스트 표시 안 함
  });
}

// 댓글 액션 통합 Hook
export function useCommentActionsSimple(postId: number, onRefresh?: () => void) {
  const { mutate: createComment, isLoading: isCreating } = useCreateComment();
  const { mutate: updateComment, isLoading: isUpdating } = useUpdateComment();
  const { mutate: deleteComment, isLoading: isDeleting } = useDeleteComment();
  const { mutate: likeComment, isLoading: isLiking } = useLikeComment();

  const handleCreate = useCallback(async (data: {
    postId: number;
    content: string;
    password?: number;
    parentId?: number;
  }) => {
    await createComment(data);
    onRefresh?.();
  }, [createComment, onRefresh]);

  const handleUpdate = useCallback(async (id: number, content: string) => {
    await updateComment({ id, content });
    onRefresh?.();
  }, [updateComment, onRefresh]);

  const handleDelete = useCallback(async (id: number, password?: number) => {
    await deleteComment({ id, password });
    onRefresh?.();
  }, [deleteComment, onRefresh]);

  const handleLike = useCallback(async (id: number) => {
    await likeComment(id);
    onRefresh?.();
  }, [likeComment, onRefresh]);

  return {
    handleCreate,
    handleUpdate,
    handleDelete,
    handleLike,
    isCreating,
    isUpdating,
    isDeleting,
    isLiking
  };
}