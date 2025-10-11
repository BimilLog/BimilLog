export type ReportType = "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT";

export interface Report {
  id: number
  reporterId: number | null
  reporterName: string
  reportType: ReportType
  targetId: number | null
  targetAuthorId: number | null
  targetAuthorName: string | null
  content: string
  createdAt: string
}