import { apiClient } from '../client'
import { Report } from '@/types/domains/admin'
import { ApiResponse, PageResponse } from '@/types/api/common'

export const adminQuery = {
  getReports: (page = 0, size = 20, reportType?: string) => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() })
    if (reportType && reportType !== "all") {
      params.append("reportType", reportType)
    }
    return apiClient.get(`/api/admin/reports?${params.toString()}`)
  },
  
  getReport: async (reportId: number): Promise<ApiResponse<Report | undefined>> => {
    try {
      const response = await apiClient.get<Report>(`/api/admin/report/${reportId}`)
      return response
    } catch (error) {
      return { success: false, error: '신고 내역 조회에 실패했습니다.' }
    }
  },
}