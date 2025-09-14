"use client";

import React, { useState, useEffect, useMemo, useCallback, memo } from "react";
import { Card, Input, Badge, Button, Loading } from "@/components";
import { Search, Filter, ChevronDown, AlertTriangle } from "lucide-react";
import { ReportFilters } from "./ReportFilters";
import { ReportCard } from "./ReportCard";
import { MobileReportCard } from "./MobileReportCard";
import { LazyReportDetailModal as ReportDetailModal } from "@/lib/utils/lazy-components";
import type { Report } from "@/types/domains/admin";

interface ReportListContainerProps {
  reports: Report[];
  isLoading: boolean;
  refetch: () => void;
  initialFilterType?: string;
  initialSearchTerm?: string;
}

const ReportListContainerComponent: React.FC<ReportListContainerProps> = ({
  reports,
  isLoading,
  refetch,
  initialFilterType = "all",
  initialSearchTerm = ""
}) => {
  const [searchTerm, setSearchTerm] = useState(initialSearchTerm);
  const [filterType, setFilterType] = useState(initialFilterType);
  const [showFilters, setShowFilters] = useState(false);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);

  // 검색 및 필터링 최적화 (debounce 효과)
  const filteredReports = useMemo(() => {
    return reports.filter(report => {
      const matchesSearch = searchTerm === "" ||
        report.targetId?.toString().includes(searchTerm) ||
        report.reporterName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        report.content?.toLowerCase().includes(searchTerm.toLowerCase());
      
      const matchesFilter = filterType === "all" || report.reportType === filterType;
      
      return matchesSearch && matchesFilter;
    });
  }, [reports, searchTerm, filterType]);

  // 신고 개수 계산 메모화
  const reportCounts = useMemo(() => ({
    all: reports.length,
    POST: reports.filter(r => r.reportType === "POST").length,
    COMMENT: reports.filter(r => r.reportType === "COMMENT").length,
    ERROR: reports.filter(r => r.reportType === "ERROR").length,
    IMPROVEMENT: reports.filter(r => r.reportType === "IMPROVEMENT").length
  }), [reports]);

  // 이벤트 핸들러 최적화
  const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  }, []);

  const handleToggleFilters = useCallback(() => {
    setShowFilters(prev => !prev);
  }, []);

  const handleReportView = useCallback((report: Report) => {
    setSelectedReport(report);
  }, []);

  const handleCloseModal = useCallback(() => {
    setSelectedReport(null);
  }, []);

  // 빈 상태 컴포넌트 메모화
  const EmptyStateComponent = useMemo(() => (
    <Card className="p-12 text-center">
      <div className="flex flex-col items-center gap-4">
        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center">
          <AlertTriangle className="w-8 h-8 text-brand-secondary" />
        </div>
        <div>
          <h3 className="text-lg font-medium text-brand-primary mb-1">
            신고 내역이 없습니다
          </h3>
          <p className="text-sm text-brand-secondary">
            {searchTerm ? "검색 결과가 없습니다. 다른 검색어를 시도해보세요." : "아직 처리할 신고가 없습니다."}
          </p>
        </div>
      </div>
    </Card>
  ), [searchTerm]);

  if (isLoading) {
    return <Loading type="card" message="신고 목록을 불러오는 중..." />;
  }

  return (
    <>
      <div className="space-y-6">
        {/* 검색 및 필터 영역 */}
        <div className="bg-white rounded-xl shadow-brand-sm border border-gray-100 p-4 sm:p-6">
          <div className="space-y-4">
            {/* 검색바 */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-brand-secondary" />
              <Input
                type="text"
                placeholder="신고 ID, 사용자명, 신고 사유로 검색..."
                value={searchTerm}
                onChange={handleSearchChange}
                className="pl-10 pr-4 py-2.5 w-full border-gray-200 focus:border-purple-500 focus:ring-purple-500"
              />
            </div>

            {/* 필터 토글 버튼 (모바일) */}
            <Button
              variant="outline"
              size="sm"
              onClick={handleToggleFilters}
              className="sm:hidden w-full flex items-center justify-between"
            >
              <span className="flex items-center gap-2">
                <Filter className="w-4 h-4" />
                필터 옵션
              </span>
              <ChevronDown className={`w-4 h-4 transition-transform ${showFilters ? 'rotate-180' : ''}`} />
            </Button>

            {/* 필터 옵션 */}
            <div className={`${showFilters ? 'block' : 'hidden'} sm:block`}>
              <ReportFilters
                filterType={filterType}
                setFilterType={setFilterType}
                reportCounts={reportCounts}
              />
            </div>
          </div>
        </div>

        {/* 신고 목록 헤더 */}
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-brand-primary">
            신고 목록
            <Badge variant="secondary" className="ml-2">
              {filteredReports.length}건
            </Badge>
          </h2>
          <Button
            variant="ghost"
            size="sm"
            onClick={refetch}
            className="text-brand-muted hover:text-brand-primary"
          >
            새로고침
          </Button>
        </div>

        {/* 신고 목록 */}
        {filteredReports.length === 0 ? (
          EmptyStateComponent
        ) : (
          <div className="space-y-4">
            {/* 데스크톱 뷰 */}
            <div className="hidden sm:block">
              <Card className="overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-gray-50 border-b border-gray-200">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-brand-secondary uppercase tracking-wider">
                          신고 정보
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-brand-secondary uppercase tracking-wider">
                          신고 대상
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-brand-secondary uppercase tracking-wider">
                          신고 사유
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-brand-secondary uppercase tracking-wider">
                          신고일
                        </th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-brand-secondary uppercase tracking-wider">
                          작업
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {filteredReports.map((report) => (
                        <ReportCard
                          key={`desktop-report-${report.id}`}
                          report={report}
                          onView={() => handleReportView(report)}
                        />
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card>
            </div>

            {/* 모바일 뷰 */}
            <div className="sm:hidden space-y-3">
              {filteredReports.map((report) => (
                <MobileReportCard
                  key={`mobile-report-${report.id}`}
                  report={report}
                  onView={() => handleReportView(report)}
                />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 신고 상세 모달 */}
      {selectedReport && (
        <ReportDetailModal
          report={selectedReport}
          isOpen={!!selectedReport}
          onClose={handleCloseModal}
          onAction={refetch}
        />
      )}
    </>
  );
};

export const ReportListContainer = memo(ReportListContainerComponent);