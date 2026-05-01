"use client";

import React from "react";
import { Card, Badge, Button } from "@/components";
import { Calendar, FileText, Eye } from "lucide-react";
import { formatRelativeDate, formatKoreanDateTime } from "@/lib/utils/date";
import {
  getReportTypeLabel,
  getReportTypeBadgeColor,
} from "@/lib/utils/admin/config";
import type { Report } from "@/types/domains/admin";

interface MobileReportCardProps {
  report: Report;
  onView: () => void;
  /** 진행 중인 액션이 있으면 다른 카드 클릭을 차단 (F-16-037) */
  disabled?: boolean;
}

/**
 * 신고 카드 (모바일 — 카드 형태).
 * 라운드 16 F-16-020/021/022/023/025/027 종합 적용.
 */
export const MobileReportCard = React.memo<MobileReportCardProps>(
  ({ report, onView, disabled = false }) => {
    const typeLabel = getReportTypeLabel(report.reportType);
    const typeColor = getReportTypeBadgeColor(report.reportType);

    const isDeletedTarget =
      (report.reportType === "POST" || report.reportType === "COMMENT") &&
      !report.targetAuthorName;

    return (
      <Card className="p-4 bg-paper-card border border-postal-navy/15 hover:shadow-brand-md transition-shadow">
        <div className="space-y-3">
          {/* 헤더 */}
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-center gap-2">
              <span className="text-sm font-semibold text-ink dark:text-foreground">
                #{report.id}
              </span>
              <Badge className={`text-xs ${typeColor}`}>
                {typeLabel}
              </Badge>
            </div>
            <div className="text-xs text-ink-soft dark:text-muted-foreground flex items-center gap-1">
              <Calendar
                className="w-3 h-3 stroke-postal-navy"
                aria-hidden="true"
              />
              <time
                dateTime={report.createdAt}
                title={formatKoreanDateTime(report.createdAt)}
              >
                {formatRelativeDate(report.createdAt)}
              </time>
            </div>
          </div>

          {/* 사용자 정보 */}
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-sm">
              <span className="text-xs text-ink-soft dark:text-muted-foreground font-medium">
                신고자:
              </span>
              {report.reporterName ? (
                <span className="text-ink dark:text-foreground">
                  {report.reporterName}
                </span>
              ) : (
                <span className="italic text-ink-soft dark:text-muted-foreground">
                  익명
                </span>
              )}
            </div>
            {(report.reportType === "POST" || report.reportType === "COMMENT") && (
              <div className="flex items-center gap-2 text-sm">
                <span className="text-xs text-ink-soft dark:text-muted-foreground font-medium">
                  대상:
                </span>
                {isDeletedTarget ? (
                  <span
                    className="italic text-stamp-red"
                    aria-label="신고 대상 사용자 삭제됨"
                  >
                    삭제됨
                  </span>
                ) : (
                  <span className="text-ink dark:text-foreground font-medium">
                    {report.targetAuthorName}
                  </span>
                )}
              </div>
            )}
          </div>

          {/* 신고 사유 — F-16-020 aria-label */}
          <div className="space-y-1">
            <div className="flex items-start gap-2">
              <FileText
                className="w-4 h-4 stroke-postal-navy mt-0.5 shrink-0"
                aria-hidden="true"
              />
              <p
                className="text-sm text-ink-soft dark:text-muted-foreground line-clamp-2 flex-1"
                aria-label={report.content}
              >
                {report.content}
              </p>
            </div>
          </div>

          {/* 액션 버튼 — F-16-027 min-h-[44px] */}
          <Button
            variant="outline"
            size="sm"
            onClick={onView}
            disabled={disabled}
            className="w-full min-h-[44px] text-stamp-red border-stamp-red/30 hover:bg-stamp-red/10 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Eye
              className="w-4 h-4 mr-2 stroke-stamp-red"
              aria-hidden="true"
            />
            상세보기
          </Button>
        </div>
      </Card>
    );
  },
);
MobileReportCard.displayName = "MobileReportCard";
