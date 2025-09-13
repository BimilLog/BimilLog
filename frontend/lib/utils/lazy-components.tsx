import dynamic from 'next/dynamic';
import React, { ComponentType } from 'react';

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
  { ssr: false }
);

export const LazyBrowserGuideModal = dynamic(
  () => import('@/components/molecules/modals/browser-guide-modal').then(mod => ({ default: mod.BrowserGuideModal })),
  { ssr: false }
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