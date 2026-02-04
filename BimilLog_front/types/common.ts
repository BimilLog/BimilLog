// Base Response Type
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T | null;
  error?: string;
  message?: string;
  needsRelogin?: boolean; // JWT 토큰 만료 시 true, 클라이언트에서 자동 로그아웃 처리
}

// Pagination Types
// PageInfo: 프론트엔드 요청 시 사용하는 간단한 페이징 정보
export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  number: number;
}

// PageResponse: Spring Data JPA가 반환하는 복잡한 페이징 응답 (백엔드 구조 그대로)
export interface PageResponse<T> {
  content: T[]; // 실제 데이터 배열
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  number: number; // 현재 페이지 번호 (0부터 시작)
  size: number;
  numberOfElements: number; // 현재 페이지의 실제 데이터 개수
  empty: boolean;
  pageable?: { // Spring Pageable 객체 정보 (정렬 등)
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      ascending: boolean;
      descending: boolean;
    };
  };
}

// CursorPageResponse: 커서 기반 페이징 응답 (무한 스크롤/더보기 버튼용)
export interface CursorPageResponse<T> {
  content: T[];           // 실제 데이터 배열
  nextCursor: number | null; // 다음 페이지 요청 시 사용할 커서 (마지막 게시글 ID)
  hasNext: boolean;       // 다음 페이지 존재 여부
  size: number;           // 요청된 페이지 크기
}

// Common Entity Types
export interface BaseEntity {
  id: number;
  createdAt: string;
  updatedAt: string;
}

export interface UserInfo {
  id?: number;
  userId?: number;
  userName: string | null;
  email?: string;
  profileImageUrl?: string | null;
}

// Error Types
export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  path?: string;
}

// Loading States
// 컴포넌트의 비동기 작업 상태를 관리 (TanStack Query와 별개로 사용)
export type LoadingState = 'idle' | 'loading' | 'success' | 'error';

// Common Props
export interface WithClassName {
  className?: string;
}

export interface WithChildren {
  children?: React.ReactNode;
}

export interface WithStyle {
  style?: React.CSSProperties;
}