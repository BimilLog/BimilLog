"use client";

import { useState } from "react";
import {
  Bell,
  BellOff,
  RefreshCw,
  Trash2,
  CheckCircle2,
  Eye,
  Clock,
  Leaf,
  MessageSquare,
  Star,
  Shield,
} from "lucide-react";
import { Button } from "@/components/atoms/button";
import { Card, CardContent } from "@/components/molecules/card";
import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from "@/components/molecules/sheet";
import { useNotifications } from "@/hooks/useNotifications";
import { isMobileOrTablet } from "@/lib/utils";
import { useAuth } from "@/hooks/useAuth";

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const { isAuthenticated } = useAuth();

  const {
    notifications,
    unreadCount,
    isLoading,
    isSSEConnected,
    connectionState,
    canConnectSSE,
    fetchNotifications,
    markAsRead,
    deleteNotification,
    markAllAsRead,
    deleteAllNotifications,
    requestNotificationPermission,
    // 디버깅용 배치 상태
    pendingReadIds,
    pendingDeleteIds,
    processBatch,
  } = useNotifications();

  // 인증되지 않았거나 SSE 연결 조건을 만족하지 않으면 숨김
  if (!isAuthenticated || !canConnectSSE()) return null;

  // 알림 벨 클릭 시 자동 새로고침
  const handleOpen = (open: boolean) => {
    setIsOpen(open);
    if (open && canConnectSSE()) {
      fetchNotifications();
    }
  };

  // 수동 새로고침
  const handleRefresh = () => {
    if (canConnectSSE()) {
      fetchNotifications();
    }
  };

  // 알림 읽음 처리
  const handleNotificationClick = async (notification: any) => {
    if (!notification.read) {
      await markAsRead(notification.id);
    }

    // URL이 있으면 이동
    if (notification.url) {
      window.location.href = notification.url;
    }
  };

  // 알림 타입별 아이콘 매핑
  const getNotificationIcon = (type: string) => {
    switch (type) {
      case "FARM":
        return <Leaf className="w-4 h-4 text-green-600" />;
      case "COMMENT":
        return <MessageSquare className="w-4 h-4 text-blue-600" />;
      case "POST_FEATURED":
        return <Star className="w-4 h-4 text-yellow-600" />;
      case "COMMENT_FEATURED":
        return <Star className="w-4 h-4 text-purple-600" />;
      case "ADMIN":
        return <Shield className="w-4 h-4 text-red-600" />;
      default:
        return <Bell className="w-4 h-4 text-gray-600" />;
    }
  };

  // 상대 시간 표시
  const getRelativeTime = (createdAt: string) => {
    const now = new Date();
    const notificationTime = new Date(createdAt);
    const diffInMinutes = Math.floor(
      (now.getTime() - notificationTime.getTime()) / (1000 * 60)
    );

    if (diffInMinutes < 1) return "방금 전";
    if (diffInMinutes < 60) return `${diffInMinutes}분 전`;

    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}시간 전`;

    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 30) return `${diffInDays}일 전`;

    return notificationTime.toLocaleDateString("ko-KR");
  };

  // 모바일 환경인지 확인
  const isMobile = typeof window !== "undefined" && isMobileOrTablet();

  const NotificationContent = () => (
    <div className="w-full max-w-md mx-auto">
      {/* 스크린 리더용 제목 (숨김) - 모바일(Sheet)에서만 */}
      {isMobile && <SheetTitle className="sr-only">알림</SheetTitle>}

      {/* 헤더 */}
      <div className="flex items-center justify-between p-4 border-b bg-white/50">
        <div className="flex items-center space-x-2">
          <Bell className="w-5 h-5 text-gray-700" />
          <h2 className="text-lg font-semibold text-gray-800">
            알림 {unreadCount > 0 && `(${unreadCount})`}
          </h2>
        </div>
        <div className="flex items-center space-x-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRefresh}
            disabled={isLoading}
            className="text-xs h-8 px-2"
            title="알림 목록 새로고침"
          >
            <RefreshCw
              className={`w-3 h-3 ${isLoading ? "animate-spin" : ""}`}
            />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={requestNotificationPermission}
            className="text-xs h-8 px-2"
            title="브라우저 알림 허용"
          >
            <Bell className="w-3 h-3" />
          </Button>
        </div>
      </div>

      {/* 전체 액션 버튼 */}
      {notifications.length > 0 && (
        <div className="p-4 bg-gray-50/80 border-b">
          <div className="flex space-x-2">
            {unreadCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={markAllAsRead}
                className="flex-1 text-xs h-8"
                title="모든 알림을 읽음으로 처리"
              >
                <CheckCircle2 className="w-3 h-3 mr-1" />
                모두 읽음
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={deleteAllNotifications}
              className="flex-1 text-xs h-8 text-red-600 hover:text-red-700 border-red-200 hover:border-red-300"
              title="모든 알림 삭제"
            >
              <Trash2 className="w-3 h-3 mr-1" />
              전체 삭제
            </Button>
          </div>
        </div>
      )}

      {/* 개발 모드에서 배치 상태 표시 */}
      {process.env.NODE_ENV === "development" &&
        (pendingReadIds.length > 0 || pendingDeleteIds.length > 0) && (
          <div className="text-xs text-gray-500 p-3 bg-yellow-50 border-b">
            배치 대기: 읽음 {pendingReadIds.length}개, 삭제{" "}
            {pendingDeleteIds.length}개
            <Button
              variant="ghost"
              size="sm"
              onClick={processBatch}
              className="ml-2 text-xs h-5"
              title="배치 처리 수동 실행 (테스트용)"
            >
              수동 실행
            </Button>
          </div>
        )}

      {/* 알림 목록 */}
      <div
        className={`${isMobile ? "max-h-[50vh]" : "max-h-96"} overflow-y-auto`}
      >
        {isLoading ? (
          <div className="p-8 text-center">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2 text-gray-400" />
            <p className="text-sm text-gray-500">알림을 불러오는 중...</p>
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
                    {getNotificationIcon(notification.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p
                      className={`text-sm ${
                        !notification.read
                          ? "font-medium text-gray-900"
                          : "text-gray-600"
                      }`}
                    >
                      {notification.data}
                    </p>
                    <div className="flex items-center justify-between mt-1">
                      <div className="flex items-center space-x-2 text-xs text-gray-500">
                        <Clock className="w-3 h-3" />
                        <span>{getRelativeTime(notification.createdAt)}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        {!notification.read && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              markAsRead(notification.id);
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
                            deleteNotification(notification.id);
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
            <BellOff className="w-12 h-12 mx-auto mb-3 text-gray-300" />
            <p className="text-sm text-gray-500 mb-2">알림이 없습니다</p>
            <p className="text-xs text-gray-400">
              새로운 알림이 오면 여기에 표시됩니다
            </p>
          </div>
        )}
      </div>

      {/* 연결 상태 (개발 모드) */}
      {process.env.NODE_ENV === "development" && (
        <div className="p-3 bg-gray-100/80 border-t">
          <div className="flex items-center justify-between text-xs">
            <span className="text-gray-600">알림 상태:</span>
            <span
              className={`font-medium ${
                isSSEConnected
                  ? "text-green-600"
                  : canConnectSSE()
                  ? "text-yellow-600"
                  : "text-gray-500"
              }`}
            >
              {isSSEConnected
                ? "연결됨"
                : canConnectSSE()
                ? connectionState
                : "연결 불가"}
            </span>
          </div>
        </div>
      )}
    </div>
  );

  // 모바일에서는 Sheet(바텀시트) 사용, 데스크톱에서는 기존 Popover 스타일 유지
  if (isMobile) {
    return (
      <Sheet open={isOpen} onOpenChange={handleOpen}>
        <SheetTrigger asChild>
          <Button
            variant="ghost"
            size="sm"
            className="relative"
            title={`알림 ${
              unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""
            }`}
          >
            {isSSEConnected ? (
              <Bell className="w-5 h-5" />
            ) : (
              <BellOff className="w-5 h-5 text-gray-400" />
            )}
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-medium">
                {unreadCount > 99 ? "99+" : unreadCount}
              </span>
            )}
          </Button>
        </SheetTrigger>
        <SheetContent side="bottom" className="h-[80vh] p-0">
          <NotificationContent />
        </SheetContent>
      </Sheet>
    );
  }

  // 데스크톱 버전 (기존 Card 스타일)
  return (
    <div className="relative">
      <Button
        variant="ghost"
        size="sm"
        onClick={() => handleOpen(!isOpen)}
        className="relative"
        title={`알림 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`}
      >
        {isSSEConnected ? (
          <Bell className="w-5 h-5" />
        ) : (
          <BellOff className="w-5 h-5 text-gray-400" />
        )}
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-medium">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </Button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 z-50">
          <Card className="w-80 shadow-xl border-0 bg-white/90 backdrop-blur-sm">
            <NotificationContent />
          </Card>
        </div>
      )}
    </div>
  );
}
