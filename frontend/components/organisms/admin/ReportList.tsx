import { useState } from "react";
import { 
  Button, 
  Card, 
  CardContent, 
  CardHeader, 
  CardTitle, 
  Input, 
  Badge, 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  AlertTriangle,
  Eye,
  Clock 
} from "@/components";
import { type Report, type PageResponse } from "@/lib/api";
import { getReportTypeConfig, formatDateTime, truncateText, hasActionableTarget } from "@/lib/admin-utils";
import { ReportDetailModal } from "./ReportDetailModal";

interface ReportListProps {
  reports: PageResponse<Report> | null;
  isLoading: boolean;
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  filterType: string;
  setFilterType: (type: string) => void;
  onReportUpdated?: () => void;
}

export const ReportList: React.FC<ReportListProps> = ({
  reports,
  isLoading,
  searchTerm,
  setSearchTerm,
  filterType,
  setFilterType,
  onReportUpdated,
}) => {
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleViewReport = (report: Report) => {
    setSelectedReport(report);
    setIsModalOpen(true);
  };

  const filteredReports = reports?.content?.filter(
    (report) =>
      searchTerm === "" ||
      report.content
        ?.toLowerCase()
        .includes(searchTerm.toLowerCase()) ||
      report.targetTitle
        ?.toLowerCase()
        .includes(searchTerm.toLowerCase()) ||
      report.reporterName
        ?.toLowerCase()
        .includes(searchTerm.toLowerCase()) ||
      (report.targetId && report.targetId.toString().includes(searchTerm))
  ) || [];

  return (
    <>
      <Card className="bg-white/90 backdrop-blur-sm border-0 shadow-lg hover:shadow-xl transition-all duration-200 rounded-lg">
        <CardHeader className="pb-4">
          <CardTitle className="flex items-center gap-3">
            <div className="p-2 rounded-full bg-gradient-to-r from-red-100 to-pink-100">
              <AlertTriangle className="w-5 h-5 text-red-600" />
            </div>
            <span className="text-xl font-semibold text-gray-800">신고 목록</span>
          </CardTitle>
          
          <div className="flex flex-col sm:flex-row gap-4 mt-6">
            <div className="flex-1">
              <Input
                placeholder="신고 내용, 제목, 신고자명으로 검색..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="bg-white border-gray-200 focus:border-purple-400 focus:ring-purple-400/20"
              />
            </div>
            <div className="flex-shrink-0">
              <Select value={filterType} onValueChange={setFilterType}>
                <SelectTrigger className="w-full sm:w-40 bg-white border-gray-200 focus:border-purple-400 focus:ring-purple-400/20">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 유형</SelectItem>
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
            <div className="flex flex-col items-center justify-center py-12">
              <div className="animate-spin rounded-full h-10 w-10 border-2 border-purple-600 border-t-transparent mb-4"></div>
              <p className="text-gray-600 font-medium">신고 목록을 불러오는 중...</p>
              <p className="text-sm text-gray-500 mt-1">잠시만 기다려주세요</p>
            </div>
          ) : filteredReports.length > 0 ? (
            <div className="space-y-4">
              {filteredReports.map((report) => {
                const typeConfig = getReportTypeConfig(report.reportType);
                
                return (
                  <div
                    key={report.id}
                    className="group p-6 bg-gradient-to-br from-white to-gray-50 rounded-xl border border-gray-200 hover:border-purple-300 hover:shadow-md transition-all duration-200"
                  >
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
                          {report.reportType === 'ERROR' || report.reportType === 'IMPROVEMENT' 
                            ? `${typeConfig.label} 신고`
                            : report.targetTitle || "신고된 콘텐츠"
                          }
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
                        
                        <p className="text-xs text-gray-500">
                          {formatDateTime(report.createdAt)}
                        </p>
                      </div>
                      
                      <div className="flex-shrink-0">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleViewReport(report)}
                          className="min-h-[48px] px-4 bg-white hover:bg-purple-50 border-purple-200 text-purple-700 hover:text-purple-800 font-medium transition-colors duration-200"
                        >
                          <Eye className="w-4 h-4 mr-2" />
                          상세보기
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <div className="p-6 rounded-full bg-gray-100 mb-4">
                <AlertTriangle className="w-12 h-12 text-gray-400" />
              </div>
              <h3 className="text-lg font-medium text-gray-700 mb-2">신고 내역이 없습니다</h3>
              <p className="text-sm text-gray-500 text-center max-w-md">
                {searchTerm || filterType !== "all" 
                  ? "검색 조건에 맞는 신고가 없습니다. 다른 검색어나 필터를 시도해보세요." 
                  : "현재 처리 대기 중인 신고가 없습니다."
                }
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      <ReportDetailModal
        report={selectedReport}
        isOpen={isModalOpen}
        onOpenChange={setIsModalOpen}
        onReportUpdated={onReportUpdated}
      />
    </>
  );
};