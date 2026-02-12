"use client";

import { useState, useCallback, useEffect, useMemo } from 'react';
import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { postQuery } from '@/lib/api';
import { usePagination } from '@/hooks/common/usePagination';
import { useDebounce } from '@/hooks/common/useDebounce';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { PageResponse, CursorPageResponse } from '@/types/common';
import { SimplePost } from '@/types/domains/post';

// ============ POST LIST HOOKS ============

interface UseInfinitePostListOptions {
  pageSize?: number;
  initialData?: CursorPageResponse<SimplePost> | null;
  initialSearchTerm?: string;
  initialSearchType?: 'TITLE' | 'TITLE_CONTENT' | 'WRITER';
}

/**
 * 커서 기반 무한 스크롤 게시글 목록 Hook
 * - 일반 목록: useInfiniteQuery + cursor 기반
 * - 검색: 기존 offset 기반 페이징 유지
 */
export function useInfinitePostList(options: UseInfinitePostListOptions = {}) {
  const {
    pageSize = 20,
    initialData,
    initialSearchTerm = '',
    initialSearchType = 'TITLE'
  } = options;

  const [searchTerm, setSearchTerm] = useState(initialSearchTerm);
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'WRITER'>(initialSearchType);
  const debouncedSearchTerm = useDebounce(searchTerm, 500);
  const actualSearch = searchTerm.trim();

  // 검색용 페이지네이션 (검색은 offset 기반 유지)
  const searchPagination = usePagination({ pageSize, initialPage: 0 });

  // 일반 목록: useInfiniteQuery (커서 기반)
  const listQuery = useInfiniteQuery({
    queryKey: queryKeys.post.infiniteList(),
    queryFn: async ({ pageParam }) => {
      return await postQuery.getAll(pageParam, pageSize);
    },
    initialPageParam: null as number | null,
    getNextPageParam: (lastPage) => lastPage.data?.nextCursor ?? undefined,
    // SSR 초기 데이터 사용
    initialData: initialData ? {
      pages: [{ success: true, data: initialData }],
      pageParams: [null],
    } : undefined,
    enabled: !actualSearch, // 검색어 없을 때만 활성화
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 검색: 기존 offset 기반 useQuery 유지
  const searchQuery = useQuery({
    queryKey: queryKeys.post.search(debouncedSearchTerm, searchPagination.currentPage),
    queryFn: async () => {
      return await postQuery.search(
        searchType,
        debouncedSearchTerm.trim(),
        searchPagination.currentPage,
        pageSize
      );
    },
    enabled: !!actualSearch && !!debouncedSearchTerm.trim(),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  // 검색 결과 totalElements 동기화
  useEffect(() => {
    if (searchQuery.data?.data?.totalElements !== undefined) {
      searchPagination.setTotalItems(searchQuery.data.data.totalElements);
    }
  }, [searchQuery.data?.data?.totalElements]);

  // URL 검색어 변경 시 동기화
  useEffect(() => {
    if (searchTerm !== initialSearchTerm) {
      setSearchTerm(initialSearchTerm);
    }
    if (searchType !== initialSearchType) {
      setSearchType(initialSearchType);
    }
  }, [initialSearchTerm, initialSearchType]);

  // 게시글 목록 통합
  const posts = useMemo(() => {
    if (actualSearch) {
      return searchQuery.data?.data?.content || [];
    }
    return listQuery.data?.pages.flatMap(page => page.data?.content || []) || [];
  }, [actualSearch, searchQuery.data, listQuery.data]);

  // 더보기 함수
  const loadMore = useCallback(() => {
    if (!actualSearch && listQuery.hasNextPage && !listQuery.isFetchingNextPage) {
      listQuery.fetchNextPage();
    }
  }, [actualSearch, listQuery]);

  return {
    posts,
    isLoading: actualSearch ? searchQuery.isLoading : listQuery.isLoading,
    error: actualSearch ? searchQuery.error : listQuery.error,
    // 커서 기반 (일반 목록)
    hasNextPage: actualSearch ? false : (listQuery.hasNextPage ?? false),
    loadMore,
    isFetchingNextPage: listQuery.isFetchingNextPage,
    // 검색용 페이지네이션
    searchPagination: actualSearch ? searchPagination : null,
    // 검색 관련
    searchTerm,
    setSearchTerm,
    searchType,
    setSearchType,
    isSearching: !!actualSearch,
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