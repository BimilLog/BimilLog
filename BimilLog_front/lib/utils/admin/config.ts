import { ReportType } from './validation';

export interface ReportTypeConfig {
  label: string;
  description: string;
  /**
   * 라운드 16 paper 토큰 + 다크 변형. (F-16-025)
   * Badge / 카드 / 모달에서 동일하게 사용되도록 통합된 스타일.
   * 라이트: stamp-red / postal-navy / seal-gold 계열
   * 다크: ring + 투명도 처리로 가시성 유지
   */
  color: string;
}

export const REPORT_TYPE_CONFIGS: Record<ReportType, ReportTypeConfig> = {
  POST: {
    label: "게시글",
    description: "게시글 관련 신고",
    color:
      "bg-seal-gold/20 text-postal-navy ring-1 ring-seal-gold/40 dark:bg-seal-gold/25 dark:text-paper-50 dark:ring-seal-gold/60",
  },
  COMMENT: {
    label: "댓글",
    description: "댓글 관련 신고",
    color:
      "bg-postal-navy/15 text-postal-navy ring-1 ring-postal-navy/30 dark:bg-postal-navy/30 dark:text-paper-50 dark:ring-postal-navy/60",
  },
  ERROR: {
    label: "오류",
    description: "시스템 오류 신고",
    color:
      "bg-stamp-red/15 text-stamp-red ring-1 ring-stamp-red/30 dark:bg-stamp-red/25 dark:text-paper-50 dark:ring-stamp-red/60",
  },
  IMPROVEMENT: {
    label: "개선사항",
    description: "서비스 개선 제안",
    color:
      "bg-paper-aged text-postal-navy ring-1 ring-postal-navy/20 dark:bg-postal-navy/20 dark:text-paper-50 dark:ring-postal-navy/50",
  },
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
    color:
      "bg-paper-soft text-ink-soft ring-1 ring-postal-navy/20 dark:bg-postal-navy/15 dark:text-paper-50 dark:ring-postal-navy/40",
  };
}

/**
 * 라운드 16 F-16-025: 신고 종류 색상 헬퍼 (paper 토큰 + 다크 변형).
 * ReportCard / MobileReportCard / ReportDetailModal 3곳의 중복 코드 통합.
 */
export function getReportTypeBadgeColor(type: string): string {
  return getReportTypeConfig(type).color;
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