import { apiClient } from '../client'
import { Report } from '@/types/domains/admin'
import { ApiResponse, PageResponse } from '@/types/common'

export const adminQuery = {
  getReports: async (page = 0, size = 20, reportType?: string): Promise<ApiResponse<PageResponse<Report>>> => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() })
    if (reportType && reportType !== "all") {
      params.append("reportType", reportType)
    }
    return apiClient.get<PageResponse<Report>>(`/api/admin/reports?${params.toString()}`)
  },

  getReport: async (reportId: number): Promise<ApiResponse<Report | undefined>> => {
    try {
      const response = await apiClient.get<Report>(`/api/admin/report/${reportId}`)
      return response
    } catch {
      return { success: false, error: '신고 내역 조회에 실패했습니다.' }
    }
  },
}