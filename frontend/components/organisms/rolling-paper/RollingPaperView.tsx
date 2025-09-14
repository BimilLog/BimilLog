"use client";

import React from "react";
import dynamic from "next/dynamic";
import { RollingPaperLayout } from "@/components/organisms/rolling-paper/RollingPaperLayout";
import { NavigationBar } from "@/components/organisms/rolling-paper/NavigationBar";
import { SummarySection } from "@/components/organisms/rolling-paper/SummarySection";
import { ResponsiveAdFitBanner } from "@/components";
import { ToastContainer, Loading, Spinner, type Toast } from "@/components";

const RollingPaperGrid = dynamic(
  () => import("@/components/organisms/rolling-paper/RollingPaperGrid").then(mod => ({ default: mod.RollingPaperGrid })),
  {
    loading: () => <Loading className="min-h-[400px]" />,
    ssr: false
  }
);

const MessageListModal = dynamic(
  () => import("@/components/organisms/rolling-paper/MessageListModal").then(mod => ({ default: mod.MessageListModal })),
  {
    ssr: false,
    loading: () => (
      <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center">
        <div className="bg-white rounded-lg p-6 flex flex-col items-center gap-3">
          <Spinner size="md" />
          <p className="text-sm text-gray-500">메시지 목록 로딩 중...</p>
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
  recentMessages: (RollingPaperMessage | VisitMessage)[];
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
  handleWebShare: () => void;
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
  recentMessages,
  totalPages,
  currentPage,
  setCurrentPage,
  isMessageListOpen,
  setIsMessageListOpen,
  highlightedPosition,
  clearHighlight,
  getMessageAt,
  getCoordsFromPageAndGrid,
  handleWebShare,
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
            <ResponsiveAdFitBanner
              position={
                isPublic ? "타인 롤링페이퍼 상단" : "내 롤링페이퍼 상단"
              }
              className="max-w-full"
            />
          </div>
        </div>

        <div className="mb-8">
          <SummarySection
            isOwner={isOwner}
            nickname={targetNickname}
            messages={recentMessages}
            onShare={handleWebShare}
            onMessageClick={handleMessageClick}
          />
        </div>

        <div className="mb-8">
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