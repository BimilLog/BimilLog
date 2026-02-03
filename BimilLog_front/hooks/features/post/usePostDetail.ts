"use client";

import { useEffect } from 'react';
import { useRouter } from "next/navigation";
import { useQuery } from '@tanstack/react-query';
import { postQuery } from '@/lib/api';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { useAuth, useToast, usePasswordModal } from '@/hooks';
import { useCommentsQuery, type CommentWithReplies } from '@/hooks/api/useCommentQueries';
import type { Post } from '@/types/domains/post';
import type { Comment } from '@/types/domains/comment';

// 게시글 상세 조회 (간단한 버전) - TanStack Query 통합
export function usePostDetailQuery(postId: number, initialPost?: Post) {
  return useQuery({
    queryKey: queryKeys.post.detail(postId),
    queryFn: () => postQuery.getById(postId),
    enabled: postId > 0,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    initialData: initialPost ? { success: true, data: initialPost } : undefined,
  });
}

// 게시글 상세 페이지 전체 로직
export function usePostDetail(id: string | null, initialPost?: Post) {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const { showError } = useToast();

  // TanStack Query - 게시글 상세 조회
  const postId = Number(id) || 0;
  const { data: postData, isLoading, error } = usePostDetailQuery(postId, initialPost);
  const post = postData?.data || null;

  // TanStack Query - 댓글 목록 조회 (BFF 통합: 인기댓글 + 일반댓글)
  const {
    comments,
    popularComments,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useCommentsQuery(postId);

  // usePasswordModal 훅으로 모달 상태 관리 통합
  const passwordModal = usePasswordModal();

  // 게시글 조회 에러 처리
  useEffect(() => {
    if (error && !isLoading) {
      showError(
        "게시글 조회 실패",
        "게시글을 불러오는 중 오류가 발생했습니다."
      );
      router.push("/board");
    }
  }, [error, isLoading, showError, router]);

  // 댓글 더보기 핸들러
  const loadMoreComments = () => {
    if (hasNextPage && !isFetchingNextPage) {
      fetchNextPage();
    }
  };

  const getTotalCommentCount = (
    comments: (Comment & { replies?: Comment[] })[]
  ): number => {
    let count = 0;
    comments.forEach((comment) => {
      count += 1;
      if (comment.replies) {
        count += getTotalCommentCount(comment.replies);
      }
    });
    return count;
  };

  // 루트 댓글 수만 계산 (답글 제외)
  const getRootCommentCount = (
    comments: (Comment & { replies?: Comment[] })[]
  ): number => {
    return comments.length;
  };

  // 권한 체크: 익명 게시글은 비로그인 사용자만 수정 가능, 로그인 게시글은 작성자만 수정 가능
  const canModify = () => {
    if (!post || isLoading) return false;
    if (post.memberName === "익명" || post.memberName === null) {
      return !isAuthenticated;  // 익명 게시글은 로그인하지 않은 사용자만 수정 가능
    }
    return isAuthenticated && user?.memberName === post.memberName;  // 로그인 게시글은 작성자만 수정 가능
  };

  const isMyComment = (comment: Comment) => {
    return isAuthenticated && user?.memberName === comment.memberName;
  };

  // 댓글 권한 체크: 익명 댓글은 비로그인 사용자만, 로그인 댓글은 작성자만 수정 가능
  const canModifyComment = (comment: Comment) => {
    if (comment.memberName === "익명" || comment.memberName === null) {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.memberName === comment.memberName;
  };

  // 게시글 조회 에러만 처리 (댓글은 useCommentsQuery에서 자동 관리)

  return {
    // Data
    post,
    comments,
    popularComments,
    loading: isLoading,

    // Pagination state
    hasMoreComments: hasNextPage,
    isLoadingMore: isFetchingNextPage,

    // Modal state (새로운 API + Legacy 호환성)
    showPasswordModal: passwordModal.showPasswordModal,
    modalPassword: passwordModal.modalPassword,
    passwordModalTitle: passwordModal.passwordModalTitle,
    deleteMode: passwordModal.deleteMode,
    targetComment: passwordModal.targetComment,

    // Actions
    loadMoreComments,
    getTotalCommentCount,
    getRootCommentCount,
    canModify,
    isMyComment,
    canModifyComment,

    // Modal actions
    openPasswordModal: passwordModal.openModal,
    closePasswordModal: passwordModal.closeModal,
    resetPasswordModal: passwordModal.resetModal,
    setModalPassword: passwordModal.setModalPassword,
  };
}
