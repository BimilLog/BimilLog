import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { commentQuery } from '@/lib/api';

/**
 * Comment 관련 Query hooks
 * 댓글 시스템에서 사용되는 데이터 조회 훅들
 */

/**
 * 인기 댓글 목록 조회
 * 게시글의 좋아요 수가 많은 댓글들을 우선적으로 조회
 */
export const usePopularComments = (postId: number) => {
  return useQuery({
    queryKey: queryKeys.comment.popular(postId),
    queryFn: () => commentQuery.getPopular(postId),
    enabled: !!postId && postId > 0,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};