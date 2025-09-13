import { useState, useCallback } from "react";
import { commentCommand, type Comment } from "@/lib/api";
import { toast } from "sonner";

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
        const response = await commentCommand.write({
          postId: Number(postId),
          content,
          parentId,
          password,
        });

        if (response.success) {
          toast.success("댓글이 작성되었습니다.");
          fetchComments();
          return true;
        } else {
          throw new Error(response.message || "댓글 작성 실패");
        }
      } catch (error: any) {
        toast.error(error.message || "댓글 작성에 실패했습니다.");
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
        const response = await commentCommand.update({
          commentId,
          content,
          password,
        });

        if (response.success) {
          toast.success("댓글이 수정되었습니다.");
          fetchComments();
          return true;
        } else {
          throw new Error(response.message || "댓글 수정 실패");
        }
      } catch (error: any) {
        toast.error(error.message || "댓글 수정에 실패했습니다.");
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
        const response = await commentCommand.delete({
          commentId,
          password,
        });

        if (response.success) {
          toast.success("댓글이 삭제되었습니다.");
          fetchComments();
          return true;
        } else {
          throw new Error(response.message || "댓글 삭제 실패");
        }
      } catch (error: any) {
        toast.error(error.message || "댓글 삭제에 실패했습니다.");
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
      } catch (error: any) {
        toast.error(error.message || "좋아요 처리에 실패했습니다.");
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