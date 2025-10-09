export interface Report {
  id: number
  reporterId: number | null
  reporterName: string
  reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
  targetId: number | null
  targetAuthorId: number | null
  targetAuthorName: string | null
  content: string
  createdAt: string
}