"use client";

import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useNotifications } from "@/hooks/features/useNotifications";
import { useNotificationList } from "@/hooks/api";
import {
  useMarkNotificationAsReadAction,
  useDeleteNotificationAction,
  useMarkAllNotificationsAsReadAction,
  useDeleteAllNotificationsAction,
  useRegisterFcmTokenAction,
} from "@/hooks/actions";
import { useAuth } from "@/hooks/common/useAuth";
import { useMediaQuery } from "@/hooks/common/useMediaQuery";
import { isKakaoInAppBrowser } from "@/lib/utils";
import { usePathname } from "next/navigation";
import { sseManager } from "@/lib/api";

export function useNotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [portalContainer, setPortalContainer] = useState<HTMLElement | null>(null);
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [popoverPosition, setPopoverPosition] = useState({ top: 0, left: 0 });
  const triggerRef = useRef<HTMLDivElement | null>(null);
  const pathname = usePathname();

  const isMobile = useMediaQuery("(max-width: 767px)");
  const { isAuthenticated } = useAuth();
  const allowBrowserPermissionPrompt = !isKakaoInAppBrowser();

  const { isSSEConnected, connectionState, canConnectSSE } = useNotifications();
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
      if (!target.closest(".notification-popover") && !target.closest(".notification-button")) {
        setIsOpen(false);
      }
    };

    // B-308: 키보드 사용자가 ESC 로 popover 를 닫을 수 있도록 (WCAG 2.1.2)
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    };

    if (isOpen && !isMobile) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("keydown", handleKeyDown);
      return () => {
        document.removeEventListener("mousedown", handleClickOutside);
        document.removeEventListener("keydown", handleKeyDown);
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

  const notifications = useMemo(
    () => (notificationResponse?.success ? notificationResponse.data || [] : []),
    [notificationResponse]
  );
  const isFetchingList = isFetching || isRefetching;
  const isInitialLoading = status === "pending" && isFetchingList;
  const unreadCount = useMemo(() => notifications.filter((n) => !n.read).length, [notifications]);

  const handleMarkAllAsRead = useCallback(
    (e?: React.MouseEvent) => {
      if (e) {
        e.preventDefault();
        e.stopPropagation();
      }
      const unreadIds = notifications.filter((n) => !n.read).map((n) => n.id);
      if (unreadIds.length === 0) return;
      markAllAsRead(unreadIds);
    },
    [notifications, markAllAsRead]
  );

  const handleDeleteAllNotifications = useCallback(
    (e?: React.MouseEvent) => {
      if (e) {
        e.preventDefault();
        e.stopPropagation();
      }
      const deleteIds = notifications.map((n) => n.id);
      if (deleteIds.length === 0) return;
      deleteAllNotifications(deleteIds);
    },
    [notifications, deleteAllNotifications]
  );

  // B-305: SSE 가 끊긴 상태에서 클릭 시 재연결 시도. 카피("클릭하여 새로고침")와 실제 동작 일치.
  const tryReconnectSSE = useCallback(() => {
    if (!canUseNotifications) return;
    if (connectionState === "CLOSED" || connectionState === "DISCONNECTED") {
      sseManager.connect();
    }
  }, [canUseNotifications, connectionState]);

  const handleOpen = useCallback(
    (open: boolean) => {
      setIsOpen(open);
      if (open && canUseNotifications) {
        if (!isMobile) {
          updateDesktopPopoverPosition();
        }
        // B-305: 끊긴 SSE 라면 재연결 시도
        tryReconnectSSE();
        refetch();
      }
    },
    [canUseNotifications, isMobile, updateDesktopPopoverPosition, refetch, tryReconnectSSE]
  );

  const handleRefresh = useCallback(() => {
    if (canUseNotifications) {
      // B-305: 새로고침 버튼은 명시적 재연결 액션으로도 동작
      tryReconnectSSE();
      refetch();
    }
  }, [canUseNotifications, refetch, tryReconnectSSE]);

  const handleNotificationClick = useCallback(
    (notification: { id: number; read: boolean; url?: string }) => {
      if (!notification.read) {
        markAsRead(notification.id);
      }
      if (notification.url) {
        window.location.href = notification.url;
      }
    },
    [markAsRead]
  );

  return {
    // 상태
    isOpen,
    isMobile,
    portalContainer,
    showPermissionModal,
    setShowPermissionModal,
    popoverPosition,
    triggerRef,
    // 알림 데이터
    notifications,
    unreadCount,
    isFetchingList,
    isInitialLoading,
    // SSE 연결 상태
    isSSEConnected,
    connectionState,
    // 권한
    canUseNotifications,
    allowBrowserPermissionPrompt,
    isAuthenticated,
    // 액션
    markAsRead,
    deleteNotification,
    registerFcmToken,
    // 이벤트 핸들러
    handleOpen,
    handleRefresh,
    handleNotificationClick,
    handleMarkAllAsRead,
    handleDeleteAllNotifications,
  };
}
