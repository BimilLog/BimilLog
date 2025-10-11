import { apiClient } from '../client'
import type { ReportType } from '@/types/domains/admin'

export const adminCommand = {
  banUser: (reportData: {
    reportType: ReportType
    targetId: number
  }) => apiClient.post("/api/admin/ban", {
    reportType: reportData.reportType,
    targetId: reportData.targetId
  }),

  forceWithdrawUser: (reportData: {
    targetId: number
    reportType: ReportType
  }) => apiClient.post("/api/admin/withdraw", {
    targetId: reportData.targetId,
    reportType: reportData.reportType
  }),
}