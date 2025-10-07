import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { postQuery } from '@/lib/api';
import type { Post, PageResponse } from '@/types';

/**
 * 게시글 목록 조회 (페이지네이션)
 */
export const usePostList = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.post.list({ page, size }),
    queryFn: () => postQuery.getAll(page, size),
    staleTime: 3 * 60 * 1000, // 3분
  });
};

/**
 * 게시글 목록 조회 (무한 스크롤)
 */
export const useInfinitePostList = (size: number = 10) => {
  return useInfiniteQuery({
    queryKey: queryKeys.post.lists(),
    queryFn: ({ pageParam = 0 }) => postQuery.getAll(pageParam, size),
    getNextPageParam: (lastPage) => {
      if (!lastPage.success || !lastPage.data) return undefined;
      const { last, number } = lastPage.data;
      return last ? undefined : number + 1;
    },
    initialPageParam: 0,
    staleTime: 3 * 60 * 1000,
  });
};

/**
 * 게시글 상세 조회
 */
export const usePostDetail = (postId: number) => {
  return useQuery({
    queryKey: queryKeys.post.detail(postId),
    queryFn: () => postQuery.getById(postId),
    enabled: !!postId && postId > 0,
    staleTime: 5 * 60 * 1000, // 5분
  });
};

/**
 * 게시글 검색
 */
export const usePostSearch = (query: string, page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.post.search(query, page),
    queryFn: () => postQuery.search('TITLE_CONTENT', query, page, size),
    enabled: !!query && query.trim().length > 0,
    staleTime: 3 * 60 * 1000,
  });
};

/**
 * 실시간 인기 게시글 목록
 */
export const useRealtimePosts = () => {
  return useQuery({
    queryKey: queryKeys.post.realtimePopular(),
    queryFn: postQuery.getRealtimePosts,
    staleTime: 10 * 60 * 1000, // 10분
    gcTime: 30 * 60 * 1000, // 30분
  });
};

/**
 * 주간 인기 게시글 목록
 */
export const useWeeklyPosts = () => {
  return useQuery({
    queryKey: queryKeys.post.weeklyPopular(),
    queryFn: postQuery.getWeeklyPosts,
    staleTime: 10 * 60 * 1000, // 10분
    gcTime: 30 * 60 * 1000, // 30분
  });
};

