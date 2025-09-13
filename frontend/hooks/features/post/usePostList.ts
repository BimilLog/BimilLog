"use client";

import { useState, useCallback, useMemo } from 'react';
import { postQuery } from '@/lib/api';
import { useApiQuery } from '@/hooks/api/useApiQuery';
import { usePagination } from '@/hooks/common/usePagination';
import { useDebounce } from '@/hooks/common/useDebounce';

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