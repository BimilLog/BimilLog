import { useState, useCallback } from "react";
import { useAuth, useToast } from "@/hooks";
import { commentCommandApi, type Comment } from "@/lib/api";
import { validatePassword } from "@/lib/utils";

export const useCommentActions = (
  postId: string,
  onRefresh: () => Promise<void>
) => {
  const { user, isAuthenticated } = useAuth();
  const { showSuccess, showError, showWarning } = useToast();

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
  const handleCommentSubmit = useCallback(async (comment: string, password: string) => {
    if (!comment.trim()) {
      showWarning("입력 확인", "댓글 내용을 입력해주세요.");
      return;
    }

    setIsSubmittingComment(true);
    try {
      const validatedPassword = validatePassword(password, isAuthenticated);
      
      await commentCommandApi.createComment({
        postId: Number(postId),
        content: comment,
        password: validatedPassword,
      });
      await onRefresh();
    } catch (error) {
      if (error instanceof Error) {
        showError("작성 실패", error.message);
      } else {
        showError("작성 실패", "댓글 작성 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmittingComment(false);
    }
  }, [newComment, commentPassword, postId, isAuthenticated, onRefresh]);

  // 답글 작성
  const handleReplySubmit = useCallback(async () => {
    if (!replyContent.trim()) {
      showWarning("입력 확인", "답글 내용을 입력해주세요.");
      return;
    }
    if (!replyingTo) return;

    setIsSubmittingReply(true);
    try {
      const validatedPassword = validatePassword(replyPassword, isAuthenticated);
      
      await commentCommandApi.createComment({
        postId: Number(postId),
        content: replyContent,
        parentId: replyingTo.id,
        password: validatedPassword,
      });

      setReplyContent("");
      setReplyPassword("");
      setReplyingTo(null);
      await onRefresh();
    } catch (error) {
      if (error instanceof Error) {
        showError("답글 작성 실패", error.message);
      } else {
        showError("답글 작성 실패", "답글 작성 중 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmittingReply(false);
    }
  }, [replyContent, replyingTo, replyPassword, postId, isAuthenticated, onRefresh]);

  // 답글 취소
  const handleCancelReply = useCallback(() => {
    setReplyingTo(null);
    setReplyContent("");
    setReplyPassword("");
  }, []);

  // 댓글 추천
  const handleLikeComment = useCallback(async (comment: Comment) => {
    try {
      await commentCommandApi.likeComment(comment.id);
      await onRefresh();
    } catch (error) {
      showError("추천 실패", "댓글 추천 중 오류가 발생했습니다.");
    }
  }, [onRefresh]);

  // 댓글 수정 시작
  const handleEditComment = useCallback((comment: Comment) => {
    setEditingComment(comment);
    setEditContent(comment.content);
    setEditPassword("");
  }, []);

  // 댓글 수정 취소
  const handleCancelEdit = useCallback(() => {
    setEditingComment(null);
    setEditContent("");
    setEditPassword("");
  }, []);

  // 댓글 수정 완료
  const handleUpdateComment = useCallback(async () => {
    if (!editingComment) return;
    if (!editContent.trim()) {
      showWarning("입력 확인", "수정할 내용을 입력해주세요.");
      return;
    }

    const isMyComment = isAuthenticated && user?.userName === editingComment.userName;

    try {
      let validatedPassword: number | undefined = undefined;
      
      // 내 댓글이 아닌 경우에만 패스워드 validation 수행
      if (!isMyComment) {
        try {
          validatedPassword = validatePassword(editPassword, false);
        } catch (error) {
          if (error instanceof Error) {
            showWarning("입력 확인", error.message);
          }
          return;
        }
      }

      await commentCommandApi.updateComment(editingComment.id, {
        content: editContent,
        password: validatedPassword,
      });

      setEditingComment(null);
      setEditContent("");
      setEditPassword("");
      await onRefresh();
    } catch (error) {
      showError("수정 실패", "댓글 수정 중 오류가 발생했습니다. 비밀번호를 확인해주세요.");
    }
  }, [editingComment, editContent, editPassword, isAuthenticated, user?.userName, onRefresh]);

  // 댓글로 스크롤
  const scrollToComment = useCallback((commentId: number) => {
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
  }, []);

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