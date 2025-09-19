import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { paperQuery } from '@/lib/api';
import { useAuth } from '@/hooks';

/**
 * 내 롤링페이퍼 메시지 조회
 * 로그인한 사용자 본인의 롤링페이퍼를 조회할 때 사용
 * RollingPaperMessage[] 타입 반환 (전체 정보 포함)
 */
export const useMyRollingPaper = (enabled: boolean = true) => {
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
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
  });
};