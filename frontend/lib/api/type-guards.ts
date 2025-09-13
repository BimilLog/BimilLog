import type { ApiResponse, PageResponse, ErrorResponse } from '@/types';
import { logger } from '@/lib/utils/logger';

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