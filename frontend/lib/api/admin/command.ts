import { apiClient } from '../client'

export const adminCommand = {
  banUser: (reportData: { 
    reporterId?: number
    reporterName?: string
    reportType: string
    targetId: number
    content: string 
  }) => apiClient.post("/api/admin/command/ban", {
    reportType: reportData.reportType,
    targetId: reportData.targetId,
    content: reportData.content
  }),
  
  forceWithdrawUser: (reportData: {
    targetId: number
    reportType: string
    content: string
  }) => apiClient.post("/api/admin/command/withdraw", {
    targetId: reportData.targetId,
    reportType: reportData.reportType,
    content: reportData.content
  }),
  
  deleteContent: (contentId: number, contentType: "POST" | "COMMENT") => 
    apiClient.delete(`/api/admin/command/content/${contentType.toLowerCase()}/${contentId}`),
  
  createNotice: (post: { title: string; content: string }) => 
    apiClient.post("/api/admin/command/notice/create", post),
  
  updateNotice: (postId: number, post: { title: string; content: string }) => 
    apiClient.put(`/api/admin/command/notice/update/${postId}`, post),
  
  deleteNotice: (postId: number) => 
    apiClient.delete(`/api/admin/command/notice/delete/${postId}`),
}