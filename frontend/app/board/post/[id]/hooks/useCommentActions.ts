import { useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { commentApi, type Comment } from "@/lib/api";

export const useCommentActions = (
  postId: string,
  onRefresh: () => Promise<void>
) => {
  const { user, isAuthenticated } = useAuth();

  // 댓글 작성 상태
  const [newComment, setNewComment] = useState("");
  const [commentPassword, setCommentPassword] = useState("");
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);

  // 답글 작성 상태
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [replyPassword, setReplyPassword] = useState("");
  const [isSubmittingReply, setIsSubmittingReply] = useState(false);

  // 댓글 수정 상태
  const [editingComment, setEditingComment] = useState<Comment | null>(null);
  const [editContent, setEditContent] = useState("");
  const [editPassword, setEditPassword] = useState("");

  // 댓글 작성
  const handleCommentSubmit = async () => {
    if (!newComment.trim()) return;
    if (!isAuthenticated && !commentPassword.trim()) return;

    setIsSubmittingComment(true);
    try {
      await commentApi.createComment({
        postId: Number(postId),
        userName: isAuthenticated ? user?.userName || "" : "익명",
        content: newComment,
        password: isAuthenticated ? undefined : Number(commentPassword),
      });

      setNewComment("");
      setCommentPassword("");
      await onRefresh();
    } catch (error) {
      console.error("댓글 작성 실패:", error);
    } finally {
      setIsSubmittingComment(false);
    }
  };

  // 답글 작성
  const handleReplySubmit = async () => {
    if (!replyContent.trim() || !replyingTo) return;
    if (!isAuthenticated && !replyPassword.trim()) return;

    setIsSubmittingReply(true);
    try {
      await commentApi.createComment({
        postId: Number(postId),
        userName: isAuthenticated ? user?.userName || "" : "익명",
        content: replyContent,
        parentId: replyingTo.id,
        password: isAuthenticated ? undefined : Number(replyPassword),
      });

      setReplyContent("");
      setReplyPassword("");
      setReplyingTo(null);
      await onRefresh();
    } catch (error) {
      console.error("답글 작성 실패:", error);
    } finally {
      setIsSubmittingReply(false);
    }
  };

  // 답글 취소
  const handleCancelReply = () => {
    setReplyingTo(null);
    setReplyContent("");
    setReplyPassword("");
  };

  // 댓글 좋아요
  const handleLikeComment = async (comment: Comment) => {
    try {
      await commentApi.likeComment(comment.id);
      await onRefresh();
    } catch (error) {
      console.error("댓글 추천 실패:", error);
    }
  };

  // 댓글 수정 시작
  const handleEditComment = (comment: Comment) => {
    setEditingComment(comment);
    setEditContent(comment.content);
    setEditPassword("");
  };

  // 댓글 수정 취소
  const handleCancelEdit = () => {
    setEditingComment(null);
    setEditContent("");
    setEditPassword("");
  };

  // 댓글 수정 완료
  const handleUpdateComment = async () => {
    if (!editingComment || !editContent.trim()) return;

    const isMyComment = isAuthenticated && user?.userName === editingComment.userName;

    try {
      const response = isMyComment
        ? await commentApi.updateComment(editingComment.id, {
            content: editContent,
          })
        : editPassword.trim()
        ? await commentApi.updateComment(editingComment.id, {
            content: editContent,
            password: editPassword,
          })
        : null;

      if (!response) return;

      if (response.success) {
        setEditingComment(null);
        setEditContent("");
        setEditPassword("");
        await onRefresh();
      } else {
        if (
          response.error &&
          response.error.includes("댓글 비밀번호가 일치하지 않습니다")
        ) {
          alert("비밀번호가 일치하지 않습니다.");
        } else {
          alert(response.error || "댓글 수정에 실패했습니다.");
        }
      }
    } catch (error) {
      console.error("댓글 수정 실패:", error);
      if (error instanceof Error && error.message.includes("403")) {
        alert("비밀번호가 일치하지 않습니다.");
      } else {
        alert("댓글 수정 중 오류가 발생했습니다.");
      }
    }
  };

  // 댓글로 스크롤
  const scrollToComment = (commentId: number) => {
    const element = document.getElementById(`comment-${commentId}`);
    if (element) {
      element.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });

      const commentContent = element.querySelector(".comment-content");
      if (commentContent) {
        commentContent.classList.add("bg-yellow-200");
        setTimeout(() => {
          commentContent.classList.remove("bg-yellow-200");
        }, 2000);
      }
    }
  };

  return {
    // 댓글 작성 상태
    newComment,
    commentPassword,
    isSubmittingComment,
    setNewComment,
    setCommentPassword,
    handleCommentSubmit,

    // 답글 상태
    replyingTo,
    replyContent,
    replyPassword,
    isSubmittingReply,
    setReplyingTo,
    setReplyContent,
    setReplyPassword,
    handleReplySubmit,
    handleCancelReply,

    // 댓글 수정 상태
    editingComment,
    editContent,
    editPassword,
    setEditContent,
    setEditPassword,
    handleEditComment,
    handleCancelEdit,
    handleUpdateComment,

    // 기타 액션
    handleLikeComment,
    scrollToComment,
  };
}; 