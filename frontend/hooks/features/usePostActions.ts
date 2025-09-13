import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { postCommand } from "@/lib/api";
import { toast } from "sonner";

export const usePostActions = (
  postId: string,
  post: any,
  canModify: () => boolean,
  setShowPasswordModal: (show: boolean) => void,
  setPasswordModalTitle: (title: string) => void,
  setDeleteMode: (mode: "post" | "comment" | null) => void,
  setModalPassword: (password: string) => void,
  fetchPost: () => void
) => {
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
        const response = await postCommand.delete(Number(postId), password);
        if (response.success) {
          toast.success("게시글이 삭제되었습니다.");
          router.push("/board");
        } else {
          throw new Error(response.message || "삭제 실패");
        }
      } catch (error: any) {
        toast.error(error.message || "게시글 삭제에 실패했습니다.");
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
    } catch (error: any) {
      toast.error(error.message || "좋아요 처리에 실패했습니다.");
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
};