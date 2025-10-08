import { apiClient } from '../client'

export const adminCommand = {
  banUser: (reportData: {
    reportType: string
    targetId: number
  }) => apiClient.post("/api/admin/ban", {
    reportType: reportData.reportType,
    targetId: reportData.targetId
  }),
  
  forceWithdrawUser: (reportData: {
    targetId: number
    reportType: string
  }) => apiClient.post("/api/admin/withdraw", {
    targetId: reportData.targetId,
    reportType: reportData.reportType
  }),
  
  deleteContent: (contentId: number, contentType: "POST" | "COMMENT") =>
    apiClient.delete(`/api/admin/content/${contentType.toLowerCase()}/${contentId}`),
}