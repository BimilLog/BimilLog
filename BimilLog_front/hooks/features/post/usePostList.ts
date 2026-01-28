"use client";

import { useState, useCallback, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { postQuery } from '@/lib/api';
import { usePagination } from '@/hooks/common/usePagination';
import { useDebounce } from '@/hooks/common/useDebounce';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { PageResponse } from '@/types/common';
import { SimplePost } from '@/types/domains/post';

// ============ POST LIST HOOKS ============

// 게시글 목록 조회 - TanStack Query 통합
export function usePostList(pageSize = 30, initialData?: PageResponse<SimplePost> | null) {
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'WRITER'>('TITLE');
  const debouncedSearchTerm = useDebounce(searchTerm, 500);

  // X 버튼 클릭 시 즉시 목록으로 복귀하기 위해 실제 searchTerm도 체크
  const actualSearch = searchTerm.trim();

  const pagination = usePagination({ pageSize });

  // TanStack Query로 게시글 목록/검색 통합 처리: 검색어가 있으면 검색, 없으면 일반 목록 조회
  // actualSearch가 비어있으면 디바운스 무시하고 즉시 일반 목록 조회
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: actualSearch && debouncedSearchTerm.trim()
      ? queryKeys.post.search(debouncedSearchTerm, pagination.currentPage)
      : queryKeys.post.list({ page: pagination.currentPage, size: pagination.pageSize }),
    queryFn: async () => {
      if (actualSearch && debouncedSearchTerm.trim()) {
        return await postQuery.search(
          searchType,
          debouncedSearchTerm.trim(),
          pagination.currentPage,
          pagination.pageSize
        );
      }
      return await postQuery.getAll(pagination.currentPage, pagination.pageSize);
    },
    // SSR에서 전달받은 초기 데이터 사용 (첫 페이지, 검색 아닐 때만)
    initialData: initialData && pagination.currentPage === 0 && !actualSearch
      ? { success: true, data: initialData }
      : undefined,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });

  // 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (data?.data?.totalElements !== undefined) {
      pagination.setTotalItems(data.data.totalElements);
    }
  }, [data?.data?.totalElements]);

  // 검색어 변경 시 페이지를 0으로 리셋
  useEffect(() => {
    if (debouncedSearchTerm.trim()) {
      pagination.setCurrentPage(0);
    }
  }, [debouncedSearchTerm]);

  // 즉시 검색 함수 (debounce 무시)
  const immediateSearch = useCallback(() => {
    pagination.setCurrentPage(0);
    refetch();
  }, [pagination, refetch]);

  return {
    posts: data?.data?.content || [],
    isLoading,
    error,
    refetch,
    pagination,
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    search: immediateSearch
  };
}

