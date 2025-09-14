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

  // 그리드 데이터 구성: 10x6 그리드에 메시지를 배치
  const gridData = useMemo(() => {
    const messages = data?.data || [];
    const grid: (VisitMessage | null)[][] = [];
    const rows = 10;
    const cols = 6;

    // 빈 그리드 초기화: null로 채워진 2차원 배열 생성
    for (let i = 0; i < rows; i++) {
      grid[i] = new Array(cols).fill(null);
    }

    // 메시지를 그리드에 배치: API의 1-based 좌표를 0-based 배열 인덱스로 변환
    messages.forEach(message => {
      const rowIndex = message.y - 1; // y(1-based)를 0-based rowIndex로 변환
      const colIndex = message.x - 1; // x(1-based)를 0-based colIndex로 변환
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
        x: messageData.colIndex + 1, // 좌표 변환: 0-based UI 인덱스를 1-based API 좌표로 변환
        y: messageData.rowIndex + 1  // 좌표 변환: 0-based UI 인덱스를 1-based API 좌표로 변환
      }
    }, {
      onSuccess: () => {
        setIsMessageFormOpen(false);
        setSelectedPosition(null);
      }
    });
  }, [createMessageMutation]);

  // 메시지 삭제: 선택된 메시지들을 개별적으로 순차 삭제
  const handleDeleteMessages = useCallback(async () => {
    if (selectedMessages.length === 0) return;

    // 각 메시지를 개별적으로 삭제 (배치 삭제 API가 없으므로 순차 처리)
    for (const messageId of selectedMessages) {
      deleteMessageMutation.mutate(messageId, {
        onSuccess: () => {
          setSelectedMessages(prev => prev.filter(id => id !== messageId));
        }
      });
    }
  }, [deleteMessageMutation, selectedMessages]);

  // 위치 선택: 그리드에서 빈 공간 클릭 시 메시지 작성 폼 열기
  const handleSelectPosition = useCallback((row: number, col: number) => {
    // 이미 메시지가 있는 위치인지 확인 (중복 배치 방지)
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
    // 빈 문자열 검색 방지
    if (!searchNickname.trim()) return;

    const trimmedNickname = searchNickname.trim();
    setIsSearching(true); // 검색 상태 활성화 (UI에서 로딩 표시)
    setSearchError(""); // 기존 에러 메시지 초기화

    try {
      // 자신의 롤링페이퍼 검색 시 API 호출 없이 직접 리다이렉트 (성능 최적화)
      if (user && user.userName === trimmedNickname) {
        // 자신의 롤링페이퍼로 리다이렉트
        router.push("/rolling-paper");
        setIsSearching(false);
        return;
      }

      // API 호출로 다른 사용자의 롤링페이퍼 조회 시도
      const response = await paperQuery.getByUserName(trimmedNickname);

      if (response.success) {
        // 성공 시 해당 사용자의 롤링페이지 페이지로 라우팅
        router.push(`/rolling-paper/${encodeURIComponent(trimmedNickname)}`);
      } else {
        // 사용자를 찾을 수 없는 경우: 에러 메시지 표시
        const appError = ErrorHandler.mapApiError(new Error(response.error || "사용자를 찾을 수 없습니다"));
        setSearchError(
          appError.userMessage ||
          "해당 닉네임의 롤링페이퍼를 찾을 수 없어요. 회원가입한 사용자의 롤링페이퍼만 존재합니다."
        );
      }
    } catch (error) {
      // 네트워크 에러 등 예상치 못한 에러 발생 시 처리
      logger.error("Search error:", error);
      const appError = ErrorHandler.mapApiError(error);
      setSearchError(appError.userMessage || "롤링페이퍼를 찾을 수 없어요.");
    } finally {
      setIsSearching(false); // 성공/실패 여부와 관계없이 로딩 상태 해제
    }
  }, [searchNickname, user, router]);

  // Enter 키 누를 시 검색 실행 (사용성 개선)
  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  }, [handleSearch]);

  // 닉네임 입력 시 기존 에러 메시지 자동 제거 (사용자 경험 개선)
  const setSearchNicknameWithClear = useCallback((nickname: string) => {
    setSearchNickname(nickname);
    if (searchError) {
      setSearchError(""); // 새로 입력할 때 에러 메시지 자동 제거
    }
  }, [searchError]);

  // 훅 반환값 메모이제이션 (불필요한 리렌더링 방지)
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
      // 카카오 공유 실패 시 브라우저 내장 공유 API로 폴백
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
      // 최종 폴백: 클립보드 복사
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

    // 네이티브 공유 API 지원 여부 확인 (모바일 브라우저 주로 지원)
    if (
      navigator.share &&
      navigator.canShare &&
      navigator.canShare(shareData)
    ) {
      try {
        await navigator.share(shareData);
      } catch (error) {
        // 사용자가 공유를 취소한 경우는 무시 (AbortError)
        if ((error as Error).name !== "AbortError") {
          logger.error("공유 실패:", error);
          fallbackShare();
        }
      }
    } else {
      // 네이티브 공유 미지원 시 폴백: 클립보드에 복사
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