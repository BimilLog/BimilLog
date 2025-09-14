"use client";

import dynamic from "next/dynamic";
import { Settings as SettingsIcon } from "lucide-react";
import { CuteLoadingSpinner, ToastContainer, ErrorAlert, Button } from "@/components";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { useSettings, useToast } from "@/hooks";

// Dynamic imports for heavy settings components
const NotificationSettings = dynamic(
  () => import("@/components/organisms/user/settings").then(mod => ({ default: mod.NotificationSettings })),
  {
    ssr: false,
    loading: () => (
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 animate-pulse">
        <div className="h-6 bg-gray-200 rounded-lg mb-4 w-32"></div>
        <div className="space-y-3">
          <div className="h-12 bg-gray-200 rounded-lg"></div>
          <div className="h-12 bg-gray-200 rounded-lg"></div>
          <div className="h-12 bg-gray-200 rounded-lg"></div>
        </div>
      </div>
    )
  }
);

const AccountSettings = dynamic(
  () => import("@/components/organisms/user/settings").then(mod => ({ default: mod.AccountSettings })),
  {
    ssr: false,
    loading: () => (
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 animate-pulse">
        <div className="h-6 bg-gray-200 rounded-lg mb-4 w-24"></div>
        <div className="h-10 bg-gray-200 rounded-lg w-32"></div>
      </div>
    )
  }
);

export default function SettingsPage() {
  const {
    settings,
    loading,
    saving,
    withdrawing,
    allEnabled,
    error,
    handleSingleToggle,
    handleAllToggle,
    handleWithdraw,
    loadSettings,
  } = useSettings();

  const { toasts, removeToast } = useToast();

  if (loading) {
    return (
      <MainLayout containerClassName="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <CuteLoadingSpinner message="설정을 불러오는 중..." />
        </div>
      </MainLayout>
    );
  }

  if (error) {
    return (
      <MainLayout containerClassName="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <ErrorAlert>
            <div className="flex items-center justify-between">
              <span>{error}</span>
              <Button onClick={loadSettings} variant="outline" size="sm" className="ml-4">
                다시 시도
              </Button>
            </div>
          </ErrorAlert>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout containerClassName="container mx-auto px-4">
      <header className="py-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-brand-button rounded-full flex items-center justify-center shadow-brand-lg">
            <SettingsIcon className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-brand-primary">설정</h1>
            <p className="text-brand-secondary">알림 설정과 계정을 관리하세요</p>
          </div>
        </div>
      </header>

      <div className="pb-8">
        <div className="max-w-2xl mx-auto space-y-6">
          <NotificationSettings
            settings={settings}
            saving={saving}
            allEnabled={allEnabled}
            onSingleToggle={handleSingleToggle}
            onAllToggle={handleAllToggle}
          />
          <AccountSettings
            withdrawing={withdrawing}
            onWithdraw={handleWithdraw}
          />
        </div>
      </div>

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </MainLayout>
  );
}