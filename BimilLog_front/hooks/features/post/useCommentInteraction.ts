"use client";

import { useState, useCallback } from "react";
import { useToast } from "@/hooks";
import type { PasswordModalMode } from "@/hooks/common/usePasswordModal";
import type { Post } from "@/types/domains/post";
import type { Comment } from "@/types/domains/comment";

// Server Action hooks의 함수 타입
interface CreateCommentFn {
  (
    data: { postId: number; content: string; parentId?: number; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ): void;
}

interface UpdateCommentFn {
  (
    data: { commentId: number; postId: number; content: string; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ): void;
}

interface DeleteCommentFn {
  (
    data: { commentId: number; postId: number; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ): void;
}

interface DeletePostFn {
  (
    data: { postId: number; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ): void;
}

interface LikePostFn {
  (postId: number): void;
}

interface LikeCommentFn {
  (commentId: number): void;
}

export interface UseCommentInteractionParams {
  postId: string;
  post: Post | null;
  isAuthenticated: boolean;
  canModify: () => boolean;
  canModifyComment: (comment: Comment) => boolean;
  openPasswordModal: (title: string, mode: PasswordModalMode, comment?: Comment) => void;
  resetPasswordModal: () => void;
  modalPassword: string;
  deleteMode: PasswordModalMode | null;
  targetComment: Comment | null;
  // Server Action hooks
  createComment: CreateCommentFn;
  updateComment: UpdateCommentFn;
  deleteComment: DeleteCommentFn;
  deletePost: DeletePostFn;
  likePost: LikePostFn;
  likeComment: LikeCommentFn;
}

export function useCommentInteraction({
  postId,
  post,
  isAuthenticated,
  canModify,
  canModifyComment,
  openPasswordModal,
  resetPasswordModal,
  modalPassword,
  deleteMode,
  targetComment,
  createComment,
  updateComment,
  deleteComment,
  deletePost,
  likePost,
  likeComment,
}: UseCommentInteractionParams) {
  const { showToast } = useToast();

  // 댓글 편집 및 답글 상태 관리
  const [editingComment, setEditingComment] = useState<Comment | null>(null);
  const [editContent, setEditContent] = useState("");
  const [editPassword, setEditPassword] = useState("");
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [replyPassword, setReplyPassword] = useState("");

  // 삭제 확인 모달 상태 관리
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [showCommentDeleteModal, setShowCommentDeleteModal] = useState(false);
  const [targetDeleteComment, setTargetDeleteComment] = useState<Comment | null>(null);

  // 비밀번호 모달 에러 상태
  const [passwordError, setPasswordError] = useState("");

  // 댓글 작성 핸들러
  const handleCommentSubmitForSection = useCallback((content: string, password: string) => {
    createComment({
      postId: Number(postId),
      content,
      password: password ? Number(password) : undefined,
    });
  }, [createComment, postId]);

  // 댓글 수정 상태 관리 - 수정 모드 진입 시 기존 내용을 대입
  const handleEditComment = useCallback((comment: Comment) => {
    setEditingComment(comment);
    setEditContent(comment.content); // 기존 내용을 편집 필드에 설정
    setEditPassword(""); // 비밀번호는 매번 새로 입력
  }, []);

  // 댓글 수정 완료 후 상태 초기화
  const handleUpdateComment = useCallback(async () => {
    if (!editingComment) return;

    // 내용 검증
    const trimmedContent = editContent.trim();
    if (!trimmedContent) {
      showToast({ type: 'error', message: '댓글 내용을 입력해주세요.' });
      return;
    }
    if (trimmedContent.length > 255) {
      showToast({ type: 'error', message: '댓글은 최대 255자까지 입력 가능합니다.' });
      return;
    }

    // 익명 댓글 수정 시 비밀번호 검증
    const isAnonymous = editingComment.memberName === "익명" || editingComment.memberName === null;
    if (isAnonymous) {
      if (!editPassword) {
        showToast({ type: 'error', message: '비밀번호를 입력해주세요.' });
        return;
      }
      const passwordNum = Number(editPassword);
      if (isNaN(passwordNum) || passwordNum < 1000 || passwordNum > 9999) {
        showToast({ type: 'error', message: '비밀번호는 4자리 숫자여야 합니다.' });
        return;
      }
    }

    updateComment(
      {
        commentId: editingComment.id,
        postId: Number(postId),
        content: trimmedContent,
        password: editPassword ? Number(editPassword) : undefined,
      },
      {
        onSuccess: () => {
          setEditingComment(null);
          setEditContent("");
          setEditPassword("");
        },
      }
    );
  }, [editingComment, editContent, editPassword, showToast, updateComment, postId]);

  const handleCancelEdit = useCallback(() => {
    // 내용이 변경되었으면 확인
    if (editingComment && editContent !== editingComment.content) {
      const confirmed = window.confirm("수정 중인 내용이 있습니다. 취소하시겠습니까?");
      if (!confirmed) {
        return;
      }
    }
    setEditingComment(null);
    setEditContent("");
    setEditPassword("");
  }, [editingComment, editContent]);

  // 댓글 답글 상태 관리 - 특정 댓글에 답글 작성 모드
  const handleReplyTo = useCallback((comment: Comment) => {
    setReplyingTo(comment); // 답글 대상 설정
    setReplyContent("");   // 답글 내용 초기화
    setReplyPassword(""); // 답글 비밀번호 초기화
  }, []);

  const handleCancelReply = useCallback(() => {
    setReplyingTo(null);
    setReplyContent("");
    setReplyPassword("");
  }, []);

  // 답글 작성 완료 - parentId로 replyingTo.id 전달
  const handleSubmitReply = useCallback(async () => {
    if (!replyingTo) return;

    createComment(
      {
        postId: Number(postId),
        content: replyContent,
        parentId: replyingTo.id,
        password: replyPassword ? Number(replyPassword) : undefined,
      },
      {
        onSuccess: () => {
          setReplyingTo(null);
          setReplyContent("");
          setReplyPassword("");
        },
      }
    );
  }, [replyingTo, replyContent, replyPassword, createComment, postId]);

  // 게시글 좋아요 핸들러
  const handleLikePost = useCallback(() => {
    if (!isAuthenticated) {
      showToast({ type: "warning", message: "로그인이 필요합니다." });
      return;
    }
    likePost(Number(postId));
  }, [isAuthenticated, showToast, likePost, postId]);

  // 게시글 삭제 클릭 핸들러
  const handleDeletePostClick = useCallback(() => {
    if (!canModify()) {
      showToast({ type: "error", message: "삭제 권한이 없습니다." });
      return;
    }

    if (post?.memberName === "익명" || post?.memberName === null) {
      // 익명 게시글의 경우 비밀번호 모달
      openPasswordModal("게시글 삭제", "post");
    } else {
      // 로그인 사용자의 경우 삭제 확인 모달
      setShowDeleteModal(true);
    }
  }, [canModify, post, showToast, openPasswordModal]);

  // 삭제 확인 후 실제 삭제 실행
  const handleConfirmDelete = useCallback(async () => {
    deletePost(
      { postId: Number(postId) },
      {
        onSuccess: () => {
          setShowDeleteModal(false);
        },
        onError: () => {
          setShowDeleteModal(false);
        },
      }
    );
  }, [deletePost, postId]);

  // 댓글 삭제 핸들러
  const handleDeleteComment = useCallback((comment: Comment) => {
    if (!canModifyComment(comment)) {
      showToast({ type: "error", message: "삭제 권한이 없습니다." });
      return;
    }

    if (comment.memberName === "익명" || comment.memberName === null) {
      // 익명 댓글의 경우 비밀번호 모달
      openPasswordModal("댓글 삭제", "comment", comment);
    } else {
      // 로그인 사용자 댓글: 확인 모달 표시
      setTargetDeleteComment(comment);
      setShowCommentDeleteModal(true);
    }
  }, [canModifyComment, showToast, openPasswordModal]);

  // 댓글 삭제 확인 후 실제 삭제 실행
  const handleConfirmCommentDelete = useCallback(async () => {
    if (!targetDeleteComment) return;

    deleteComment(
      {
        commentId: targetDeleteComment.id,
        postId: Number(postId),
      },
      {
        onSuccess: () => {
          setShowCommentDeleteModal(false);
          setTargetDeleteComment(null);
        },
        onError: () => {
          setShowCommentDeleteModal(false);
          setTargetDeleteComment(null);
        },
      }
    );
  }, [targetDeleteComment, deleteComment, postId]);

  // 댓글 좋아요 핸들러
  const handleLikeComment = useCallback((comment: Comment) => {
    if (!isAuthenticated) {
      showToast({ type: "warning", message: "로그인이 필요합니다." });
      return;
    }
    likeComment(comment.id);
  }, [isAuthenticated, showToast, likeComment]);

  // 비밀번호 모달 제출 - 게시글/댓글 삭제 모드에 따라 분기 처리
  const handlePasswordSubmit = useCallback(async () => {
    // 에러 초기화
    setPasswordError("");

    if (deleteMode === "post") {
      deletePost(
        {
          postId: Number(postId),
          password: modalPassword ? Number(modalPassword) : undefined,
        },
        {
          onSuccess: () => {
            resetPasswordModal();
            setPasswordError("");
          },
          onError: (error: string) => {
            setPasswordError(error || "비밀번호가 올바르지 않습니다.");
          },
        }
      );
    } else if (deleteMode === "comment" && targetComment) {
      deleteComment(
        {
          commentId: targetComment.id,
          postId: Number(postId),
          password: modalPassword ? Number(modalPassword) : undefined,
        },
        {
          onSuccess: () => {
            resetPasswordModal();
            setPasswordError("");
          },
          onError: (error: string) => {
            setPasswordError(error || "비밀번호가 올바르지 않습니다.");
          },
        }
      );
    }
  }, [deleteMode, modalPassword, targetComment, deletePost, deleteComment, postId, resetPasswordModal]);

  return {
    // 상태
    editingComment,
    editContent,
    editPassword,
    replyingTo,
    replyContent,
    replyPassword,
    showDeleteModal,
    showCommentDeleteModal,
    targetDeleteComment,
    passwordError,

    // 핸들러
    handleCommentSubmitForSection,
    handleEditComment,
    handleUpdateComment,
    handleCancelEdit,
    handleReplyTo,
    handleCancelReply,
    handleSubmitReply,
    handleLikePost,
    handleDeletePostClick,
    handleConfirmDelete,
    handleDeleteComment,
    handleConfirmCommentDelete,
    handleLikeComment,
    handlePasswordSubmit,

    // setter 래퍼
    setEditContent,
    setEditPassword,
    setReplyContent,
    setReplyPassword,
    setShowDeleteModal,
    setShowCommentDeleteModal,
    setTargetDeleteComment,
    setPasswordError,
  };
}
