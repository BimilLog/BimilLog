import { apiClient } from '../client'

export const adminCommand = {
  banUser: (reportData: { 
    reporterId?: number
    reporterName?: string
    reportType: string
    targetId: number
    content: string 
  }) => apiClient.post("/api/admin/ban", {
    reportType: reportData.reportType,
    targetId: reportData.targetId,
    content: reportData.content
  }),
  
  forceWithdrawUser: (reportData: {
    targetId: number
    reportType: string
    content: string
  }) => apiClient.post("/api/admin/withdraw", {
    targetId: reportData.targetId,
    reportType: reportData.reportType,
    content: reportData.content
  }),
  
  deleteContent: (contentId: number, contentType: "POST" | "COMMENT") =>
    apiClient.delete(`/api/admin/content/${contentType.toLowerCase()}/${contentId}`),
}