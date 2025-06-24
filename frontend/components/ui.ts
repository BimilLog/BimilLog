// 호환성을 위한 UI 컴포넌트 Re-export
// 기존 @/components/ui/* 경로를 새로운 아토믹 구조로 매핑

// Atoms Re-exports
export * from './atoms/button';
export * from './atoms/input';
export * from './atoms/label';
export * from './atoms/textarea';
export * from './atoms/avatar';
export * from './atoms/badge';
export * from './atoms/switch';
export * from './atoms/icon';
export * from './atoms/spinner';
export { default as SafeHTML } from './atoms/SafeHTML';

// Molecules Re-exports
export * from './molecules/alert';
export * from './molecules/card';
export * from './molecules/popover';
export * from './molecules/tabs';
export * from './molecules/dropdown-menu';
export * from './molecules/select';
export * from './molecules/dialog';
export * from './molecules/sheet';
export { ReportModal } from './molecules/ReportModal';
export { default as Editor } from './molecules/editor';
export * from './molecules/search-box';
export * from './molecules/form-field';

// State Components (새로 추가) - Spinner 충돌 방지를 위해 선택적 export
export { 
  Loading,
  BrandSpinner,
  Skeleton,
  CardSkeleton,
  ListSkeleton,
  PullToRefreshLoader,
  loadingStyles
} from './molecules/loading';
export * from './molecules/empty-state'; 