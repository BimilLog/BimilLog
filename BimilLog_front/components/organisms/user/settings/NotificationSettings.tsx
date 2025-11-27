import React from "react";
import { Label, Switch } from "@/components";
import { SettingsSection, SettingToggle } from "@/components/molecules";
import { Bell, Heart, MessageCircle, TrendingUp, UserPlus } from "lucide-react";
import { Setting } from "@/lib/api";

type SettingField = keyof Setting;

interface NotificationSettingsProps {
  settings: Setting | null;
  saving: boolean;
  savingFields: Record<SettingField, boolean>;
  allEnabled: boolean;
  isIndeterminate?: boolean;
  onSingleToggle: (
    field: keyof Pick<Setting, "messageNotification" | "commentNotification" | "postFeaturedNotification" | "friendSendNotification">,
    value: boolean
  ) => void;
  onAllToggle: (enabled: boolean) => void;
  className?: string;
}

export const NotificationSettings: React.FC<NotificationSettingsProps> = ({
  settings,
  saving,
  savingFields,
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
            <Label className="font-medium text-brand-primary">
              전체 알림 설정
            </Label>
            <p className="text-sm text-brand-muted">모든 알림을 한번에 켜거나 끌 수 있습니다.</p>
          </div>
          <div className="relative">
            <Switch
              checked={allEnabled === true}
              onCheckedChange={onAllToggle}
              disabled={saving}
            />
          </div>
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
          saving={savingFields.messageNotification}
          gradient="from-pink-500 to-red-500"
        />

        <SettingToggle
          icon={<MessageCircle className="w-4 h-4 stroke-green-600 fill-green-100" />}
          label="댓글 알림"
          description="내 게시글에 새로운 댓글이 달렸을 때"
          checked={settings?.commentNotification === true}
          onChange={(value) => onSingleToggle("commentNotification", value)}
          disabled={saving}
          saving={savingFields.commentNotification}
          gradient="from-green-500 to-teal-500"
        />

        <SettingToggle
          icon={<TrendingUp className="w-4 h-4 stroke-green-600 fill-green-100" />}
          label="인기글 알림"
          description="내 게시글이 인기글이 되었을 때"
          checked={settings?.postFeaturedNotification === true}
          onChange={(value) => onSingleToggle("postFeaturedNotification", value)}
          disabled={saving}
          saving={savingFields.postFeaturedNotification}
          gradient="from-orange-500 to-yellow-500"
        />

        <SettingToggle
          icon={<UserPlus className="w-4 h-4 stroke-blue-500 fill-blue-100" />}
          label="친구 요청 알림"
          description="새로운 친구 요청이 왔을 때"
          checked={settings?.friendSendNotification === true}
          onChange={(value) => onSingleToggle("friendSendNotification", value)}
          disabled={saving}
          saving={savingFields.friendSendNotification}
          gradient="from-blue-500 to-cyan-500"
        />
      </div>
    </div>
  </SettingsSection>
);
