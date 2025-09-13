"use client";

import { useState, useCallback, useMemo } from 'react';

interface UsePaginationOptions {
  initialPage?: number;
  pageSize?: number;
  totalItems?: number;
}

interface UsePaginationResult {
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

  const pageRange = useMemo(() => {
    const range: number[] = [];
    const maxVisible = 5; // 표시할 최대 페이지 버튼 수
    
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages - 1, start + maxVisible - 1);
    
    // 끝에 가까울 때 시작점 조정
    if (end - start < maxVisible - 1) {
      start = Math.max(0, end - maxVisible + 1);
    }
    
    for (let i = start; i <= end; i++) {
      range.push(i);
    }
    
    return range;
  }, [currentPage, totalPages]);

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