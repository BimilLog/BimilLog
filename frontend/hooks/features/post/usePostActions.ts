"use client";

import { useState, useCallback } from 'react';
import { useRouter } from "next/navigation";
import { postCommand } from '@/lib/api';
import {
  useCreatePost as useCreatePostMutation,
  useUpdatePost as useUpdatePostMutation,
  useDeletePost as useDeletePostMutation,
  useLikePost as useLikePostMutation
} from '@/hooks/api/usePostMutations';
import { toast } from "sonner";
import type { Post } from '@/types/domains/post';

// 기존 API와의 호환성을 위해 re-export
export {
  useCreatePostMutation as useCreatePost,
  useUpdatePostMutation as useUpdatePost,
  useDeletePostMutation as useDeletePost,
  useLikePostMutation as useLikePost
};

// 게시글 액션 통합 Hook (상세 페이지용)
export function usePostActions(
  postId: string,
  post: Post | null,
  canModify: () => boolean,
  setShowPasswordModal: (show: boolean) => void,
  setPasswordModalTitle: (title: string) => void,
  setDeleteMode: (mode: "post" | "comment" | null) => void,
  setModalPassword: (password: string) => void,
  fetchPost: () => void,
  setPost?: (post: Post) => void
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

    // 익명 게시글의 경우 비밀번호 입력 모달을 표시, 로그인 사용자 게시글은 바로 삭제
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
    if (!postId || isLiking || !post) return;

    setIsLiking(true);
    try {
      const response = await postCommand.like(Number(postId));
      if (response.success) {
        // 즉시 post 상태 업데이트 (낙관적 업데이트)
        setPost && setPost({
          ...post,
          isLiked: !post.isLiked,
          likeCount: post.isLiked ? post.likeCount - 1 : post.likeCount + 1
        });
        // 백그라운드에서 서버 데이터로 동기화
        fetchPost();
      } else {
        throw new Error(response.message || "좋아요 실패");
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : "좋아요 처리에 실패했습니다.";
      toast.error(errorMessage);
      // 에러 시 서버 데이터로 되돌리기
      fetchPost();
    } finally {
      setIsLiking(false);
    }
  }, [postId, isLiking, post, fetchPost, setPost]);

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
  const { mutate: deletePost, isPending: isDeleting } = useDeletePostMutation();
  const { mutate: likePost, isPending: isLiking } = useLikePostMutation();
  const { mutate: updatePost, isPending: isUpdating } = useUpdatePostMutation();

  const handleDelete = useCallback(async (password?: string) => {
    deletePost(postId);
  }, [deletePost, postId]);

  const handleLike = useCallback(async () => {
    likePost(postId);
  }, [likePost, postId]);

  const handleUpdate = useCallback(async (data: Partial<Post>) => {
    updatePost({ postId, ...data } as { postId: number } & Post);
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