"use client";

import React, { useState, useMemo, useCallback, useEffect, useRef, memo } from "react";
import { Card, Button, Loading, EmptyView } from "@/components";
import { Filter, ChevronDown, AlertTriangle, AlertCircle, RefreshCcw } from "lucide-react";
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
  totalElements: number;
  totalPages: number;
  /**
   * 라운드 16 F-16-036: 차단/탈퇴 시 동일 targetId 신고를 옵티미스틱 제거하는 콜백.
   * 모달이 mutation 성공 후 호출.
   */
  removeResolvedReports: (targetId: number, reportType: string) => number;
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
  totalElements,
  totalPages,
  removeResolvedReports,
}) => {
  const [showFilters, setShowFilters] = useState(false);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [isModalActionInFlight, setIsModalActionInFlight] = useState(false);

  // 라운드 16 F-16-017/F-16-028: 페이지/필터 변경 후 list 영역에 focus 이동
  const listRef = useRef<HTMLDivElement | null>(null);
  const lastPageRef = useRef(page);
  const lastFilterRef = useRef(filterType);

  // 이벤트 핸들러 최적화
  const handleToggleFilters = useCallback(() => {
    setShowFilters(prev => !prev);
  }, []);

  const handleReportView = useCallback((report: Report) => {
    // F-16-037: 차단/탈퇴 진행 중이면 다른 카드 열기 차단
    if (isModalActionInFlight) return;
    setSelectedReport(report);
  }, [isModalActionInFlight]);

  const handleCloseModal = useCallback(() => {
    setSelectedReport(null);
  }, []);

  /**
   * F-16-036: 모달의 mutation 성공 시 호출. 옵티미스틱 제거 후 closing.
   * onAction 의 책임 = 같은 대상의 신고를 함께 정리 (refetch 는 호출하지 않아도 됨).
   */
  const handleModalAction = useCallback(
    (targetId: number, reportType: string) => {
      removeResolvedReports(targetId, reportType);
    },
    [removeResolvedReports],
  );

  useEffect(() => {
    if (lastPageRef.current !== page || lastFilterRef.current !== filterType) {
      lastPageRef.current = page;
      lastFilterRef.current = filterType;
      // 다음 페인트 후에 focus (DOM 업데이트 보장)
      const id = window.requestAnimationFrame(() => {
        listRef.current?.focus();
      });
      return () => window.cancelAnimationFrame(id);
    }
  }, [page, filterType]);

  // 빈 상태 컴포넌트 메모화 — paper 토큰 일관 (F-13-BUG-17 ✓ 회귀 검증)
  const EmptyStateComponent = useMemo(() => (
    <Card className="bg-paper-card border border-stamp-red/20">
      <EmptyView
        icon={
          <AlertTriangle
            className="w-9 h-9 stroke-stamp-red"
            strokeWidth={1.6}
            aria-hidden="true"
          />
        }
        title="처리할 신고가 없어요"
        description="새로운 신고가 들어오면 여기에 표시돼요."
      />
    </Card>
  ), []);

  // 로딩 — F-16-013: 첫 로딩만 전체 placeholder, refetch 시엔 placeholder 유지 안 됨이지만
  // 백엔드 갱신은 전체 페이지 교체이므로 카드 영역만 Loading 으로 처리.
  if (isLoading && reports.length === 0) {
    return <Loading type="card" message="신고 목록을 불러오는 중..." />;
  }

  // 에러 상태 — paper 토큰 + stamp-red CTA (F-13-BUG-17 ✓)
  if (error) {
    return (
      <Card className="bg-paper-card border border-stamp-red/30">
        <EmptyView
          assertive
          icon={
            <AlertCircle
              className="w-9 h-9 stroke-stamp-red"
              strokeWidth={1.6}
              aria-hidden="true"
            />
          }
          title="신고 목록을 불러올 수 없어요"
          description={error}
          onRetry={refetch}
        />
      </Card>
    );
  }

  return (
    <>
      <div className="space-y-6">
        {/* 필터 영역 — paper 토큰 (F-16-012) */}
        <div className="bg-paper-card border border-postal-navy/15 rounded-xl shadow-brand-sm p-4 sm:p-6">
          <div className="space-y-4">
            {/* 필터 토글 버튼 (모바일) — F-16-014 aria-expanded / aria-controls */}
            <Button
              variant="outline"
              size="sm"
              onClick={handleToggleFilters}
              aria-expanded={showFilters}
              aria-controls="report-filters-panel"
              className="sm:hidden w-full flex items-center justify-between min-h-[44px]"
            >
              <span className="flex items-center gap-2">
                <Filter className="w-4 h-4 stroke-postal-navy" aria-hidden="true" />
                필터 옵션
              </span>
              <ChevronDown
                className={`w-4 h-4 transition-transform stroke-postal-navy ${showFilters ? 'rotate-180' : ''}`}
                aria-hidden="true"
              />
            </Button>

            {/* 필터 옵션 */}
            <div
              id="report-filters-panel"
              className={`${showFilters ? 'block' : 'hidden'} sm:block`}
            >
              <ReportFilters
                filterType={filterType}
                setFilterType={setFilterType}
              />
            </div>
          </div>
        </div>

        {/* 신고 목록 헤더 */}
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold font-display text-ink dark:text-foreground break-keep">
            신고 목록
            {totalElements > 0 && (
              <span
                className="ml-2 text-sm font-medium text-ink-soft dark:text-muted-foreground"
                aria-label={`총 ${totalElements}건`}
              >
                · 총 {totalElements}건
              </span>
            )}
          </h2>
          <Button
            variant="ghost"
            size="sm"
            onClick={refetch}
            disabled={isLoading}
            className="text-ink-soft hover:text-postal-navy hover:bg-postal-navy/5 min-h-[44px]"
          >
            <RefreshCcw
              className={`w-4 h-4 mr-1 ${isLoading ? 'animate-spin' : ''}`}
              aria-hidden="true"
            />
            새로고침
          </Button>
        </div>

        {/* 신고 목록 — F-16-017 listRef + tabIndex */}
        <div
          ref={listRef}
          tabIndex={-1}
          aria-busy={isLoading}
          className="focus:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy/40 rounded-lg"
        >
          {reports.length === 0 ? (
            EmptyStateComponent
          ) : (
            <div className="space-y-4">
              {/* 데스크톱 뷰 — F-16-018/019 scope/aria-label */}
              <div className="hidden sm:block">
                <Card className="overflow-hidden bg-paper-card border border-postal-navy/15">
                  <div className="overflow-x-auto">
                    <table
                      className="w-full"
                      aria-label="신고 목록"
                    >
                      <thead className="bg-paper-soft border-b border-postal-navy/15">
                        <tr>
                          <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-ink-soft dark:text-muted-foreground uppercase tracking-wider"
                          >
                            신고 정보
                          </th>
                          <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-ink-soft dark:text-muted-foreground uppercase tracking-wider"
                          >
                            사용자 정보
                          </th>
                          <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-ink-soft dark:text-muted-foreground uppercase tracking-wider"
                          >
                            신고 사유
                          </th>
                          <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-ink-soft dark:text-muted-foreground uppercase tracking-wider"
                          >
                            신고일
                          </th>
                          <th
                            scope="col"
                            className="px-6 py-3 text-right text-xs font-medium text-ink-soft dark:text-muted-foreground uppercase tracking-wider"
                          >
                            작업
                          </th>
                        </tr>
                      </thead>
                      <tbody className="bg-paper-card divide-y divide-postal-navy/10">
                        {reports.map((report) => (
                          <ReportCard
                            key={`desktop-report-${report.id}`}
                            report={report}
                            onView={() => handleReportView(report)}
                            disabled={isModalActionInFlight}
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
                    disabled={isModalActionInFlight}
                  />
                ))}
              </div>
            </div>
          )}
        </div>

        {/* 페이지네이션 — F-16-028 nav aria-label */}
        {totalPages > 1 && (
          <nav
            aria-label="신고 페이지"
            className="flex justify-center mt-6"
          >
            <BoardPagination
              currentPage={page}
              totalPages={totalPages}
              setCurrentPage={setPage}
              totalElements={totalElements}
              itemLabel="건"
            />
          </nav>
        )}
      </div>

      {/* 신고 상세 모달 */}
      {selectedReport && (
        <ReportDetailModal
          report={selectedReport}
          isOpen={!!selectedReport}
          onClose={handleCloseModal}
          onAction={handleModalAction}
          onProcessingChange={setIsModalActionInFlight}
        />
      )}
    </>
  );
};

export const ReportListContainer = memo(ReportListContainerComponent);
