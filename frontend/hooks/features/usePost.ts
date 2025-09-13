"use client";

import { useState, useEffect, useCallback, useMemo } from 'react';
import { useRouter } from "next/navigation";
import { postQuery, postCommand, commentQuery } from '@/lib/api';
import { useApiQuery } from '@/hooks/api/useApiQuery';
import { useApiMutation } from '@/hooks/api/useApiMutation';
import { usePagination } from '@/hooks/common/usePagination';
import { useDebounce } from '@/hooks/common/useDebounce';
import { useAuth } from '@/hooks';
import { toast } from "sonner";
import type { Post, SimplePost } from '@/types/domains/post';
import type { Comment } from '@/types/domains/comment';

interface CommentWithReplies extends Comment {
  replies?: CommentWithReplies[];
}

// ============ POST LIST HOOKS ============

// 게시글 목록 조회
export function usePostList(pageSize = 30) {
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'AUTHOR'>('TITLE');
  const debouncedSearchTerm = useDebounce(searchTerm, 500);
  
  const pagination = usePagination({ pageSize });

  const queryFn = useCallback(async () => {
    if (debouncedSearchTerm.trim()) {
      return await postQuery.search(
        searchType,
        debouncedSearchTerm.trim(),
        pagination.currentPage,
        pagination.pageSize
      );
    }
    return await postQuery.getAll(pagination.currentPage, pagination.pageSize);
  }, [debouncedSearchTerm, searchType, pagination.currentPage, pagination.pageSize]);

  const { data, isLoading, refetch } = useApiQuery(queryFn, {
    onSuccess: (response) => {
      if (response) {
        pagination.setTotalItems(response.totalElements || 0);
      }
    }
  });

  return {
    posts: data?.content || [],
    isLoading,
    refetch,
    pagination,
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    search: refetch
  };
}

// 인기 게시글 조회
export function usePopularPostsTabs() {
  const [activeTab, setActiveTab] = useState<'realtime' | 'weekly' | 'legend'>('realtime');
  
  const { data: popularData, refetch: refetchPopular } = useApiQuery(
    () => postQuery.getPopular(),
    {
      enabled: activeTab !== 'legend',
      cacheTime: 5 * 60 * 1000, // 5분 캐싱
      staleTime: 5 * 60 * 1000
    }
  );

  const { data: legendData, refetch: refetchLegend } = useApiQuery(
    () => postQuery.getLegend(0, 10),
    {
      enabled: activeTab === 'legend',
      cacheTime: 5 * 60 * 1000,
      staleTime: 5 * 60 * 1000
    }
  );

  const posts = useMemo(() => {
    if (activeTab === 'realtime') return popularData?.realtime || [];
    if (activeTab === 'weekly') return popularData?.weekly || [];
    if (activeTab === 'legend') return legendData?.content || [];
    return [];
  }, [activeTab, popularData, legendData]);

  return {
    posts,
    activeTab,
    setActiveTab,
    refetch: activeTab === 'legend' ? refetchLegend : refetchPopular
  };
}

// ============ POST DETAIL HOOKS ============

// 게시글 상세 조회 (간단한 버전)
export function usePostDetailQuery(postId: number) {
  return useApiQuery(() => postQuery.getById(postId), {
    enabled: postId > 0
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

  const buildCommentTree = useCallback((comments: Comment[]): CommentWithReplies[] => {
    const commentMap = new Map<number, CommentWithReplies>();
    const rootComments: CommentWithReplies[] = [];

    comments.forEach((comment) => {
      commentMap.set(comment.id, { ...comment, replies: [] });
    });

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

  // Permission checks
  const canModify = () => {
    if (!post || loading) return false;
    if (post.userName === "익명" || post.userName === null) {
      return !isAuthenticated;
    }
    return isAuthenticated && user?.userName === post.userName;
  };

  const isMyComment = (comment: Comment) => {
    return isAuthenticated && user?.userName === comment.userName;
  };

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

// ============ POST ACTION HOOKS ============

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
    ({ id, data }: { id: number; data: any }) => postCommand.update({ ...data, id } as Post),
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
  post: any,
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

  const handleUpdate = useCallback(async (data: any) => {
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