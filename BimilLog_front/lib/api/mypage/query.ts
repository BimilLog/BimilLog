import { apiClient } from '../client';
import { ApiResponse } from '@/types/common';
import type { MyPageDTO } from '@/types/domains/mypage';

/**
 * 마이페이지 조회 API
 */
export const mypageQuery = {
  /**
   * 마이페이지 통합 정보 조회
   * - 작성글, 작성댓글, 추천글, 추천댓글을 1번의 API 호출로 조회
   *
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기 (기본 10)
   * @returns MyPageDTO
   */
  getMyPageInfo: async (page = 0, size = 10): Promise<ApiResponse<MyPageDTO>> => {
    return apiClient.get(`/api/mypage/?page=${page}&size=${size}`);
  },
};
