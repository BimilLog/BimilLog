"use client";

import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import {
  Bell,
  BellOff,
  RefreshCw,
  Trash2,
  CheckCircle2,
  Eye,
  Leaf,
  MessageSquare,
  Star,
  Shield,
} from "lucide-react";
import { Button } from "@/components";
import { Card } from "@/components";
import { TimeBadge } from "@/components";
import {
  useNotifications,
  useNotificationList,
  useNotificationSync,
  useMarkNotificationAsRead,
  useDeleteNotification,
  useMarkAllNotificationsAsRead,
  useDeleteAllNotifications
} from "@/hooks/features";
import { useAuth } from "@/hooks";
import { Spinner as FlowbiteSpinner, Badge, Drawer } from "flowbite-react";

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [portalContainer, setPortalContainer] = useState<HTMLElement | null>(null);
  const { isAuthenticated } = useAuth();

  const { data: notificationResponse, isLoading, refetch } = useNotificationList();
  const markAsReadMutation = useMarkNotificationAsRead();
  const deleteNotificationMutation = useDeleteNotification();
  const markAllAsReadMutation = useMarkAllNotificationsAsRead();
  const deleteAllNotificationsMutation = useDeleteAllNotifications();
  const { syncNow } = useNotificationSync();

  const {
    isSSEConnected,
    canConnectSSE,
    requestNotificationPermission,
  } = useNotifications();

  useEffect(() => {
    const checkIsMobile = () => {
      const isSmallScreen = window.innerWidth < 768;
      setIsMobile(isSmallScreen);
    };

    checkIsMobile();
    window.addEventListener('resize', checkIsMobile);
    return () => window.removeEventListener('resize', checkIsMobile);
  }, []);

  useEffect(() => {
    setPortalContainer(document.body);
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (!target.closest('.notification-popover') && !target.closest('.notification-button')) {
        setIsOpen(false);
      }
    };

    if (isOpen && !isMobile) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }
  }, [isOpen, isMobile]);

  const notifications = notificationResponse?.success ? (notificationResponse.data || []) : [];
  const unreadCount = notifications.filter(n => !n.read).length;

  const handleMarkAsRead = (notificationId: number) => {
    markAsReadMutation.mutate(notificationId);
  };

  const handleDeleteNotification = (notificationId: number) => {
    deleteNotificationMutation.mutate(notificationId);
  };

  const handleMarkAllAsRead = (e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const unreadIds = notifications
      .filter((notification) => !notification.read)
      .map((notification) => notification.id);

    if (unreadIds.length === 0) return;

    markAllAsReadMutation.mutate(unreadIds);
  };

  const handleDeleteAllNotifications = (e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }

    const deleteIds = notifications.map((notification) => notification.id);
    if (deleteIds.length === 0) return;

    deleteAllNotificationsMutation.mutate(deleteIds);
  };

  const handleFetchNotifications = () => {
    refetch();
  };

  if (!isAuthenticated || !canConnectSSE()) return null;

  const handleOpen = (open: boolean) => {
    setIsOpen(open);
    if (open && canConnectSSE()) {
      syncNow();
      handleFetchNotifications();
    } else if (!open) {
      // 알림 패널 닫을 때 즉시 동기화
      syncNow();
    }
  };

  const handleRefresh = () => {
    if (canConnectSSE()) {
      syncNow();
      handleFetchNotifications();
    }
  };

  const handleNotificationClick = async (notification: { id: number; read: boolean; url?: string }) => {
    if (!notification.read) {
      handleMarkAsRead(notification.id);
    }
    if (notification.url) {
      window.location.href = notification.url;
    }
  };

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


  const NotificationContent = () => (
    <div className="w-full h-full flex flex-col">
      <div className="flex items-center justify-between p-4 border-b bg-white/50 flex-shrink-0">
        <div className="flex items-center gap-2">
          <Bell className="w-5 h-5 text-brand-primary" />
          <h2 className="text-lg font-semibold text-brand-primary">
            알림 {unreadCount > 0 && `(${unreadCount})`}
          </h2>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRefresh}
            disabled={isLoading}
            className="text-sm min-h-[44px] min-w-[44px] touch-manipulation"
            title="알림 목록 새로고침"
          >
            {isLoading ? (
              <FlowbiteSpinner color="pink" size="sm" aria-label="새로고침 중..." />
            ) : (
              <RefreshCw className="w-4 h-4" />
            )}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={requestNotificationPermission}
            className="text-sm min-h-[44px] min-w-[44px] touch-manipulation"
            title="브라우저 알림 허용"
          >
            <Bell className="w-4 h-4" />
          </Button>
        </div>
      </div>
      {notifications.length > 0 && (
        <div className="p-4 bg-gray-50/80 border-b flex-shrink-0">
          <div className="flex gap-2">
            {unreadCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={(e) => handleMarkAllAsRead(e)}
                className="flex-1 text-sm min-h-[44px] touch-manipulation"
                title="모든 알림을 읽음으로 처리"
              >
                <CheckCircle2 className="w-4 h-4 mr-2" />
                모두 읽음
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={(e) => handleDeleteAllNotifications(e)}
              className="flex-1 text-sm min-h-[44px] text-red-600 hover:text-red-700 border-red-200 hover:border-red-300 touch-manipulation"
              title="모든 알림 삭제"
            >
              <Trash2 className="w-4 h-4 mr-2" />
              전체 삭제
            </Button>
          </div>
        </div>
      )}

      <div className="flex-1 overflow-y-auto">
        {isLoading ? (
          <div className="p-8 flex flex-col items-center">
            <FlowbiteSpinner color="pink" size="xl" aria-label="알림을 불러오는 중..." />
            <p className="mt-2 text-sm text-brand-secondary">알림을 불러오는 중...</p>
          </div>
        ) : notifications.length > 0 ? (
          <div className="divide-y divide-gray-100">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`p-4 hover:bg-gray-50 transition-colors cursor-pointer ${
                  !notification.read
                    ? "bg-blue-50/50 border-l-2 border-l-blue-500"
                    : ""
                }`}
                onClick={() => handleNotificationClick(notification)}
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
                              handleMarkAsRead(notification.id);
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
                            handleDeleteNotification(notification.id);
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
            ))}
          </div>
        ) : (
          <div className="p-8 text-center">
            <BellOff className="w-12 h-12 mx-auto mb-3 text-brand-muted" />
            <p className="text-sm text-brand-secondary mb-2">알림이 없습니다</p>
            <p className="text-xs text-brand-secondary">
              새로운 알림이 오면 여기에 표시됩니다
            </p>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <div className="relative">
      {isMobile ? (
        <>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleOpen(!isOpen)}
            className="flex items-center gap-1 min-h-[44px] px-2 touch-manipulation"
            title={`알림 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`}
          >
            {isSSEConnected ? (
              <Bell className="w-5 h-5 text-brand-primary" />
            ) : (
              <BellOff className="w-5 h-5 text-brand-secondary" />
            )}
            {unreadCount > 0 && (
              <Badge color="failure" size="xs" className="px-1.5">
                {unreadCount > 99 ? "99+" : unreadCount}
              </Badge>
            )}
          </Button>
          {isOpen && portalContainer &&
            createPortal(
              <Drawer
                open={true}
                onClose={() => handleOpen(false)}
                position="bottom"
                className="rounded-t-xl !z-[60]"
              >
                <div className="h-[80vh] max-h-[80vh] flex flex-col bg-white rounded-t-xl overflow-hidden">
                  <NotificationContent />
                </div>
              </Drawer>,
              portalContainer
            )}
        </>
      ) : (
        <>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleOpen(!isOpen)}
            className="flex items-center gap-1 min-h-[44px] px-2 touch-manipulation notification-button"
            title={`알림 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`}
          >
            {isSSEConnected ? (
              <Bell className="w-5 h-5 text-purple-500" />
            ) : (
              <BellOff className="w-5 h-5 text-brand-secondary" />
            )}
            {unreadCount > 0 && (
              <Badge color="failure" size="xs" className="px-1.5">
                {unreadCount > 99 ? "99+" : unreadCount}
              </Badge>
            )}
          </Button>

          {isOpen && (
            <div className="absolute right-0 top-full mt-2 z-50 notification-popover">
              <Card className="w-80 shadow-brand-xl border-0 bg-white/90 backdrop-blur-sm">
                <NotificationContent />
              </Card>
            </div>
          )}
        </>
      )}
    </div>
  );
}
