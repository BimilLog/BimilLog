import React from "react";
import { Badge, Button, Eye, Clock } from "@/components";
import { type Report } from "@/lib/api";
import { 
  getReportTypeConfig, 
  formatDateTime, 
  truncateText, 
  hasActionableTarget 
} from "@/lib/admin-utils";

interface ReportCardProps {
  report: Report;
  onViewDetail: (report: Report) => void;
}

export const ReportCard = React.memo<ReportCardProps>(({ report, onViewDetail }) => {
  const typeConfig = getReportTypeConfig(report.reportType);

  return (
    <div className="group p-6 bg-gradient-to-br from-white to-gray-50 rounded-xl border border-gray-200 hover:border-purple-300 hover:shadow-md transition-all duration-200 active:scale-[0.99] touch-manipulation">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-3">
            <Badge className={`text-xs font-medium ${typeConfig.color}`}>
              {typeConfig.label}
            </Badge>
            <Badge className="text-xs bg-yellow-100 text-yellow-800 border-yellow-200 font-medium">
              <Clock className="w-3 h-3 mr-1" />
              처리 대기
            </Badge>
          </div>

          <h3 className="font-semibold text-gray-900 mb-2 line-clamp-2">
            {report.reportType === "ERROR" || report.reportType === "IMPROVEMENT"
              ? `${typeConfig.label} 신고`
              : report.targetTitle || "신고된 콘텐츠"}
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm text-gray-600 mb-3">
            <p className="truncate">
              <span className="font-medium">신고자:</span> {report.reporterName} (ID: {report.reporterId})
            </p>
            {report.targetId && hasActionableTarget(report.reportType) && (
              <p className="truncate">
                <span className="font-medium">대상 ID:</span> {report.targetId}
              </p>
            )}
          </div>

          <p className="text-sm text-gray-700 mb-3 line-clamp-2">
            <span className="font-medium">신고 내용:</span> {truncateText(report.content, 100)}
          </p>

          <p className="text-xs text-gray-500">{formatDateTime(report.createdAt)}</p>
        </div>

        <div className="flex-shrink-0">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onViewDetail(report)}
            className="min-h-[48px] px-4 bg-white hover:bg-purple-50 border-purple-200 text-purple-700 hover:text-purple-800 font-medium transition-colors duration-200 touch-manipulation"
          >
            <Eye className="w-4 h-4 mr-2" />
            상세보기
          </Button>
        </div>
      </div>
    </div>
  );
});

ReportCard.displayName = "ReportCard";