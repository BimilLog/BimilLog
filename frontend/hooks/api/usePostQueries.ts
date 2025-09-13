'use client';

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
 * 인기 게시글 목록
 */
export const usePopularPosts = () => {
  return useQuery({
    queryKey: queryKeys.post.popular(),
    queryFn: postQuery.getPopular,
    staleTime: 10 * 60 * 1000, // 10분
    gcTime: 30 * 60 * 1000, // 30분
  });
};

/**
 * 레전드 게시글 목록
 */
export const useLegendPosts = () => {
  return useQuery({
    queryKey: queryKeys.post.legend(),
    queryFn: () => postQuery.getLegend(),
    staleTime: 30 * 60 * 1000, // 30분
    gcTime: 60 * 60 * 1000, // 1시간
  });
};

/**
 * 공지사항 목록
 */
export const useNoticePosts = () => {
  return useQuery({
    queryKey: queryKeys.post.notice(),
    queryFn: postQuery.getNotices,
    staleTime: 60 * 60 * 1000, // 1시간
    gcTime: 24 * 60 * 60 * 1000, // 24시간
  });
};