"use client";

import { memo } from "react";
import { createPortal } from "react-dom";
import { Drawer } from "flowbite-react";
import { NotificationList } from "./NotificationList";
import type { Notification } from "@/types/domains/notification";

interface NotificationDrawerProps {
  /** 드로어 표시 여부 */
  isOpen: boolean;
  /** 포탈 컨테이너 (document.body) */
  portalContainer: HTMLElement | null;
  /** 닫기 핸들러 */
  onClose: () => void;
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

/** 모바일 드로어 UI */
export const NotificationDrawer = memo(function NotificationDrawer({
  isOpen,
  portalContainer,
  onClose,
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
}: NotificationDrawerProps) {
  if (!isOpen || !portalContainer) return null;

  return createPortal(
    <Drawer
      open={true}
      onClose={onClose}
      position="bottom"
      className="rounded-t-xl !z-[60]"
    >
      <div className="h-[80vh] max-h-[80vh] flex flex-col bg-white rounded-t-xl overflow-hidden">
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
      </div>
    </Drawer>,
    portalContainer
  );
});

NotificationDrawer.displayName = "NotificationDrawer";
