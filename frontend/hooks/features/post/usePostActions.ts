"use client";

import { useState, useCallback } from 'react';
import { useRouter } from "next/navigation";
import { postCommand } from '@/lib/api';
import { useApiMutation } from '@/hooks/api/useApiMutation';
import { toast } from "sonner";
import type { Post } from '@/types/domains/post';

// 게시글 작성
export function useCreatePost() {
  return useApiMutation(postCommand.create, {
    showSuccessToast: true,
    successMessage: '게시글이 작성되었습니다.'
  });
}

// 게시글 수정
export function useUpdatePost() {
  return useApiMutation(
    ({ id, data }: { id: number; data: Partial<Post> }) => postCommand.update({ ...data, id } as Post),
    {
      showSuccessToast: true,
      successMessage: '게시글이 수정되었습니다.'
    }
  );
}

// 게시글 삭제
export function useDeletePost() {
  return useApiMutation(postCommand.delete, {
    showSuccessToast: true,
    successMessage: '게시글이 삭제되었습니다.'
  });
}

// 게시글 좋아요
export function useLikePost() {
  return useApiMutation(postCommand.like, {
    showErrorToast: false // 좋아요는 에러 토스트 표시 안 함
  });
}

// 게시글 액션 통합 Hook (상세 페이지용)
export function usePostActions(
  postId: string,
  post: Post | null,
  canModify: () => boolean,
  setShowPasswordModal: (show: boolean) => void,
  setPasswordModalTitle: (title: string) => void,
  setDeleteMode: (mode: "post" | "comment" | null) => void,
  setModalPassword: (password: string) => void,
  fetchPost: () => void
) {
  const router = useRouter();
  const [isDeleting, setIsDeleting] = useState(false);
  const [isLiking, setIsLiking] = useState(false);

  const handleEdit = useCallback(() => {
    if (!canModify()) {
      toast.error("수정 권한이 없습니다.");
      return;
    }
    router.push(`/board/post/${postId}/edit`);
  }, [canModify, router, postId]);

  const handleDeleteClick = useCallback(() => {
    if (!canModify()) {
      toast.error("삭제 권한이 없습니다.");
      return;
    }

    if (post?.userName === "익명" || post?.userName === null) {
      setPasswordModalTitle("게시글 삭제");
      setDeleteMode("post");
      setShowPasswordModal(true);
      setModalPassword("");
    } else {
      handleDelete();
    }
  }, [canModify, post, setPasswordModalTitle, setDeleteMode, setShowPasswordModal, setModalPassword]);

  const handleDelete = useCallback(
    async (password?: string) => {
      if (!postId || isDeleting) return;

      setIsDeleting(true);
      try {
        const response = await postCommand.delete(Number(postId));
        if (response.success) {
          toast.success("게시글이 삭제되었습니다.");
          router.push("/board");
        } else {
          throw new Error(response.message || "삭제 실패");
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : "게시글 삭제에 실패했습니다.";
        toast.error(errorMessage);
      } finally {
        setIsDeleting(false);
        setShowPasswordModal(false);
        setModalPassword("");
      }
    },
    [postId, isDeleting, router, setShowPasswordModal, setModalPassword]
  );

  const handleLike = useCallback(async () => {
    if (!postId || isLiking) return;

    setIsLiking(true);
    try {
      const response = await postCommand.like(Number(postId));
      if (response.success) {
        fetchPost();
      } else {
        throw new Error(response.message || "좋아요 실패");
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : "좋아요 처리에 실패했습니다.";
      toast.error(errorMessage);
    } finally {
      setIsLiking(false);
    }
  }, [postId, isLiking, fetchPost]);

  return {
    handleEdit,
    handleDeleteClick,
    handleDelete,
    handleLike,
    isDeleting,
    isLiking,
  };
}

// 간단한 게시글 액션 Hook
export function usePostActionsSimple(postId: number) {
  const { mutate: deletePost, isLoading: isDeleting } = useDeletePost();
  const { mutate: likePost, isLoading: isLiking } = useLikePost();
  const { mutate: updatePost, isLoading: isUpdating } = useUpdatePost();

  const handleDelete = useCallback(async (password?: string) => {
    await deletePost(postId);
  }, [deletePost, postId]);

  const handleLike = useCallback(async () => {
    await likePost(postId);
  }, [likePost, postId]);

  const handleUpdate = useCallback(async (data: Partial<Post>) => {
    await updatePost({ id: postId, data });
  }, [updatePost, postId]);

  return {
    handleDelete,
    handleLike,
    handleUpdate,
    isDeleting,
    isLiking,
    isUpdating
  };
}