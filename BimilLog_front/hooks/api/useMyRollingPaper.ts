import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { paperQuery } from '@/lib/api';
import { useAuth } from '@/hooks';
import type { RollingPaperMessage } from '@/types/domains/paper';
import type { ApiResponse } from '@/types/common';

/**
 * 내 롤링페이퍼 메시지 조회
 */
export const useMyRollingPaper = (
  enabled: boolean = true,
  initialData?: RollingPaperMessage[] | null,
) => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: queryKeys.paper.my,
    queryFn: async () => {
      const response = await paperQuery.getMy();
      if (!response.success) {
        throw new Error(response.error || '롤링페이퍼를 불러올 수 없습니다');
      }
      return response;
    },
    enabled: isAuthenticated && enabled,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    initialData: initialData ? { success: true, data: initialData } as ApiResponse<RollingPaperMessage[]> : undefined,
    initialDataUpdatedAt: initialData ? Date.now() : undefined,
  });
};
