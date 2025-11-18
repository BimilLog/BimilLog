import { logger } from '@/lib/utils/logger';

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
  userMessage?: string;
  originalError?: unknown;
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

// message/status 형태의 에러 객체 가드 (백엔드 원본 payload 그대로 노출될 때)
function isApiErrorObject(error: unknown): error is { status?: number; message: string; target?: string } {
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

      // 에러 메시지 패턴 매칭을 통한 에러 타입 분류
      // includes() 검사로 한국어 에러 메시지에서 핵심 키워드를 추출하여 적절한 에러 타입 결정

      // 블랙리스트 차단 에러
      if (errorMessage.includes('차단')) {
        // 원본 메시지에서 더 사용자 친화적인 메시지로 변환
        let userFriendlyMessage = error.error;

        // "롤링페이퍼" 키워드가 있으면 다른 컨텍스트일 수 있으므로 적절히 변환
        if (errorMessage.includes('롤링페이퍼')) {
          // 롤링페이퍼 관련은 원본 메시지 유지하거나 약간 수정
          userFriendlyMessage = '차단되거나 차단한 회원의 콘텐츠는 조회할 수 없습니다.';
        } else {
          // 일반적인 차단 메시지
          userFriendlyMessage = '차단되거나 차단한 회원과는 상호작용할 수 없습니다.';
        }

        return {
          type: 'PERMISSION_DENIED',
          title: '차단된 사용자',
          message: error.error,
          userMessage: userFriendlyMessage,
          originalError: error
        };
      }

      // 롤링페이퍼 삭제 권한 에러 (명시적 매칭)
      if (errorMessage.includes('본인 롤링페이퍼') ||
          errorMessage.includes('메시지만 삭제')) {
        return {
          type: 'PERMISSION_DENIED',
          title: '권한 없음',
          message: error.error,
          userMessage: error.error,
          originalError: error
        };
      }

      // 메시지를 찾을 수 없음 (롤링페이퍼 관련)
      if (errorMessage.includes('메시지를 찾을 수 없')) {
        return {
          type: 'NOT_FOUND',
          title: '메시지 없음',
          message: error.error,
          userMessage: error.error,
          originalError: error
        };
      }

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

    // JSON 문자열 형태의 Error 메시지 디코딩 (예: "{...message: '댓글 작성에 실패...'}")
    if (isErrorWithMessage(error)) {
      const raw = error.message.trim();
      if (raw.startsWith('{') && raw.endsWith('}')) {
        try {
          const parsed = JSON.parse(raw) as { message?: string };
          if (parsed.message) {
            return this.mapApiError(parsed);
          }
        } catch {
          // ignore parse failure
        }
      }
    }

    // 일반 Error 객체에서 차단 키워드 감지 (예: fetch wrapper가 Error로 던질 때)
    if (isErrorWithMessage(error) && error.message.toLowerCase().includes('차단')) {
      const lower = error.message.toLowerCase();
      const userFriendlyMessage = lower.includes('롤링페이퍼')
        ? '차단된 상대의 롤링페이퍼는 볼 수 없습니다.'
        : '차단된 사용자와는 상호작용할 수 없습니다.';

      return {
        type: 'PERMISSION_DENIED',
        title: '차단된 사용자',
        message: error.message,
        userMessage: userFriendlyMessage,
        originalError: error
      };
    }

    // 일반 Error 객체에서 댓글 작성 실패 메시지 정제
    if (isErrorWithMessage(error) && error.message.includes('댓글 작성에 실패')) {
      return {
        type: 'SERVER_ERROR',
        title: '댓글 작성 오류',
        message: '댓글 작성에 실패했습니다.',
        userMessage: '댓글 작성 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.',
        originalError: error
      };
    }

    // message/status 형태의 에러 객체 처리
    if (isApiErrorObject(error)) {
      const lowerMsg = error.message.toLowerCase();

      if (lowerMsg.includes('차단')) {
        const userFriendlyMessage = lowerMsg.includes('롤링페이퍼')
          ? '차단된 상대의 롤링페이퍼는 볼 수 없습니다.'
          : '차단된 사용자와는 상호작용할 수 없습니다.';

        return {
          type: 'PERMISSION_DENIED',
          title: '차단된 사용자',
          message: error.message,
          userMessage: userFriendlyMessage,
          originalError: error
        };
      }

      if (typeof error.status === 'number') {
        return this.mapApiError({ status: error.status });
      }

      return {
        type: 'UNKNOWN_ERROR',
        title: '오류 발생',
        message: error.message,
        userMessage: error.message,
        originalError: error
      };
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
    // 그리드 위치 중복 에러를 특별히 처리하여 사용자에게 명확한 안내 메시지 제공
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

// handleApiError를 export (기존 코드 호환성)
export const handleApiError = async (error: unknown): Promise<void> => {
  const appError = ErrorHandler.mapApiError(error);
  logger.error(appError.title, appError.message, appError.originalError);

  // Toast 표시 로직 - 동적 import를 사용하여 토스트 스토어 순환 의존성 방지
  const { showToast } = (await import('@/stores/toast.store')).useToastStore.getState();
  showToast({
    type: 'error',
    message: appError.userMessage || appError.message
  });
};
