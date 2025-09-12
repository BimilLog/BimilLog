import { useState, useCallback } from "react";

export interface LoadingState {
  [key: string]: boolean;
}

export interface UseLoadingStateReturn {
  // State
  loading: LoadingState;
  isLoading: (key?: string) => boolean;
  isAnyLoading: () => boolean;

  // Actions
  setLoading: (key: string, value: boolean) => void;
  startLoading: (key: string) => void;
  stopLoading: (key: string) => void;
  resetLoading: () => void;

  // Async wrapper
  withLoading: <T>(key: string, asyncFn: () => Promise<T>) => Promise<T>;
}

/**
 * 다중 로딩 상태를 관리하는 공통 훅
 * 
 * 사용 예시:
 * const { loading, setLoading, withLoading } = useLoadingState();
 * 
 * // 단순 사용
 * setLoading('posts', true);
 * 
 * // async wrapper 사용
 * const posts = await withLoading('posts', () => boardApi.getPosts());
 */
export const useLoadingState = (initialState: LoadingState = {}): UseLoadingStateReturn => {
  const [loading, setLoadingState] = useState<LoadingState>(initialState);

  const setLoading = useCallback((key: string, value: boolean) => {
    setLoadingState(prev => ({
      ...prev,
      [key]: value
    }));
  }, []);

  const startLoading = useCallback((key: string) => {
    setLoading(key, true);
  }, [setLoading]);

  const stopLoading = useCallback((key: string) => {
    setLoading(key, false);
  }, [setLoading]);

  const resetLoading = useCallback(() => {
    setLoadingState({});
  }, []);

  const isLoading = useCallback((key?: string) => {
    if (key) {
      return loading[key] || false;
    }
    // key가 없으면 'loading' 키를 기본으로 사용 (레거시 호환)
    return loading.loading || false;
  }, [loading]);

  const isAnyLoading = useCallback(() => {
    return Object.values(loading).some(Boolean);
  }, [loading]);

  const withLoading = useCallback(async <T>(key: string, asyncFn: () => Promise<T>): Promise<T> => {
    try {
      startLoading(key);
      const result = await asyncFn();
      return result;
    } finally {
      stopLoading(key);
    }
  }, [startLoading, stopLoading]);

  return {
    loading,
    isLoading,
    isAnyLoading,
    setLoading,
    startLoading,
    stopLoading,
    resetLoading,
    withLoading,
  };
};