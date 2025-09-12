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
export { KakaoShareButton } from './atoms/kakao-share-button';
export { DecoIcon, EmojiStyleDecoIcon } from './atoms/deco-icon';
export { AuthLoadingScreen } from './atoms/AuthLoadingScreen';

// Refactored atoms
export { StatCard } from './atoms/stat-card';
export { LoadingSpinner } from './atoms/loading-spinner';
export { EmptyState } from './atoms/empty-state';

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
  CardAction
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

// Refactored molecules
export { ActivityCard } from './molecules/activity-card';
export { ProfileCard } from './molecules/profile-card';
export { SettingsSection } from './molecules/settings-section';
export { ToastContainer, ToastComponent } from './molecules/toast';
export { SettingToggle } from './molecules/setting-toggle';

// Advertisement Components
export { AdFitBanner, AD_SIZES, AD_UNITS, getAdUnit } from './molecules/adfit-banner';
export { ResponsiveAdFitBanner } from './molecules/responsive-adfit-banner';

// State Components
export { 
  Loading,
  Spinner as MoleculeSpinner,
  Skeleton,
  loadingStyles
} from './molecules/loading';

// ==============================================
// ORGANISMS - 복잡한 컴포넌트들
// ==============================================
// Navigation
export { AuthHeader } from './organisms/auth-header';
export { MobileNav } from './organisms/mobile-nav';
export { NotificationBell } from './organisms/notification-bell';
export { Breadcrumb } from './organisms/breadcrumb';

// Auth Components
export { NicknameSetupForm } from './organisms/NicknameSetupForm';

// Board Components
export { BoardSearch } from './organisms/board/board-search';
export { BoardPagination } from './organisms/board/board-pagination';
export { PostList } from './organisms/board/post-list';
export { PopularPostList } from './organisms/board/popular-post-list';
export { NoticeList } from './organisms/board/notice-list';
export { BoardHeader } from './organisms/board/BoardHeader';
export { BoardTabs } from './organisms/board/BoardTabs';

// Home Components
export { HomeHero } from './organisms/home/HomeHero';
export { HomeFeatures } from './organisms/home/HomeFeatures';
export { HomeFooter } from './organisms/home/HomeFooter';

// Write Components
export { WritePageHeader } from './organisms/WritePageHeader';
export { WriteForm } from './organisms/WriteForm';

// Admin Components
export { AdminHeader } from './organisms/admin/AdminHeader';
export { AdminStats } from './organisms/admin/AdminStats';
export { ReportList } from './organisms/admin/ReportList';
export { ReportDetailModal } from './organisms/admin/ReportDetailModal';

// Settings Components
export { NotificationSettings } from './organisms/settings/NotificationSettings';
export { AccountSettings } from './organisms/settings/AccountSettings';

// User Components
export { UserStatsSection } from './organisms/user/UserStatsSection';
export { UserActivitySection } from './organisms/user/UserActivitySection';

// ==============================================
// ROLLING PAPER COMPONENTS - 롤링페이퍼 전용 컴포넌트들
// ==============================================
export { RecentVisits } from '../app/rolling-paper/components/RecentVisits';
export { RollingPaperHeader } from '../app/rolling-paper/components/RollingPaperHeader';
export { MessageForm } from '../app/rolling-paper/components/MessageForm';
export { MessageView } from '../app/rolling-paper/components/MessageView';
export { RollingPaperGrid } from '../app/rolling-paper/components/RollingPaperGrid';
export { RecentMessages } from '../app/rolling-paper/components/RecentMessages';
export { RollingPaperLayout } from '../app/rolling-paper/components/RollingPaperLayout';
export { PageNavigation } from '../app/rolling-paper/components/PageNavigation';
export { InfoCard } from '../app/rolling-paper/components/InfoCard';
export { RollingPaperClient } from '../app/rolling-paper/components/RollingPaperClient';
export { MessageListModal } from '../app/rolling-paper/components/MessageListModal';

// ==============================================
// LAYOUT COMPONENTS
// ==============================================
export { AuthLayout } from './organisms/AuthLayout';

// ==============================================
// UTILITY FUNCTIONS & HOOKS
// ==============================================
export * from '../lib/cookies';
export { useRollingPaper } from '../hooks/useRollingPaper';
export { useRollingPaperShare } from '../hooks/useRollingPaperShare';

// ==============================================
// LUCIDE ICONS - 필수 아이콘들
// ==============================================
export {
  AlertTriangle,
  TrendingUp,
  Shield,
  Eye,
  Clock,
  UserX,
} from 'lucide-react';

// ==============================================
// TYPE EXPORTS
// ==============================================
export type {
  ColorScale,
  FontSize,
  Spacing,
  BorderRadius
} from '../lib/design-tokens'; 