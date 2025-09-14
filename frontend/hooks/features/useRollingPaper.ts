"use client";

import { useState, useCallback, useMemo } from 'react';
import { useRouter } from "next/navigation";
import { paperQuery } from '@/lib/api';
import { useAuth, useToast } from '@/hooks';
import { ErrorHandler } from "@/lib/api/helpers";
import { copyRollingPaperLink } from "@/lib/utils/clipboard";
import { logger } from '@/lib/utils/logger';
import type { VisitMessage, DecoType } from '@/types/domains/paper';

// TanStack Query Hooks re-exports
export { useRollingPaper } from '@/hooks/api/useRollingPaperQueries';
export {
  useCreateRollingPaperMessage,
  useDeleteRollingPaperMessage
} from '@/hooks/api/useRollingPaperMutations';

// Import for local usage
import { useRollingPaper } from '@/hooks/api/useRollingPaperQueries';
import {
  useCreateRollingPaperMessage,
  useDeleteRollingPaperMessage
} from '@/hooks/api/useRollingPaperMutations';

// 롤링페이퍼 통합 Hook
export function useRollingPaperActions(userName: string) {
  const [selectedPosition, setSelectedPosition] = useState<{ row: number; col: number } | null>(null);
  const [isMessageFormOpen, setIsMessageFormOpen] = useState(false);
  const [selectedMessages, setSelectedMessages] = useState<number[]>([]);

  const { data, isLoading, refetch } = useRollingPaper(userName);
  const createMessageMutation = useCreateRollingPaperMessage();
  const deleteMessageMutation = useDeleteRollingPaperMessage();

  // 그리드 데이터 구성
  const gridData = useMemo(() => {
    const messages = data?.data || [];
    const grid: (VisitMessage | null)[][] = [];
    const rows = 10;
    const cols = 6;

    // 빈 그리드 초기화
    for (let i = 0; i < rows; i++) {
      grid[i] = new Array(cols).fill(null);
    }

    // 메시지를 그리드에 배치 (x, y는 1-based 좌표)
    messages.forEach(message => {
      const rowIndex = message.y - 1; // y를 0-based rowIndex로 변환
      const colIndex = message.x - 1; // x를 0-based colIndex로 변환
      if (rowIndex >= 0 && rowIndex < rows && colIndex >= 0 && colIndex < cols) {
        grid[rowIndex][colIndex] = message;
      }
    });

    return grid;
  }, [data]);

  // 메시지 작성
  const handleCreateMessage = useCallback((messageData: {
    userName: string;
    content: string;
    anonymity: string;
    decoType: DecoType;
    rowIndex: number;
    colIndex: number;
  }) => {
    createMessageMutation.mutate({
      userName: messageData.userName,
      message: {
        decoType: messageData.decoType,
        anonymity: messageData.anonymity,
        content: messageData.content,
        x: messageData.colIndex + 1, // 0-based to 1-based
        y: messageData.rowIndex + 1  // 0-based to 1-based
      }
    }, {
      onSuccess: () => {
        setIsMessageFormOpen(false);
        setSelectedPosition(null);
      }
    });
  }, [createMessageMutation]);

  // 메시지 삭제
  const handleDeleteMessages = useCallback(async () => {
    if (selectedMessages.length === 0) return;

    // 각 메시지를 개별적으로 삭제
    for (const messageId of selectedMessages) {
      deleteMessageMutation.mutate(messageId, {
        onSuccess: () => {
          setSelectedMessages(prev => prev.filter(id => id !== messageId));
        }
      });
    }
  }, [deleteMessageMutation, selectedMessages]);

  // 위치 선택
  const handleSelectPosition = useCallback((row: number, col: number) => {
    // 이미 메시지가 있는 위치인지 확인
    if (gridData[row][col]) {
      return false;
    }

    setSelectedPosition({ row, col });
    setIsMessageFormOpen(true);
    return true;
  }, [gridData]);

  // 메시지 선택/해제
  const toggleMessageSelection = useCallback((messageId: number) => {
    setSelectedMessages(prev => {
      if (prev.includes(messageId)) {
        return prev.filter(id => id !== messageId);
      }
      return [...prev, messageId];
    });
  }, []);

  return {
    messages: data?.data || [],
    gridData,
    isLoading,
    isCreating: createMessageMutation.isPending,
    isDeleting: deleteMessageMutation.isPending,
    selectedPosition,
    isMessageFormOpen,
    selectedMessages,
    setIsMessageFormOpen,
    handleCreateMessage,
    handleDeleteMessages,
    handleSelectPosition,
    toggleMessageSelection,
    refetch
  };
}

// ===== ROLLING PAPER SEARCH =====
interface UseRollingPaperSearchReturn {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  searchError: string;
  handleSearch: () => Promise<void>;
  handleKeyPress: (e: React.KeyboardEvent) => void;
  clearError: () => void;
}

/**
 * 롤링페이퍼 검색 및 navigation을 담당하는 Hook
 */
