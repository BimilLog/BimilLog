import React from "react";
import { Badge, Clock } from "@/components";
import { ChevronRight } from "lucide-react";
import { type Report } from "@/lib/api";
import { 
  getReportTypeConfig, 
  formatDateTime, 
  truncateText, 
  hasActionableTarget 
} from "@/lib/admin-utils";

interface MobileReportCardProps {
  report: Report;
  onViewDetail: (report: Report) => void;
}

export const MobileReportCard = React.memo<MobileReportCardProps>(({ 
  report, 
  onViewDetail 
}) => {
  const typeConfig = getReportTypeConfig(report.reportType);

  return (
    <div 
      onClick={() => onViewDetail(report)}
      className="block w-full p-4 bg-white rounded-lg border border-gray-200 active:bg-gray-50 transition-colors touch-manipulation cursor-pointer"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex flex-wrap items-center gap-2">
          <Badge className={`text-xs font-medium ${typeConfig.color}`}>
            {typeConfig.label}
          </Badge>
          <Badge className="text-xs bg-yellow-100 text-yellow-800 border-yellow-200 font-medium">
            <Clock className="w-3 h-3 mr-1" />
            대기중
          </Badge>
        </div>
        <ChevronRight className="w-5 h-5 text-gray-400 flex-shrink-0" />
      </div>

      <h3 className="font-semibold text-gray-900 mb-2 text-sm line-clamp-1">
        {report.reportType === "ERROR" || report.reportType === "IMPROVEMENT"
          ? `${typeConfig.label} 신고`
          : report.targetTitle || "신고된 콘텐츠"}
      </h3>

      <div className="space-y-1 text-xs text-gray-600 mb-2">
        <p className="truncate">
          <span className="font-medium">신고자:</span> {report.reporterName}
        </p>
        {report.targetId && hasActionableTarget(report.reportType) && (
          <p className="truncate">
            <span className="font-medium">대상 ID:</span> {report.targetId}
          </p>
        )}
      </div>

      <p className="text-xs text-gray-700 line-clamp-2 mb-2">
        {truncateText(report.content, 80)}
      </p>

      <p className="text-xs text-gray-500">
        {formatDateTime(report.createdAt)}
      </p>
    </div>
  );
});

MobileReportCard.displayName = "MobileReportCard";