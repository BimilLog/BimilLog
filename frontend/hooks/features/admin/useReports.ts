"use client";

import { useState, useEffect, useCallback, useRef } from "react";
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

  // 이전 filterType을 추적하여 중복 API 호출 방지
  const prevFilterType = useRef(filterType);

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

  // 필터 타입 변경 시 페이지를 0으로 리셋 (중복 호출 방지)
  useEffect(() => {
    if (prevFilterType.current !== filterType) {
      prevFilterType.current = filterType;
      setPage(0);
    }
  }, [filterType]);

  // 페이지 또는 페이지 크기 변경 시 데이터 fetch
  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  const totalElements = reports?.totalElements || 0;
  const totalPages = reports?.totalPages || 0;

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
    refetch: fetchReports,
  };
}