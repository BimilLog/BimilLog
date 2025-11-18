import { apiClient } from '../../client'
import { PageResponse } from '@/types/common'
import { BlacklistDTO } from '@/types/domains/blacklist'

/**
 * 블랙리스트 조회 API
 */
export const blacklistQuery = {
  /**
   * 블랙리스트 조회 (페이징)
   * @param page - 페이지 번호 (0부터 시작)
   * @param size - 페이지 크기
   * @returns 블랙리스트 페이지 응답
   */
  getBlacklist: (page = 0, size = 20) =>
    apiClient.get<PageResponse<BlacklistDTO>>(
      `/api/member/blacklist?page=${page}&size=${size}&sort=createdAt,desc`
    ),
}
