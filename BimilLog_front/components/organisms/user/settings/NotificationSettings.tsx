import React from "react";
import { Label, Switch } from "@/components";
import { SettingsSection, SettingToggle } from "@/components/molecules";
import { Bell, Heart, Minus, MessageCircle, TrendingUp, UserPlus } from "lucide-react";
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

/**
 * 알림 설정 섹션.
 *
 * B-304 (라운드 10): isIndeterminate prop 시각/접근성 표시.
 * - aria-checked="mixed" (WAI-ARIA Tri-State Checkbox)
 * - dash 마커 + 보조 텍스트 ("일부 알림만 켜져 있어요")
 * - 일부만 켜진 상태에서 전체 토글이 OFF 처럼 보이는 오해 차단.
 */
export const NotificationSettings: React.FC<NotificationSettingsProps> = React.memo(({
  settings,
  saving,
  savingFields,
  allEnabled,
  isIndeterminate = false,
  onSingleToggle,
  onAllToggle,
  className,
}) => {
  // 켜진 알림 개수 (보조 텍스트용)
  const enabledCount = settings
    ? [
        settings.messageNotification,
        settings.commentNotification,
        settings.postFeaturedNotification,
        settings.friendSendNotification,
      ].filter(Boolean).length
    : 0;
  const totalCount = 4;

  return (
    <SettingsSection
      icon={<Bell className="w-5 h-5 text-postal-navy" aria-hidden="true" />}
      title="푸시 알림 설정"
      description="각 알림 유형을 개별적으로 설정할 수 있어요."
      className={className}
    >
      <div className="space-y-6">
        <div className="p-4 bg-postal-navy/10 dark:bg-postal-navy/20 rounded-lg border border-postal-navy/20 dark:border-postal-navy/30">
          <div className="flex items-center justify-between gap-4">
            <div className="space-y-1 flex-1 min-w-0">
              <Label
                htmlFor="all-notifications-toggle"
                className="font-medium text-ink-900 dark:text-ink-100 break-keep"
              >
                전체 알림 설정
              </Label>
              <p className="text-sm text-ink-soft dark:text-ink-300 break-keep">
                모든 알림을 한번에 켜거나 끌 수 있어요.
              </p>
              {isIndeterminate && (
                <p
                  className="text-xs text-postal-navy dark:text-ink-100 mt-1 flex items-center gap-1 break-keep"
                  role="status"
                  aria-live="polite"
                >
                  <Minus className="w-3 h-3" aria-hidden="true" />
                  일부 알림만 켜져 있어요 ({enabledCount}/{totalCount})
                </p>
              )}
            </div>
            <div className="relative shrink-0">
              <Switch
                id="all-notifications-toggle"
                checked={allEnabled === true}
                onCheckedChange={onAllToggle}
                disabled={saving}
                aria-label="전체 알림 설정"
                aria-checked={isIndeterminate ? "mixed" : allEnabled}
                data-indeterminate={isIndeterminate ? "true" : undefined}
              />
              {isIndeterminate && (
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute inset-0 flex items-center justify-center"
                >
                  <Minus className="w-3 h-3 text-postal-navy dark:text-ink-100" />
                </span>
              )}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <SettingToggle
            icon={<Heart className="w-4 h-4 stroke-red-500 fill-red-100" />}
            label="메시지 알림"
            description="롤링페이퍼에 새로운 편지가 도착했을 때"
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
});

NotificationSettings.displayName = "NotificationSettings";
