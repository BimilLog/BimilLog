import type { ApiResponse, PageResponse } from '@/types';
import { logger } from '@/lib/utils/logger';
import { isValidApiResponse, isPageResponse, validateResponseData } from './type-guards';

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

    // API 응답 구조 검증 - 기본 ApiResponse 형태인지 확인
    if (!isValidApiResponse<T>(response)) {
      logger.warn('Invalid API response structure:', response);
      return {
        success: false,
        error: 'Invalid response format'
      };
    }

    // 성공 응답의 데이터 유효성 검증 및 fallback 처리
    // validator 실패 시 fallback 데이터를 사용하여 앱 crash 방지
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

    // content 배열의 각 아이템 검증 및 필터링
    // 잘못된 데이터가 포함된 아이템들을 제거하여 UI 렌더링 오류 방지
    if (options?.itemValidator && Array.isArray(response.data.content)) {
      const validatedContent = response.data.content.filter(options.itemValidator);

      // 필터링된 아이템이 있는 경우 페이지네이션 정보 업데이트
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