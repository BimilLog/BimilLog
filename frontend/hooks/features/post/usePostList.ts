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
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'WRITER'>('TITLE');
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

  // 레전드 탭용 페이지네이션
  const legendPagination = usePagination({ pageSize: 10 });

  // 실시간 인기글 조회
  const { data: realtimeData, refetch: refetchRealtime } = useQuery({
    queryKey: queryKeys.post.realtimePopular(),
    queryFn: postQuery.getRealtimePosts,
    enabled: activeTab === 'realtime',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 주간 인기글 조회
  const { data: weeklyData, refetch: refetchWeekly } = useQuery({
    queryKey: queryKeys.post.weeklyPopular(),
    queryFn: postQuery.getWeeklyPosts,
    enabled: activeTab === 'weekly',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 레전드 글 조회 (페이징 지원)
  const { data: legendData, refetch: refetchLegend } = useQuery({
    queryKey: queryKeys.post.legend({
      page: legendPagination.currentPage,
      size: legendPagination.pageSize
    }),
    queryFn: () => postQuery.getLegend(legendPagination.currentPage, legendPagination.pageSize),
    enabled: activeTab === 'legend',
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 레전드 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (legendData?.data?.totalElements !== undefined) {
      legendPagination.setTotalItems(legendData.data.totalElements);
    }
  }, [legendData?.data?.totalElements]);

  // 현재 활성 탭에 따라 표시할 게시글 데이터 선택
  const posts = useMemo(() => {
    if (activeTab === 'realtime') return realtimeData?.data || [];
    if (activeTab === 'weekly') return weeklyData?.data || [];
    if (activeTab === 'legend') return legendData?.data?.content || [];
    return [];
  }, [activeTab, realtimeData, weeklyData, legendData]);

  // 현재 탭에 맞는 refetch 함수 선택
  const refetch = useCallback(() => {
    if (activeTab === 'realtime') return refetchRealtime();
    if (activeTab === 'weekly') return refetchWeekly();
    return refetchLegend();
  }, [activeTab, refetchRealtime, refetchWeekly, refetchLegend]);

  return {
    posts,
    activeTab,
    setActiveTab,
    refetch,
    // 레전드 탭 전용 페이지네이션
    legendPagination: activeTab === 'legend' ? legendPagination : null
  };
}

// 공지사항 조회
export function useNoticePosts(enabled = true) {
  const { data, isLoading, refetch } = useQuery({
    queryKey: queryKeys.post.notices(),
    queryFn: postQuery.getNoticePosts,
    enabled, // 조건부 조회 (기본값: true)
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });

  return {
    noticePosts: data?.data || [],
    isLoading,
    refetch,
  };
}