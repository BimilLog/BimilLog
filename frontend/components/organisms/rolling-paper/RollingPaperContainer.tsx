"use client";

import React, { useState, useCallback, useMemo } from "react";
import { MessageSquare } from "lucide-react";
import { useRollingPaperActions } from "@/hooks/features/useRollingPaper";
import { useAuth, useToast } from "@/hooks";
import { RollingPaperView } from "@/components/organisms/rolling-paper/RollingPaperView";
import type { RollingPaperMessage, VisitMessage, DecoType } from "@/types/domains/paper";

interface RollingPaperContainerProps {
  nickname?: string;
}

export const RollingPaperContainer: React.FC<RollingPaperContainerProps> = ({
  nickname,
}) => {
  const targetNickname = nickname || "";
  const { user, isAuthenticated } = useAuth();
  const { toasts, removeToast, showSuccess, showError } = useToast();
  
  // 롤링페이퍼 데이터 및 액션
  const {
    messages,
    gridData,
    isLoading,
    handleCreateMessage,
    toggleMessageSelection,
    refetch
  } = useRollingPaperActions(targetNickname);

  // UI 상태
  const [currentPage, setCurrentPage] = useState(1);
  const [isMessageListOpen, setIsMessageListOpen] = useState(false);
  const [highlightedPosition, setHighlightedPosition] = useState<{ x: number; y: number } | null>(null);

  // 모바일 체크
  const isMobile = useMemo(() => {
    if (typeof window !== 'undefined') {
      return window.innerWidth < 768;
    }
    return false;
  }, []);

  // 소유자 체크
  const isOwner = useMemo(() => {
    return isAuthenticated && user?.userName === targetNickname;
  }, [isAuthenticated, user?.userName, targetNickname]);

  // 공개 여부 (현재는 모든 롤링페이퍼가 공개)
  const isPublic = true;

  // 페이지 계산
  const totalPages = useMemo(() => {
    const messagesPerPage = isMobile ? 40 : 60; // 모바일 4x10, PC 6x10
    return Math.ceil(messages.length / messagesPerPage) || 1;
  }, [messages.length, isMobile]);

  // 최근 메시지 (최신 5개) - RollingPaperMessage만 정렬 가능 (createdAt 필드 존재)
  const recentMessages = useMemo(() => {
    return [...messages]
      .filter((msg): msg is RollingPaperMessage => 'createdAt' in msg)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);
  }, [messages]);

  // 특정 위치의 메시지 가져오기
  const getMessageAt = useCallback((x: number, y: number): RollingPaperMessage | VisitMessage | null => {
    const rowIndex = y - 1;
    const colIndex = x - 1;
    if (gridData[rowIndex] && gridData[rowIndex][colIndex]) {
      return gridData[rowIndex][colIndex];
    }
    return null;
  }, [gridData]);

  // 페이지와 그리드 위치에서 실제 좌표 계산
  const getCoordsFromPageAndGrid = useCallback((page: number, gridX: number, gridY: number) => {
    const cols = isMobile ? 4 : 6;
    const baseX = (page - 1) * cols;
    return {
      x: baseX + gridX,
      y: gridY
    };
  }, [isMobile]);

  // 하이라이트 지우기
  const clearHighlight = useCallback(() => {
    setHighlightedPosition(null);
  }, []);

  // 웹 공유
  const handleWebShare = useCallback(async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: `${targetNickname}님의 롤링페이퍼`,
          text: `${targetNickname}님에게 메시지를 남겨보세요!`,
          url: window.location.href
        });
        showSuccess('공유 완료', '롤링페이퍼 링크가 공유되었습니다.');
      } catch (error) {
        if ((error as Error).name !== 'AbortError') {
          showError('공유 실패', '공유하기에 실패했습니다.');
        }
      }
    } else {
      // 클립보드 복사 폴백
      try {
        await navigator.clipboard.writeText(window.location.href);
        showSuccess('복사 완료', '링크가 클립보드에 복사되었습니다.');
      } catch {
        showError('복사 실패', '링크 복사에 실패했습니다.');
      }
    }
  }, [targetNickname, showSuccess, showError]);

  // 메시지 클릭 핸들러
  const handleMessageClick = useCallback((message: RollingPaperMessage | VisitMessage) => {
    // 메시지 상세 보기 또는 선택 토글
    if ('id' in message) {
      toggleMessageSelection(message.id);
    }
  }, [toggleMessageSelection]);

  // 메시지 제출 핸들러
  const handleMessageSubmit = useCallback(async (position: { x: number; y: number }, data: unknown) => {
    if (data && typeof data === 'object' && 'content' in data && 'anonymity' in data && 'decoType' in data) {
      await handleCreateMessage({
        userName: targetNickname,
        content: data.content as string,
        anonymity: data.anonymity as string,
        decoType: data.decoType as DecoType,
        rowIndex: position.y - 1,
        colIndex: position.x - 1
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
          <p className="text-gray-600 font-medium">
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
          <p className="text-gray-600 font-medium">
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