"use client";

import React, { useState, useMemo, useCallback, memo } from "react";
import { Card, Badge, Button, Loading } from "@/components";
import { Filter, ChevronDown, AlertTriangle, AlertCircle } from "lucide-react";
import { ReportFilters } from "./ReportFilters";
import { ReportCard } from "./ReportCard";
import { MobileReportCard } from "./MobileReportCard";
import { BoardPagination } from "@/components/organisms/board/board-pagination";
import { LazyReportDetailModal as ReportDetailModal } from "@/lib/utils/lazy-components";
import type { Report } from "@/types/domains/admin";

interface ReportListContainerProps {
  reports: Report[];
  isLoading: boolean;
  error: string | null;
  refetch: () => void;
  filterType: string;
  setFilterType: (type: string) => void;
  page: number;
  setPage: (page: number) => void;
  totalPages: number;
}

const ReportListContainerComponent: React.FC<ReportListContainerProps> = ({
  reports,
  isLoading,
  error,
  refetch,
  filterType,
  setFilterType,
  page,
  setPage,
  totalPages,
}) => {
  const [showFilters, setShowFilters] = useState(false);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);

  // 이벤트 핸들러 최적화
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
          <AlertTriangle className="w-8 h-8 stroke-amber-600 fill-amber-100" />
        </div>
        <div>
          <h3 className="text-lg font-medium text-brand-primary mb-1">
            신고 내역이 없습니다
          </h3>
          <p className="text-sm text-brand-secondary">
            아직 처리할 신고가 없습니다.
          </p>
        </div>
      </div>
    </Card>
  ), []);

  if (isLoading) {
    return <Loading type="card" message="신고 목록을 불러오는 중..." />;
  }

  // 에러 상태
  if (error) {
    return (
      <Card className="p-12 text-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
            <AlertCircle className="w-8 h-8 stroke-red-600 fill-red-100" />
          </div>
          <div>
            <h3 className="text-lg font-medium text-brand-primary mb-1">
              신고 목록을 불러올 수 없습니다
            </h3>
            <p className="text-sm text-brand-secondary mb-4">
              {error}
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={refetch}
              className="text-purple-600 border-purple-200 hover:bg-purple-50"
            >
              다시 시도
            </Button>
          </div>
        </div>
      </Card>
    );
  }

  return (
    <>
      <div className="space-y-6">
        {/* 필터 영역 */}
        <div className="bg-white rounded-xl shadow-brand-sm border border-gray-100 p-4 sm:p-6">
          <div className="space-y-4">
            {/* 필터 토글 버튼 (모바일) */}
            <Button
              variant="outline"
              size="sm"
              onClick={handleToggleFilters}
              className="sm:hidden w-full flex items-center justify-between"
            >
              <span className="flex items-center gap-2">
                <Filter className="w-4 h-4 stroke-slate-600" />
                필터 옵션
              </span>
              <ChevronDown className={`w-4 h-4 transition-transform stroke-slate-600 ${showFilters ? 'rotate-180' : ''}`} />
            </Button>

            {/* 필터 옵션 */}
            <div className={`${showFilters ? 'block' : 'hidden'} sm:block`}>
              <ReportFilters
                filterType={filterType}
                setFilterType={setFilterType}
              />
            </div>
          </div>
        </div>

        {/* 신고 목록 헤더 */}
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-brand-primary">
            신고 목록
            <Badge variant="secondary" className="ml-2">
              {reports.length}건
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
        {reports.length === 0 ? (
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
                      {reports.map((report) => (
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
              {reports.map((report) => (
                <MobileReportCard
                  key={`mobile-report-${report.id}`}
                  report={report}
                  onView={() => handleReportView(report)}
                />
              ))}
            </div>
          </div>
        )}

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="flex justify-center mt-6">
            <BoardPagination
              currentPage={page}
              totalPages={totalPages}
              setCurrentPage={setPage}
            />
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