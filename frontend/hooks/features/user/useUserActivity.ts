"use client";

import { useState, useEffect } from "react";
import { logger } from '@/lib/utils/logger';

// ===== ACTIVITY DATA =====
interface PaginatedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

interface UseActivityDataOptions<T> {
  fetchData: (page?: number, size?: number) => Promise<PaginatedData<T>>;
}

export function useActivityData<T>({ fetchData }: UseActivityDataOptions<T>) {
  const [items, setItems] = useState<T[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  // 활동 데이터 로드: 일반 로드와 더보기(무한 스크롤) 로드를 구분하여 처리
  const loadData = async (page = 0, append = false) => {
    try {
      if (!append) {
        setIsLoading(true);  // 첫 로드 시 전체 로딩 상태
      } else {
        setIsLoadingMore(true);  // 더보기 시 추가 로딩 상태
      }
      setError(null);

      const result = await fetchData(page, 10);

      // append가 true면 기존 데이터에 추가, false면 새로 교체
      if (append) {
        setItems((prev) => [...prev, ...result.content]);
      } else {
        setItems(result.content);
      }

      setCurrentPage(result.currentPage);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      logger.error("Failed to fetch activity data:", err);
      setError("데이터를 불러오는 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadData(0);
  }, [fetchData]);

  // 무한 스크롤을 위한 더보기 처리: 다음 페이지가 있을 때만 실행
  const handleLoadMore = () => {
    if (currentPage < totalPages - 1) {
      loadData(currentPage + 1, true);
    }
  };

  // 페이지 직접 변경: 스크롤을 맨 위로 이동하여 UX 개선
  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      loadData(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const retry = () => {
    loadData(currentPage);
  };

  return {
    items,
    isLoading,
    error,
    currentPage,
    totalPages,
    totalElements,
    isLoadingMore,
    handleLoadMore,
    handlePageChange,
    retry,
  };
}

export type { PaginatedData, UseActivityDataOptions };