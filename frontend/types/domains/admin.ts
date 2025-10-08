export interface Report {
  id: number
  reporterId: number | null
  reporterName: string
  reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
  targetId: number
  content: string
  createdAt: string
}