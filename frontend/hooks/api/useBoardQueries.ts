/**
 * 게시판 관련 Query Hooks
 * @deprecated usePostQueries.ts의 훅들을 사용하세요.
 * 이 파일은 하위 호환성을 위해 유지되며, 실제 구현은 usePostQueries.ts로 이동되었습니다.
 */

// usePostQueries.ts의 훅들을 re-export (하위 호환성 유지)
export {
  usePostList as useBoardPosts,
  usePopularPosts as useBoardPopularPosts,
  useLegendPosts as useBoardLegendPosts,
  useNoticePosts as useBoardNoticePosts
} from './usePostQueries';

// useBoardSearch는 더 유연한 검색 타입을 지원하는 버전
import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { postQuery } from '@/lib/api';

export const useBoardSearch = (
  searchType: 'TITLE' | 'TITLE_CONTENT' | 'AUTHOR',
  query: string,
  page: number = 0,
  size: number = 10
) => {
  return useQuery({
    queryKey: queryKeys.post.search(query, page),
    queryFn: () => postQuery.search(searchType, query, page, size),
    // 검색어가 있을 때만 쿼리 실행 - 불필요한 API 호출 방지
    enabled: !!query && query.trim().length > 0,
    staleTime: 5 * 60 * 1000,
  });
};