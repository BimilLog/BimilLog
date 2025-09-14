"use client";

import dynamic from 'next/dynamic';
import { ComponentType } from 'react';

// Lazy load heavy components
export const LazyEditor = dynamic(
  () => import('@/components/molecules/forms/editor'),
  { 
    loading: () => <div className="h-64 bg-gray-100 animate-pulse rounded-lg" />,
    ssr: false 
  }
);

export const LazyReportModal = dynamic(
  () => import('@/components/organisms/common/ReportModal').then(mod => ({ default: mod.ReportModal })),
  { ssr: false }
);

export const LazyKakaoFriendsModal = dynamic(
  () => import('@/components/molecules/modals/kakao-friends-modal').then(mod => ({ default: mod.KakaoFriendsModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg max-w-md w-full mx-4 max-h-[80vh] overflow-hidden">
          <div className="p-4 bg-gradient-to-r from-yellow-400 to-yellow-500">
            <div className="h-6 bg-yellow-300/50 rounded animate-pulse"></div>
          </div>
          <div className="flex items-center justify-center min-h-[300px]">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-yellow-600"></div>
          </div>
        </div>
      </div>
    )
  }
);

export const LazyReportDetailModal = dynamic(
  () => import('@/components/organisms/admin/ReportDetailModal').then(mod => ({ default: mod.ReportDetailModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden">
          <div className="p-6 border-b bg-gradient-to-r from-purple-50 to-pink-50">
            <div className="h-6 bg-purple-200 rounded animate-pulse"></div>
          </div>
          <div className="flex items-center justify-center min-h-[400px]">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purple-600"></div>
          </div>
        </div>
      </div>
    )
  }
);

export const LazyBrowserGuideModal = dynamic(
  () => import('@/components/molecules/modals/browser-guide-modal').then(mod => ({ default: mod.BrowserGuideModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-white p-6 rounded-lg max-w-md w-full mx-4">
          <div className="flex items-center justify-center min-h-[200px]">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purple-600"></div>
          </div>
        </div>
      </div>
    )
  }
);

// Lazy load heavy organisms
export const LazyWriteForm = dynamic(
  () => import('@/components/organisms/board/WriteForm').then(mod => ({ default: mod.WriteForm })),
  { 
    loading: () => <div className="min-h-screen bg-gray-100 animate-pulse" />,
    ssr: false 
  }
);

export const LazyAdminStats = dynamic(
  () => import('@/components/organisms/admin/AdminStats').then(mod => ({ default: mod.AdminStats })),
  { 
    loading: () => <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-pulse">
      <div className="h-32 bg-gray-200 rounded-lg" />
      <div className="h-32 bg-gray-200 rounded-lg" />
      <div className="h-32 bg-gray-200 rounded-lg" />
    </div>
  }
);

export const LazyReportListContainer = dynamic(
  () => import('@/components/organisms/admin/ReportListContainer').then(mod => ({ default: mod.ReportListContainer })),
  {
    loading: () => <div className="space-y-4 animate-pulse">
      <div className="h-20 bg-gray-200 rounded-lg" />
      <div className="h-20 bg-gray-200 rounded-lg" />
      <div className="h-20 bg-gray-200 rounded-lg" />
    </div>
  }
);

// 추가로 무거운 컴포넌트들을 위한 lazy loading 컴포넌트들
export const LazyMessageListModal = dynamic(
  () => import('@/components/organisms/rolling-paper/MessageListModal').then(mod => ({ default: mod.MessageListModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center">
        <div className="bg-white rounded-lg p-6 flex flex-col items-center gap-3">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-cyan-600"></div>
          <p className="text-sm text-brand-secondary">메시지 목록 로딩 중...</p>
        </div>
      </div>
    ),
  }
);


export const LazyCommentItem = dynamic(
  () => import('@/components/organisms/board/CommentItem').then(mod => ({ default: mod.CommentItem })),
  {
    loading: () => <div className="p-4 border border-gray-200 rounded-lg animate-pulse">
      <div className="flex items-center space-x-3 mb-2">
        <div className="w-8 h-8 bg-gray-200 rounded-full" />
        <div className="h-4 bg-gray-200 rounded w-20" />
      </div>
      <div className="h-4 bg-gray-200 rounded w-full mb-2" />
      <div className="h-4 bg-gray-200 rounded w-3/4" />
    </div>
  }
);

export const LazyNotificationBell = dynamic(
  () => import('@/components/organisms/common/notification-bell').then(mod => ({ default: mod.NotificationBell })),
  {
    ssr: false,
    loading: () => (
      <div className="relative">
        <div className="w-10 h-10 bg-gray-200 rounded-full animate-pulse" />
      </div>
    )
  }
);

export const LazyRollingPaperGrid = dynamic(
  () => import('@/components/organisms/rolling-paper/RollingPaperGrid').then(mod => ({ default: mod.RollingPaperGrid })),
  {
    loading: () => <div className="grid grid-cols-4 md:grid-cols-6 gap-2 animate-pulse">
      {Array.from({ length: 24 }).map((_, i) => (
        <div key={i} className="h-20 bg-gray-200 rounded-lg" />
      ))}
    </div>,
    ssr: false
  }
);

export const LazyUserActivitySection = dynamic(
  () => import('@/components/organisms/user/UserActivitySection').then(mod => ({ default: mod.UserActivitySection })),
  {
    loading: () => <div className="space-y-4 animate-pulse">
      <div className="h-40 bg-gray-200 rounded-lg" />
      <div className="h-32 bg-gray-200 rounded-lg" />
      <div className="h-32 bg-gray-200 rounded-lg" />
    </div>
  }
);
