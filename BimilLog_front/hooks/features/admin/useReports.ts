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

  /**
   * 라운드 16 F-16-036 옵션 B (백엔드 변경 보류 → 프론트 옵티미스틱):
   * 차단 / 강제탈퇴 mutation 성공 시 동일 targetId 의 신고를 RESOLVED 처리한 것처럼 즉시 제거.
   * 백엔드 응답이 신고 목록을 갱신해주지 않으므로 클라이언트에서만 옵티미스틱하게 정리.
   *
   * @param targetId 처리된 대상 사용자 ID
   * @param reportType 처리된 신고 종류 (POST/COMMENT — 같은 reportType + targetId 조합)
   * @returns 옵티미스틱하게 제거된 신고 개수
   */
  const removeResolvedReports = useCallback(
    (targetId: number, reportType: string): number => {
      let removedCount = 0;
      setReports((prev) => {
        if (!prev) return prev;
        const filtered = prev.content.filter((r) => {
          // 동일 targetId 이면서 (POST/COMMENT 류) 같은 reportType 인 신고 제거
          const isSameTarget =
            r.targetId === targetId &&
            (r.reportType === "POST" || r.reportType === "COMMENT") &&
            r.reportType === reportType;
          if (isSameTarget) removedCount += 1;
          return !isSameTarget;
        });
        if (filtered.length === prev.content.length) return prev;
        return {
          ...prev,
          content: filtered,
          // totalElements 도 줄여 헤더/뱃지 즉시 반영
          totalElements: Math.max(0, (prev.totalElements ?? 0) - removedCount),
          empty: filtered.length === 0,
        } as PageResponse<Report>;
      });
      return removedCount;
    },
    [],
  );

  // 라운드 16 F-16-029: 페이지 N 마지막 항목 처리 후 빈 페이지 회귀 방지.
  // 옵티미스틱 제거 후 현재 페이지가 비고 page > 0 이면 자동으로 한 페이지 후퇴.
  useEffect(() => {
    if (!isLoading && reports && reports.content.length === 0 && page > 0) {
      setPage((p) => Math.max(0, p - 1));
    }
  }, [isLoading, reports, page]);

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
    removeResolvedReports,
  };
}
