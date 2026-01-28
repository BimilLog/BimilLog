import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { paperQuery } from '@/lib/api';

/**
 * 롤링페이퍼 메시지 조회
 */
export const useRollingPaper = (userName: string, enabled: boolean = true) => {
  return useQuery({
    queryKey: queryKeys.paper.detail(userName),
    queryFn: async () => {
      const response = await paperQuery.getByUserName(userName);
      if (!response.success) {
        throw new Error(response.error || '롤링페이퍼를 불러올 수 없습니다');
      }
      return response;
    },
    enabled: !!userName && enabled,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};

/**
 * 실시간 인기 롤링페이퍼 조회
 */
export const usePopularPapers = (
  page: number = 0,
  size: number = 10,
  options?: { enabled?: boolean }
) => {
  return useQuery({
    queryKey: queryKeys.paper.popular(page, size),
    queryFn: async () => {
      const response = await paperQuery.getPopularPapers(page, size);
      if (!response.success) {
        throw new Error(response.error || '인기 롤링페이퍼를 불러올 수 없습니다');
      }
      return response.data;
    },
    enabled: options?.enabled ?? true,
    staleTime: 60 * 1000, // 1분
    gcTime: 5 * 60 * 1000, // 5분
    retry: 1,
    refetchInterval: (data) => (data ? 60 * 1000 : false), // refetch every minute only after first success
  });
};
