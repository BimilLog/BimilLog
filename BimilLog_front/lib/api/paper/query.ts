import { apiClient } from '../client'
import { MyPaperDTO, VisitPaperResult, PopularPaperInfo } from '@/types/domains/paper'
import { CursorPageResponse } from '@/types/common'

export const paperQuery = {
  getMy: () =>
    apiClient.get<MyPaperDTO>("/api/paper"),

  getByUserName: (userName: string) =>
    apiClient.get<VisitPaperResult>(`/api/paper/${encodeURIComponent(userName)}`),

  getPopularPapers: (cursor?: number | null, size: number = 10) =>
    apiClient.get<CursorPageResponse<PopularPaperInfo>>(
      `/api/paper/popular?size=${size}${cursor != null ? `&cursor=${cursor}` : ''}`
    ),
}