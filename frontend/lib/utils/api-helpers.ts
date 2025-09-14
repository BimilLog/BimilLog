/**
 * API 응답 처리를 위한 공통 유틸리티
 */

import { toast } from "sonner";
import { AppError, ErrorHandler } from '@/lib/api/helpers';
import type { ApiResponse } from '@/types/common';

/**
 * API 응답 성공 처리 패턴
 */
interface SuccessHandlerOptions {
  showToast?: boolean;
  message?: string;
  onSuccess?: (data: any) => void;
  redirectUrl?: string;
}

export const handleApiSuccess = <T>(
  response: ApiResponse<T>,
  options: SuccessHandlerOptions = {}
): T | null => {
  const {
    showToast = false,
    message = '성공적으로 처리되었습니다.',
    onSuccess,
    redirectUrl
  } = options;

  if (response.success && response.data !== undefined) {
    // 성공 토스트 표시
    if (showToast) {
      toast.success(message);
    }

    // 성공 콜백 실행
    if (onSuccess) {
      onSuccess(response.data);
    }

    // 리다이렉션 처리
    if (redirectUrl && typeof window !== 'undefined') {
      window.location.href = redirectUrl;
    }

    return response.data;
  }

  return null;
};

/**
 * API 응답 에러 처리 패턴
 */
interface ErrorHandlerOptions {
  showToast?: boolean;
  customMessage?: string;
  onError?: (error: AppError) => void;
  fallbackData?: any;
}

export const handleApiError = (
  error: unknown,
  options: ErrorHandlerOptions = {}
): any => {
  const {
    showToast = true,
    customMessage,
    onError,
    fallbackData = null
  } = options;

  const appError = ErrorHandler.mapApiError(error);

  // 커스텀 메시지 적용
  if (customMessage) {
    appError.userMessage = customMessage;
  }

  // 에러 토스트 표시
  if (showToast) {
    const { title, message } = ErrorHandler.formatErrorForToast(appError);
    toast.error(`${title}: ${message}`);
  }

  // 에러 콜백 실행
  if (onError) {
    onError(appError);
  }

  return fallbackData;
};

/**
 * API 호출 래퍼 - 성공/실패 처리 자동화
 */
interface ApiCallOptions<T> extends SuccessHandlerOptions, ErrorHandlerOptions {
  loadingMessage?: string;
  showLoadingToast?: boolean;
}

export const executeApiCall = async <T>(
  apiCall: () => Promise<ApiResponse<T>>,
  options: ApiCallOptions<T> = {}
): Promise<T | null> => {
  const {
    loadingMessage = '처리 중...',
    showLoadingToast = false,
    ...handlerOptions
  } = options;

  let loadingToast: string | number | undefined;

  try {
    // 로딩 토스트 표시
    if (showLoadingToast) {
      loadingToast = toast.loading(loadingMessage);
    }

    const response = await apiCall();

    // 로딩 토스트 제거
    if (loadingToast) {
      toast.dismiss(loadingToast);
    }

    return handleApiSuccess(response, handlerOptions);

  } catch (error) {
    // 로딩 토스트 제거
    if (loadingToast) {
      toast.dismiss(loadingToast);
    }

    return handleApiError(error, handlerOptions);
  }
};

/**
 * 여러 API 호출을 병렬 처리
 */
interface ParallelApiCall<T> {
  name: string;
  call: () => Promise<ApiResponse<T>>;
  options?: ApiCallOptions<T>;
}

export const executeParallelApiCalls = async <T>(
  calls: ParallelApiCall<T>[]
): Promise<Record<string, T | null>> => {
  const results = await Promise.allSettled(
    calls.map(async ({ name, call, options = {} }) => {
      const result = await executeApiCall(call, {
        ...options,
        showToast: false // 병렬 호출에서는 개별 토스트 비활성화
      });
      return { name, result };
    })
  );

  const finalResults: Record<string, T | null> = {};
  let hasError = false;

  results.forEach((result, index) => {
    const callName = calls[index].name;

    if (result.status === 'fulfilled') {
      finalResults[callName] = result.value.result;
    } else {
      finalResults[callName] = null;
      hasError = true;
    }
  });

  // 전체 결과에 대한 토스트 (옵션)
  if (hasError) {
    toast.error('일부 데이터를 불러오는데 실패했습니다.');
  }

  return finalResults;
};

/**
 * API 재시도 로직
 */
interface RetryOptions<T> {
  maxRetries?: number;
  retryDelay?: number;
  shouldRetry?: (error: unknown, attempt: number) => boolean;
  onRetry?: (attempt: number, error: unknown) => void;
}

export const executeApiWithRetry = async <T>(
  apiCall: () => Promise<ApiResponse<T>>,
  options: RetryOptions<T> & ApiCallOptions<T> = {}
): Promise<T | null> => {
  const {
    maxRetries = 3,
    retryDelay = 1000,
    shouldRetry = (error, attempt) => attempt < maxRetries,
    onRetry,
    ...apiOptions
  } = options;

  let lastError: unknown;

  for (let attempt = 1; attempt <= maxRetries + 1; attempt++) {
    try {
      return await executeApiCall(apiCall, {
        ...apiOptions,
        showLoadingToast: attempt === 1 // 첫 시도에만 로딩 표시
      });
    } catch (error) {
      lastError = error;

      if (attempt <= maxRetries && shouldRetry(error, attempt)) {
        if (onRetry) {
          onRetry(attempt, error);
        }

        // 재시도 전 대기
        await new Promise(resolve => setTimeout(resolve, retryDelay * attempt));
        continue;
      }

      break;
    }
  }

  // 최종 실패 처리
  return handleApiError(lastError, apiOptions);
};

/**
 * API 응답 캐싱 헬퍼 (간단한 메모리 캐시)
 */
const apiCache = new Map<string, { data: any; timestamp: number; ttl: number }>();

interface CacheOptions<T> {
  key: string;
  ttl?: number; // milliseconds
  forceRefresh?: boolean;
}

export const executeApiWithCache = async <T>(
  apiCall: () => Promise<ApiResponse<T>>,
  cacheOptions: CacheOptions<T>,
  apiOptions: ApiCallOptions<T> = {}
): Promise<T | null> => {
  const { key, ttl = 5 * 60 * 1000, forceRefresh = false } = cacheOptions;

  // 캐시 확인
  if (!forceRefresh && apiCache.has(key)) {
    const cached = apiCache.get(key)!;
    const now = Date.now();

    if (now - cached.timestamp < cached.ttl) {
      return cached.data;
    }

    // 만료된 캐시 제거
    apiCache.delete(key);
  }

  // API 호출
  const result = await executeApiCall(apiCall, apiOptions);

  // 성공 시 캐시 저장
  if (result !== null) {
    apiCache.set(key, {
      data: result,
      timestamp: Date.now(),
      ttl
    });
  }

  return result;
};

/**
 * 캐시 관리 유틸리티
 */
export const cacheUtils = {
  clear: (key?: string) => {
    if (key) {
      apiCache.delete(key);
    } else {
      apiCache.clear();
    }
  },

  has: (key: string) => apiCache.has(key),

  size: () => apiCache.size,

  keys: () => Array.from(apiCache.keys())
};