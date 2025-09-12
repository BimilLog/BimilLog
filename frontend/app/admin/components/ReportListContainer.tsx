import React, { useState } from "react";
import { 
  Card, 
  CardContent, 
  CardHeader, 
  CardTitle, 
  AlertTriangle 
} from "@/components";
import { type Report } from "@/lib/api";
import { useMediaQuery } from "@/hooks/useMediaQuery";
import { ReportCard } from "./ReportCard";
import { MobileReportCard } from "./MobileReportCard";
import { ReportFilters } from "./ReportFilters";
import { EmptyState } from "./EmptyState";
import { LoadingState } from "./LoadingState";
import { ReportDetailModalImproved } from "./ReportDetailModalImproved";

interface ReportListContainerProps {
  reports: Report[];
  isLoading: boolean;
  error?: string | null;
  searchTerm: string;
  onSearchChange: (value: string) => void;
  filterType: string;
  onFilterChange: (value: string) => void;
  onReportUpdated?: () => void;
}

export const ReportListContainer: React.FC<ReportListContainerProps> = ({
  reports,
  isLoading,
  error,
  searchTerm,
  onSearchChange,
  filterType,
  onFilterChange,
  onReportUpdated,
}) => {
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const isMobile = useMediaQuery("(max-width: 640px)");

  const handleViewReport = (report: Report) => {
    setSelectedReport(report);
    setIsModalOpen(true);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedReport(null);
  };

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

          <div className="mt-6">
            <ReportFilters
              searchTerm={searchTerm}
              onSearchChange={onSearchChange}
              filterType={filterType}
              onFilterChange={onFilterChange}
            />
          </div>
        </CardHeader>

        <CardContent>
          {error ? (
            <div className="flex flex-col items-center justify-center py-12">
              <div className="p-6 rounded-full bg-red-100 mb-4">
                <AlertTriangle className="w-12 h-12 text-red-500" />
              </div>
              <h3 className="text-lg font-medium text-gray-700 mb-2">오류가 발생했습니다</h3>
              <p className="text-sm text-gray-500 text-center max-w-md">{error}</p>
            </div>
          ) : isLoading ? (
            <LoadingState />
          ) : reports.length > 0 ? (
            <div className="space-y-4">
              {reports.map((report) => 
                isMobile ? (
                  <MobileReportCard
                    key={report.id}
                    report={report}
                    onViewDetail={handleViewReport}
                  />
                ) : (
                  <ReportCard
                    key={report.id}
                    report={report}
                    onViewDetail={handleViewReport}
                  />
                )
              )}
            </div>
          ) : (
            <EmptyState
              hasSearchFilter={true}
              searchTerm={searchTerm}
              filterType={filterType}
            />
          )}
        </CardContent>
      </Card>

      <ReportDetailModalImproved
        report={selectedReport}
        isOpen={isModalOpen}
        onOpenChange={handleModalClose}
        onReportUpdated={onReportUpdated}
      />
    </>
  );
};