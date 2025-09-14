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
  

  withdraw: () =>
    apiClient.delete("/api/user/withdraw"),
}