"use client";

import React from "react";
import dynamic from "next/dynamic";
import { RollingPaperLayout } from "@/components/organisms/rolling-paper/RollingPaperLayout";
import { NavigationBar } from "@/components/organisms/rolling-paper/NavigationBar";
import { MessageListButton } from "@/components/organisms/rolling-paper/MessageListButton";
import { ResponsiveAdFitBanner } from "@/components";
import { ToastContainer, Loading, Spinner, type Toast } from "@/components";

// 롤링페이퍼 그리드 컴포넌트를 동적 로딩으로 최적화
// SSR 비활성화로 클라이언트 사이드에서만 렌더링하여 상호작용 요소 최적화
const RollingPaperGrid = dynamic(
  () => import("@/components/organisms/rolling-paper/RollingPaperGrid").then(mod => ({ default: mod.RollingPaperGrid })),
  {
    loading: () => <Loading className="min-h-[400px]" />,
    ssr: false
  }
);

// 메시지 목록 모달을 동적 로딩으로 최적화
// 모달은 필요할 때만 로드하여 초기 번들 크기 감소
const MessageListModal = dynamic(
  () => import("@/components/organisms/rolling-paper/MessageListModal").then(mod => ({ default: mod.MessageListModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center">
        <div className="bg-white rounded-lg p-6 flex flex-col items-center gap-3">
          <Spinner size="md" />
          <p className="text-sm text-brand-secondary">메시지 목록 로딩 중...</p>
        </div>
      </div>
    ),
  }
);
import type { RollingPaperMessage, VisitMessage } from "@/types/domains/paper";

interface RollingPaperViewProps {
  targetNickname: string;
  isPublic: boolean;
  isOwner: boolean;
  isMobile: boolean;
  messages: (RollingPaperMessage | VisitMessage)[];
  messageCount: number;
  totalPages: number;
  currentPage: number;
  setCurrentPage: (page: number) => void;
  isMessageListOpen: boolean;
  setIsMessageListOpen: (open: boolean) => void;
  highlightedPosition?: { x: number; y: number } | null;
  clearHighlight: () => void;
  getMessageAt: (x: number, y: number) => RollingPaperMessage | VisitMessage | null;
  getCoordsFromPageAndGrid: (
    page: number,
    gridX: number,
    gridY: number
  ) => { x: number; y: number };
  handleMessageSubmit?: (position: { x: number; y: number }, data: unknown) => void;
  handleMessageClick: (message: RollingPaperMessage | VisitMessage) => void;
  refetchMessages: () => Promise<void>;
  toasts: Toast[];
  removeToast: (id: string) => void;
  showSuccess: (title: string, message: string) => void;
  showError: (title: string, message: string) => void;
}

export const RollingPaperView: React.FC<RollingPaperViewProps> = ({
  targetNickname,
  isPublic,
  isOwner,
  isMobile,
  messages,
  messageCount,
  totalPages,
  currentPage,
  setCurrentPage,
  isMessageListOpen,
  setIsMessageListOpen,
  highlightedPosition,
  clearHighlight,
  getMessageAt,
  getCoordsFromPageAndGrid,
  handleMessageSubmit,
  handleMessageClick,
  refetchMessages,
  toasts,
  removeToast,
  showSuccess,
  showError,
}) => {
  return (
    <RollingPaperLayout>
      {/* 네비게이션 바: 소유자만 메시지 목록 버튼 활성화, 모바일에서는 페이지네이션 숨김 */}
      <NavigationBar
        nickname={targetNickname}
        messageCount={messageCount}
        isOwner={isOwner}
        onShowMessages={isOwner ? () => setIsMessageListOpen(true) : undefined}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={setCurrentPage}
        showPagination={!isMobile && totalPages > 1}
        className="sticky top-[68px] sm:top-[80px] z-40"
      />

      <div className="container mx-auto px-2 md:px-4">
        <div className="py-6">
          <div className="flex justify-center">
            {/* 광고 배너: 공개/비공개 여부에 따라 다른 위치 구분 */}
            <ResponsiveAdFitBanner
              position={
                isPublic ? "타인 롤링페이퍼 상단" : "내 롤링페이퍼 상단"
              }
              className="max-w-full"
            />
          </div>
        </div>

        <div className="mb-8">
          <MessageListButton
            isOwner={isOwner}
            nickname={targetNickname}
            messageCount={messageCount}
            onOpenMessageList={() => setIsMessageListOpen(true)}
          />
        </div>

        <div className="mb-8">
          {/* 롤링페이퍼 그리드: 소유자가 아닐 때만 메시지 작성 가능 */}
          <RollingPaperGrid
            messages={messages}
            nickname={targetNickname}
            isOwner={isOwner}
            isMobile={isMobile}
            totalPages={totalPages}
            currentPage={currentPage}
            setCurrentPage={setCurrentPage}
            onMessageSubmit={!isOwner ? handleMessageSubmit : undefined}
            getMessageAt={getMessageAt}
            getCoordsFromPageAndGrid={getCoordsFromPageAndGrid}
            highlightedPosition={highlightedPosition}
            onHighlightClear={clearHighlight}
            onSuccess={(message) => showSuccess("성공", message)}
            onError={(message) => showError("오류", message)}
            onRefresh={refetchMessages}
          />
        </div>
      </div>

      {/* 메시지 목록 모달: RollingPaperMessage만 필터링 (VisitMessage 제외) */}
      {/* 타입 가드를 사용하여 실제 메시지만 표시하고 방문 기록은 제외 */}
      <MessageListModal
        isOpen={isMessageListOpen}
        onClose={() => setIsMessageListOpen(false)}
        messages={messages.filter(
          (msg): msg is RollingPaperMessage =>
            "content" in msg && "createdAt" in msg
        )}
        onMessageClick={handleMessageClick}
      />

      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </RollingPaperLayout>
  );
};