// 인기 게시글 조회 - TanStack Query 통합 (탭 데이터 캐싱 개선)
export function usePopularPostsTabs(initialRealtimeData?: PageResponse<SimplePost> | null) {
  const [activeTab, setActiveTab] = useState<'realtime' | 'weekly' | 'legend'>('realtime');

  // 각 탭별 페이지네이션
  const realtimePagination = usePagination({ pageSize: 5 });
  const weeklyPagination = usePagination({ pageSize: 10 });
  const legendPagination = usePagination({ pageSize: 10 });

  // 실시간 인기글 조회 - 페이징 적용
  const { data: realtimeData, isLoading: realtimeLoading, error: realtimeError } = useQuery({
    queryKey: queryKeys.post.realtimePopular({
      page: realtimePagination.currentPage,
      size: realtimePagination.pageSize
    }),
    queryFn: () => postQuery.getRealtimePosts(
      realtimePagination.currentPage,
      realtimePagination.pageSize
    ),
    // SSR에서 전달받은 초기 데이터 사용 (첫 페이지만)
    initialData: initialRealtimeData && realtimePagination.currentPage === 0
      ? { success: true, data: initialRealtimeData }
      : undefined,
    enabled: activeTab === 'realtime',
    placeholderData: (previousData) => previousData, // 탭 전환 시 이전 데이터 유지
    staleTime: 2 * 60 * 1000, // 2분 (실시간성 강화)
    gcTime: 10 * 60 * 1000,
  });

  // 주간 인기글 조회 - 페이징 적용
  const { data: weeklyData, isLoading: weeklyLoading, error: weeklyError } = useQuery({
    queryKey: queryKeys.post.weeklyPopular({
      page: weeklyPagination.currentPage,
      size: weeklyPagination.pageSize
    }),
    queryFn: () => postQuery.getWeeklyPosts(
      weeklyPagination.currentPage,
      weeklyPagination.pageSize
    ),
    enabled: activeTab === 'weekly',
    placeholderData: (previousData) => previousData, // 탭 전환 시 이전 데이터 유지
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000,
  });

  // 레전드 글 조회 (페이징 지원) - 이전 데이터 유지
  const { data: legendData, isLoading: legendLoading, error: legendError } = useQuery({
    queryKey: queryKeys.post.legend({
      page: legendPagination.currentPage,
      size: legendPagination.pageSize
    }),
    queryFn: () => postQuery.getLegend(legendPagination.currentPage, legendPagination.pageSize),
    enabled: activeTab === 'legend',
    placeholderData: (previousData) => previousData, // 탭 전환 시 이전 데이터 유지
    staleTime: 10 * 60 * 1000, // 10분 (레전드는 자주 변경되지 않음)
    gcTime: 10 * 60 * 1000,
  });

  // 실시간 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (realtimeData?.data?.totalElements !== undefined) {
      realtimePagination.setTotalItems(realtimeData.data.totalElements);
    }
  }, [realtimeData?.data?.totalElements, realtimePagination.setTotalItems]);

  // 주간 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (weeklyData?.data?.totalElements !== undefined) {
      weeklyPagination.setTotalItems(weeklyData.data.totalElements);
    }
  }, [weeklyData?.data?.totalElements, weeklyPagination.setTotalItems]);

  // 레전드 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (legendData?.data?.totalElements !== undefined) {
      legendPagination.setTotalItems(legendData.data.totalElements);
    }
  }, [legendData?.data?.totalElements, legendPagination.setTotalItems]);

  // 각 탭의 실제 데이터 반환 (Page 응답의 content 사용)
  const realtimePosts = useMemo(() => realtimeData?.data?.content || [], [realtimeData]);
  const weeklyPosts = useMemo(() => weeklyData?.data?.content || [], [weeklyData]);
  const legendPosts = useMemo(() => legendData?.data?.content || [], [legendData]);

  // 현재 활성 탭의 로딩/에러 상태 반환
  const isLoading = useMemo(() => {
    if (activeTab === 'realtime') return realtimeLoading;
    if (activeTab === 'weekly') return weeklyLoading;
    if (activeTab === 'legend') return legendLoading;
    return false;
  }, [activeTab, realtimeLoading, weeklyLoading, legendLoading]);

  const error = useMemo(() => {
    if (activeTab === 'realtime') return realtimeError;
    if (activeTab === 'weekly') return weeklyError;
    if (activeTab === 'legend') return legendError;
    return null;
  }, [activeTab, realtimeError, weeklyError, legendError]);

  // 현재 활성 탭의 페이지네이션 반환
  const currentPagination = useMemo(() => {
    if (activeTab === 'realtime') return realtimePagination;
    if (activeTab === 'weekly') return weeklyPagination;
    if (activeTab === 'legend') return legendPagination;
    return null;
  }, [activeTab, realtimePagination, weeklyPagination, legendPagination]);

  return {
    realtimePosts,
    weeklyPosts,
    legendPosts,
    activeTab,
    setActiveTab,
    realtimePagination: activeTab === 'realtime' ? realtimePagination : null,
    weeklyPagination: activeTab === 'weekly' ? weeklyPagination : null,
    legendPagination: activeTab === 'legend' ? legendPagination : null,
    currentPagination,
    isLoading,
    error,
  };
}

// 공지사항 조회 - 페이징 적용
export function useNoticePosts(enabled = true, initialData?: PageResponse<SimplePost> | null) {
  const pagination = usePagination({ pageSize: 10 });

  const { data, isLoading, refetch } = useQuery({
    queryKey: queryKeys.post.notices({
      page: pagination.currentPage,
      size: pagination.pageSize
    }),
    queryFn: () => postQuery.getNoticePosts(
      pagination.currentPage,
      pagination.pageSize
    ),
    // SSR에서 전달받은 초기 데이터 사용 (첫 페이지만)
    initialData: initialData && pagination.currentPage === 0
      ? { success: true, data: initialData }
      : undefined,
    enabled, // 조건부 조회 (기본값: true)
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });

  // 데이터 변경 시 페이지네이션 업데이트
  useEffect(() => {
    if (data?.data?.totalElements !== undefined) {
      pagination.setTotalItems(data.data.totalElements);
    }
  }, [data?.data?.totalElements, pagination.setTotalItems]);

  return {
    noticePosts: data?.data?.content || [],
    pagination,
    isLoading,
    refetch,
  };
}