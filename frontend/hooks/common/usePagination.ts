"use client";

import { useState, useCallback, useMemo } from 'react';

export interface UsePaginationOptions {
  initialPage?: number;
  pageSize?: number;
  totalItems?: number;
}

export interface UsePaginationResult {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
  hasNextPage: boolean;
  hasPreviousPage: boolean;
  goToPage: (page: number) => void;
  setCurrentPage: (page: number) => void;
  nextPage: () => void;
  previousPage: () => void;
  firstPage: () => void;
  lastPage: () => void;
  setPageSize: (size: number) => void;
  setTotalItems: (total: number) => void;
  pageRange: number[];
  offset: number;
}

export type PaginationState = UsePaginationResult;

export function usePagination(options: UsePaginationOptions = {}): UsePaginationResult {
  const {
    initialPage = 0,
    pageSize: initialPageSize = 30,
    totalItems: initialTotalItems = 0
  } = options;

  const [currentPage, setCurrentPage] = useState(initialPage);
  const [pageSize, setPageSizeState] = useState(initialPageSize);
  const [totalItems, setTotalItemsState] = useState(initialTotalItems);

  const totalPages = useMemo(() => {
    return Math.max(1, Math.ceil(totalItems / pageSize));
  }, [totalItems, pageSize]);

  const hasNextPage = useMemo(() => {
    return currentPage < totalPages - 1;
  }, [currentPage, totalPages]);

  const hasPreviousPage = useMemo(() => {
    return currentPage > 0;
  }, [currentPage]);

  const offset = useMemo(() => {
    return currentPage * pageSize;
  }, [currentPage, pageSize]);

  // 페이지네이션 UI에 표시할 페이지 범위 계산
  const pageRange = useMemo(() => {
    const range: number[] = [];
    const maxVisible = 5; // 표시할 최대 페이지 버튼 수

    // 현재 페이지를 중심으로 시작점 계산
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages - 1, start + maxVisible - 1);

    // 마지막 페이지 근처에서 시작점 보정: 항상 5개 버튼 유지
    if (end - start < maxVisible - 1) {
      start = Math.max(0, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      range.push(i);
    }

    return range;
  }, [currentPage, totalPages]);

  // 페이지 이동: 유효한 범위 내로 제한
  const goToPage = useCallback((page: number) => {
    const newPage = Math.max(0, Math.min(page, totalPages - 1));
    setCurrentPage(newPage);
  }, [totalPages]);

  const nextPage = useCallback(() => {
    if (hasNextPage) {
      setCurrentPage(prev => prev + 1);
    }
  }, [hasNextPage]);

  const previousPage = useCallback(() => {
    if (hasPreviousPage) {
      setCurrentPage(prev => prev - 1);
    }
  }, [hasPreviousPage]);

  const firstPage = useCallback(() => {
    setCurrentPage(0);
  }, []);

  const lastPage = useCallback(() => {
    setCurrentPage(totalPages - 1);
  }, [totalPages]);

  // 페이지 크기 변경: 첫 페이지로 자동 이동하여 데이터 일관성 유지
  const setPageSize = useCallback((size: number) => {
    setPageSizeState(size);
    setCurrentPage(0); // 페이지 크기 변경 시 첫 페이지로
  }, []);

  const setTotalItems = useCallback((total: number) => {
    setTotalItemsState(total);
  }, []);

  return {
    currentPage,
    pageSize,
    totalPages,
    totalItems,
    hasNextPage,
    hasPreviousPage,
    goToPage,
    setCurrentPage: goToPage, // Alias for backward compatibility
    nextPage,
    previousPage,
    firstPage,
    lastPage,
    setPageSize,
    setTotalItems,
    pageRange,
    offset
  };
}