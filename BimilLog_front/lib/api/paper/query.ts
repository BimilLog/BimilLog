import { apiClient } from '../client'
import { RollingPaperMessage, VisitPaperResult, PopularPaperInfo } from '@/types/domains/paper'
import { PageResponse } from '@/types/common'

export const paperQuery = {
  getMy: () =>
    apiClient.get<RollingPaperMessage[]>("/api/paper"),

  getByUserName: (userName: string) =>
    apiClient.get<VisitPaperResult>(`/api/paper/${encodeURIComponent(userName)}`),

  getPopularPapers: (page: number = 0, size: number = 10) =>
    apiClient.get<PageResponse<PopularPaperInfo>>(`/api/paper/popular?page=${page}&size=${size}`),
}