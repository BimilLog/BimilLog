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

interface UseActivityDataOptions {
  fetchData: (page?: number, size?: number) => Promise<PaginatedData<any>>;
}

export function useActivityData({ fetchData }: UseActivityDataOptions) {
  const [items, setItems] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  const loadData = async (page = 0, append = false) => {
    try {
      if (!append) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setError(null);

      const result = await fetchData(page, 10);

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

  const handleLoadMore = () => {
    if (currentPage < totalPages - 1) {
      loadData(currentPage + 1, true);
    }
  };

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