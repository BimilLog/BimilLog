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
  
  deleteContent: (contentId: number, contentType: "POST" | "COMMENT") => 
    apiClient.delete(`/api/admin/content/${contentType.toLowerCase()}/${contentId}`),
  
  createNotice: (post: { title: string; content: string }) => 
    apiClient.post("/api/admin/notice", post),
  
  updateNotice: (postId: number, post: { title: string; content: string }) => 
    apiClient.put(`/api/admin/notice/${postId}`, post),
  
  deleteNotice: (postId: number) => 
    apiClient.delete(`/api/admin/notice/${postId}`),
}