export function useRollingPaperSearch(): UseRollingPaperSearchReturn {
  const { user } = useAuth();
  const router = useRouter();
  const [searchNickname, setSearchNickname] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState("");

  const clearError = useCallback(() => {
    setSearchError("");
  }, []);

  const handleSearch = useCallback(async () => {
    if (!searchNickname.trim()) return;

    const trimmedNickname = searchNickname.trim();
    setIsSearching(true);
    setSearchError("");

    try {
      // 로그인한 사용자의 경우 자신의 닉네임인지 먼저 확인 (API 호출 전)
      if (user && user.userName === trimmedNickname) {
        // 자신의 롤링페이퍼로 리다이렉트
        router.push("/rolling-paper");
        setIsSearching(false);
        return;
      }

      // 다른 사용자의 롤링페이퍼 조회 시도
      const response = await paperQuery.getByUserName(trimmedNickname);

      if (response.success) {
        // 성공적으로 조회된 경우 방문 페이지로 이동
        router.push(`/rolling-paper/${encodeURIComponent(trimmedNickname)}`);
      } else {
        // 사용자를 찾을 수 없는 경우
        const appError = ErrorHandler.mapApiError(new Error(response.error || "사용자를 찾을 수 없습니다"));
        setSearchError(
          appError.userMessage ||
          "해당 닉네임의 롤링페이퍼를 찾을 수 없어요. 회원가입한 사용자의 롤링페이퍼만 존재합니다."
        );
      }
    } catch (error) {
      logger.error("Search error:", error);
      const appError = ErrorHandler.mapApiError(error);
      setSearchError(appError.userMessage || "롤링페이퍼를 찾을 수 없어요.");
    } finally {
      setIsSearching(false);
    }
  }, [searchNickname, user, router]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  }, [handleSearch]);

  const setSearchNicknameWithClear = useCallback((nickname: string) => {
    setSearchNickname(nickname);
    if (searchError) {
      setSearchError("");
    }
  }, [searchError]);

  const memoizedReturn = useMemo(() => ({
    searchNickname,
    setSearchNickname: setSearchNicknameWithClear,
    isSearching,
    searchError,
    handleSearch,
    handleKeyPress,
    clearError,
  }), [searchNickname, setSearchNicknameWithClear, isSearching, searchError, handleSearch, handleKeyPress, clearError]);

  return memoizedReturn;
}

// ===== ROLLING PAPER SHARE =====
interface UseRollingPaperShareProps {
  nickname: string;
  messageCount: number;
  isOwner?: boolean;
}

export function useRollingPaperShare({
  nickname,
  messageCount,
  isOwner = false,
}: UseRollingPaperShareProps) {
  const { showSuccess } = useToast();

  const handleKakaoShare = useCallback(async () => {
    if (!nickname) return;

    const { shareRollingPaper, fallbackShare } = await import(
      "@/lib/auth/kakao"
    );

    try {
      const success = await shareRollingPaper(nickname, messageCount);
      if (!success) {
        const url = `${
          window.location.origin
        }/rolling-paper/${encodeURIComponent(nickname)}`;
        fallbackShare(
          url,
          `${nickname}님의 롤링페이퍼`,
          `${nickname}님에게 따뜻한 메시지를 남겨보세요!`
        );
      }
    } catch (error) {
      logger.error("카카오 공유 중 오류 발생:", error);
      await copyRollingPaperLink(nickname, messageCount);
    }
  }, [nickname, messageCount, showSuccess]);

  const fallbackShare = useCallback(
    () => {
      copyRollingPaperLink(nickname, messageCount);
    },
    [nickname, messageCount]
  );

  const handleWebShare = useCallback(async () => {
    const url = isOwner
      ? `${window.location.origin}/rolling-paper/${encodeURIComponent(
          nickname
        )}`
      : window.location.href;

    const shareData = {
      title: `${nickname}님의 롤링페이퍼`,
      text: `${nickname}님에게 익명으로 따뜻한 메시지를 남겨보세요! 현재 ${messageCount}개의 메시지가 있어요`,
      url: url,
    };

    // 네이티브 공유 API 사용 가능한지 확인
    if (
      navigator.share &&
      navigator.canShare &&
      navigator.canShare(shareData)
    ) {
      try {
        await navigator.share(shareData);
      } catch (error) {
        // 사용자가 공유를 취소한 경우는 무시
        if ((error as Error).name !== "AbortError") {
          logger.error("공유 실패:", error);
          fallbackShare();
        }
      }
    } else {
      // 폴백: 클립보드에 복사
      fallbackShare();
    }
  }, [nickname, messageCount, isOwner, fallbackShare]);

  const getShareUrl = useCallback(() => {
    return isOwner
      ? `${window.location.origin}/rolling-paper/${encodeURIComponent(
          nickname
        )}`
      : window.location.href;
  }, [nickname, isOwner]);

  return {
    handleKakaoShare,
    handleWebShare,
    getShareUrl,
  };
}