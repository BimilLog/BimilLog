import { DependencyList } from 'react';

// API Hook Types
export interface UseApiState<T = any> {
  data: T | null;
  loading: boolean;
  error: Error | null;
}

export interface UseApiOptions {
  immediate?: boolean;
  onSuccess?: (data: any) => void;
  onError?: (error: Error) => void;
  retryCount?: number;
  retryDelay?: number;
}

// Pagination Hook Types
export interface UsePaginationState {
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalItems: number;
}

export interface UsePaginationOptions {
  initialPage?: number;
  initialPageSize?: number;
  onChange?: (page: number, pageSize: number) => void;
}

// Loading State Hook Types
export interface UseLoadingStateReturn {
  loading: Record<string, boolean>;
  isLoading: (key?: string) => boolean;
  isAnyLoading: () => boolean;
  setLoading: (key: string, value: boolean) => void;
  startLoading: (key: string) => void;
  stopLoading: (key: string) => void;
  resetLoading: () => void;
  withLoading: <T>(key: string, asyncFn: () => Promise<T>) => Promise<T>;
}

// Form Hook Types
export interface UseFormState<T = any> {
  values: T;
  errors: Partial<Record<keyof T, string>>;
  touched: Partial<Record<keyof T, boolean>>;
  isSubmitting: boolean;
  isValid: boolean;
}

export interface UseFormOptions<T = any> {
  initialValues: T;
  validate?: (values: T) => Partial<Record<keyof T, string>>;
  onSubmit: (values: T) => void | Promise<void>;
  validateOnChange?: boolean;
  validateOnBlur?: boolean;
}

// Local Storage Hook Types
export interface UseLocalStorageOptions {
  serializer?: (value: any) => string;
  deserializer?: (value: string) => any;
  syncData?: boolean;
}

// Debounce Hook Types
export interface UseDebounceOptions {
  delay?: number;
  maxWait?: number;
  leading?: boolean;
  trailing?: boolean;
}

// Media Query Hook Types
export interface UseMediaQueryOptions {
  defaultValue?: boolean;
  initializeWithValue?: boolean;
}

export interface UseBreakpointReturn {
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
  isSmallScreen: boolean;
  isMediumScreen: boolean;
  isLargeScreen: boolean;
}

// Event Hook Types
export interface UseEventListenerOptions {
  target?: EventTarget | null;
  capture?: boolean;
  once?: boolean;
  passive?: boolean;
}

// Intersection Observer Hook Types
export interface UseIntersectionObserverOptions extends IntersectionObserverInit {
  freezeOnceVisible?: boolean;
}

// Fetch Hook Types
export interface UseFetchOptions extends RequestInit {
  manual?: boolean;
  dependencies?: DependencyList;
  onSuccess?: (data: any) => void;
  onError?: (error: Error) => void;
}

// Timer Hook Types
export interface UseIntervalOptions {
  immediate?: boolean;
  pauseOnHide?: boolean;
}

export interface UseTimeoutOptions {
  autoStart?: boolean;
}

// Clipboard Hook Types
export interface UseClipboardReturn {
  value: string | null;
  copy: (text: string) => Promise<void>;
  paste: () => Promise<string>;
  isSupported: boolean;
}

// Online Status Hook Types
export interface UseOnlineStatusReturn {
  isOnline: boolean;
  wasOnline: boolean;
  lastOnlineTime: Date | null;
}

// Permission Hook Types
export type PermissionName = 
  | 'geolocation'
  | 'notifications'
  | 'camera'
  | 'microphone'
  | 'clipboard-read'
  | 'clipboard-write';

export interface UsePermissionReturn {
  state: PermissionState | null;
  isGranted: boolean;
  isDenied: boolean;
  isPrompt: boolean;
  request: () => Promise<PermissionState>;
}