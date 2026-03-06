"use client";

import { memo } from "react";
import {
  Bell,
  BellOff,
  RefreshCw,
  CheckCircle2,
  Trash2,
} from "lucide-react";
import { Button } from "@/components";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import { NotificationItem } from "./NotificationItem";
import type { Notification } from "@/types/domains/notification";

interface NotificationListProps {
  /** 알림 목록 */
  notifications: Notification[];
  /** 읽지 않은 알림 수 */
  unreadCount: number;
  /** 목록 로딩 중 여부 */
  isInitialLoading: boolean;
  /** 목록 패칭 중 여부 */
  isFetchingList: boolean;
  /** 브라우저 알림 허용 프롬프트 표시 가능 여부 */
  allowBrowserPermissionPrompt: boolean;
  /** 새로고침 핸들러 */
  onRefresh: () => void;
  /** 브라우저 알림 권한 요청 모달 열기 */
  onOpenPermissionModal: () => void;
  /** 개별 알림 읽음 처리 */
  onMarkAsRead: (id: number) => void;
  /** 개별 알림 삭제 */
  onDelete: (id: number) => void;
  /** 알림 클릭 (읽음 처리 + URL 이동) */
  onNotificationClick: (notification: Notification) => void;
  /** 모두 읽음 처리 */
  onMarkAllAsRead: (e?: React.MouseEvent) => void;
  /** 전체 삭제 */
  onDeleteAll: (e?: React.MouseEvent) => void;
}

/** 알림 목록 컴포넌트 (헤더, 전체 읽음/삭제 버튼, 빈 상태 포함) */
export const NotificationList = memo(function NotificationList({
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
}: NotificationListProps) {
  return (
    <div className="w-full h-full flex flex-col">
      {/* 헤더 */}
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
            onClick={onRefresh}
            disabled={isFetchingList}
            className="text-sm min-h-[44px] min-w-[44px] touch-manipulation"
            title="알림 목록 새로고침"
          >
            {isFetchingList ? (
              <FlowbiteSpinner color="pink" size="sm" aria-label="새로고침 중.." />
            ) : (
              <RefreshCw className="w-4 h-4" />
            )}
          </Button>
          {allowBrowserPermissionPrompt && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onOpenPermissionModal}
              className="text-sm min-h-[44px] min-w-[44px] touch-manipulation"
              title="브라우저 알림 허용"
            >
              <Bell className="w-4 h-4" />
            </Button>
          )}
        </div>
      </div>

      {/* 전체 읽음/삭제 버튼 영역 */}
      {notifications.length > 0 && (
        <div className="p-4 bg-gray-50/80 border-b flex-shrink-0">
          <div className="flex gap-2">
            {unreadCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={(e) => onMarkAllAsRead(e)}
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
              onClick={(e) => onDeleteAll(e)}
              className="flex-1 text-sm min-h-[44px] text-red-600 hover:text-red-700 border-red-200 hover:border-red-300 touch-manipulation"
              title="모든 알림 삭제"
            >
              <Trash2 className="w-4 h-4 mr-2" />
              전체 삭제
            </Button>
          </div>
        </div>
      )}

      {/* 알림 목록 본문 */}
      <div className="flex-1 overflow-y-auto">
        {isInitialLoading ? (
          <div className="p-8 flex flex-col items-center">
            <FlowbiteSpinner color="pink" size="xl" aria-label="알림을 불러오는 중..." />
            <p className="mt-2 text-sm text-brand-secondary">알림을 불러오는 중...</p>
          </div>
        ) : notifications.length > 0 ? (
          <div className="divide-y divide-gray-100">
            {notifications.map((notification) => (
              <NotificationItem
                key={notification.id}
                notification={notification}
                onMarkAsRead={onMarkAsRead}
                onDelete={onDelete}
                onClick={onNotificationClick}
              />
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
});

NotificationList.displayName = "NotificationList";
