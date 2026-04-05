"use client";

import { useMemo } from "react";
import { Bell, BellOff } from "lucide-react";
import { Button } from "@/components";
import { Badge } from "flowbite-react";
import { NotificationPermissionModal } from "@/components/organisms/notification";
import { NotificationPopover } from "./NotificationPopover";
import { NotificationDrawer } from "./NotificationDrawer";
import { useNotificationBell } from "@/hooks/features";

export function NotificationBell() {
  const {
    isOpen,
    isMobile,
    portalContainer,
    showPermissionModal,
    setShowPermissionModal,
    popoverPosition,
    triggerRef,
    notifications,
    unreadCount,
    isFetchingList,
    isInitialLoading,
    isSSEConnected,
    connectionState,
    canUseNotifications,
    allowBrowserPermissionPrompt,
    isAuthenticated,
    markAsRead,
    deleteNotification,
    registerFcmToken,
    handleOpen,
    handleRefresh,
    handleNotificationClick,
    handleMarkAllAsRead,
    handleDeleteAllNotifications,
  } = useNotificationBell();

  // SSE 연결 상태에 따른 벨 아이콘 결정
  const bellInfo = useMemo(() => {
    if (connectionState === "CONNECTING") {
      return {
        icon: <Bell className="w-5 h-5 text-gray-400 animate-pulse" />,
        tooltip: "실시간 알림 연결 중...",
        className: "opacity-60",
      };
    } else if (connectionState === "DISCONNECTED" || connectionState === "CLOSED") {
      return {
        icon: <BellOff className="w-5 h-5 text-red-500" />,
        tooltip: "실시간 알림 연결 실패 (클릭하여 새로고침)",
        className: "",
      };
    } else if (isSSEConnected) {
      return {
        icon: <Bell className="w-5 h-5 text-purple-500 animate-pulse" />,
        tooltip: `실시간 알림 활성화 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`,
        className: "",
      };
    } else {
      return {
        icon: <BellOff className="w-5 h-5 text-brand-secondary" />,
        tooltip: "실시간 알림 비활성화",
        className: "",
      };
    }
  }, [connectionState, isSSEConnected, unreadCount]);

  if (!canUseNotifications) return null;

  const notificationListProps = {
    notifications,
    unreadCount,
    isInitialLoading,
    isFetchingList,
    allowBrowserPermissionPrompt,
    onRefresh: handleRefresh,
    onOpenPermissionModal: () => setShowPermissionModal(true),
    onMarkAsRead: markAsRead,
    onDelete: deleteNotification,
    onNotificationClick: handleNotificationClick,
    onMarkAllAsRead: handleMarkAllAsRead,
    onDeleteAll: handleDeleteAllNotifications,
  } as const;

  return (
    <div className="relative" ref={triggerRef}>
      {/* 벨 아이콘 버튼 (모바일/데스크톱 공통) */}
      <Button
        variant="ghost"
        size="sm"
        onClick={() => handleOpen(!isOpen)}
        className={`flex items-center gap-1 min-h-[44px] px-2 touch-manipulation ${
          !isMobile ? "notification-button " : ""
        }${bellInfo.className}`}
        aria-label="알림"
        title={bellInfo.tooltip}
      >
        {bellInfo.icon}
        {unreadCount > 0 && (
          <Badge color="failure" size="xs" className="px-1.5">
            {unreadCount > 99 ? "99+" : unreadCount}
          </Badge>
        )}
      </Button>

      {/* 모바일: 하단 드로어 */}
      {isMobile && (
        <NotificationDrawer
          isOpen={isOpen}
          portalContainer={portalContainer}
          onClose={() => handleOpen(false)}
          {...notificationListProps}
        />
      )}

      {/* 데스크톱: 팝오버 */}
      {!isMobile && (
        <NotificationPopover
          isOpen={isOpen}
          portalContainer={portalContainer}
          popoverPosition={popoverPosition}
          {...notificationListProps}
        />
      )}

      {/* 알림 권한 요청 모달 */}
      {allowBrowserPermissionPrompt && (
        <NotificationPermissionModal
          show={showPermissionModal}
          onClose={() => setShowPermissionModal(false)}
          onSuccess={(token) => {
            localStorage.setItem("fcm_token", token);
            localStorage.removeItem("notification_permission_skipped");
            if (isAuthenticated) {
              registerFcmToken(token, {
                onError: (error) => {
                  console.warn("FCM 토큰 서버 등록 실패:", error);
                },
              });
            }
            setShowPermissionModal(false);
          }}
          onSkip={() => {
            const skipUntil = Date.now() + 7 * 24 * 60 * 60 * 1000;
            localStorage.setItem("notification_permission_skipped", skipUntil.toString());
            setShowPermissionModal(false);
          }}
        />
      )}
    </div>
  );
}
