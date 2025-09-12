import { useState, useCallback } from "react";

export interface PaginationState {
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalItems: number;
}

export interface UsePaginationReturn extends PaginationState {
  // Actions
  goToPage: (page: number) => void;
  nextPage: () => void;
  prevPage: () => void;
  firstPage: () => void;
  lastPage: () => void;
  setPageSize: (size: number) => void;
  setTotalPages: (total: number) => void;
  setTotalItems: (total: number) => void;
  reset: () => void;

  // Computed properties
  hasNextPage: boolean;
  hasPrevPage: boolean;
  isFirstPage: boolean;
  isLastPage: boolean;
  startIndex: number;
  endIndex: number;

  // Helper for setting pagination data from API response
  updateFromApiResponse: (response: { 
    number: number; 
    totalPages: number; 
    totalElements?: number;
    size?: number;
  }) => void;
}

export interface UsePaginationConfig {
  initialPage?: number;
  initialPageSize?: number;
  initialTotalPages?: number;
  initialTotalItems?: number;
}

/**
 * 페이지네이션 상태를 관리하는 공통 훅
 * 
 * 사용 예시:
 * const pagination = usePagination({ initialPageSize: 30 });
 * 
 * // API 응답으로부터 페이지네이션 정보 업데이트
 * pagination.updateFromApiResponse(response.data);
 * 
 * // 페이지 이동
 * pagination.goToPage(2);
 */
export const usePagination = ({
  initialPage = 0,
  initialPageSize = 10,
  initialTotalPages = 0,
  initialTotalItems = 0,
}: UsePaginationConfig = {}): UsePaginationReturn => {
  const [state, setState] = useState<PaginationState>({
    currentPage: initialPage,
    totalPages: initialTotalPages,
    pageSize: initialPageSize,
    totalItems: initialTotalItems,
  });

  const goToPage = useCallback((page: number) => {
    if (page >= 0 && page < state.totalPages) {
      setState(prev => ({
        ...prev,
        currentPage: page,
      }));
    }
  }, [state.totalPages]);

  const nextPage = useCallback(() => {
    if (state.currentPage < state.totalPages - 1) {
      goToPage(state.currentPage + 1);
    }
  }, [state.currentPage, state.totalPages, goToPage]);

  const prevPage = useCallback(() => {
    if (state.currentPage > 0) {
      goToPage(state.currentPage - 1);
    }
  }, [state.currentPage, goToPage]);

  const firstPage = useCallback(() => {
    goToPage(0);
  }, [goToPage]);

  const lastPage = useCallback(() => {
    if (state.totalPages > 0) {
      goToPage(state.totalPages - 1);
    }
  }, [state.totalPages, goToPage]);

  const setPageSize = useCallback((size: number) => {
    setState(prev => ({
      ...prev,
      pageSize: size,
      currentPage: 0, // 페이지 크기 변경 시 첫 페이지로 이동
    }));
  }, []);

  const setTotalPages = useCallback((total: number) => {
    setState(prev => ({
      ...prev,
      totalPages: total,
    }));
  }, []);

  const setTotalItems = useCallback((total: number) => {
    setState(prev => ({
      ...prev,
      totalItems: total,
    }));
  }, []);

  const reset = useCallback(() => {
    setState({
      currentPage: initialPage,
      totalPages: initialTotalPages,
      pageSize: initialPageSize,
      totalItems: initialTotalItems,
    });
  }, [initialPage, initialTotalPages, initialPageSize, initialTotalItems]);

  const updateFromApiResponse = useCallback((response: {
    number: number;
    totalPages: number;
    totalElements?: number;
    size?: number;
  }) => {
    setState(prev => ({
      ...prev,
      currentPage: response.number,
      totalPages: response.totalPages,
      totalItems: response.totalElements || prev.totalItems,
      pageSize: response.size || prev.pageSize,
    }));
  }, []);

  // Computed properties
  const hasNextPage = state.currentPage < state.totalPages - 1;
  const hasPrevPage = state.currentPage > 0;
  const isFirstPage = state.currentPage === 0;
  const isLastPage = state.currentPage === state.totalPages - 1;
  const startIndex = state.currentPage * state.pageSize;
  const endIndex = Math.min(startIndex + state.pageSize - 1, state.totalItems - 1);

  return {
    ...state,
    goToPage,
    nextPage,
    prevPage,
    firstPage,
    lastPage,
    setPageSize,
    setTotalPages,
    setTotalItems,
    reset,
    updateFromApiResponse,
    hasNextPage,
    hasPrevPage,
    isFirstPage,
    isLastPage,
    startIndex,
    endIndex,
  };
};