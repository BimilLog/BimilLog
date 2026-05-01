"use client";

import { memo } from "react";
import {
  Bell,
  MailOpen,
  AlertCircle,
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
  /** F-1110: fetch 에러 상태 */
  isErrored?: boolean;
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
  isErrored = false,
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
      {/* 헤더 — 다크 토큰 회귀: bg-white/50 → paper-soft/60 dark:bg-background/60 */}
      <div className="flex items-center justify-between p-4 border-b border-ink-soft bg-paper-soft/60 dark:bg-background/60 flex-shrink-0">
        <div className="flex items-center gap-2">
          <Bell className="w-5 h-5 text-stamp-red" aria-hidden="true" />
          <h2
            id="notification-heading"
            className="text-lg font-semibold text-ink dark:text-foreground break-keep"
          >
            알림 {unreadCount > 0 && `(${unreadCount > 99 ? "99+" : unreadCount})`}
          </h2>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={onRefresh}
            disabled={isFetchingList}
            className="text-sm min-h-[44px] min-w-[44px] touch-manipulation"
            aria-label="알림 목록 새로고침"
            title="알림 목록 새로고침"
          >
            {isFetchingList ? (
              <FlowbiteSpinner color="pink" size="sm" aria-label="새로고침 중.." />
            ) : (
              <RefreshCw className="w-4 h-4" aria-hidden="true" />
            )}
          </Button>
          {allowBrowserPermissionPrompt && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onOpenPermissionModal}
              className="text-sm min-h-[44px] min-w-[44px] touch-manipulation"
              aria-label="브라우저 알림 허용"
              title="브라우저 알림 허용"
            >
              <Bell className="w-4 h-4" aria-hidden="true" />
            </Button>
          )}
        </div>
      </div>

      {/* 전체 읽음/삭제 버튼 영역 — 다크 토큰 회귀: bg-gray-50/80 → paper-soft/40 dark:bg-paper-card/40 */}
      {notifications.length > 0 && (
        <div className="p-4 bg-paper-soft/40 dark:bg-paper-card/40 border-b border-ink-soft flex-shrink-0">
          <div className="flex gap-2">
            {unreadCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={(e) => onMarkAllAsRead(e)}
                className="flex-1 text-sm min-h-[44px] touch-manipulation break-keep"
                title="모든 알림을 읽음으로 처리"
              >
                <CheckCircle2 className="w-4 h-4 mr-2" aria-hidden="true" />
                모두 읽음
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={(e) => onDeleteAll(e)}
              className="flex-1 text-sm min-h-[44px] text-stamp-red hover:opacity-80 border-stamp-red/30 hover:border-stamp-red/60 touch-manipulation break-keep"
              title="모든 알림 삭제"
            >
              <Trash2 className="w-4 h-4 mr-2" aria-hidden="true" />
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
            <p className="mt-2 text-sm text-ink-soft dark:text-foreground/70 break-keep">
              알림을 불러오는 중...
            </p>
          </div>
        ) : isErrored ? (
          // F-1110: 에러 상태 — 빈 상태와 분리, 재시도 버튼 제공
          <div
            role="alert"
            className="p-8 text-center bg-stamp-red/10 dark:bg-stamp-red/20 rounded-md mx-3 my-4"
          >
            <AlertCircle
              className="w-12 h-12 mx-auto mb-3 text-stamp-red"
              aria-hidden="true"
            />
            <p className="text-sm font-semibold text-ink dark:text-foreground mb-1 break-keep">
              알림을 불러오지 못했어요
            </p>
            <p className="text-xs text-ink-soft dark:text-foreground/70 mb-4 break-keep">
              잠시 후 다시 시도해주세요
            </p>
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={isFetchingList}
              className="min-h-[44px] touch-manipulation break-keep"
            >
              <RefreshCw className="w-4 h-4 mr-2" aria-hidden="true" />
              다시 시도
            </Button>
          </div>
        ) : notifications.length > 0 ? (
          <div className="divide-y divide-ink-soft">
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
          // 빈 상태 — 종이/편지 메타포 + role="status" (라운드 1~10 패턴)
          <div
            role="status"
            aria-live="polite"
            className="p-8 text-center bg-paper-soft/40 dark:bg-paper-card/30 rounded-md mx-3 my-4"
          >
            <MailOpen
              className="w-12 h-12 mx-auto mb-3 text-stamp-red/70 dark:text-stamp-red/80"
              aria-hidden="true"
            />
            <p className="text-sm font-semibold text-ink dark:text-foreground mb-1 break-keep">
              받은 편지가 없어요
            </p>
            <p className="text-xs text-ink-soft dark:text-foreground/70 break-keep">
              새 알림이 오면 여기로 도착해요
            </p>
          </div>
        )}
      </div>
    </div>
  );
});

NotificationList.displayName = "NotificationList";
