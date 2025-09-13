"use client";

import { useState, useEffect, useCallback, useRef } from 'react';
import type { ApiResponse, ErrorResponse } from '@/types/common';
import { ErrorHandler } from '@/lib/api/helpers';
import { useToast } from '@/hooks';

interface UseApiQueryOptions<T> {
  enabled?: boolean;
  onSuccess?: (data: T) => void;
  onError?: (error: ErrorResponse) => void;
  refetchInterval?: number;
  cacheTime?: number;
  staleTime?: number;
  retry?: number;
  retryDelay?: number;
  showErrorToast?: boolean;
}

interface UseApiQueryResult<T> {
  data: T | null;
  isLoading: boolean;
  isError: boolean;
  error: ErrorResponse | null;
  refetch: () => Promise<void>;
  isRefetching: boolean;
}

export function useApiQuery<T>(
  queryFn: () => Promise<ApiResponse<T>>,
  options: UseApiQueryOptions<T> = {}
): UseApiQueryResult<T> {
  const {
    enabled = true,
    onSuccess,
    onError,
    refetchInterval,
    cacheTime = 5 * 60 * 1000, // 5분
    staleTime = 0,
    retry = 1,
    retryDelay = 1000,
    showErrorToast = true
  } = options;

  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [isRefetching, setIsRefetching] = useState(false);
  
  const { showError } = useToast();
  const cacheRef = useRef<{ data: T | null; timestamp: number }>({ data: null, timestamp: 0 });
  const retryCountRef = useRef(0);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const fetchData = useCallback(async (isRefetch = false) => {
    if (!enabled) return;

    // 캐시 체크
    const now = Date.now();
    if (cacheRef.current.data && (now - cacheRef.current.timestamp < staleTime)) {
      setData(cacheRef.current.data);
      setIsLoading(false);
      return;
    }

    if (isRefetch) {
      setIsRefetching(true);
    } else {
      setIsLoading(true);
    }
    
    setIsError(false);
    setError(null);

    try {
      const response = await queryFn();
      
      if (response.success && response.data !== undefined) {
        setData(response.data);
        cacheRef.current = { data: response.data, timestamp: Date.now() };
        retryCountRef.current = 0;
        if (response.data) {
          onSuccess?.(response.data as T);
        }
      } else if (response.needsRelogin) {
        // 리로그인 필요 시 전역 이벤트는 apiClient에서 처리됨
        setIsError(true);
        setError({ message: response.error || 'Relogin required', code: '401', timestamp: new Date().toISOString() });
      } else {
        throw new Error(response.error || 'Unknown error');
      }
    } catch (err) {
      const appError = ErrorHandler.mapApiError(err);
      setIsError(true);
      const errorResponse: ErrorResponse = {
        message: appError.message,
        code: '500',
        timestamp: new Date().toISOString()
      };
      setError(errorResponse);

      // 재시도 로직
      if (retryCountRef.current < retry) {
        retryCountRef.current++;
        setTimeout(() => fetchData(isRefetch), retryDelay * retryCountRef.current);
        return;
      }

      if (showErrorToast) {
        const { title, message } = ErrorHandler.formatErrorForToast(appError);
        showError(title, message);
      }

      onError?.(errorResponse);
    } finally {
      setIsLoading(false);
      setIsRefetching(false);
    }
  }, [enabled, queryFn, onSuccess, onError, staleTime, retry, retryDelay, showError, showErrorToast]);

  // 초기 fetch
  useEffect(() => {
    if (enabled) {
      fetchData();
    }
  }, [enabled]);

  // refetch interval 설정
  useEffect(() => {
    if (refetchInterval && enabled) {
      intervalRef.current = setInterval(() => {
        fetchData(true);
      }, refetchInterval);
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [refetchInterval, enabled, fetchData]);

  const refetch = useCallback(async () => {
    await fetchData(true);
  }, [fetchData]);

  return {
    data,
    isLoading,
    isError,
    error,
    refetch,
    isRefetching
  };
}