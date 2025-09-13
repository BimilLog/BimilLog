"use client";

import { Settings as SettingsIcon, AlertTriangle, RefreshCw } from "lucide-react";
import { LoadingSpinner, ToastContainer, Alert, AlertDescription, Button } from "@/components";
import { NotificationSettings, AccountSettings } from "@/components/organisms/user/settings";
import { useSettings } from "@/hooks";
import { useToast } from "@/hooks";

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
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <LoadingSpinner
            variant="gradient"
            message="설정을 불러오는 중..."
            className="py-16"
          />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <Alert className="border-red-200 bg-red-50">
            <AlertTriangle className="h-4 w-4 text-red-600" />
            <AlertDescription className="text-red-800">
              <div className="flex items-center justify-between">
                <span>{error}</span>
                <Button onClick={loadSettings} variant="outline" size="sm" className="ml-4">
                  <RefreshCw className="w-4 h-4 mr-2" />
                  다시 시도
                </Button>
              </div>
            </AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <>
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

      <div className="container mx-auto px-4 pb-8">
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
    </>
  );
}