/**
 * 롤링페이퍼 도메인 타입 정의 인덱스
 *
 * 기존 paper.ts 파일과의 호환성을 유지하면서
 * 새로운 타입 정의들을 추가로 export
 */

// 기존 타입 re-export (../paper.ts에서)
export {
  type DecoType,
  type RollingPaperMessage,
  type VisitMessage,
  decoTypeMap
} from '../paper';

// API 관련 타입
export {
  type CreateMessageRequest,
  type DeleteMessageRequest,
  type ApiResponse,
  type CreateMessageResponse,
  type DeleteMessageResponse,
  type RollingPaperQueryParams
} from './api';

// UI 관련 타입
export {
  type GridPosition,
  type MessageGrid,
  type RollingPaperViewState,
  type MessageFormData,
  type ShareOptions,
  type RollingPaperContainerProps,
  type RollingPaperViewProps,
  type CreateMessageParams,
  type DeleteMessageParams,
  type MessageFilterOptions,
  type MessageSortOptions
} from './ui';