"use client";

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from "next/navigation";
import { useQuery } from '@tanstack/react-query';
import { postQuery, commentQuery } from '@/lib/api';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { useAuth } from '@/hooks';
import type { Post } from '@/types/domains/post';
import type { Comment } from '@/types/domains/comment';

export interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

// 게시글 상세 조회 (간단한 버전) - TanStack Query 통합
export function usePostDetailQuery(postId: number) {
  return useQuery({
    queryKey: queryKeys.post.detail(postId),
    queryFn: () => postQuery.getById(postId),
    enabled: postId > 0,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
}

// 게시글 상세 페이지 전체 로직
export function usePostDetail(id: string | null, initialPost?: Post) {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();

  // Post 상태
  const [post, setPost] = useState<Post | null>(initialPost || null);
  const [loading, setLoading] = useState(!initialPost);

  // Comment 상태
  const [comments, setComments] = useState<CommentWithReplies[]>([]);
  const [popularComments, setPopularComments] = useState<Comment[]>([]);

  // Modal 상태
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [modalPassword, setModalPassword] = useState("");
  const [passwordModalTitle, setPasswordModalTitle] = useState("");
  const [deleteMode, setDeleteMode] = useState<"post" | "comment" | null>(null);
  const [targetComment, setTargetComment] = useState<Comment | null>(null);

  // Data fetching
  const fetchPost = useCallback(async () => {
    if (!id) return;

    try {
      const response = await postQuery.getById(Number(id));
      if (response.success && response.data) {
        setPost(response.data);
      } else {
        throw new Error("게시글을 불러올 수 없습니다");
      }
    } catch (error) {
      router.push("/board");
    } finally {
      setLoading(false);
    }
  }, [id, router]);

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
      if (comment.parentId && commentMap.has(comment.parentId)) {
        const parent = commentMap.get(comment.parentId)!;
        const child = commentMap.get(comment.id)!;
        parent.replies!.push(child);
      } else {
        rootComments.push(commentMap.get(comment.id)!);
      }
    });

    return rootComments;
  }, []);

  const fetchComments = useCallback(async () => {
    if (!id) return;

    try {
      const [commentsResponse, popularResponse] = await Promise.all([
        commentQuery.getByPostId(Number(id), 0),
        commentQuery.getPopular(Number(id)),
      ]);

      if (commentsResponse.success && commentsResponse.data) {
        const treeComments = buildCommentTree(commentsResponse.data.content);
        setComments(treeComments);
      } else {
        setComments([]);
      }

      if (popularResponse.success && popularResponse.data) {
        setPopularComments(popularResponse.data);
      } else {
        setPopularComments([]);
      }
    } catch (error) {
      setComments([]);
      setPopularComments([]);
    }
  }, [id, buildCommentTree]);

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
    if (!post || loading) return false;
    if (post.userName === "익명" || post.userName === null) {
      return !isAuthenticated;  // 익명 게시글은 로그인하지 않은 사용자만 수정 가능
    }
    return isAuthenticated && user?.userName === post.userName;  // 로그인 게시글은 작성자만 수정 가능
  };

  const isMyComment = (comment: Comment) => {
    return isAuthenticated && user?.userName === comment.userName;
  };

  // 댓글 권한 체크: 익명 댓글은 비로그인 사용자만, 로그인 댓글은 작성자만 수정 가능
  const canModifyComment = (comment: Comment) => {
    if (comment.userName === "익명" || comment.userName === null) {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.userName === comment.userName;
  };

  // Effects
  useEffect(() => {
    if (!id) return;
    if (!initialPost) {
      fetchPost();
    }
    fetchComments();
  }, [id, initialPost, fetchPost, fetchComments]);

  return {
    // Data
    post,
    comments,
    popularComments,
    loading,

    // Modal state
    showPasswordModal,
    modalPassword,
    passwordModalTitle,
    deleteMode,
    targetComment,

    // Actions
    fetchPost,
    fetchComments,
    getTotalCommentCount,
    canModify,
    isMyComment,
    canModifyComment,

    // Modal actions
    setShowPasswordModal,
    setModalPassword,
    setPasswordModalTitle,
    setDeleteMode,
    setTargetComment,
  };
}