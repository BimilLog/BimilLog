"use client";

import { useState, useCallback, useMemo } from 'react';
import type { DecoType } from '@/types/domains/paper';

// Import TanStack Query hooks for local usage
import {
  useCreateRollingPaperMessage,
  useDeleteRollingPaperMessage
} from '@/hooks/api/useRollingPaperMutations';

// 롤링페이퍼 액션 Hook - 메시지 작성/삭제/선택 기능만 제공
export function useRollingPaperActions(userName: string) {
  const [selectedMessages, setSelectedMessages] = useState<number[]>([]);
  const createMessageMutation = useCreateRollingPaperMessage();
  const deleteMessageMutation = useDeleteRollingPaperMessage();

  // 메시지 작성 - Promise 반환
  const handleCreateMessage = useCallback((messageData: {
    userName: string;
    content: string;
    anonymity: string;
    decoType: DecoType;
    x: number;
    y: number;
  }): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMessageMutation.mutate({
        userName: messageData.userName,
        message: {
          decoType: messageData.decoType,
          anonymity: messageData.anonymity,
          content: messageData.content,
          x: messageData.x,
          y: messageData.y
        }
      }, {
        onSuccess: () => {
          resolve();
        },
        onError: (error) => {
          reject(error);
        }
      });
    });
  }, [createMessageMutation]);

  // 메시지 삭제
  const handleDeleteMessage = useCallback((messageId: number) => {
    deleteMessageMutation.mutate(messageId, {
      onSuccess: () => {
        setSelectedMessages(prev => prev.filter(id => id !== messageId));
      }
    });
  }, [deleteMessageMutation]);

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
    handleCreateMessage,
    handleDeleteMessage,
    toggleMessageSelection,
    selectedMessages,
    isCreating: createMessageMutation.isPending,
    isDeleting: deleteMessageMutation.isPending
  };
}

// ===== ROLLING PAPER SEARCH =====
interface UseRollingPaperSearchReturn {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  handleSearch: () => void;
}

/**
 * 롤링페이퍼 검색 및 navigation을 담당하는 Hook
 */
export function useRollingPaperSearch(): UseRollingPaperSearchReturn {
  const [searchNickname, setSearchNickname] = useState("");

  const handleSearch = useCallback(() => {
    const trimmed = searchNickname.trim();
    if (trimmed.length === 0) {
      return;
    }

    if (trimmed !== searchNickname) {
      setSearchNickname(trimmed);
    }
  }, [searchNickname]);

  return useMemo(() => ({
    searchNickname,
    setSearchNickname,
    isSearching: false,
    handleSearch,
  }), [searchNickname, handleSearch]);
}


