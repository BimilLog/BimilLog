"use client";

import { useState, useEffect, useRef } from "react";
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
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from "@/components";
import {
  useNotifications,
  useNotificationList,
  useMarkNotificationAsRead,
  useDeleteNotification,
  useMarkAllNotificationsAsRead,
  useDeleteAllNotifications
} from "@/hooks/features";
import { useAuth } from "@/hooks";
import { Spinner as FlowbiteSpinner } from "flowbite-react";

/**
 * 실시간 알림 벨 컴포넌트
 * - SSE 연결을 통한 실시간 알림 수신
 * - 모바일/데스크톱 환경별 UI 분기 (Sheet vs Popover)
 * - TanStack Query 기반 알림 상태 관리
 * - 알림 읽음/삭제 처리 및 일괄 처리 지원
 */

export function NotificationBell() {
  // 알림 패널 열림/닫힘 상태 관리
  const [isOpen, setIsOpen] = useState(false);
  // 미디어 쿼리 기반 모바일/데스크톱 감지
  const [isMobile, setIsMobile] = useState(false);
  const { isAuthenticated } = useAuth();
  const dropdownRef = useRef<HTMLDivElement>(null);

  // TanStack Query 기반 알림 데이터 관리
  const { data: notificationResponse, isLoading, refetch } = useNotificationList();
  const markAsReadMutation = useMarkNotificationAsRead();
  const deleteNotificationMutation = useDeleteNotification();
  const markAllAsReadMutation = useMarkAllNotificationsAsRead();
  const deleteAllNotificationsMutation = useDeleteAllNotifications();

  // SSE(Server-Sent Events) 실시간 연결 관리
  const {
    isSSEConnected,
    connectionState,
    canConnectSSE,
    requestNotificationPermission,
  } = useNotifications();

  // 미디어 쿼리 기반 모바일/데스크톱 감지
  useEffect(() => {
    const checkIsMobile = () => {
      setIsMobile(window.innerWidth < 1024); // lg breakpoint
    };

    // 초기값 설정
    checkIsMobile();

    // 윈도우 리사이즈 이벤트 리스너
    window.addEventListener('resize', checkIsMobile);
    return () => window.removeEventListener('resize', checkIsMobile);
  }, []);

  // 외부 클릭 감지로 데스크톱 팝오버 닫기
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen && !isMobile) { // 데스크톱에서만 외부 클릭 감지
      document.addEventListener('mousedown', handleClickOutside);
      return () => {
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }
  }, [isOpen, isMobile]);

  // API 응답에서 알림 데이터 추출 및 읽지 않은 알림 개수 계산
  const notifications = notificationResponse?.success ? (notificationResponse.data || []) : [];
  const unreadCount = notifications.filter(n => !n.read).length;

  // 알림 처리 핸들러 함수들 (TanStack Query mutation 실행)
  const handleMarkAsRead = (notificationId: number) => {
    markAsReadMutation.mutate(notificationId);
  };

  const handleDeleteNotification = (notificationId: number) => {
    deleteNotificationMutation.mutate(notificationId);
  };

  const handleMarkAllAsRead = () => {
    markAllAsReadMutation.mutate();
  };

  const handleDeleteAllNotifications = () => {
    deleteAllNotificationsMutation.mutate();
  };

  // 알림 목록 수동 새로고침
  const handleFetchNotifications = () => {
    refetch();
  };

  // 인증되지 않은 사용자이거나 SSE 연결 조건을 만족하지 않으면 컴포넌트 숨김
  // (브라우저 호환성, HTTPS 환경 등 SSE 연결 요구사항 체크)
  if (!isAuthenticated || !canConnectSSE()) return null;

  // 알림 패널 열기/닫기 처리 (열릴 때 자동으로 새로고침 실행)
  const handleOpen = (open: boolean) => {
    setIsOpen(open);
    if (open && canConnectSSE()) {
      handleFetchNotifications();
    }
  };

  // 새로고침 버튼 클릭 핸들러
  const handleRefresh = () => {
    if (canConnectSSE()) {
      handleFetchNotifications();
    }
  };

  // 개별 알림 클릭 시 실행되는 핸들러 (읽음 처리 + 페이지 이동)
  const handleNotificationClick = async (notification: { id: number; read: boolean; url?: string }) => {
    // 읽지 않은 알림인 경우 자동으로 읽음 처리
    if (!notification.read) {
      handleMarkAsRead(notification.id);
    }

    // 알림에 연결된 URL이 있으면 해당 페이지로 이동
    if (notification.url) {
      window.location.href = notification.url;
    }
  };

  // 백엔드 v2.0 NotificationType enum과 매핑되는 아이콘 반환
  // 각 알림 유형별로 시각적으로 구분되는 아이콘과 색상 적용
  const getNotificationIcon = (notificationType: string) => {
    switch (notificationType) {
      case "PAPER":
        return <Leaf className="w-4 h-4 text-green-600" />; // 롤링페이퍼 알림
      case "COMMENT":
        return <MessageSquare className="w-4 h-4 text-blue-600" />; // 댓글 알림
      case "POST_FEATURED":
        return <Star className="w-4 h-4 text-yellow-600" />; // 게시글 추천 알림
      case "ADMIN":
        return <Shield className="w-4 h-4 text-purple-600" />; // 관리자 알림
      case "INITIATE":
        return <Bell className="w-4 h-4 text-brand-primary" />; // 일반 시스템 알림
      default:
        return <Bell className="w-4 h-4 text-brand-primary" />; // 기본 알림
    }
  };


  // 알림 패널 내용을 렌더링하는 공통 컴포넌트 (모바일/데스크톱에서 재사용)
  const NotificationContent = ({ isMobile = false }: { isMobile?: boolean }) => (
    <div className="w-full max-w-md mx-auto">
      {/* 접근성: 모바일에서만 SheetTitle 사용, 데스크톱에서는 일반 span */}
      {isMobile ? (
        <SheetTitle className="sr-only">알림</SheetTitle>
      ) : (
        <span className="sr-only">알림</span>
      )}

      {/* 알림 패널 헤더 (제목, 새로고침, 브라우저 알림 허용 버튼) */}
      <div className="flex items-center justify-between p-4 border-b bg-white/50">
        <div className="flex items-center gap-2">
          <Bell className="w-5 h-5 text-brand-primary" />
          <h2 className="text-lg font-semibold text-brand-primary">
            알림 {unreadCount > 0 && `(${unreadCount})`}
          </h2>
        </div>
        <div className="flex items-center gap-2">
          {/* 수동 새로고침 버튼 (로딩 중에는 회전 애니메이션) */}
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
          {/* 브라우저 푸시 알림 권한 요청 버튼 */}
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

      {/* 일괄 처리 버튼 영역 (알림이 있을 때만 표시) */}
      {notifications.length > 0 && (
        <div className="p-4 bg-gray-50/80 border-b">
          <div className="flex gap-2">
            {/* 모든 알림 읽음 처리 버튼 (읽지 않은 알림이 있을 때만 표시) */}
            {unreadCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleMarkAllAsRead}
                className="flex-1 text-sm min-h-[44px] touch-manipulation"
                title="모든 알림을 읽음으로 처리"
              >
                <CheckCircle2 className="w-4 h-4 mr-2" />
                모두 읽음
              </Button>
            )}
            {/* 모든 알림 삭제 버튼 */}
            <Button
              variant="outline"
              size="sm"
              onClick={handleDeleteAllNotifications}
              className="flex-1 text-sm min-h-[44px] text-red-600 hover:text-red-700 border-red-200 hover:border-red-300 touch-manipulation"
              title="모든 알림 삭제"
            >
              <Trash2 className="w-4 h-4 mr-2" />
              전체 삭제
            </Button>
          </div>
        </div>
      )}

      {/* 알림 목록 컨테이너 (반응형 높이 설정) */}
      <div className="max-h-[50vh] lg:max-h-96 overflow-y-auto">
        {/* 로딩 상태 */}
        {isLoading ? (
          <div className="p-8 flex flex-col items-center">
            <FlowbiteSpinner color="pink" size="xl" aria-label="알림을 불러오는 중..." />
            <p className="mt-2 text-sm text-brand-secondary">알림을 불러오는 중...</p>
          </div>
        ) : notifications.length > 0 ? (
          /* 알림 목록 렌더링 */
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
                  {/* 알림 유형별 아이콘 */}
                  <div className="flex-shrink-0 mt-0.5">
                    {getNotificationIcon(notification.notificationType)}
                  </div>
                  <div className="flex-1 min-w-0">
                    {/* 알림 내용 (읽지 않은 알림은 굵게 표시) */}
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
                      {/* 상대 시간 표시 */}
                      <TimeBadge dateString={notification.createdAt} size="xs" />
                      {/* 개별 알림 액션 버튼들 */}
                      <div className="flex items-center space-x-1">
                        {/* 읽음 처리 버튼 (읽지 않은 알림에만 표시) */}
                        {!notification.read && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation(); // 부모 onClick 이벤트 차단
                              handleMarkAsRead(notification.id);
                            }}
                            className="h-6 px-2 text-xs text-blue-600 hover:text-blue-700"
                            title="읽음 처리"
                          >
                            <Eye className="w-3 h-3" />
                          </Button>
                        )}
                        {/* 알림 삭제 버튼 */}
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation(); // 부모 onClick 이벤트 차단
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
          /* 알림이 없을 때 표시되는 빈 상태 */
          <div className="p-8 text-center">
            <BellOff className="w-12 h-12 mx-auto mb-3 text-brand-muted" />
            <p className="text-sm text-brand-secondary mb-2">알림이 없습니다</p>
            <p className="text-xs text-brand-secondary">
              새로운 알림이 오면 여기에 표시됩니다
            </p>
          </div>
        )}
      </div>

      {/* SSE 연결 상태 표시 (개발 모드에서만 표시되는 디버깅 정보) */}
      {process.env.NODE_ENV === "development" && (
        <div className="p-3 bg-gray-100/80 border-t">
          <div className="flex items-center justify-between text-xs">
            <span className="text-brand-muted">알림 상태:</span>
            <span
              className={`font-medium ${
                isSSEConnected
                  ? "text-green-600"
                  : canConnectSSE()
                  ? "text-yellow-600"
                  : "text-brand-secondary"
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

  // JavaScript 기반 조건부 렌더링: 모바일은 바텀 시트, 데스크톱은 팝오버
  return (
    <div ref={dropdownRef} className="relative">
      {isMobile ? (
        /* 모바일/태블릿: 바텀 시트(Sheet) UI */
        <Sheet open={isOpen} onOpenChange={handleOpen}>
          <SheetTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="relative min-h-[44px] min-w-[44px] touch-manipulation"
              title={`알림 ${
                unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""
              }`}
            >
              {/* SSE 연결 상태에 따른 벨 아이콘 변화 */}
              {isSSEConnected ? (
                <Bell className="w-5 h-5 text-brand-primary" />
              ) : (
                <BellOff className="w-5 h-5 text-brand-secondary" />
              )}
              {/* 읽지 않은 알림 개수 배지 (99개 이상은 99+로 표시) */}
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-medium">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              )}
            </Button>
          </SheetTrigger>
          {/* 화면 하단에서 올라오는 바텀 시트 (모바일 UX 최적화) */}
          <SheetContent side="bottom" className="h-[80vh] p-0">
            <NotificationContent isMobile={true} />
          </SheetContent>
        </Sheet>
      ) : (
        /* 데스크톱: 팝오버 카드 형태 */
        <>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleOpen(!isOpen)}
            className="relative min-h-[44px] min-w-[44px] touch-manipulation"
            title={`알림 ${unreadCount > 0 ? `(${unreadCount}개 읽지 않음)` : ""}`}
          >
            {/* SSE 연결 상태에 따른 벨 아이콘 변화 */}
            {isSSEConnected ? (
              <Bell className="w-5 h-5 stroke-purple-500 fill-purple-100" />
            ) : (
              <BellOff className="w-5 h-5 text-brand-secondary stroke-gray-500 fill-gray-100" />
            )}
            {/* 읽지 않은 알림 개수 배지 */}
            {unreadCount > 0 && (
              <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-medium">
                {unreadCount > 99 ? "99+" : unreadCount}
              </span>
            )}
          </Button>

          {/* 데스크톱 전용 팝오버 (우측 상단에서 아래로 펼쳐지는 드롭다운) */}
          {isOpen && (
            <div className="absolute right-0 top-full mt-2 z-50">
              <Card className="w-80 shadow-brand-xl border-0 bg-white/90 backdrop-blur-sm">
                <NotificationContent isMobile={false} />
              </Card>
            </div>
          )}
        </>
      )}
    </div>
  );
}
