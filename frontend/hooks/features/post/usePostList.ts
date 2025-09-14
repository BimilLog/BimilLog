"use client";

import { useState, useCallback, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { postQuery } from '@/lib/api';
import { usePagination } from '@/hooks/common/usePagination';
import { useDebounce } from '@/hooks/common/useDebounce';
import { queryKeys } from '@/lib/tanstack-query/keys';

// ============ POST LIST HOOKS ============

// 게시글 목록 조회 - TanStack Query 통합
export function usePostList(pageSize = 30) {
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'AUTHOR'>('TITLE');
  const debouncedSearchTerm = useDebounce(searchTerm, 500);

  const pagination = usePagination({ pageSize });

  // TanStack Query로 게시글 목록/검색 통합 처리: 검색어가 있으면 검색, 없으면 일반 목록 조회
  const { data, isLoading, refetch } = useQuery({
    queryKey: debouncedSearchTerm.trim()
      ? queryKeys.post.search(debouncedSearchTerm, pagination.currentPage)
      : queryKeys.post.list({ page: pagination.currentPage, size: pagination.pageSize }),
    queryFn: async () => {
      if (debouncedSearchTerm.trim()) {
        return await postQuery.search(
          searchType,
          debouncedSearchTerm.trim(),
          pagination.currentPage,
          pagination.pageSize
        );
      }
      return await postQuery.getAll(pagination.currentPage, pagination.pageSize);
    },
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });

  // 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (data?.data?.totalElements !== undefined) {
      pagination.setTotalItems(data.data.totalElements);
    }
  }, [data?.data?.totalElements]);

  return {
    posts: data?.data?.content || [],
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

// 인기 게시글 조회 - TanStack Query 통합
export function usePopularPostsTabs() {
  const [activeTab, setActiveTab] = useState<'realtime' | 'weekly' | 'legend'>('realtime');

  const { data: popularData, refetch: refetchPopular } = useQuery({
    queryKey: queryKeys.post.popular(),
    queryFn: () => postQuery.getPopular(),
    enabled: activeTab !== 'legend',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  const { data: legendData, refetch: refetchLegend } = useQuery({
    queryKey: queryKeys.post.legend(),
    queryFn: () => postQuery.getLegend(0, 10),
    enabled: activeTab === 'legend',
    staleTime: 30 * 60 * 1000,
    gcTime: 60 * 60 * 1000,
  });

  // 현재 활성 탭에 따라 표시할 게시글 데이터 선택
  const posts = useMemo(() => {
    if (activeTab === 'realtime') return popularData?.data?.realtime || [];
    if (activeTab === 'weekly') return popularData?.data?.weekly || [];
    if (activeTab === 'legend') return legendData?.data?.content || [];
    return [];
  }, [activeTab, popularData, legendData]);

  return {
    posts,
    activeTab,
    setActiveTab,
    refetch: activeTab === 'legend' ? refetchLegend : refetchPopular
  };
}