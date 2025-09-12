// Admin and report-related type definitions

// 신고 타입 - v2 백엔드 ReportDTO 호환
export interface Report {
  id: number
  reporterId: number
  reporterName: string
  reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
  targetId: number
  content: string
  createdAt: string
  // 임시 호환용 (나중에 제거 필요)
  targetTitle?: string
  userId?: number // reporterId 대신 사용되는 경우가 있음
}