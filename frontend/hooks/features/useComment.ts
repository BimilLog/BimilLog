"use client";

import { useState, useCallback } from 'react';
import { commentCommand } from '@/lib/api';
import { toast } from "sonner";

// ============ COMMENT QUERY HOOKS ============
export {
  useCommentList,
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

// ============ LEGACY COMMENT ACTION HOOKS ============
// Legacy 호환: 기존 PostDetailClient 컴포넌트와의 호환성 유지 (점진적 마이그레이션을 위함)

/**
 * 댓글 액션 통합 Hook (기존 PostDetailClient용 - Legacy)
 * @deprecated TanStack Query 훅들로 마이그레이션 예정
 */
export const useCommentActions = (
  postId: string,
  fetchComments: () => void
) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDeletingComment, setIsDeletingComment] = useState(false);
  const [isLikingComment, setIsLikingComment] = useState(false);

  const handleCommentSubmit = useCallback(
    async (content: string, parentId?: number, password?: string) => {
      if (!postId || isSubmitting) return;

      setIsSubmitting(true);
      try {
        // 댓글 작성 API 호출: password는 숫자로 변환하여 전달 (익명 댓글 비밀번호)
        const response = await commentCommand.create({
          postId: Number(postId),
          content,
          parentId,
          password: password ? Number(password) : undefined,
        });

        if (response.success) {
          toast.success("댓글이 작성되었습니다.");
          fetchComments();
          return true;
        } else {
          throw new Error(response.message || "댓글 작성 실패");
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : "댓글 작성에 실패했습니다.";
        toast.error(errorMessage);
        return false;
      } finally {
        setIsSubmitting(false);
      }
    },
    [postId, isSubmitting, fetchComments]
  );

  const handleCommentEdit = useCallback(
    async (commentId: number, content: string, password?: string) => {
      if (isSubmitting) return;

      setIsSubmitting(true);
      try {
        const response = await commentCommand.update(commentId, {
          content,
          password: password ? Number(password) : undefined,
        });

        if (response.success) {
          toast.success("댓글이 수정되었습니다.");
          fetchComments();
          return true;
        } else {
          throw new Error(response.message || "댓글 수정 실패");
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : "댓글 수정에 실패했습니다.";
        toast.error(errorMessage);
        return false;
      } finally {
        setIsSubmitting(false);
      }
    },
    [isSubmitting, fetchComments]
  );

  const handleCommentDelete = useCallback(
    async (commentId: number, password?: string) => {
      if (isDeletingComment) return;

      setIsDeletingComment(true);
      try {
        const response = await commentCommand.delete(
          commentId,
          password ? Number(password) : undefined
        );

        if (response.success) {
          toast.success("댓글이 삭제되었습니다.");
          fetchComments();
          return true;
        } else {
          throw new Error(response.message || "댓글 삭제 실패");
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : "댓글 삭제에 실패했습니다.";
        toast.error(errorMessage);
        return false;
      } finally {
        setIsDeletingComment(false);
      }
    },
    [isDeletingComment, fetchComments]
  );

  const handleCommentLike = useCallback(
    async (commentId: number) => {
      if (isLikingComment) return;

      setIsLikingComment(true);
      try {
        const response = await commentCommand.like(commentId);
        if (response.success) {
          fetchComments();
        } else {
          throw new Error(response.message || "좋아요 실패");
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : "좋아요 처리에 실패했습니다.";
        toast.error(errorMessage);
      } finally {
        setIsLikingComment(false);
      }
    },
    [isLikingComment, fetchComments]
  );

  return {
    handleCommentSubmit,
    handleCommentEdit,
    handleCommentDelete,
    handleCommentLike,
    isSubmitting,
    isDeletingComment,
    isLikingComment,
  };
};