"use client";

import React from "react";
import { Badge, Button } from "@/components";
import { Calendar, FileText, Eye } from "lucide-react";
import { formatRelativeDate, formatKoreanDateTime } from "@/lib/utils/date";
import {
  getReportTypeLabel,
  getReportTypeBadgeColor,
} from "@/lib/utils/admin/config";
import type { Report } from "@/types/domains/admin";

interface ReportCardProps {
  report: Report;
  onView: () => void;
  /** 진행 중인 액션이 있으면 다른 카드 클릭을 차단 (F-16-037) */
  disabled?: boolean;
}

/**
 * 신고 카드 (데스크톱 — 테이블 row).
 * 라운드 16 F-16-018~027 종합 적용.
 */
export const ReportCard = React.memo<ReportCardProps>(({ report, onView, disabled = false }) => {
  const typeLabel = getReportTypeLabel(report.reportType);
  const typeColor = getReportTypeBadgeColor(report.reportType);

  const isDeletedTarget =
    (report.reportType === "POST" || report.reportType === "COMMENT") &&
    !report.targetAuthorName;

  return (
    <tr className="hover:bg-paper-soft transition-colors">
      <td className="px-6 py-4 whitespace-nowrap">
        <div className="flex items-center gap-3">
          <div>
            <div className="text-sm font-medium text-ink dark:text-foreground">
              #{report.id}
            </div>
            <Badge className={`text-xs ${typeColor}`}>
              {typeLabel}
            </Badge>
          </div>
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-xs text-ink-soft dark:text-muted-foreground">신고자:</span>
            {/* F-16-023: 익명 폴백 시각 분리 (italic + ink-soft) */}
            {report.reporterName ? (
              <span className="text-sm text-ink dark:text-foreground">
                {report.reporterName}
              </span>
            ) : (
              <span className="text-sm italic text-ink-soft dark:text-muted-foreground">
                익명
              </span>
            )}
          </div>
          {(report.reportType === "POST" || report.reportType === "COMMENT") && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-ink-soft dark:text-muted-foreground">대상:</span>
              {isDeletedTarget ? (
                <span
                  className="text-sm italic text-stamp-red"
                  aria-label="신고 대상 사용자 삭제됨"
                >
                  삭제됨
                </span>
              ) : (
                <span className="text-sm text-ink dark:text-foreground font-medium">
                  {report.targetAuthorName}
                </span>
              )}
            </div>
          )}
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center gap-2">
          <FileText className="w-4 h-4 stroke-postal-navy shrink-0" aria-hidden="true" />
          {/* F-16-020: line-clamp 위 SR 보호용 aria-label 로 전체 콘텐츠 노출 */}
          <span
            className="text-sm text-ink-soft dark:text-muted-foreground line-clamp-2"
            aria-label={report.content}
          >
            {report.content}
          </span>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap">
        <div className="flex items-center gap-2 text-sm text-ink-soft dark:text-muted-foreground">
          <Calendar className="w-4 h-4 stroke-postal-navy" aria-hidden="true" />
          {/* F-16-021/022: time dateTime + relative + 정확한 ISO title */}
          <time
            dateTime={report.createdAt}
            title={formatKoreanDateTime(report.createdAt)}
          >
            {formatRelativeDate(report.createdAt)}
          </time>
        </div>
      </td>
      <td className="px-6 py-4 text-right">
        <Button
          variant="ghost"
          size="sm"
          onClick={onView}
          disabled={disabled}
          className="text-stamp-red hover:text-stamp-red hover:bg-stamp-red/10 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Eye className="w-4 h-4 mr-1 stroke-stamp-red" aria-hidden="true" />
          상세보기
        </Button>
      </td>
    </tr>
  );
});
ReportCard.displayName = "ReportCard";
