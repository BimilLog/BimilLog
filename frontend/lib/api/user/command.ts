import { apiClient } from '../client'
import { Setting } from '@/types/domains/user'

export const userCommand = {
  updateUserName: (userName: string) =>
    apiClient.post("/api/member/username", { userName }),

  updateSettings: (settings: Setting) =>
    apiClient.post("/api/member/setting", settings),

  submitReport: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    targetId?: number
    content: string
  }) => apiClient.post("/api/member/report", report),


  withdraw: () =>
    apiClient.delete("/api/member/withdraw"),
}