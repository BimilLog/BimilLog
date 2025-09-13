import type { ApiResponse, PageResponse, ErrorResponse } from '@/types';
import { logger } from '@/lib/utils/logger';

/**
 * API 헬퍼 유틸리티
 */

// 전역 이벤트를 사용한 리로그인 알림
export const triggerReloginRequired = () => {
  const event = new CustomEvent('needsRelogin', {
    detail: {
      title: '다른 기기에서 로그아웃됨',
      message: '다른 기기에서 로그아웃 하셨습니다. 다시 로그인 해주세요.'
    }
  });
  window.dispatchEvent(event);
};

// API 응답을 래핑하는 헬퍼 함수
export const handleApiResponse = <T>(response: ApiResponse<T>): ApiResponse<T> => {
  // needsRelogin 플래그가 있으면 전역 이벤트 발생
  if (response.needsRelogin) {
    triggerReloginRequired();
  }

  return response;
};

// API 호출을 래핑하는 헬퍼 함수
export const apiCall = async <T>(
  apiFunction: () => Promise<ApiResponse<T>>
): Promise<ApiResponse<T>> => {
  try {
    const response = await apiFunction();
    return handleApiResponse(response);
  } catch (error) {
    logger.error('API call failed:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
};

// 사용 예시를 위한 타입 정의
export type ApiCallFunction<T> = () => Promise<ApiResponse<T>>;

/**
 * 에러 핸들링 유틸리티
 */

export type ErrorType =
  | 'NETWORK_ERROR'
  | 'AUTH_ERROR'
  | 'VALIDATION_ERROR'
  | 'NOT_FOUND'
  | 'SERVER_ERROR'
  | 'PERMISSION_DENIED'
  | 'DUPLICATE_POSITION'
  | 'UNKNOWN_ERROR';

export interface AppError {
  type: ErrorType;
  title: string;
  message: string;
  userMessage?: string; // 사용자에게 표시할 친화적인 메시지
  originalError?: unknown;
}

/**
 * 타입 가드 함수들
 */

// 기본 객체 타입 가드
function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

// API 응답 기본 구조 검증
export function isValidApiResponse<T>(data: unknown): data is ApiResponse<T> {
  if (!isObject(data)) return false;

  // success 필드는 필수
  if (typeof data.success !== 'boolean') return false;

  // error가 있다면 string이어야 함
  if ('error' in data && typeof data.error !== 'string') return false;

  // message가 있다면 string이어야 함
  if ('message' in data && typeof data.message !== 'string') return false;

  // needsRelogin이 있다면 boolean이어야 함
  if ('needsRelogin' in data && typeof data.needsRelogin !== 'boolean') return false;

  return true;
}

// 페이지네이션 응답 검증
export function isPageResponse<T>(data: unknown): data is PageResponse<T> {
  if (!isObject(data)) return false;

  const requiredFields = [
    'content', 'totalElements', 'totalPages', 'first', 'last',
    'number', 'size', 'numberOfElements', 'empty'
  ] as const;

  // 필수 필드 검증
  for (const field of requiredFields) {
    if (!(field in data)) return false;

    const value = (data as Record<string, unknown>)[field];

    if (field === 'content') {
      if (!Array.isArray(value)) return false;
    } else if (field === 'first' || field === 'last' || field === 'empty') {
      if (typeof value !== 'boolean') return false;
    } else {
      if (typeof value !== 'number') return false;
    }
  }

  return true;
}

// 에러 응답 검증
export function isErrorResponse(data: unknown): data is ErrorResponse {
  if (!isObject(data)) return false;

  return (
    typeof data.code === 'string' &&
    typeof data.message === 'string' &&
    typeof data.timestamp === 'string' &&
    (data.path === undefined || typeof data.path === 'string')
  );
}

// 성공 응답에서 data 필드 검증
export function validateResponseData<T>(
  response: ApiResponse<T>,
  validator?: (data: unknown) => data is T
): T | null {
  if (!response.success || !response.data) {
    return null;
  }

  // 커스텀 validator가 있으면 사용
  if (validator && !validator(response.data)) {
    logger.warn('Response data validation failed:', response.data);
    return null;
  }

  return response.data;
}

// API 에러 응답 타입 가드
function isApiErrorResponse(error: unknown): error is { error: string } {
  return (
    typeof error === 'object' &&
    error !== null &&
    'error' in error &&
    typeof (error as Record<string, unknown>).error === 'string'
  );
}

// HTTP 상태 코드 포함 에러 타입 가드
function isHttpStatusError(error: unknown): error is { status: number } {
  return (
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    typeof (error as Record<string, unknown>).status === 'number'
  );
}

// 메시지 포함 에러 타입 가드
function isErrorWithMessage(error: unknown): error is { message: string } {
  return (
    typeof error === 'object' &&
    error !== null &&
    'message' in error &&
    typeof (error as Record<string, unknown>).message === 'string'
  );
}

export class ErrorHandler {
  static mapApiError(error: unknown): AppError {
    // 네트워크 오류
    if (error instanceof TypeError && error.message.includes('fetch')) {
      return {
        type: 'NETWORK_ERROR',
        title: '네트워크 오류',
        message: '서버와 연결할 수 없습니다. 인터넷 연결을 확인해주세요.',
        userMessage: '서버와 연결할 수 없습니다. 인터넷 연결을 확인해주세요.',
        originalError: error
      };
    }

    // API 응답 에러
    if (isApiErrorResponse(error)) {
      const errorMessage = error.error.toLowerCase();

      if (errorMessage.includes('인증') || errorMessage.includes('로그인')) {
        return {
          type: 'AUTH_ERROR',
          title: '인증 오류',
          message: '로그인이 필요하거나 세션이 만료되었습니다.',
          userMessage: '로그인이 필요하거나 세션이 만료되었습니다.',
          originalError: error
        };
      }

      if (errorMessage.includes('권한')) {
        return {
          type: 'PERMISSION_DENIED',
          title: '권한 없음',
          message: '이 작업을 수행할 권한이 없습니다.',
          userMessage: '이 작업을 수행할 권한이 없습니다.',
          originalError: error
        };
      }

      if (errorMessage.includes('찾을 수 없')) {
        return {
          type: 'NOT_FOUND',
          title: '찾을 수 없음',
          message: error.error,
          userMessage: error.error,
          originalError: error
        };
      }

      if (errorMessage.includes('유효하지')) {
        return {
          type: 'VALIDATION_ERROR',
          title: '입력 오류',
          message: error.error,
          userMessage: error.error,
          originalError: error
        };
      }
    }

    // HTTP 상태 코드 기반 에러
    if (isHttpStatusError(error)) {
      switch (error.status) {
        case 400:
          return {
            type: 'VALIDATION_ERROR',
            title: '잘못된 요청',
            message: '요청 형식이 올바르지 않습니다.',
            userMessage: '요청 형식이 올바르지 않습니다.',
            originalError: error
          };
        case 401:
          return {
            type: 'AUTH_ERROR',
            title: '인증 필요',
            message: '로그인이 필요한 서비스입니다.',
            userMessage: '로그인이 필요한 서비스입니다.',
            originalError: error
          };
        case 403:
          return {
            type: 'PERMISSION_DENIED',
            title: '접근 거부',
            message: '이 리소스에 접근할 권한이 없습니다.',
            userMessage: '이 리소스에 접근할 권한이 없습니다.',
            originalError: error
          };
        case 404:
          return {
            type: 'NOT_FOUND',
            title: '찾을 수 없음',
            message: '요청한 리소스를 찾을 수 없습니다.',
            userMessage: '요청한 리소스를 찾을 수 없습니다.',
            originalError: error
          };
        case 500:
        case 502:
        case 503:
          return {
            type: 'SERVER_ERROR',
            title: '서버 오류',
            message: '서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.',
            userMessage: '서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.',
            originalError: error
          };
      }
    }

    // 기본 에러
    const defaultMessage = '알 수 없는 오류가 발생했습니다.';
    const errorMessage = isErrorWithMessage(error) ? error.message : defaultMessage;

    return {
      type: 'UNKNOWN_ERROR',
      title: '오류 발생',
      message: errorMessage,
      userMessage: errorMessage,
      originalError: error
    };
  }

  static handleRollingPaperError(error: unknown): AppError {
    const baseError = this.mapApiError(error);

    // 롤링페이퍼 특화 에러 처리
    if (isErrorWithMessage(error) && error.message.includes('위치')) {
      return {
        type: 'DUPLICATE_POSITION',
        title: '위치 중복',
        message: '이미 다른 메시지가 있는 위치입니다. 다른 위치를 선택해주세요.',
        userMessage: '이미 다른 메시지가 있는 위치입니다. 다른 위치를 선택해주세요.',
        originalError: error
      };
    }

    return baseError;
  }

  static formatErrorForToast(error: AppError): { title: string; message: string } {
    return {
      title: error.title,
      message: error.message
    };
  }
}

// 안전한 API 호출을 위한 래퍼 함수
export const safeApiCall = async <T>(
  apiFunction: () => Promise<ApiResponse<T>>,
  options?: {
    validator?: (data: unknown) => data is T;
    fallback?: T;
    logErrors?: boolean;
  }
): Promise<ApiResponse<T>> => {
  try {
    const response = await apiFunction();

    // API 응답 구조 검증
    if (!isValidApiResponse<T>(response)) {
      logger.warn('Invalid API response structure:', response);
      return {
        success: false,
        error: 'Invalid response format'
      };
    }

    // 성공 응답의 데이터 유효성 검증
    if (response.success && response.data && options?.validator) {
      const validatedData = validateResponseData(response, options.validator);
      if (validatedData === null && response.data !== null) {
        logger.warn('Response data validation failed, using fallback:', response.data);
        return {
          success: true,
          data: options.fallback || null as T
        };
      }
    }

    return handleApiResponse(response);
  } catch (error) {
    if (options?.logErrors !== false) {
      logger.error('Safe API call failed:', error);
    }

    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
};

// 페이지네이션 응답을 위한 안전한 API 호출
export const safePagedApiCall = async <T>(
  apiFunction: () => Promise<ApiResponse<PageResponse<T>>>,
  options?: {
    itemValidator?: (item: unknown) => item is T;
    fallbackContent?: T[];
    logErrors?: boolean;
  }
): Promise<ApiResponse<PageResponse<T>>> => {
  try {
    const response = await apiFunction();

    if (!response.success || !response.data) {
      return response;
    }

    // 페이지네이션 응답 구조 검증
    if (!isPageResponse<T>(response.data)) {
      logger.warn('Invalid page response structure:', response.data);
      return {
        success: false,
        error: 'Invalid page response format'
      };
    }

    // content 배열의 각 아이템 검증
    if (options?.itemValidator && Array.isArray(response.data.content)) {
      const validatedContent = response.data.content.filter(options.itemValidator);

      if (validatedContent.length !== response.data.content.length) {
        logger.warn(
          `Filtered ${response.data.content.length - validatedContent.length} invalid items from page response`
        );

        return {
          success: true,
          data: {
            ...response.data,
            content: validatedContent,
            numberOfElements: validatedContent.length
          }
        };
      }
    }

    return handleApiResponse(response);
  } catch (error) {
    if (options?.logErrors !== false) {
      logger.error('Safe paged API call failed:', error);
    }

    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
};

// handleApiError를 export (기존 코드 호환성)
export const handleApiError = (error: unknown): void => {
  const appError = ErrorHandler.mapApiError(error);
  logger.error(appError.title, appError.message, appError.originalError);

  // Toast 표시 로직 등 추가 가능
  const { showToast } = require('@/stores/toast.store').useToastStore.getState();
  showToast({
    type: 'error',
    message: appError.userMessage || appError.message
  });
};