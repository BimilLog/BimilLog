"use client";

import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { Bell, BellOff } from "lucide-react";
import { Button } from "@/components";
import { useNotifications } from "@/hooks/features";
import { useNotificationList } from "@/hooks/api";
import {
  useMarkNotificationAsReadAction,
  useDeleteNotificationAction,
  useMarkAllNotificationsAsReadAction,
  useDeleteAllNotificationsAction,
  useRegisterFcmTokenAction,
} from "@/hooks/actions";
import { useAuth } from "@/hooks";
import { isKakaoInAppBrowser } from "@/lib/utils";
import { Badge } from "flowbite-react";
import { NotificationPermissionModal } from "@/components/organisms/notification";
import { usePathname } from "next/navigation";
import { NotificationPopover } from "./NotificationPopover";
import { NotificationDrawer } from "./NotificationDrawer";

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [portalContainer, setPortalContainer] = useState<HTMLElement | null>(null);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [popoverPosition, setPopoverPosition] = useState({ top: 0, left: 0 });
  const pathname = usePathname();
  const triggerRef = useRef<HTMLDivElement | null>(null);
  const { isAuthenticated } = useAuth();
  const allowBrowserPermissionPrompt = !isKakaoInAppBrowser();

  const {
    isSSEConnected,
    connectionState,
    canConnectSSE,
  } = useNotifications();
  const canUseNotifications = isAuthenticated && canConnectSSE();

  const {
    data: notificationResponse,
    status,
    isFetching,
    isRefetching,
    refetch,
  } = useNotificationList({ enabled: canUseNotifications });
  const { markAsRead } = useMarkNotificationAsReadAction();
  const { deleteNotification } = useDeleteNotificationAction();
  const { markAllAsRead } = useMarkAllNotificationsAsReadAction();
  const { deleteAllNotifications } = useDeleteAllNotificationsAction();
  const { registerFcmToken } = useRegisterFcmTokenAction();

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
    if (canUseNotifications) {
      refetch();
    }
  }, [canUseNotifications, refetch, pathname]);

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

  const updateDesktopPopoverPosition = useCallback(() => {
    if (typeof window === "undefined") return;
    const triggerEl = triggerRef.current;
    if (!triggerEl) return;
    const rect = triggerEl.getBoundingClientRect();
    setPopoverPosition({
      top: rect.bottom + 12,
      left: rect.right,
    });
  }, []);

  useEffect(() => {
    if (!isOpen || isMobile) return;
    updateDesktopPopoverPosition();
    const handleWindowChange = () => updateDesktopPopoverPosition();
    window.addEventListener("resize", handleWindowChange);
    window.addEventListener("scroll", handleWindowChange, true);
    return () => {
      window.removeEventListener("resize", handleWindowChange);
      window.removeEventListener("scroll", handleWindowChange, true);
    };
  }, [isOpen, isMobile, updateDesktopPopoverPosition]);

  const notifications = useMemo(() => notificationResponse?.success ? (notificationResponse.data || []) : [], [notificationResponse]);
  const isFetchingList = isFetching || isRefetching;
  const isInitialLoading = status === "pending" && isFetchingList;
  const unreadCount = notifications.filter(n => !n.read).length;

  const handleMarkAllAsRead = useCallback((e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    const unreadIds = notifications
      .filter((notification) => !notification.read)
      .map((notification) => notification.id);

    if (unreadIds.length === 0) return;

    markAllAsRead(unreadIds);
  }, [notifications, markAllAsRead]);

  const handleDeleteAllNotifications = useCallback((e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }

    const deleteIds = notifications.map((notification) => notification.id);
    if (deleteIds.length === 0) return;

    deleteAllNotifications(deleteIds);
  }, [notifications, deleteAllNotifications]);

  if (!canUseNotifications) return null;

  const handleOpen = (open: boolean) => {
    setIsOpen(open);
    if (open && canUseNotifications) {
      if (!isMobile) {
        updateDesktopPopoverPosition();
      }
      refetch();
    }
  };

  const handleRefresh = () => {
    if (canUseNotifications) {
      refetch();
    }
  };

  const handleNotificationClick = (notification: { id: number; read: boolean; url?: string }) => {
    if (!notification.read) {
      markAsRead(notification.id);
    }
    if (notification.url) {
      window.location.href = notification.url;
    }
  };

  // SSE 연결 상태에 따른 벨 아이콘 및 클래스 결정
  const getBellIconAndClass = () => {
    if (connectionState === "CONNECTING") {
      return {
        icon: <Bell className="w-5 h-5 text-gray-400 animate-pulse" />,
        tooltip: "실시간 알림 연결 중...",
        className: "opacity-60"
      }
    } else if (connectionState === "DISCONNECTED" || connectionState === "CLOSED") {
      return {
        icon: <BellOff className="w-5 h-5 text-red-500" />,
        tooltip: "실시간 알림 연결 실패 (클릭하여 새로고침)",
        className: ""
      }
    } else if (isSSEConnected) {
      return {
        icon: <Bell className="w-5 h-5 text-purple-500 animate-pulse" />,
        tooltip: `실시간 알림 활성화 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`,
        className: ""
      }
    } else {
      return {
        icon: <BellOff className="w-5 h-5 text-brand-secondary" />,
        tooltip: "실시간 알림 비활성화",
        className: ""
      }
    }
  }

  const bellInfo = getBellIconAndClass();

  /** 공통 알림 목록 props */
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
