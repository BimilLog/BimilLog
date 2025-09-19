import React from "react";
import { ToggleSwitch, Spinner as FlowbiteSpinner } from "flowbite-react";
import { Label } from "@/components";
import { SettingsSection, SettingToggle } from "@/components/molecules";
import { Bell, Heart, MessageCircle, TrendingUp } from "lucide-react";
import { Setting } from "@/lib/api";

interface NotificationSettingsProps {
  settings: Setting | null;
  saving: boolean;
  allEnabled: boolean;
  onSingleToggle: (
    field: keyof Pick<Setting, "messageNotification" | "commentNotification" | "postFeaturedNotification">,
    value: boolean
  ) => void;
  onAllToggle: (enabled: boolean) => void;
  className?: string;
}

export const NotificationSettings: React.FC<NotificationSettingsProps> = ({
  settings,
  saving,
  allEnabled,
  onSingleToggle,
  onAllToggle,
  className,
}) => (
  <SettingsSection
    icon={<Bell className="w-5 h-5 stroke-purple-500 fill-purple-100" />}
    title="푸시 알림 설정"
    description="각 알림 유형을 개별적으로 설정할 수 있습니다."
    className={className}
  >
    <div className="space-y-6">
      <div className="p-4 bg-gradient-to-r from-purple-50 to-indigo-50 rounded-lg border border-purple-100">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <Label className="font-medium text-brand-primary">전체 알림 설정</Label>
            <p className="text-sm text-brand-muted">모든 알림을 한번에 켜거나 끌 수 있습니다.</p>
          </div>
          <ToggleSwitch
            checked={allEnabled === true}
            onChange={onAllToggle}
            disabled={saving}
          />
        </div>
      </div>

      <div className="space-y-4">
        <SettingToggle
          icon={<Heart className="w-4 h-4 stroke-red-500 fill-red-100" />}
          label="메시지 알림"
          description="롤링페이퍼에 새로운 메시지가 도착했을 때"
          checked={settings?.messageNotification === true}
          onChange={(value) => onSingleToggle("messageNotification", value)}
          disabled={saving}
          gradient="from-pink-500 to-red-500"
        />

        <SettingToggle
          icon={<MessageCircle className="w-4 h-4 stroke-green-600 fill-green-100" />}
          label="댓글 알림"
          description="내 게시글에 새로운 댓글이 달렸을 때"
          checked={settings?.commentNotification === true}
          onChange={(value) => onSingleToggle("commentNotification", value)}
          disabled={saving}
          gradient="from-green-500 to-teal-500"
        />

        <SettingToggle
          icon={<TrendingUp className="w-4 h-4 stroke-green-600 fill-green-100" />}
          label="인기글 알림"
          description="내 게시글이 인기글로 선정되었을 때"
          checked={settings?.postFeaturedNotification === true}
          onChange={(value) => onSingleToggle("postFeaturedNotification", value)}
          disabled={saving}
          gradient="from-orange-500 to-yellow-500"
        />
      </div>

      {saving && (
        <div className="flex items-center justify-center py-4">
          <div className="flex items-center gap-2">
            <FlowbiteSpinner color="pink" size="sm" aria-label="설정을 저장하는 중..." />
            <span className="text-sm text-brand-muted">설정을 저장하는 중...</span>
          </div>
        </div>
      )}
    </div>
  </SettingsSection>
);