import { useState, useCallback } from 'react';
import { useToast } from './useToast';

export interface ApiState<T = any> {
  data: T | null;
  loading: boolean;
  error: Error | null;
}

export interface UseApiReturn<T = any> {
  data: T | null;
  loading: boolean;
  error: Error | null;
  execute: (...args: any[]) => Promise<T | null>;
  reset: () => void;
}

interface UseApiOptions {
  showErrorToast?: boolean;
  showSuccessToast?: boolean;
  successMessage?: string;
  errorMessage?: string;
  onSuccess?: (data: any) => void;
  onError?: (error: Error) => void;
}

export function useApi<T = any>(
  apiFunction: (...args: any[]) => Promise<{ success: boolean; data?: T; error?: string }>,
  options: UseApiOptions = {}
): UseApiReturn<T> {
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    loading: false,
    error: null,
  });

  const { showError, showSuccess } = useToast();

  const execute = useCallback(async (...args: any[]): Promise<T | null> => {
    setState({ data: null, loading: true, error: null });

    try {
      const response = await apiFunction(...args);

      if (response.success && response.data) {
        setState({ data: response.data, loading: false, error: null });
        
        if (options.showSuccessToast && options.successMessage) {
          showSuccess('성공', options.successMessage);
        }
        
        options.onSuccess?.(response.data);
        return response.data;
      } else {
        const error = new Error(response.error || '요청 처리 중 오류가 발생했습니다.');
        setState({ data: null, loading: false, error });
        
        if (options.showErrorToast !== false) {
          showError('오류', options.errorMessage || error.message);
        }
        
        options.onError?.(error);
        return null;
      }
    } catch (error) {
      const err = error instanceof Error ? error : new Error('알 수 없는 오류가 발생했습니다.');
      setState({ data: null, loading: false, error: err });
      
      if (options.showErrorToast !== false) {
        showError('오류', options.errorMessage || err.message);
      }
      
      options.onError?.(err);
      return null;
    }
  }, [apiFunction, showError, showSuccess, options]);

  const reset = useCallback(() => {
    setState({ data: null, loading: false, error: null });
  }, []);

  return {
    data: state.data,
    loading: state.loading,
    error: state.error,
    execute,
    reset,
  };
}