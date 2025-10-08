import { apiClient } from '../client'
import { Setting } from '@/types/domains/user'

export const userCommand = {
  updateUserName: (memberName: string) =>
    apiClient.post("/api/member/username", { memberName }),

  updateSettings: (settings: Setting) =>
    apiClient.post("/api/member/setting", settings),

  submitReport: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    targetId?: number
    content: string
    reporterId: number | null
    reporterName: string
  }) => apiClient.post("/api/member/report", report),


  withdraw: () =>
    apiClient.delete("/api/member/withdraw"),
}