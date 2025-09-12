import { useState, useEffect, useCallback, useMemo } from "react";
import { adminApi, type Report, type PageResponse } from "@/lib/api";
import { useDebounce } from "@/hooks/useDebounce";

interface UseReportsOptions {
  initialFilterType?: string;
  initialSearchTerm?: string;
  pageSize?: number;
}

export function useReports(options: UseReportsOptions = {}) {
  const {
    initialFilterType = "all",
    initialSearchTerm = "",
    pageSize = 20,
  } = options;

  const [reports, setReports] = useState<PageResponse<Report> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterType, setFilterType] = useState(initialFilterType);
  const [searchTerm, setSearchTerm] = useState(initialSearchTerm);
  const [page, setPage] = useState(0);

  const debouncedSearchTerm = useDebounce(searchTerm, 300);

  const fetchReports = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const reportType = filterType === "all" ? undefined : filterType;
      const response = await adminApi.getReports(page, pageSize, reportType);
      
      if (response.success && response.data) {
        setReports(response.data as PageResponse<Report>);
      } else {
        throw new Error(response.error || "Failed to fetch reports");
      }
    } catch (error) {
      console.error("Failed to fetch reports:", error);
      setError(error instanceof Error ? error.message : "Failed to fetch reports");
    } finally {
      setIsLoading(false);
    }
  }, [filterType, page, pageSize]);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  const filteredReports = useMemo(() => {
    if (!reports?.content) return [];
    if (!debouncedSearchTerm) return reports.content;

    const searchLower = debouncedSearchTerm.toLowerCase();
    return reports.content.filter(
      (report) =>
        report.content?.toLowerCase().includes(searchLower) ||
        report.targetTitle?.toLowerCase().includes(searchLower) ||
        report.reporterName?.toLowerCase().includes(searchLower) ||
        (report.targetId && report.targetId.toString().includes(debouncedSearchTerm))
    );
  }, [reports?.content, debouncedSearchTerm]);

  const totalElements = reports?.totalElements || 0;
  const totalPages = reports?.totalPages || 0;
  const hasNextPage = page < totalPages - 1;
  const hasPreviousPage = page > 0;

  return {
    reports: filteredReports,
    isLoading,
    error,
    filterType,
    setFilterType,
    searchTerm,
    setSearchTerm,
    page,
    setPage,
    totalElements,
    totalPages,
    hasNextPage,
    hasPreviousPage,
    refetch: fetchReports,
  };
}