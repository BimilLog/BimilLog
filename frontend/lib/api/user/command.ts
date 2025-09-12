import { apiClient } from '../client'
import { Setting } from '@/types/domains/user'

export const userCommand = {
  updateUserName: (userName: string) => 
    apiClient.post("/api/user/username", { userName }),
  
  updateSettings: (settings: Setting) => 
    apiClient.post("/api/user/setting", settings),
  
  submitReport: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    targetId?: number
    content: string
  }) => apiClient.post("/api/user/report", report),
  
  submitSuggestion: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT" | "SUGGESTION"
    userId?: number
    targetId?: number
    content: string
  }) => {
    const mappedType = report.reportType === "SUGGESTION" 
      ? "IMPROVEMENT" 
      : report.reportType as "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    
    return apiClient.post("/api/user/report", {
      reportType: mappedType,
      targetId: report.targetId,
      content: report.content
    })
  },
}