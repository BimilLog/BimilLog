import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { paperQuery } from '@/lib/api';

/**
 * 롤링페이퍼 메시지 조회
 */
export const useRollingPaper = (userName: string) => {
  return useQuery({
    queryKey: queryKeys.paper.detail(userName),
    queryFn: () => paperQuery.getByUserName(userName),
    enabled: !!userName,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};