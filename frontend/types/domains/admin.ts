export interface Report {
  id: number
  reporterId: number
  reporterName: string
  reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
  targetId: number
  content: string
  createdAt: string
  targetTitle?: string
}