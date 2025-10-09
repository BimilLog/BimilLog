"use client";

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from "next/navigation";
import { useQuery } from '@tanstack/react-query';
import { postQuery, commentQuery } from '@/lib/api';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { useAuth, useToast, usePasswordModal } from '@/hooks';
import { usePopularComments } from '@/hooks/api/useCommentQueries';
import type { Post } from '@/types/domains/post';
import type { Comment } from '@/types/domains/comment';

export interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

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

  // TanStack Query - 인기 댓글 조회
  const { data: popularCommentsData } = usePopularComments(postId);
  const popularComments = popularCommentsData?.data || [];

  // Comment 상태
  const [comments, setComments] = useState<CommentWithReplies[]>([]);

  // Pagination 상태
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMoreComments, setHasMoreComments] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

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

  // 트리 구조를 평면 배열로 변환: 페이지네이션 시 기존 댓글 보존을 위해 필요
  const flattenComments = useCallback((comments: CommentWithReplies[]): Comment[] => {
    const result: Comment[] = [];

    const flatten = (comment: CommentWithReplies) => {
      result.push(comment);
      if (comment.replies && comment.replies.length > 0) {
        comment.replies.forEach(flatten);
      }
    };

    comments.forEach(flatten);
    return result;
  }, []);

  // 댓글 트리 구조 빌드: parentId를 기반으로 평면 배열을 계층 구조로 변환
  const buildCommentTree = useCallback((comments: Comment[]): CommentWithReplies[] => {
    const commentMap = new Map<number, CommentWithReplies>();
    const rootComments: CommentWithReplies[] = [];

    // 모든 댓글을 Map에 저장하여 빠른 조회가 가능하도록 함
    comments.forEach((comment) => {
      commentMap.set(comment.id, { ...comment, replies: [] });
    });

    // 부모-자식 관계 설정: parentId가 있으면 해당 부모의 replies에 추가
    comments.forEach((comment) => {
      // 클로저 테이블에서 루트 댓글은 parentId가 자기 자신을 가리키거나 null
      if (!comment.parentId || comment.parentId === comment.id) {
        // parentId가 없거나 자기 자신을 가리키는 경우만 루트로 처리
        rootComments.push(commentMap.get(comment.id)!);
      } else if (commentMap.has(comment.parentId)) {
        // parentId가 존재하고 부모 댓글이 Map에 있는 경우 자식으로 추가
        const parent = commentMap.get(comment.parentId)!;
        const child = commentMap.get(comment.id)!;
        parent.replies!.push(child);
      }
    });

    return rootComments;
  }, []);

  const fetchComments = useCallback(async () => {
    if (!id) return;

    try {
      const commentsResponse = await commentQuery.getByPostId(Number(id), 0);

      if (commentsResponse.success && commentsResponse.data) {
        const treeComments = buildCommentTree(commentsResponse.data.content);
        setComments(treeComments);

        // 페이지네이션 상태 초기화 - PageResponse.last 활용
        setCurrentPage(0);
        setHasMoreComments(!commentsResponse.data.last);
      } else {
        setComments([]);
        setHasMoreComments(false);
      }
    } catch (error) {
      setComments([]);
      setHasMoreComments(false);
      showError("댓글 로딩 실패", "잠시 후 다시 시도해주세요.");
    }
  }, [id, buildCommentTree, showError]);

  const loadMoreComments = useCallback(async () => {
    if (!id || isLoadingMore || !hasMoreComments) return;

    setIsLoadingMore(true);
    const nextPage = currentPage + 1;

    try {
      const commentsResponse = await commentQuery.getByPostId(Number(id), nextPage);

      if (commentsResponse.success && commentsResponse.data) {
        const newComments = commentsResponse.data.content;

        if (newComments.length > 0) {
          // 1. 기존 트리를 평면 배열로 변환 (replies 보존)
          const existingFlat = flattenComments(comments);

          // 2. 기존 + 새 댓글 합치기 (중복 제거)
          const allComments = [...existingFlat, ...newComments].reduce((acc, comment) => {
            const exists = acc.find((c) => c.id === comment.id);
            if (!exists) acc.push(comment);
            return acc;
          }, [] as Comment[]);

          // 3. 평면 배열을 트리 구조로 재구성
          const treeComments = buildCommentTree(allComments);
          setComments(treeComments);
          setCurrentPage(nextPage);

          // PageResponse.last 속성으로 종료 조건 판단
          setHasMoreComments(!commentsResponse.data.last);
        } else {
          setHasMoreComments(false);
        }
      } else {
        setHasMoreComments(false);
      }
    } catch (error) {
      setHasMoreComments(false);
      showError("댓글 로딩 실패", "잠시 후 다시 시도해주세요.");
    } finally {
      setIsLoadingMore(false);
    }
  }, [id, currentPage, hasMoreComments, isLoadingMore, comments, flattenComments, buildCommentTree, showError]);

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

  // Effects
  useEffect(() => {
    if (!id) return;
    fetchComments();
  }, [id, fetchComments]);

  return {
    // Data
    post,
    comments,
    popularComments,
    loading: isLoading,

    // Pagination state
    hasMoreComments,
    isLoadingMore,

    // Modal state (새로운 API + Legacy 호환성)
    showPasswordModal: passwordModal.showPasswordModal,
    modalPassword: passwordModal.modalPassword,
    passwordModalTitle: passwordModal.passwordModalTitle,
    deleteMode: passwordModal.deleteMode,
    targetComment: passwordModal.targetComment,

    // Actions
    fetchComments,
    loadMoreComments,
    getTotalCommentCount,
    canModify,
    isMyComment,
    canModifyComment,

    // Modal actions - 새로운 API
    openPasswordModal: passwordModal.openModal,
    closePasswordModal: passwordModal.closeModal,
    resetPasswordModal: passwordModal.resetModal,
    setModalPassword: passwordModal.setModalPassword,

    // Legacy compatibility - 기존 코드와의 호환성을 위해 유지
    setShowPasswordModal: passwordModal.setShowPasswordModal,
    setPasswordModalTitle: passwordModal.setPasswordModalTitle,
    setDeleteMode: passwordModal.setDeleteMode,
    setTargetComment: passwordModal.setTargetComment,
  };
}