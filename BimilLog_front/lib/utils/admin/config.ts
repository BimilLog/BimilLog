import { ReportType } from './validation';

export interface ReportTypeConfig {
  label: string;
  description: string;
  color: string;
}

export const REPORT_TYPE_CONFIGS: Record<ReportType, ReportTypeConfig> = {
  POST: {
    label: "게시글",
    description: "게시글 관련 신고",
    color: "bg-blue-100 text-blue-800 border-blue-200"
  },
  COMMENT: {
    label: "댓글",
    description: "댓글 관련 신고",
    color: "bg-green-100 text-green-800 border-green-200"
  },
  ERROR: {
    label: "오류",
    description: "시스템 오류 신고",
    color: "bg-red-100 text-red-800 border-red-200"
  },
  IMPROVEMENT: {
    label: "개선사항",
    description: "서비스 개선 제안",
    color: "bg-purple-100 text-purple-800 border-purple-200"
  }
} as const;

export function getReportTypeLabel(type: string): string {
  const reportType = type as ReportType;
  return REPORT_TYPE_CONFIGS[reportType]?.label || "기타";
}

export function getReportTypeConfig(type: string): ReportTypeConfig {
  const reportType = type as ReportType;
  return REPORT_TYPE_CONFIGS[reportType] || {
    label: "기타",
    description: "기타 신고",
    color: "bg-gray-100 text-gray-800 border-gray-200"
  };
}

export const REPORT_STATUS_CONFIGS = {
  PENDING: {
    label: "처리 대기",
    color: "bg-yellow-100 text-yellow-800 border-yellow-200",
    icon: "Clock"
  },
  PROCESSING: {
    label: "처리 중",
    color: "bg-blue-100 text-blue-800 border-blue-200",
    icon: "Loader"
  },
  COMPLETED: {
    label: "완료",
    color: "bg-green-100 text-green-800 border-green-200",
    icon: "CheckCircle"
  },
  REJECTED: {
    label: "거부",
    color: "bg-red-100 text-red-800 border-red-200",
    icon: "XCircle"
  }
} as const;

export function formatDateTime(dateString: string): string {
  try {
    return new Date(dateString).toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch {
    return dateString;
  }
}

export function truncateText(text: string, maxLength: number = 50): string {
  if (text.length <= maxLength) return text;
  return text.slice(0, maxLength) + '...';
}

export function hasActionableTarget(reportType: string): boolean {
  return reportType !== "ERROR" && reportType !== "IMPROVEMENT";
}