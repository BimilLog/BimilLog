// Base Response Type
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T | null;
  error?: string;
  message?: string;
  needsRelogin?: boolean;
}

// Pagination Types
export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  number: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  number: number;
  size: number;
  numberOfElements: number;
  empty: boolean;
  pageable?: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      ascending: boolean;
      descending: boolean;
    };
  };
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