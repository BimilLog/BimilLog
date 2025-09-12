"use client";

import { Settings as SettingsIcon } from "lucide-react";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import { LoadingSpinner } from "@/components/atoms";
import { ToastContainer } from "@/components/molecules/toast";
import { NotificationSettings, AccountSettings } from "@/components/organisms/settings";
import { useSettings } from "@/hooks/useSettings";
import { useToast } from "@/hooks/useToast";

export default function SettingsPage() {
  const {
    settings,
    loading,
    saving,
    withdrawing,
    allEnabled,
    isAuthenticated,
    isLoading,
    handleSingleToggle,
    handleAllToggle,
    handleWithdraw,
  } = useSettings();

  const { toasts, removeToast } = useToast();

  if (isLoading || !isAuthenticated) {
    return null;
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        <div className="container mx-auto px-4 py-8">
          <div className="max-w-2xl mx-auto">
            <LoadingSpinner
              variant="gradient"
              message="설정을 불러오는 중..."
              className="py-16"
            />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      <header className="py-6">
        <div className="container mx-auto px-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-r from-purple-500 to-indigo-500 rounded-full flex items-center justify-center">
              <SettingsIcon className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-800">설정</h1>
              <p className="text-gray-600">알림 설정과 계정을 관리하세요</p>
            </div>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 pb-8">
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
      </main>

      <HomeFooter />
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </div>
  );
}