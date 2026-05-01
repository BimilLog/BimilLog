"use client";

import { useMemo } from "react";
import { Bell, BellOff, Trash2 } from "lucide-react";
import { Button, ConfirmModal } from "@/components";
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
    isErrored,
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
    confirmDeleteIds,
    cancelDeleteAllNotifications,
    confirmDeleteAllNotifications,
  } = useNotificationBell();

  // SSE 연결 상태에 따른 벨 아이콘 결정
  const bellInfo = useMemo(() => {
    if (connectionState === "CONNECTING") {
      return {
        icon: <Bell className="w-5 h-5 text-gray-400 animate-pulse" aria-hidden="true" />,
        tooltip: "실시간 알림 연결 중...",
        className: "opacity-60",
      };
    } else if (connectionState === "DISCONNECTED" || connectionState === "CLOSED") {
      return {
        icon: <BellOff className="w-5 h-5 text-stamp-red" aria-hidden="true" />,
        tooltip: "실시간 알림 연결 실패 (클릭하여 새로고침)",
        className: "",
      };
    } else if (isSSEConnected) {
      return {
        icon: <Bell className="w-5 h-5 text-purple-500 animate-pulse" aria-hidden="true" />,
        tooltip: `실시간 알림 활성화 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`,
        className: "",
      };
    } else {
      return {
        icon: <BellOff className="w-5 h-5 text-brand-secondary" aria-hidden="true" />,
        tooltip: "실시간 알림 비활성화",
        className: "",
      };
    }
  }, [connectionState, isSSEConnected, unreadCount]);

  // F-1112: notificationListProps 객체 useMemo — memo 처리된 Popover/Drawer 가 매 렌더 리렌더 방지
  const notificationListProps = useMemo(
    () => ({
      notifications,
      unreadCount,
      isInitialLoading,
      isFetchingList,
      isErrored,
      allowBrowserPermissionPrompt,
      onRefresh: handleRefresh,
      onOpenPermissionModal: () => setShowPermissionModal(true),
      onMarkAsRead: markAsRead,
      onDelete: deleteNotification,
      onNotificationClick: handleNotificationClick,
      onMarkAllAsRead: handleMarkAllAsRead,
      onDeleteAll: handleDeleteAllNotifications,
    }),
    [
      notifications,
      unreadCount,
      isInitialLoading,
      isFetchingList,
      isErrored,
      allowBrowserPermissionPrompt,
      handleRefresh,
      setShowPermissionModal,
      markAsRead,
      deleteNotification,
      handleNotificationClick,
      handleMarkAllAsRead,
      handleDeleteAllNotifications,
    ]
  );

  if (!canUseNotifications) return null;

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
        // F-1111: WAI-ARIA Disclosure / Dialog Pattern
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        aria-controls="notification-popover"
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

      {/* F-1102: 전체 삭제 확인 모달 — destructive 액션 두 단계 확인 (Nielsen Norman) */}
      <ConfirmModal
        isOpen={(confirmDeleteIds?.length ?? 0) > 0}
        onClose={cancelDeleteAllNotifications}
        onConfirm={confirmDeleteAllNotifications}
        title="받은 편지함을 비울까요?"
        message={"삭제된 알림은 복구할 수 없어요."}
        confirmText="비우기"
        cancelText="취소"
        confirmButtonVariant="destructive"
        icon={<Trash2 className="h-8 w-8 stroke-stamp-red" aria-hidden="true" />}
      />

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
