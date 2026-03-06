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
} from "lucide-react";
import { Button, TimeBadge } from "@/components";
import type { Notification } from "@/types/domains/notification";

interface NotificationItemProps {
  notification: Notification;
  onMarkAsRead: (id: number) => void;
  onDelete: (id: number) => void;
  onClick: (notification: Notification) => void;
}

/** 알림 유형별 아이콘 반환 */
const getNotificationIcon = (notificationType: string) => {
  switch (notificationType) {
    case "PAPER":
      return <Leaf className="w-4 h-4 text-green-600" />;
    case "COMMENT":
      return <MessageSquare className="w-4 h-4 text-blue-600" />;
    case "POST_FEATURED":
      return <Star className="w-4 h-4 text-yellow-600" />;
    case "ADMIN":
      return <Shield className="w-4 h-4 text-purple-600" />;
    case "INITIATE":
      return <Bell className="w-4 h-4 text-brand-primary" />;
    default:
      return <Bell className="w-4 h-4 text-brand-primary" />;
  }
};

/** 개별 알림 아이템 렌더링 컴포넌트 */
export const NotificationItem = memo(function NotificationItem({
  notification,
  onMarkAsRead,
  onDelete,
  onClick,
}: NotificationItemProps) {
  return (
    <div
      className={`p-4 hover:bg-gray-50 transition-colors cursor-pointer ${
        !notification.read
          ? "bg-blue-50/50 border-l-2 border-l-blue-500"
          : ""
      }`}
      onClick={() => onClick(notification)}
    >
      <div className="flex items-start space-x-3">
        <div className="flex-shrink-0 mt-0.5">
          {getNotificationIcon(notification.notificationType)}
        </div>
        <div className="flex-1 min-w-0">
          <p
            className={`text-sm ${
              !notification.read
                ? "font-medium text-brand-primary"
                : "text-brand-muted"
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
                  className="h-6 px-2 text-xs text-blue-600 hover:text-blue-700"
                  title="읽음 처리"
                >
                  <Eye className="w-3 h-3" />
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(notification.id);
                }}
                className="h-6 px-2 text-xs text-red-600 hover:text-red-700"
                title="삭제"
              >
                <Trash2 className="w-3 h-3" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
});

NotificationItem.displayName = "NotificationItem";
