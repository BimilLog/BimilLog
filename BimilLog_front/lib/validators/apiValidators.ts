import type { ApiResponse, PageResponse, ErrorResponse } from '@/types/common';

/**
 * API 응답 타입 가드 함수들
 * 런타임에서 API 응답의 타입을 안전하게 검증합니다.
 */

// 기본 객체 타입 검증
function isObject(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

// API 응답 기본 구조 검증
export function isValidApiResponse<T = unknown>(data: unknown): data is ApiResponse<T> {
  if (!isObject(data)) return false;

  // success 필드는 필수
  if (typeof data.success !== 'boolean') return false;

  // success가 true일 때는 data 필드가 있어야 함
  if (data.success && !('data' in data)) return false;

  // success가 false일 때는 error 필드가 있어야 함
  if (!data.success && !('error' in data)) return false;

  // needsRelogin은 선택사항이지만 있다면 boolean이어야 함
  if ('needsRelogin' in data && typeof data.needsRelogin !== 'boolean') return false;

  return true;
}

// 페이지네이션 응답 검증
export function isPageResponse<T = unknown>(data: unknown): data is PageResponse<T> {
  if (!isObject(data)) return false;

  // 필수 필드 검증
  if (!Array.isArray(data.content)) return false;
  if (typeof data.totalElements !== 'number') return false;
  if (typeof data.totalPages !== 'number') return false;
  if (typeof data.number !== 'number') return false;
  if (typeof data.size !== 'number') return false;

  // 선택 필드 검증
  if ('first' in data && typeof data.first !== 'boolean') return false;
  if ('last' in data && typeof data.last !== 'boolean') return false;
  if ('numberOfElements' in data && typeof data.numberOfElements !== 'number') return false;
  if ('empty' in data && typeof data.empty !== 'boolean') return false;

  return true;
}

// 에러 응답 검증
export function isErrorResponse(data: unknown): data is ErrorResponse {
  if (!isObject(data)) return false;

  // 필수 필드 검증
  if (typeof data.code !== 'string') return false;
  if (typeof data.message !== 'string') return false;
  if (typeof data.timestamp !== 'string') return false;

  // 선택 필드 검증
  if ('path' in data && typeof data.path !== 'string') return false;

  return true;
}

// API 응답에서 데이터 추출 (타입 안전)
export function extractApiData<T>(response: unknown): T | null {
  if (!isValidApiResponse<T>(response)) return null;
  if (!response.success) return null;
  return response.data ?? null;
}

// 페이지 응답에서 컨텐츠 추출 (타입 안전)
export function extractPageContent<T>(data: unknown): T[] {
  if (!isPageResponse<T>(data)) return [];
  return data.content;
}

// 에러 메시지 추출 (타입 안전)
export function extractErrorMessage(response: unknown): string {
  if (isErrorResponse(response)) {
    return response.message;
  }

  if (isObject(response) && typeof response.message === 'string') {
    return response.message;
  }

  if (isObject(response) && typeof response.error === 'string') {
    return response.error;
  }

  return '알 수 없는 오류가 발생했습니다';
}

// 재로그인 필요 여부 확인
export function needsRelogin(response: unknown): boolean {
  if (!isObject(response)) return false;
  return response.needsRelogin === true;
}

// 배열 응답 검증
export function isArrayResponse<T = unknown>(data: unknown): data is T[] {
  return Array.isArray(data);
}

// 문자열 응답 검증
export function isStringResponse(data: unknown): data is string {
  return typeof data === 'string';
}

// 숫자 응답 검증
export function isNumberResponse(data: unknown): data is number {
  return typeof data === 'number' && !isNaN(data);
}

// 불린 응답 검증
export function isBooleanResponse(data: unknown): data is boolean {
  return typeof data === 'boolean';
}