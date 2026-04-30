"use client";

import { memo } from "react";
import {
  Bell,
  Leaf,
  MessageSquare,
  Star,
  Shield,
  Eye,
  Trash2,
  UserPlus,
} from "lucide-react";
import { Button, TimeBadge } from "@/components";
import type { Notification } from "@/types/domains/notification";

interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: (id: number) => void;
  onDelete: (id: number) => void;
  onClick: (notification: Notification) => void;
}

/** 알림 유형별 아이콘 반환 — F-1103: FRIEND 케이스 추가 (NotificationSettings 와 동일 UserPlus). */
const getNotificationIcon = (notificationType: string) => {
  switch (notificationType) {
    case "PAPER":
      return <Leaf className="w-4 h-4 text-green-600" />;
    case "COMMENT":
      return <MessageSquare className="w-4 h-4 text-blue-600" />;
    case "POST_FEATURED":
      return <Star className="w-4 h-4 text-yellow-600" />;
    case "FRIEND":
      return <UserPlus className="w-4 h-4 text-blue-500" />;
    case "ADMIN":
      return <Shield className="w-4 h-4 text-purple-600" />;
    case "INITIATE":
      return <Bell className="w-4 h-4 text-brand-primary" />;
    default:
      return <Bell className="w-4 h-4 text-brand-primary" />;
  }
};

/** ARIA-label 용 type 한국어 라벨 */
const getNotificationTypeLabel = (notificationType: string): string => {
  switch (notificationType) {
    case "PAPER":
      return "편지 알림";
    case "COMMENT":
      return "댓글 알림";
    case "POST_FEATURED":
      return "인기글 알림";
    case "FRIEND":
      return "친구 알림";
    case "ADMIN":
      return "관리자 안내";
    case "INITIATE":
      return "환영 알림";
    default:
      return "알림";
  }
};

/** 개별 알림 아이템 렌더링 컴포넌트 */
export const NotificationItem = memo(function NotificationItem({
  notification,
  onMarkAsRead,
  onDelete,
  onClick,
}: NotificationItemProps) {
  const typeLabel = getNotificationTypeLabel(notification.notificationType);
  return (
    <div
      // 다크 토큰 회귀: bg-gray-50 → paper-soft, bg-blue-50 → postal-navy 톤.
      // unread 좌측 보더는 stamp-red 로 통일 (라운드 5~10 패턴).
      className={`p-4 hover:bg-paper-soft/60 dark:hover:bg-paper-card/40 transition-colors cursor-pointer ${
        !notification.read
          ? "bg-postal-navy/5 dark:bg-postal-navy/20 border-l-2 border-l-stamp-red"
          : ""
      }`}
      onClick={() => onClick(notification)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick(notification);
        }
      }}
      aria-label={`${typeLabel} - ${notification.read ? "읽음" : "읽지 않음"}`}
    >
      <div className="flex items-start space-x-3">
        <div className="flex-shrink-0 mt-0.5">
          {getNotificationIcon(notification.notificationType)}
        </div>
        <div className="flex-1 min-w-0">
          {/* F-1109: 긴 알림 content — line-clamp-2 + break-words + 한국어 break-keep */}
          <p
            className={`text-sm line-clamp-2 break-words break-keep leading-snug ${
              !notification.read
                ? "font-medium text-ink dark:text-foreground"
                : "text-ink-soft dark:text-foreground/70"
            }`}
          >
            {notification.content}
          </p>
          <div className="flex items-center justify-between mt-1">
            <TimeBadge dateString={notification.createdAt} size="xs" />
            <div className="flex items-center space-x-1">
              {!notification.read && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    onMarkAsRead(notification.id);
                  }}
                  // F-1108: 모바일 터치 타겟 44×44px (WCAG 2.5.5)
                  className="min-h-[44px] min-w-[44px] px-2 text-xs text-blue-600 hover:text-blue-700 touch-manipulation"
                  aria-label="읽음 처리"
                  title="읽음 처리"
                >
                  <Eye className="w-4 h-4" aria-hidden="true" />
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(notification.id);
                }}
                // F-1108: 모바일 터치 타겟 44×44px
                className="min-h-[44px] min-w-[44px] px-2 text-xs text-stamp-red hover:opacity-80 touch-manipulation"
                aria-label="알림 삭제"
                title="삭제"
              >
                <Trash2 className="w-4 h-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
});

NotificationItem.displayName = "NotificationItem";
