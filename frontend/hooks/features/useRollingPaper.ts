"use client";

import { useState, useCallback, useMemo } from 'react';
import { paperQuery, paperCommand } from '@/lib/api';
import { useApiQuery } from '@/hooks/api/useApiQuery';
import { useApiMutation } from '@/hooks/api/useApiMutation';
import type { RollingPaperMessage, VisitMessage, DecoType } from '@/types/domains/paper';

// 롤링페이퍼 메시지 조회
export function useRollingPaper(userName: string) {
  const { data, isLoading, refetch } = useApiQuery(
    () => paperQuery.getByUserName(userName),
    {
      enabled: !!userName,
      cacheTime: 10 * 60 * 1000, // 10분 캐싱
      staleTime: 5 * 60 * 1000 // 5분 후 stale
    }
  );

  // 그리드 데이터 구성
  const gridData = useMemo(() => {
    const messages = data || [];
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

  return {
    messages: data || [],
    gridData,
    isLoading,
    refetch
  };
}

// 롤링페이퍼 메시지 작성
export function useCreateRollingPaperMessage() {
  return useApiMutation(
    ({ userName, message }: {
      userName: string;
      message: {
        decoType: DecoType;
        anonymity: string;
        content: string;
        x: number;
        y: number;
      }
    }) => paperCommand.createMessage(userName, message),
    {
      showSuccessToast: true,
      successMessage: '메시지가 작성되었습니다.'
    }
  );
}

// 롤링페이퍼 메시지 삭제
export function useDeleteRollingPaperMessage() {
  return useApiMutation(
    (messageId: number) => paperCommand.deleteMessage(messageId),
    {
      showSuccessToast: true,
      successMessage: '메시지가 삭제되었습니다.'
    }
  );
}

// 롤링페이퍼 통합 Hook
export function useRollingPaperActions(userName: string) {
  const [selectedPosition, setSelectedPosition] = useState<{ row: number; col: number } | null>(null);
  const [isMessageFormOpen, setIsMessageFormOpen] = useState(false);
  const [selectedMessages, setSelectedMessages] = useState<number[]>([]);
  
  const { messages, gridData, isLoading, refetch } = useRollingPaper(userName);
  const { mutate: createMessage, isLoading: isCreating } = useCreateRollingPaperMessage();
  const { mutate: deleteMessages, isLoading: isDeleting } = useDeleteRollingPaperMessage();

  // 메시지 작성
  const handleCreateMessage = useCallback(async (data: {
    userName: string;
    content: string;
    anonymity: string;
    decoType: DecoType;
    rowIndex: number;
    colIndex: number;
  }) => {
    await createMessage({
      userName: data.userName,
      message: {
        decoType: data.decoType,
        anonymity: data.anonymity,
        content: data.content,
        x: data.colIndex + 1, // 0-based to 1-based
        y: data.rowIndex + 1  // 0-based to 1-based
      }
    });
    await refetch();
    setIsMessageFormOpen(false);
    setSelectedPosition(null);
  }, [createMessage, refetch]);

  // 메시지 삭제
  const handleDeleteMessages = useCallback(async () => {
    if (selectedMessages.length === 0) return;

    // 각 메시지를 개별적으로 삭제
    for (const messageId of selectedMessages) {
      await deleteMessages(messageId);
    }
    await refetch();
    setSelectedMessages([]);
  }, [deleteMessages, selectedMessages, refetch]);

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
    messages,
    gridData,
    isLoading,
    isCreating,
    isDeleting,
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