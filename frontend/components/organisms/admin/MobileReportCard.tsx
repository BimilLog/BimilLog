"use client";

import React from "react";
import { Card, Badge, Button } from "@/components";
import { Calendar, User, FileText, Eye } from "lucide-react";
import type { Report } from "@/types/domains/admin";

interface MobileReportCardProps {
  report: Report;
  onView: () => void;
}

export const MobileReportCard = React.memo<MobileReportCardProps>(({ report, onView }) => {
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
    <Card className="p-4 hover:shadow-brand-lg transition-shadow">
      <div className="space-y-3">
        {/* 헤더 */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-brand-primary">
              #{report.id}
            </span>
            <Badge className={`text-xs ${getReportTypeColor(report.reportType)}`}>
              {getReportTypeLabel(report.reportType)}
            </Badge>
          </div>
          <div className="text-xs text-brand-secondary flex items-center gap-1">
            <Calendar className="w-3 h-3" />
            {new Date(report.createdAt).toLocaleDateString('ko-KR')}
          </div>
        </div>

        {/* 신고 대상 */}
        <div className="space-y-1">
          <div className="flex items-center gap-2 text-sm">
            <User className="w-4 h-4 text-brand-secondary" />
            <span className="text-brand-primary">
              {report.reporterName || "익명"}
            </span>
            <span className="text-xs text-brand-secondary">
              (ID: {report.targetId})
            </span>
          </div>
        </div>

        {/* 신고 사유 */}
        <div className="space-y-1">
          <div className="flex items-start gap-2">
            <FileText className="w-4 h-4 text-brand-secondary mt-0.5" />
            <p className="text-sm text-brand-muted line-clamp-2 flex-1">
              {report.content}
            </p>
          </div>
        </div>

        {/* 액션 버튼 */}
        <Button
          variant="outline"
          size="sm"
          onClick={onView}
          className="w-full text-purple-600 border-purple-200 hover:bg-purple-50"
        >
          <Eye className="w-4 h-4 mr-2" />
          상세보기
        </Button>
      </div>
    </Card>
  );
});
MobileReportCard.displayName = "MobileReportCard";