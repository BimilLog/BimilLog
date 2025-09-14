import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { postQuery } from '@/lib/api';

/**
 * Board 관련 Query hooks
 * 게시판 페이지에서 사용되는 데이터 조회 훅들
 */

/**
 * 게시글 목록 조회 (Board 페이지용)
 * 기본 페이지네이션과 함께 사용
 */
export const useBoardPosts = (page: number = 0, size: number = 10) => {
  return useQuery({
    queryKey: queryKeys.post.list({ page, size }),
    queryFn: () => postQuery.getAll(page, size),
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};

/**
 * 게시글 검색 (Board 검색 기능)
 */
export const useBoardSearch = (
  searchType: 'TITLE' | 'TITLE_CONTENT' | 'AUTHOR',
  query: string,
  page: number = 0,
  size: number = 10
) => {
  return useQuery({
    queryKey: queryKeys.post.search(query, page),
    queryFn: () => postQuery.search(searchType, query, page, size),
    enabled: !!query && query.trim().length > 0,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};

/**
 * 인기 게시글 목록 (Board 메인 페이지용)
 */
export const useBoardPopularPosts = () => {
  return useQuery({
    queryKey: queryKeys.post.popular(),
    queryFn: postQuery.getPopular,
    staleTime: 10 * 60 * 1000, // 10분
    gcTime: 30 * 60 * 1000, // 30분
  });
};

/**
 * 레전드 게시글 목록 (Board 메인 페이지용)
 */
export const useBoardLegendPosts = () => {
  return useQuery({
    queryKey: queryKeys.post.legend(),
    queryFn: () => postQuery.getLegend(),
    staleTime: 30 * 60 * 1000, // 30분
    gcTime: 60 * 60 * 1000, // 1시간
  });
};

/**
 * 공지사항 목록 (Board 메인 페이지용)
 */
export const useBoardNoticePosts = () => {
  return useQuery({
    queryKey: queryKeys.post.notice(),
    queryFn: postQuery.getNotices,
    staleTime: 60 * 60 * 1000, // 1시간
    gcTime: 24 * 60 * 60 * 1000, // 24시간
  });
};