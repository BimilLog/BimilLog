import { useQuery, UseQueryResult, UseQueryOptions } from '@tanstack/react-query';
import { mypageQuery } from '@/lib/api';
import { queryKeys } from '@/lib/tanstack-query/keys';
import type { MyPageDTO } from '@/types';
import type { ApiResponse } from '@/types/common';

/**
 * 마이페이지 정보 조회 훅
 *
 * 작성글, 작성댓글, 추천글, 추천댓글을 1번의 API 호출로 조회
 *
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 * @param options useQuery 추가 옵션
 * @returns UseQueryResult<ApiResponse<MyPageDTO>>
 */
export const useMyPageInfo = (
  page: number = 0,
  size: number = 10,
  options?: Partial<UseQueryOptions<ApiResponse<MyPageDTO>>>
): UseQueryResult<ApiResponse<MyPageDTO>> => {
  return useQuery({
    queryKey: queryKeys.mypage.info(page, size),
    queryFn: () => mypageQuery.getMyPageInfo(page, size),
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000,   // 10분 (구 cacheTime)
    ...options, // 추가 옵션 병합
  });
};
