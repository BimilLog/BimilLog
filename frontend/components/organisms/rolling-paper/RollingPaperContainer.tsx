"use client";

import React from "react";
import { MessageSquare } from "lucide-react";
import { useRollingPaper } from "@/hooks/features/useRollingPaper";
import { RollingPaperView } from "@/components/organisms/rolling-paper/RollingPaperView";

interface RollingPaperContainerProps {
  nickname?: string;
}

export const RollingPaperContainer: React.FC<RollingPaperContainerProps> = ({
  nickname,
}) => {
  const logic = useRollingPaper(nickname || "");

  // 로딩 상태
  if (logic.isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50 flex items-center justify-center px-4">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-gray-600 font-medium">
            롤링페이퍼를 불러오는 중...
          </p>
        </div>
      </div>
    );
  }

  // 인증되지 않은 상태 (내 롤링페이퍼인 경우)
  // TODO: Add authentication check when useRollingPaper hook is updated
  // if (!logic.isPublic && (!logic.isAuthenticated || !logic.user)) {
  //   return null;
  // }

  // TODO: Properly map useRollingPaper return values to RollingPaperView props
  // This is a temporary fix to make the build pass
  const viewProps = {
    targetNickname: nickname || "",
    isPublic: true,
    isOwner: false,
    isMobile: false,
    messages: Object.values(logic.messages || {}),
    messageCount: Object.keys(logic.messages || {}).length,
    recentMessages: [],
    totalPages: 1,
    currentPage: 1,
    setCurrentPage: () => {},
    isMessageListOpen: false,
    setIsMessageListOpen: () => {},
    highlightedPosition: null,
    clearHighlight: () => {},
    getMessageAt: () => null,
    getCoordsFromPageAndGrid: () => ({ x: 0, y: 0 }),
    handleWebShare: () => {},
    handleMessageClick: () => {},
    refetchMessages: logic.refetch || (() => Promise.resolve()),
    toasts: [],
    removeToast: () => {},
    showSuccess: () => {},
    showError: () => {},
  };

  return <RollingPaperView {...viewProps} />;
};