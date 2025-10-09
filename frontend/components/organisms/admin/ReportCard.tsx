"use client";

import React from "react";
import { Badge, Button } from "@/components";
import { Calendar, FileText, Eye } from "lucide-react";
import { formatDate } from "@/lib/utils";
import type { Report } from "@/types/domains/admin";

interface ReportCardProps {
  report: Report;
  onView: () => void;
}

export const ReportCard = React.memo<ReportCardProps>(({ report, onView }) => {
  const getReportTypeLabel = (type: string) => {
    switch(type) {
      case "POST": return "게시글";
      case "COMMENT": return "댓글";
      case "ERROR": return "오류";
      case "IMPROVEMENT": return "개선";
      default: return type;
    }
  };

  const getReportTypeColor = (type: string) => {
    switch(type) {
      case "POST": return "bg-yellow-100 text-yellow-700";
      case "COMMENT": return "bg-green-100 text-green-700";
      case "ERROR": return "bg-red-100 text-red-700";
      case "IMPROVEMENT": return "bg-blue-100 text-blue-700";
      default: return "bg-gray-100 text-brand-primary";
    }
  };

  return (
    <tr className="hover:bg-gray-50 transition-colors">
      <td className="px-6 py-4 whitespace-nowrap">
        <div className="flex items-center gap-3">
          <div>
            <div className="text-sm font-medium text-brand-primary">
              #{report.id}
            </div>
            <Badge className={`text-xs ${getReportTypeColor(report.reportType)}`}>
              {getReportTypeLabel(report.reportType)}
            </Badge>
          </div>
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-xs text-brand-secondary">신고자:</span>
            <span className="text-sm text-brand-primary">
              {report.reporterName || "익명"}
            </span>
          </div>
          {(report.reportType === "POST" || report.reportType === "COMMENT") && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-brand-secondary">대상:</span>
              <span className="text-sm text-brand-primary font-medium">
                {report.targetAuthorName || "삭제됨"}
              </span>
            </div>
          )}
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center gap-2">
          <FileText className="w-4 h-4 stroke-blue-600 fill-blue-100" />
          <span className="text-sm text-brand-muted line-clamp-2">
            {report.content}
          </span>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap">
        <div className="flex items-center gap-2 text-sm text-brand-secondary">
          <Calendar className="w-4 h-4 stroke-indigo-600 fill-indigo-100" />
          {formatDate(report.createdAt)}
        </div>
      </td>
      <td className="px-6 py-4 text-right">
        <Button
          variant="ghost"
          size="sm"
          onClick={onView}
          className="text-purple-600 hover:text-purple-700 hover:bg-purple-50"
        >
          <Eye className="w-4 h-4 mr-1 stroke-purple-600 fill-purple-100" />
          상세보기
        </Button>
      </td>
    </tr>
  );
});
ReportCard.displayName = "ReportCard";