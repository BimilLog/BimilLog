import type { ApiResponse } from '@/types';

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
    console.error('API call failed:', error);
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
    if (error && typeof error === 'object' && 'error' in error && typeof (error as any).error === 'string') {
      const errorMessage = (error as any).error.toLowerCase();

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
          message: (error as any).error,
          userMessage: (error as any).error,
          originalError: error
        };
      }

      if (errorMessage.includes('유효하지')) {
        return {
          type: 'VALIDATION_ERROR',
          title: '입력 오류',
          message: (error as any).error,
          userMessage: (error as any).error,
          originalError: error
        };
      }
    }

    // HTTP 상태 코드 기반 에러
    if (error && typeof error === 'object' && 'status' in error && typeof (error as any).status === 'number') {
      switch ((error as any).status) {
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
    return {
      type: 'UNKNOWN_ERROR',
      title: '오류 발생',
      message: (error && typeof error === 'object' && 'message' in error && typeof (error as any).message === 'string') ? (error as any).message : '알 수 없는 오류가 발생했습니다.',
      userMessage: (error && typeof error === 'object' && 'message' in error && typeof (error as any).message === 'string') ? (error as any).message : '알 수 없는 오류가 발생했습니다.',
      originalError: error
    };
  }

  static handleRollingPaperError(error: unknown): AppError {
    const baseError = this.mapApiError(error);

    // 롤링페이퍼 특화 에러 처리
    if (error && typeof error === 'object' && 'message' in error && typeof (error as any).message === 'string' && (error as any).message.includes('위치')) {
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

// handleApiError를 export (기존 코드 호환성)
export const handleApiError = (error: unknown): void => {
  const appError = ErrorHandler.mapApiError(error);
  console.error(appError.title, appError.message, appError.originalError);

  // Toast 표시 로직 등 추가 가능
  const { showToast } = require('@/stores/toast.store').useToastStore.getState();
  showToast({
    type: 'error',
    message: appError.userMessage || appError.message
  });
};