import { apiClient } from '../../client'
import { ApiResponse, PageResponse } from '@/types/common'
import { BlacklistDTO } from '@/types/domains/blacklist'

/**
 * 블랙리스트 변경 API (추가, 삭제)
 */
export const blacklistCommand = {
  /**
   * 블랙리스트에 사용자 추가
   * @param memberName - 차단할 사용자 이름
   * @returns 성공/실패 응답
   */
  addToBlacklist: (memberName: string): Promise<ApiResponse<void>> =>
    apiClient.post('/api/member/blacklist', { memberName }),

  /**
   * 블랙리스트에서 사용자 삭제
   * @param id - 블랙리스트 ID
   * @param page - 현재 페이지 번호 (삭제 후 업데이트된 목록 조회)
   * @param size - 페이지 크기
   * @returns 업데이트된 블랙리스트 목록
   */
  removeFromBlacklist: (
    id: number,
    page = 0,
    size = 20
  ): Promise<ApiResponse<PageResponse<BlacklistDTO>>> =>
    apiClient.delete(
      `/api/member/blacklist?page=${page}&size=${size}&sort=createdAt,desc`,
      { id }
    ),
}
