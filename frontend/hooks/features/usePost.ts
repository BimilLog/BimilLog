import { useState, useCallback, useMemo } from 'react';
import { postQuery, postCommand } from '@/lib/api';
import { useApiQuery } from '@/hooks/api/useApiQuery';
import { useApiMutation } from '@/hooks/api/useApiMutation';
import { usePagination } from '@/hooks/common/usePagination';
import { useDebounce } from '@/hooks/common/useDebounce';
import type { Post, SimplePost } from '@/types/domains/post';

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

// 게시글 상세 조회 (간단한 버전)
export function usePostDetailQuery(postId: number) {
  return useApiQuery(() => postQuery.getById(postId), {
    enabled: postId > 0
  });
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
    ({ id, data }: { id: number; data: any }) => postCommand.update(id, data),
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

// 게시글 액션 통합 Hook
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