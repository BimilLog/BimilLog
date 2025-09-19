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
interface ApiCallOptions extends SuccessHandlerOptions, ErrorHandlerOptions {
  loadingMessage?: string;
  showLoadingToast?: boolean;
}

export const executeApiCall = async <T>(
  apiCall: () => Promise<ApiResponse<T>>,
  options: ApiCallOptions = {}
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
  options?: ApiCallOptions;
}

export const executeParallelApiCalls = async <T>(
  calls: ParallelApiCall<T>[]
): Promise<Record<string, T | null>> => {
  // Promise.allSettled 사용으로 일부 호출 실패해도 나머지 계속 실행
  // Promise.all과 달리 하나 실패해도 전체가 실패하지 않음
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

  // allSettled 결과 처리: fulfilled/rejected 상태별로 분기
  results.forEach((result, index) => {
    const callName = calls[index].name;

    if (result.status === 'fulfilled') {
      // 성공한 호출의 데이터 저장
      finalResults[callName] = result.value.result;
    } else {
      // 실패한 호출은 null로 처리하여 부분 실패 허용
      finalResults[callName] = null;
      hasError = true;
    }
  });

  // 전체 결과에 대한 토스트 (옵션) - 개별 에러가 아닌 전체 상황 알림
  if (hasError) {
    toast.error('일부 데이터를 불러오는데 실패했습니다.');
  }

  return finalResults;
};

/**
 * API 재시도 로직
 */
interface RetryOptions {
  maxRetries?: number;
  retryDelay?: number;
  shouldRetry?: (error: unknown, attempt: number) => boolean;
  onRetry?: (attempt: number, error: unknown) => void;
}

export const executeApiWithRetry = async <T>(
  apiCall: () => Promise<ApiResponse<T>>,
  options: RetryOptions & ApiCallOptions = {}
): Promise<T | null> => {
  const {
    maxRetries = 3,
    retryDelay = 1000,
    shouldRetry = (error, attempt) => attempt < maxRetries,
    onRetry,
    ...apiOptions
  } = options;

  let lastError: unknown;

  // 최대 재시도 횟수 + 1 (초기 시도 포함)만큼 반복
  for (let attempt = 1; attempt <= maxRetries + 1; attempt++) {
    try {
      return await executeApiCall(apiCall, {
        ...apiOptions,
        showLoadingToast: attempt === 1 // 첫 시도에만 로딩 표시
      });
    } catch (error) {
      lastError = error;

      // 재시도 가능한 상황인지 확인 (최대 횟수 내, shouldRetry 조건 만족)
      if (attempt <= maxRetries && shouldRetry(error, attempt)) {
        if (onRetry) {
          onRetry(attempt, error);
        }

        // 지수 백오프 전략: 재시도 횟수에 비례해서 대기 시간 증가
        // 1초 -> 2초 -> 3초... 서버 부하 분산 및 일시적 장애 복구 대기
        await new Promise(resolve => setTimeout(resolve, retryDelay * attempt));
        continue;
      }

      // 재시도 불가능하거나 최대 횟수 초과 시 반복문 종료
      break;
    }
  }

  // 최종 실패 처리
  return handleApiError(lastError, apiOptions);
};

/**
 * API 응답 캐싱 헬퍼 (간단한 메모리 캐시)
 * TTL(Time To Live) 기반 메모리 캐시로 불필요한 API 호출 감소
 */
const apiCache = new Map<string, { data: any; timestamp: number; ttl: number }>();

interface CacheOptions {
  key: string;
  ttl?: number; // milliseconds
  forceRefresh?: boolean;
}

export const executeApiWithCache = async <T>(
  apiCall: () => Promise<ApiResponse<T>>,
  cacheOptions: CacheOptions,
  apiOptions: ApiCallOptions = {}
): Promise<T | null> => {
  const { key, ttl = 5 * 60 * 1000, forceRefresh = false } = cacheOptions;

  // 캐시 확인 - forceRefresh가 false이고 해당 키의 캐시가 존재하는 경우
  if (!forceRefresh && apiCache.has(key)) {
    const cached = apiCache.get(key)!;
    const now = Date.now();

    // TTL 검사: 현재 시간 - 캐시 생성 시간 < TTL이면 캐시 유효
    if (now - cached.timestamp < cached.ttl) {
      return cached.data; // 유효한 캐시 데이터 즉시 반환
    }

    // 만료된 캐시 제거하여 메모리 정리
    apiCache.delete(key);
  }

  // 캐시 미스 또는 만료된 경우 실제 API 호출
  const result = await executeApiCall(apiCall, apiOptions);

  // 성공 시에만 캐시 저장 - 실패한 응답은 캐싱하지 않음
  if (result !== null) {
    apiCache.set(key, {
      data: result,
      timestamp: Date.now(), // 캐시 생성 시점 기록
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