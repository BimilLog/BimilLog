"use client";

import { useState, useCallback } from 'react';
import type { ApiResponse } from '@/types/common';
import { ErrorHandler } from '@/lib/api/helpers';
import { useToast } from '@/hooks';

interface UseApiMutationOptions<T> {
  onSuccess?: (data: T) => void;
  onError?: (error: any) => void;
  showSuccessToast?: boolean;
  showErrorToast?: boolean;
  successMessage?: string;
  errorMessage?: string;
}

interface UseApiMutationResult<T, V> {
  mutate: (variables?: V) => Promise<void>;
  mutateAsync: (variables?: V) => Promise<T | null>;
  data: T | null;
  isLoading: boolean;
  isError: boolean;
  error: any;
  isSuccess: boolean;
  reset: () => void;
}

export function useApiMutation<T = any, V = any>(
  mutationFn: (variables: V) => Promise<ApiResponse<T>>,
  options: UseApiMutationOptions<T> = {}
): UseApiMutationResult<T, V> {
  const {
    onSuccess,
    onError,
    showSuccessToast = false,
    showErrorToast = true,
    successMessage = '작업이 완료되었습니다.',
    errorMessage
  } = options;

  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isError, setIsError] = useState(false);
  const [error, setError] = useState<any>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  
  const { showSuccess, showError } = useToast();

  const reset = useCallback(() => {
    setData(null);
    setIsLoading(false);
    setIsError(false);
    setError(null);
    setIsSuccess(false);
  }, []);

  const mutateAsync = useCallback(async (variables?: V): Promise<T | null> => {
    setIsLoading(true);
    setIsError(false);
    setError(null);
    setIsSuccess(false);

    try {
      const response = await mutationFn(variables as V);
      
      if (response.success && response.data !== undefined) {
        setData(response.data);
        setIsSuccess(true);
        
        if (showSuccessToast) {
          showSuccess('성공', successMessage);
        }
        
        if (response.data) {
          onSuccess?.(response.data as T);
        }
        return response.data;
      } else if (response.needsRelogin) {
        // 리로그인 필요 시 전역 이벤트는 apiClient에서 처리됨
        setIsError(true);
        setError(response.error);
        return null;
      } else {
        throw new Error(response.error || 'Unknown error');
      }
    } catch (err) {
      const appError = ErrorHandler.mapApiError(err);
      setIsError(true);
      setError(appError);
      
      if (showErrorToast) {
        const { title, message } = ErrorHandler.formatErrorForToast(appError);
        showError(title, errorMessage || message);
      }
      
      onError?.(appError);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, [mutationFn, onSuccess, onError, showSuccess, showError, showSuccessToast, showErrorToast, successMessage, errorMessage]);

  const mutate = useCallback(async (variables?: V): Promise<void> => {
    await mutateAsync(variables);
  }, [mutateAsync]);

  return {
    mutate,
    mutateAsync,
    data,
    isLoading,
    isError,
    error,
    isSuccess,
    reset
  };
}