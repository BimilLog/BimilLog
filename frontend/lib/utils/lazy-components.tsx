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