"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { userApi, authApi, type Setting } from "@/lib/api";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import {
  Bell,
  Settings as SettingsIcon,
  MessageCircle,
  Heart,
  TrendingUp,
  AlertTriangle,
  LogOut,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { AuthHeader } from "@/components/organisms/auth-header";

export default function SettingsPage() {
  const { user, isAuthenticated } = useAuth();
  const router = useRouter();
  const [settings, setSettings] = useState<Setting | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }

    loadSettings();
  }, [isAuthenticated, router]);

  const loadSettings = async () => {
    try {
      setLoading(true);
      const response = await userApi.getUserSettings();
      if (response.success && response.data) {
        setSettings(response.data);
      }
    } catch (error) {
      console.error("설정 로드 실패:", error);
    } finally {
      setLoading(false);
    }
  };

  const updateSettings = async (newSettings: Partial<Setting>) => {
    if (!settings) return;

    try {
      setSaving(true);
      const response = await userApi.updateUserSettings(newSettings);
      if (response.success) {
        setSettings({ ...settings, ...newSettings });
      }
    } catch (error) {
      console.error("설정 저장 실패:", error);
    } finally {
      setSaving(false);
    }
  };

  const handleSingleToggle = (
    field: keyof Pick<
      Setting,
      "messageNotification" | "commentNotification" | "postFeaturedNotification"
    >,
    value: boolean
  ) => {
    updateSettings({ [field]: value });
  };

  const handleAllToggle = (enabled: boolean) => {
    updateSettings({
      messageNotification: enabled,
      commentNotification: enabled,
      postFeaturedNotification: enabled,
    });
  };

  const handleWithdraw = async () => {
    if (
      !window.confirm(
        "정말로 탈퇴하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며, 복구할 수 없습니다.\n작성한 게시글과 댓글, 롤링페이퍼 메시지가 모두 삭제됩니다.\n\n이 작업은 되돌릴 수 없습니다."
      )
    ) {
      return;
    }

    try {
      setWithdrawing(true);
      const response = await authApi.deleteAccount();
      if (response.success) {
        alert("회원탈퇴가 완료되었습니다.");
        router.push("/");
        window.location.reload();
      } else {
        alert("회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("회원탈퇴 실패:", error);
      alert("회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.");
    } finally {
      setWithdrawing(false);
    }
  };

  const allEnabled = Boolean(
    settings &&
      settings.messageNotification === true &&
      settings.commentNotification === true &&
      settings.postFeaturedNotification === true
  );

  if (!isAuthenticated) {
    return null;
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <AuthHeader />
        <div className="container mx-auto px-4 py-8">
          <div className="max-w-2xl mx-auto">
            <div className="animate-pulse space-y-4">
              <div className="h-8 bg-white/60 rounded w-1/3"></div>
              <div className="h-32 bg-white/60 rounded"></div>
              <div className="h-32 bg-white/60 rounded"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* Header */}
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
          {/* FCM 알림 설정 카드 */}
          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Bell className="w-5 h-5 text-purple-600" />
                FCM 알림 설정
              </CardTitle>
              <CardDescription>
                각 알림 유형을 개별적으로 설정할 수 있습니다.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* 전체 알림 토글 */}
              <div className="p-4 bg-gradient-to-r from-purple-50 to-indigo-50 rounded-lg border border-purple-100">
                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <Label className="font-medium text-gray-800">
                      전체 알림 설정
                    </Label>
                    <p className="text-sm text-gray-600">
                      모든 알림을 한번에 켜거나 끌 수 있습니다.
                    </p>
                  </div>
                  <Switch
                    checked={allEnabled === true}
                    onCheckedChange={(value) => handleAllToggle(value)}
                    disabled={saving}
                    className="data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-purple-500 data-[state=checked]:to-indigo-500"
                  />
                </div>
              </div>

              {/* 개별 알림 설정 */}
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-red-500 rounded-full flex items-center justify-center">
                      <Heart className="w-4 h-4 text-white" />
                    </div>
                    <div className="space-y-1">
                      <Label className="font-medium text-gray-800">
                        메시지 알림
                      </Label>
                      <p className="text-sm text-gray-600">
                        롤링페이퍼에 새로운 메시지가 도착했을 때
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={settings?.messageNotification === true}
                    onCheckedChange={(value) =>
                      handleSingleToggle("messageNotification", value)
                    }
                    disabled={saving}
                  />
                </div>

                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-gradient-to-r from-green-500 to-teal-500 rounded-full flex items-center justify-center">
                      <MessageCircle className="w-4 h-4 text-white" />
                    </div>
                    <div className="space-y-1">
                      <Label className="font-medium text-gray-800">
                        댓글 알림
                      </Label>
                      <p className="text-sm text-gray-600">
                        내 게시글에 새로운 댓글이 달렸을 때
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={settings?.commentNotification === true}
                    onCheckedChange={(value) =>
                      handleSingleToggle("commentNotification", value)
                    }
                    disabled={saving}
                  />
                </div>

                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-gradient-to-r from-orange-500 to-yellow-500 rounded-full flex items-center justify-center">
                      <TrendingUp className="w-4 h-4 text-white" />
                    </div>
                    <div className="space-y-1">
                      <Label className="font-medium text-gray-800">
                        인기글 알림
                      </Label>
                      <p className="text-sm text-gray-600">
                        내 게시글이 인기글로 선정되었을 때
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={settings?.postFeaturedNotification === true}
                    onCheckedChange={(value) =>
                      handleSingleToggle("postFeaturedNotification", value)
                    }
                    disabled={saving}
                  />
                </div>
              </div>

              {saving && (
                <div className="flex items-center justify-center py-4">
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 border-2 border-purple-500 border-t-transparent rounded-full animate-spin"></div>
                    <span className="text-sm text-gray-600">
                      설정을 저장하는 중...
                    </span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 계정 관리 카드 */}
          <Card className="border-0 shadow-lg hover:shadow-xl transition-shadow bg-white/80 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <LogOut className="w-5 h-5 text-red-600" />
                계정 관리
              </CardTitle>
              <CardDescription>
                계정과 관련된 설정을 관리할 수 있습니다.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="p-4 bg-red-50 border border-red-100 rounded-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0" />
                    <div>
                      <h3 className="font-medium text-red-800">회원 탈퇴</h3>
                      <p className="text-sm text-red-700 mt-1">
                        계정을 완전히 삭제합니다.
                      </p>
                    </div>
                  </div>
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={handleWithdraw}
                    disabled={withdrawing}
                    className="bg-red-600 hover:bg-red-700"
                  >
                    {withdrawing ? (
                      <>
                        <div className="w-3 h-3 border-2 border-white border-t-transparent rounded-full animate-spin mr-1"></div>
                        처리 중...
                      </>
                    ) : (
                      "탈퇴"
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
}
