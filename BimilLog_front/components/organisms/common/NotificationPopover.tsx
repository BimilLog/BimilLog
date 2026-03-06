"use client";

import { memo } from "react";
import { createPortal } from "react-dom";
import { Card } from "@/components";
import { NotificationList } from "./NotificationList";
import type { Notification } from "@/types/domains/notification";

interface NotificationPopoverProps {
  /** 팝오버 표시 여부 */
  isOpen: boolean;
  /** 포탈 컨테이너 (document.body) */
  portalContainer: HTMLElement | null;
  /** 팝오버 위치 */
  popoverPosition: { top: number; left: number };
  /** 알림 목록 */
  notifications: Notification[];
  /** 읽지 않은 알림 수 */
  unreadCount: number;
  /** 초기 로딩 여부 */
  isInitialLoading: boolean;
  /** 패칭 중 여부 */
  isFetchingList: boolean;
  /** 브라우저 알림 허용 프롬프트 표시 가능 여부 */
  allowBrowserPermissionPrompt: boolean;
  /** 새로고침 핸들러 */
  onRefresh: () => void;
  /** 알림 권한 모달 열기 */
  onOpenPermissionModal: () => void;
  /** 개별 알림 읽음 처리 */
  onMarkAsRead: (id: number) => void;
  /** 개별 알림 삭제 */
  onDelete: (id: number) => void;
  /** 알림 클릭 */
  onNotificationClick: (notification: Notification) => void;
  /** 모두 읽음 */
  onMarkAllAsRead: (e?: React.MouseEvent) => void;
  /** 전체 삭제 */
  onDeleteAll: (e?: React.MouseEvent) => void;
}

/** 데스크톱 팝오버 UI */
export const NotificationPopover = memo(function NotificationPopover({
  isOpen,
  portalContainer,
  popoverPosition,
  notifications,
  unreadCount,
  isInitialLoading,
  isFetchingList,
  allowBrowserPermissionPrompt,
  onRefresh,
  onOpenPermissionModal,
  onMarkAsRead,
  onDelete,
  onNotificationClick,
  onMarkAllAsRead,
  onDeleteAll,
}: NotificationPopoverProps) {
  if (!isOpen || !portalContainer) return null;

  return createPortal(
    <div
      className="notification-popover z-[70]"
      style={{
        position: "fixed",
        top: popoverPosition.top,
        left: popoverPosition.left,
        transform: "translateX(-100%)",
      }}
    >
      <Card className="w-80 shadow-brand-xl border-0 bg-white/90 backdrop-blur-sm">
        <NotificationList
          notifications={notifications}
          unreadCount={unreadCount}
          isInitialLoading={isInitialLoading}
          isFetchingList={isFetchingList}
          allowBrowserPermissionPrompt={allowBrowserPermissionPrompt}
          onRefresh={onRefresh}
          onOpenPermissionModal={onOpenPermissionModal}
          onMarkAsRead={onMarkAsRead}
          onDelete={onDelete}
          onNotificationClick={onNotificationClick}
          onMarkAllAsRead={onMarkAllAsRead}
          onDeleteAll={onDeleteAll}
        />
      </Card>
    </div>,
    portalContainer
  );
});

NotificationPopover.displayName = "NotificationPopover";
