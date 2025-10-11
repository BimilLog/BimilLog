"use client";

import dynamic from "next/dynamic";
import { Settings as SettingsIcon } from "lucide-react";
import { CuteLoadingSpinner, ErrorAlert, Button } from "@/components";
import { useSettings } from "@/hooks";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";

// Dynamic imports for heavy settings components
const NotificationSettings = dynamic(
  () => import("@/components/organisms/user/settings").then(mod => ({ default: mod.NotificationSettings })),
  {
    ssr: false,
    loading: () => (
      <div className="bg-white rounded-xl shadow-brand-sm border border-gray-100 p-6 animate-pulse">
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
      <div className="bg-white rounded-xl shadow-brand-sm border border-gray-100 p-6 animate-pulse">
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
    savingFields,
    savedFields,
    withdrawing,
    showWithdrawModal,
    allEnabled,
    isIndeterminate,
    error,
    handleSingleToggle,
    handleAllToggle,
    handleOpenWithdrawModal,
    handleCloseWithdrawModal,
    handleConfirmWithdraw,
    loadSettings,
  } = useSettings();


  if (loading) {
    return (
      <MainLayout
        className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
        containerClassName="container mx-auto px-4"
      >
        <div className="py-8">
          <div className="max-w-3xl mx-auto">
            <CuteLoadingSpinner message="설정을 불러오는 중..." />
          </div>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout
      className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
      containerClassName="container mx-auto px-4"
    >
      <header className="py-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-brand-button rounded-full flex items-center justify-center shadow-brand-lg">
            <SettingsIcon className="w-6 h-6 stroke-white fill-white/20" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-brand-primary inline-block">설정</h1>
            <p className="text-brand-secondary">알림 설정과 계정을 관리하세요</p>
          </div>
        </div>
      </header>

      <div className="pb-8">
        <div className="max-w-3xl mx-auto space-y-6">
          {error && (
            <ErrorAlert>
              <div className="flex items-center justify-between">
                <span>{error}</span>
                <Button onClick={loadSettings} variant="outline" size="sm" className="ml-4">
                  다시 시도
                </Button>
              </div>
            </ErrorAlert>
          )}

          <NotificationSettings
            settings={settings}
            saving={saving}
            savingFields={savingFields}
            savedFields={savedFields}
            allEnabled={allEnabled}
            isIndeterminate={isIndeterminate}
            onSingleToggle={handleSingleToggle}
            onAllToggle={handleAllToggle}
          />
          <AccountSettings
            withdrawing={withdrawing}
            showWithdrawModal={showWithdrawModal}
            onOpenWithdrawModal={handleOpenWithdrawModal}
            onCloseWithdrawModal={handleCloseWithdrawModal}
            onConfirmWithdraw={handleConfirmWithdraw}
          />
        </div>
      </div>
    </MainLayout>
  );
}