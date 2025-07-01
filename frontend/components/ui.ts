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
export { KakaoShareButton } from './atoms/kakao-share-button';

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
export { KakaoFriendsModal } from './molecules/kakao-friends-modal';
export { default as Editor } from './molecules/editor';
export * from './molecules/search-box';
export * from './molecules/form-field';

// State Components (새로 추가) - Spinner 충돌 방지를 위해 선택적 export
export { 
  Loading,
  Skeleton,
  loadingStyles
} from './molecules/loading';
export * from './molecules/empty-state';

// Rolling Paper Components Re-exports (호환성용)
export { RecentVisits } from '../app/rolling-paper/components/RecentVisits';
export { RollingPaperHeader } from '../app/rolling-paper/components/RollingPaperHeader';
export { MessageForm } from '../app/rolling-paper/components/MessageForm';
export { MessageView } from '../app/rolling-paper/components/MessageView';
export { RollingPaperGrid } from '../app/rolling-paper/components/RollingPaperGrid';
export { RecentMessages } from '../app/rolling-paper/components/RecentMessages';

// Organisms Re-exports
export { AuthHeader } from './organisms/auth-header';
export { MobileNav } from './organisms/mobile-nav';
export { NotificationBell } from './organisms/notification-bell';
export { BoardSearch } from './organisms/board/board-search';
export { BoardPagination } from './organisms/board/board-pagination';
export { PostList } from './organisms/board/post-list';
export { PopularPostList } from './organisms/board/popular-post-list';
export { NoticeList } from './organisms/board/notice-list'; 