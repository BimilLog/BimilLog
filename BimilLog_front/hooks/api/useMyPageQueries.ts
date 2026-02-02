import { useQuery, UseQueryResult, UseQueryOptions } from '@tanstack/react-query';
import { mypageQuery } from '@/lib/api';
import { queryKeys } from '@/lib/tanstack-query/keys';
import type { MyPageDTO } from '@/types';
import type { ApiResponse } from '@/types/common';

/**
 * 마이페이지 정보 조회 훅
 */
export const useMyPageInfo = (
  page: number = 0,
  size: number = 10,
  options?: Partial<UseQueryOptions<ApiResponse<MyPageDTO>>>,
  initialData?: MyPageDTO | null,
): UseQueryResult<ApiResponse<MyPageDTO>> => {
  return useQuery({
    queryKey: queryKeys.mypage.info(page, size),
    queryFn: () => mypageQuery.getMyPageInfo(page, size),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    ...options,
    initialData: initialData ? { success: true, data: initialData } as ApiResponse<MyPageDTO> : undefined,
    initialDataUpdatedAt: initialData ? Date.now() : undefined,
  });
};
