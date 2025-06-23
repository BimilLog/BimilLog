import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { AlertTriangle, Eye, Clock, CheckCircle, XCircle } from "lucide-react";
import { type Report, type PageResponse } from "@/lib/api";
import { ReportDetailModal } from "./ReportDetailModal";

interface ReportListProps {
  reports: PageResponse<Report> | null;
  isLoading: boolean;
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  filterType: string;
  setFilterType: (type: string) => void;
}

export const ReportList: React.FC<ReportListProps> = ({
  reports,
  isLoading,
  searchTerm,
  setSearchTerm,
  filterType,
  setFilterType,
}) => {
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const getStatusColor = (status: string) => {
    switch (status) {
      case "pending":
        return "bg-yellow-100 text-yellow-800 border-yellow-200";
      case "investigating":
        return "bg-blue-100 text-blue-800 border-blue-200";
      case "resolved":
        return "bg-green-100 text-green-800 border-green-200";
      case "rejected":
        return "bg-red-100 text-red-800 border-red-200";
      default:
        return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case "pending":
        return <Clock className="w-4 h-4" />;
      case "investigating":
        return <Eye className="w-4 h-4" />;
      case "resolved":
        return <CheckCircle className="w-4 h-4" />;
      case "rejected":
        return <XCircle className="w-4 h-4" />;
      default:
        return <Clock className="w-4 h-4" />;
    }
  };

  const getReportTypeLabel = (type: string) => {
    switch (type) {
      case "POST":
        return "게시글";
      case "COMMENT":
        return "댓글";
      case "ERROR":
        return "오류";
      case "IMPROVEMENT":
        return "개선사항";
      default:
        return "기타";
    }
  };

  const handleViewReport = (report: Report) => {
    setSelectedReport(report);
    setIsModalOpen(true);
  };

  return (
    <>
      <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-lg">
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <AlertTriangle className="w-5 h-5 text-red-600" />
            <span>신고 목록</span>
          </CardTitle>
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2">
              <Input
                placeholder="검색..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-64 bg-white"
              />
            </div>
            <div className="flex items-center space-x-2">
              <Select value={filterType} onValueChange={setFilterType}>
                <SelectTrigger className="w-32 bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체</SelectItem>
                  <SelectItem value="POST">게시글</SelectItem>
                  <SelectItem value="COMMENT">댓글</SelectItem>
                  <SelectItem value="ERROR">오류</SelectItem>
                  <SelectItem value="IMPROVEMENT">개선사항</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
              <p className="mt-4 text-gray-600">신고 목록을 불러오는 중...</p>
            </div>
          ) : reports && reports.content.length > 0 ? (
            <div className="space-y-4">
              {reports.content
                .filter(
                  (report) =>
                    searchTerm === "" ||
                    report.content
                      ?.toLowerCase()
                      .includes(searchTerm.toLowerCase()) ||
                    report.targetTitle
                      ?.toLowerCase()
                      .includes(searchTerm.toLowerCase())
                )
                .map((report) => (
                  <div
                    key={report.reportId}
                    className="flex items-center justify-between p-4 bg-white rounded-lg border border-gray-200"
                  >
                    <div className="flex-1">
                      <div className="flex items-center space-x-2 mb-2">
                        <Badge variant="outline" className="text-xs">
                          {getReportTypeLabel(report.reportType)}
                        </Badge>
                        <Badge
                          className={`text-xs ${getStatusColor(
                            report.status || "pending"
                          )}`}
                        >
                          <div className="flex items-center space-x-1">
                            {getStatusIcon(report.status || "pending")}
                            <span>
                              {report.status === "pending"
                                ? "대기중"
                                : report.status === "investigating"
                                ? "조사중"
                                : report.status === "resolved"
                                ? "해결됨"
                                : "반려됨"}
                            </span>
                          </div>
                        </Badge>
                      </div>
                      <h3 className="font-medium text-gray-800 mb-1">
                        {report.targetTitle || "신고 내용"}
                      </h3>
                      <p className="text-sm text-gray-600 mb-2">
                        대상 ID: {report.targetId} | 신고자 ID: {report.userId}
                      </p>
                      <p className="text-sm text-gray-700 mb-2">
                        내용: {report.content}
                      </p>
                      <p className="text-xs text-gray-500">
                        {report.createdAt || "날짜 미상"}
                      </p>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleViewReport(report)}
                      >
                        <Eye className="w-4 h-4 mr-2" />
                        상세보기
                      </Button>
                    </div>
                  </div>
                ))}
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              신고 내역이 없습니다.
            </div>
          )}
        </CardContent>
      </Card>

      <ReportDetailModal
        report={selectedReport}
        isOpen={isModalOpen}
        onOpenChange={setIsModalOpen}
      />
    </>
  );
};
