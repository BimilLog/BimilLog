// v2 백엔드 API 타입 정의
// 각 도메인별 타입은 './domains/*'에서 관리

export * from './domains/post';
export * from './domains/comment';
export * from './domains/user';
export * from './domains/notification';
export * from './domains/paper';
export * from './domains/admin';
export * from './domains/auth';

// 검색 타입 (v2 백엔드 호환)
export type SearchType = 'TITLE' | 'TITLE_CONTENT' | 'AUTHOR';

export interface SearchRequest {
  type: SearchType;
  keyword: string;
  page?: number;
  size?: number;
}