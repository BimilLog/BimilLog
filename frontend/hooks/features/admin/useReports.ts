"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { adminQuery, type Report, type PageResponse } from "@/lib/api";
import { useDebounce } from "@/hooks/common/useDebounce";
import { logger } from '@/lib/utils/logger';

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

  // 신고 목록 조회: 필터 타입, 페이지, 페이지 크기에 따른 신고 데이터 가져오기
  const fetchReports = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      // "all" 필터인 경우 undefined로 전송하여 모든 타입 조회
      const reportType = filterType === "all" ? undefined : filterType;
      const response = await adminQuery.getReports(page, pageSize, reportType);

      if (response.success && response.data) {
        setReports(response.data as PageResponse<Report>);
      } else {
        throw new Error(response.error || "Failed to fetch reports");
      }
    } catch (error) {
      logger.error("Failed to fetch reports:", error);
      setError(error instanceof Error ? error.message : "Failed to fetch reports");
    } finally {
      setIsLoading(false);
    }
  }, [filterType, page, pageSize]);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  // 검색어 기반 신고 목록 필터링: 신고 내용, 신고자명, 대상 ID 검색
  const filteredReports = useMemo(() => {
    if (!reports?.content) return [];
    if (!debouncedSearchTerm) return reports.content;

    const searchLower = debouncedSearchTerm.toLowerCase();
    return reports.content.filter(
      (report) =>
        report.content?.toLowerCase().includes(searchLower) ||
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