import { DecoType } from '@/types/domains/paper';

/**
 * 롤링페이퍼 API 요청/응답 타입 정의
 * 백엔드 API와 완벽히 호환되는 타입들
 */

// ===== Request Types =====

/**
 * 메시지 작성 요청 타입
 * POST /api/paper/write
 */
export interface CreateMessageRequest {
  ownerId: number;
  decoType: DecoType;
  anonymity: string;
  content: string;
  x: number;
  y: number;
}

/**
 * 메시지 삭제 요청 타입
 * POST /api/paper/delete
 */
export interface DeleteMessageRequest {
  messageId: number;
}

// ===== Response Types =====

/**
 * API 응답 래퍼 타입
 */
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

/**
 * 메시지 작성 응답 타입
 */
export type CreateMessageResponse = ApiResponse<string>;

/**
 * 메시지 삭제 응답 타입
 */
export type DeleteMessageResponse = ApiResponse<string>;

// ===== Query Params Types =====

/**
 * 롤링페이퍼 조회 쿼리 파라미터
 */
export interface RollingPaperQueryParams {
  userName?: string;
  page?: number;
  size?: number;
}