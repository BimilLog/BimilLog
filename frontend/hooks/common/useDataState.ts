"use client";

import { useState, useCallback, useMemo } from 'react';
import { useErrorHandler } from './useErrorHandler';
import type { ApiResponse } from '@/types/common';

/**
 * 통합 데이터 상태 관리 Hook
 * Loading, Error, Success 상태를 한번에 관리
 */
interface DataState<T> {
  data: T | null;
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  isEmpty: boolean;
  hasData: boolean;
}

interface UseDataStateOptions<T> {
  initialData?: T;
  emptyChecker?: (data: T) => boolean;
  onError?: (error: Error) => void;
  showErrorToast?: boolean;
  domain?: string;
}

export function useDataState<T>(options: UseDataStateOptions<T> = {}) {
  const {
    initialData = null,
    emptyChecker,
    onError,
    showErrorToast = true,
    domain
  } = options;

  const [data, setData] = useState<T | null>(initialData);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const errorHandler = useErrorHandler({
    showToast: showErrorToast,
    domain,
    onError: onError ? (appError) => onError(new Error(appError.message)) : undefined
  });

  // 메모화된 상태 계산: 불필요한 리렌더링 방지
  const state: DataState<T> = useMemo(() => {
    // emptyChecker가 있으면 사용자 정의 검사, 없으면 기본 null 검사
    const isEmpty = data ? (emptyChecker ? emptyChecker(data) : false) : true;
    const hasData = data !== null && !isEmpty;

    return {
      data,
      isLoading,
      isError,
      error,
      isEmpty,
      hasData
    };
  }, [data, isLoading, isError, error, emptyChecker]);

  // 성공 상태 설정
  const setSuccess = useCallback((newData: T) => {
    setData(newData);
    setIsLoading(false);
    setIsError(false);
    setError(null);
  }, []);

  // 에러 상태 설정: ErrorHandler를 통한 통합 에러 처리
  const setErrorState = useCallback(async (err: unknown) => {
    const appError = await errorHandler.handleError(err);
    setError(new Error(appError.message));
    setIsError(true);
    setIsLoading(false);
  }, [errorHandler]);

  // 로딩 상태 설정
  const setLoadingState = useCallback((loading: boolean) => {
    setIsLoading(loading);
    if (loading) {
      setIsError(false);
      setError(null);
    }
  }, []);

  // 상태 리셋
  const reset = useCallback(() => {
    setData(initialData);
    setIsLoading(false);
    setIsError(false);
    setError(null);
  }, [initialData]);

  // API 호출 래퍼: 로딩-성공-에러 상태를 자동 관리
  const executeApi = useCallback(async <R>(
    apiCall: () => Promise<ApiResponse<R>>,
    onSuccess?: (data: R) => T // API 응답 데이터 변환 함수
  ): Promise<void> => {
    setLoadingState(true);

    try {
      const response = await apiCall();

      if (response.success && response.data !== undefined && response.data !== null) {
        // onSuccess가 있으면 데이터 변환, 없으면 그대로 사용
        const transformedData = onSuccess ? onSuccess(response.data as R) : response.data as unknown as T;
        setSuccess(transformedData);
      } else {
        throw new Error(response.error || 'API 호출 실패');
      }
    } catch (err) {
      await setErrorState(err);
    }
  }, [setLoadingState, setSuccess, setErrorState]);

  return {
    ...state,
    setSuccess,
    setError: setErrorState,
    setLoading: setLoadingState,
    reset,
    executeApi
  };
}

/**
 * 배열 데이터 전용 상태 관리
 */
export function useArrayDataState<T>(
  options: Omit<UseDataStateOptions<T[]>, 'emptyChecker'> = {}
) {
  return useDataState<T[]>({
    ...options,
    initialData: [],
    emptyChecker: (data) => Array.isArray(data) && data.length === 0
  });
}

/**
 * 페이지네이션과 함께 사용하는 데이터 상태
 */
export function usePaginatedDataState<T>(
  options: UseDataStateOptions<T[]> = {}
) {
  const dataState = useArrayDataState<T>(options);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  // 페이지네이션 데이터 일괄 업데이트
  const setPaginatedData = useCallback((data: T[], page: number, total: number) => {
    dataState.setSuccess(data);
    setCurrentPage(page);
    setTotalPages(total);
    setHasMore(page < total); // 다음 페이지 존재 여부 계산
  }, [dataState]);

  const resetPagination = useCallback(() => {
    setCurrentPage(1);
    setTotalPages(0);
    setHasMore(false);
    dataState.reset();
  }, [dataState]);

  return {
    ...dataState,
    currentPage,
    totalPages,
    hasMore,
    setPaginatedData,
    resetPagination,
    setCurrentPage
  };
}