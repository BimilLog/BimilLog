// 아토믹 디자인 패턴에 따른 컴포넌트 Export
// Brad Frost의 아토믹 디자인 방법론을 기반으로 구성

// ==============================================
// DESIGN SYSTEM
// ==============================================
export * from '../lib/design-tokens';

// ==============================================
// ATOMS - 가장 기본적인 UI 요소들
// ==============================================
// Form Controls
export { Button, buttonVariants } from './atoms/button';
export { Input } from './atoms/input';
export { Label } from './atoms/label';
export { Textarea } from './atoms/textarea';
export { Switch } from './atoms/switch';

// Media & Content
export { Avatar, AvatarImage, AvatarFallback } from './atoms/avatar';
export { Badge } from './atoms/badge';
export { Icon } from './atoms/icon';
export { Spinner } from './atoms/spinner';
export { default as SafeHTML } from './atoms/SafeHTML';

// ==============================================
// MOLECULES - 조합된 컴포넌트들
// ==============================================
// Form & Input Components
export { SearchBox } from './molecules/search-box';
export { FormField } from './molecules/form-field';

// Layout & Structure
export { 
  Card, 
  CardHeader, 
  CardTitle, 
  CardDescription, 
  CardContent, 
  CardFooter,
  CardAction,
  FeatureCard,
  CTACard,
  BottomSheetCard
} from './molecules/card';
export { Alert, AlertDescription, AlertTitle } from './molecules/alert';
export { Tabs, TabsList, TabsTrigger, TabsContent } from './molecules/tabs';

// Interactive Components
export { 
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from './molecules/dialog';
export { 
  Popover,
  PopoverContent,
  PopoverTrigger
} from './molecules/popover';
export {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from './molecules/dropdown-menu';
export {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from './molecules/select';
export {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger
} from './molecules/sheet';

// Content Components
export { default as Editor } from './molecules/editor';
export { ReportModal } from './molecules/ReportModal';
export { KakaoFriendsModal } from './molecules/kakao-friends-modal';

// State Components
export { 
  Loading,
  Spinner as MoleculeSpinner,
  BrandSpinner,
  Skeleton,
  CardSkeleton,
  ListSkeleton,
  PullToRefreshLoader,
  loadingStyles
} from './molecules/loading';
export {
  EmptyState,
  EmptyPosts,
  EmptyMessages,
  EmptySearch,
  ErrorState,
  OfflineState,
  WelcomeState,
  PageEmptyState
} from './molecules/empty-state';

// ==============================================
// ORGANISMS - 복잡한 컴포넌트들
// ==============================================
// Navigation
export { AuthHeader } from './organisms/auth-header';
export { MobileNav } from './organisms/mobile-nav';
export { NotificationBell } from './organisms/notification-bell';

// Board Components
export { BoardSearch } from './organisms/board/board-search';
export { BoardPagination } from './organisms/board/board-pagination';
export { PostList } from './organisms/board/post-list';
export { PopularPostList } from './organisms/board/popular-post-list';
export { NoticeList } from './organisms/board/notice-list';

// ==============================================
// TEMPLATES - 페이지 레이아웃들
// ==============================================
export { PageTemplate } from './templates/page-template';
export { AuthTemplate } from './templates/auth-template';
export { DashboardTemplate } from './templates/dashboard-template';

// ==============================================
// UTILITY & EXAMPLES
// ==============================================
export { AtomicShowcase } from './examples/atomic-showcase';

// ==============================================
// TYPE EXPORTS
// ==============================================
export type {
  ColorScale,
  FontSize,
  Spacing,
  BorderRadius
} from '../lib/design-tokens'; 