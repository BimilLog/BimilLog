import React, { useMemo } from "react";
import { Card, CardContent } from "@/components";
import { AlertTriangle, FileText, MessageSquare, AlertCircle, Sparkles } from "lucide-react";
import type { Report } from "@/types/domains/admin";

interface AdminStatsProps {
  reports: Report[];
  /** 전체 신고 건수 (백엔드 PageResponse.totalElements) */
  totalElements: number;
}

/**
 * 관리자 대시보드 — 신고 미니 통계 카드.
 *
 * 라운드 16 F-16-007/008/010/011:
 * - placeholder 제거. 백엔드 응답 (totalElements + reports.content) 만으로 즉시 가능한
 *   집계 4종을 노출.
 * - paper 토큰 일괄 적용 + 다크 변형.
 */
export const AdminStats: React.FC<AdminStatsProps> = React.memo(
  ({ reports, totalElements }) => {
    const stats = useMemo(() => {
      // 현재 페이지의 reports 만 집계 가능 (페이징된 응답이라 분포는 페이지 단위 기준).
      const counts = {
        POST: 0,
        COMMENT: 0,
        ERROR: 0,
        IMPROVEMENT: 0,
      } as Record<string, number>;
      for (const r of reports) {
        if (r.reportType in counts) counts[r.reportType] += 1;
      }
      const actionable = reports.filter(
        (r) =>
          (r.reportType === "POST" || r.reportType === "COMMENT") &&
          r.targetAuthorName !== null,
      ).length;
      return { counts, actionable };
    }, [reports]);

    return (
      <section
        aria-label="신고 통계 요약"
        className="grid grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4"
      >
        <Card className="bg-paper-card border border-postal-navy/15 shadow-brand-sm">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-stamp-red/15 flex items-center justify-center shrink-0">
              <AlertTriangle
                className="w-5 h-5 stroke-stamp-red"
                aria-hidden="true"
              />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-ink-soft dark:text-muted-foreground break-keep">
                전체 신고
              </p>
              <p
                className="text-xl font-bold text-ink dark:text-foreground"
                aria-label={`전체 신고 ${totalElements}건`}
              >
                {totalElements.toLocaleString()}
                <span className="text-sm font-medium text-ink-soft ml-1">건</span>
              </p>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-paper-card border border-postal-navy/15 shadow-brand-sm">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-seal-gold/20 flex items-center justify-center shrink-0">
              <FileText
                className="w-5 h-5 stroke-postal-navy"
                aria-hidden="true"
              />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-ink-soft dark:text-muted-foreground break-keep">
                게시글 (이 페이지)
              </p>
              <p
                className="text-xl font-bold text-ink dark:text-foreground"
                aria-label={`게시글 신고 ${stats.counts.POST}건`}
              >
                {stats.counts.POST}
                <span className="text-sm font-medium text-ink-soft ml-1">건</span>
              </p>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-paper-card border border-postal-navy/15 shadow-brand-sm">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-postal-navy/15 flex items-center justify-center shrink-0">
              <MessageSquare
                className="w-5 h-5 stroke-postal-navy"
                aria-hidden="true"
              />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-ink-soft dark:text-muted-foreground break-keep">
                댓글 (이 페이지)
              </p>
              <p
                className="text-xl font-bold text-ink dark:text-foreground"
                aria-label={`댓글 신고 ${stats.counts.COMMENT}건`}
              >
                {stats.counts.COMMENT}
                <span className="text-sm font-medium text-ink-soft ml-1">건</span>
              </p>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-paper-card border border-postal-navy/15 shadow-brand-sm">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-paper-aged flex items-center justify-center shrink-0">
              <AlertCircle
                className="w-5 h-5 stroke-stamp-red"
                aria-hidden="true"
              />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-ink-soft dark:text-muted-foreground break-keep">
                오류·개선
              </p>
              <p
                className="text-xl font-bold text-ink dark:text-foreground"
                aria-label={`오류 ${stats.counts.ERROR}건, 개선 ${stats.counts.IMPROVEMENT}건`}
              >
                {stats.counts.ERROR + stats.counts.IMPROVEMENT}
                <span className="text-sm font-medium text-ink-soft ml-1">건</span>
              </p>
            </div>
          </CardContent>
        </Card>

        {/* 처리 가능 신고 — 모바일에서는 한 줄 차지 */}
        <Card className="bg-paper-card border border-postal-navy/15 shadow-brand-sm col-span-2 md:col-span-4">
          <CardContent className="p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-stamp-red/10 flex items-center justify-center shrink-0">
              <Sparkles
                className="w-5 h-5 stroke-stamp-red"
                aria-hidden="true"
              />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs text-ink-soft dark:text-muted-foreground break-keep">
                이 페이지에서 제재 가능한 신고
              </p>
              <p
                className="text-base font-semibold text-ink dark:text-foreground"
                aria-label={`제재 가능 ${stats.actionable}건`}
              >
                {stats.actionable}
                <span className="text-sm font-medium text-ink-soft ml-1">
                  건 / {reports.length}건
                </span>
              </p>
            </div>
          </CardContent>
        </Card>
      </section>
    );
  },
);
AdminStats.displayName = "AdminStats";
