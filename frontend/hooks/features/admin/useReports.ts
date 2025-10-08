"use client";

import { useState, useEffect, useCallback } from "react";
import { adminQuery, type Report, type PageResponse } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

interface UseReportsOptions {
  initialFilterType?: string;
  pageSize?: number;
}

export function useReports(options: UseReportsOptions = {}) {
  const {
    initialFilterType = "all",
    pageSize = 20,
  } = options;

  const [reports, setReports] = useState<PageResponse<Report> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterType, setFilterType] = useState(initialFilterType);
  const [page, setPage] = useState(0);

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

  const totalElements = reports?.totalElements || 0;
  const totalPages = reports?.totalPages || 0;
  const hasNextPage = page < totalPages - 1;
  const hasPreviousPage = page > 0;

  return {
    reports: reports?.content || [],
    isLoading,
    error,
    filterType,
    setFilterType,
    page,
    setPage,
    totalElements,
    totalPages,
    hasNextPage,
    hasPreviousPage,
    refetch: fetchReports,
  };
}