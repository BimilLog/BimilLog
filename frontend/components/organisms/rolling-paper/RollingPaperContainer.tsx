"use client";

import React, { useState, useCallback } from "react";
import { MessageSquare } from "lucide-react";
import { useRollingPaperData } from "@/hooks/features/useRollingPaperData";
import { useRollingPaperActions } from "@/hooks/features/useRollingPaper";
import { useRollingPaperGrid } from "@/hooks/features/useRollingPaperGrid";
import { useRollingPaperShare } from "@/hooks/features/useRollingPaperShare";
import { useToast } from "@/hooks";
import { RollingPaperView } from "@/components/organisms/rolling-paper/RollingPaperView";
import { gridPositionToDBIndex } from "@/lib/utils/rolling-paper";
import type { RollingPaperMessage, VisitMessage, DecoType } from "@/types/domains/paper";

interface RollingPaperContainerProps {
  nickname?: string;
}

export const RollingPaperContainer: React.FC<RollingPaperContainerProps> = ({
  nickname,
}) => {
  const targetNickname = nickname || "";
  const { toasts, removeToast, showSuccess, showError } = useToast();

  // 롤링페이퍼 데이터 조회 (본인/타인 구분)
  const {
    messages,
    isLoading,
    isOwner,
    refetch
  } = useRollingPaperData(nickname);

  // 롤링페이퍼 액션 (작성, 선택)
  const {
    handleCreateMessage,
    toggleMessageSelection
  } = useRollingPaperActions(targetNickname);

  // 그리드 관련 로직
  const {
    totalPages,
    recentMessages,
    isMobile,
    getMessageAt,
    getCoordsFromPageAndGrid
  } = useRollingPaperGrid({ messages });

  // UI 상태
  const [currentPage, setCurrentPage] = useState(1);
  const [isMessageListOpen, setIsMessageListOpen] = useState(false);
  const [highlightedPosition, setHighlightedPosition] = useState<{ x: number; y: number } | null>(null);

  // 공개 여부 (현재는 모든 롤링페이퍼가 공개)
  const isPublic = true;

  // 공유 기능
  const { handleWebShare } = useRollingPaperShare({
    nickname: targetNickname,
    messageCount: messages.length,
    isOwner
  });

  // 하이라이트 지우기
  const clearHighlight = useCallback(() => {
    setHighlightedPosition(null);
  }, []);

  // 메시지 클릭 핸들러
  const handleMessageClick = useCallback((message: RollingPaperMessage | VisitMessage) => {
    // 메시지 상세 보기 또는 선택 토글
    if ('id' in message) {
      toggleMessageSelection(message.id);
    }
  }, [toggleMessageSelection]);

  // 메시지 제출 핸들러 - UI 좌표를 DB 인덱스로 변환하여 저장
  const handleMessageSubmit = useCallback(async (position: { x: number; y: number }, data: unknown) => {
    // 타입 검증을 통해 필요한 필드들이 있는지 확인
    if (data && typeof data === 'object' && 'content' in data && 'anonymity' in data && 'decoType' in data) {
      const { rowIndex, colIndex } = gridPositionToDBIndex(position);
      await handleCreateMessage({
        userName: targetNickname,
        content: data.content as string,
        anonymity: data.anonymity as string,
        decoType: data.decoType as DecoType,
        rowIndex,
        colIndex
      });
    }
  }, [handleCreateMessage, targetNickname]);

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50 flex items-center justify-center px-4">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white animate-pulse" />
          </div>
          <p className="text-brand-muted font-medium">
            롤링페이퍼를 불러오는 중...
          </p>
        </div>
      </div>
    );
  }

  // 인증 체크: 비공개 롤링페이퍼는 소유자만 볼 수 있음
  if (!isPublic && !isOwner) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50 flex items-center justify-center px-4">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-r from-red-500 to-pink-600 rounded-xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-7 h-7 text-white" />
          </div>
          <p className="text-brand-muted font-medium">
            이 롤링페이퍼는 비공개입니다.
          </p>
        </div>
      </div>
    );
  }

  const viewProps = {
    targetNickname,
    isPublic,
    isOwner,
    isMobile,
    messages,
    messageCount: messages.length,
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
    refetchMessages: async () => {
      await refetch();
    },
    toasts,
    removeToast,
    showSuccess,
    showError,
  };

  return <RollingPaperView {...viewProps} />;
};