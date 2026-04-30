"use client";

import { useState, useCallback, useMemo, useEffect } from 'react';
import type { DecoType } from '@/types/domains/paper';
import { useDebounce } from '@/hooks/common/useDebounce';

// Server Actions 훅 사용
import {
  useCreateMessageAction,
  useDeleteMessageAction
} from '@/hooks/actions/usePaperActions';

// 롤링페이퍼 액션 Hook - 메시지 작성/삭제/선택 기능만 제공
export function useRollingPaperActions(userName: string) {
  const [selectedMessages, setSelectedMessages] = useState<number[]>([]);
  const { createMessage, isPending: isCreating } = useCreateMessageAction();
  const { deleteMessage, isPending: isDeleting } = useDeleteMessageAction();

  // 메시지 작성 - Promise 반환
  const handleCreateMessage = useCallback((messageData: {
    ownerId: number;
    ownerName?: string;
    content: string;
    anonymity: string;
    decoType: DecoType;
    x: number;
    y: number;
  }): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMessage(
        {
          ownerId: messageData.ownerId,
          ownerName: messageData.ownerName,
          decoType: messageData.decoType,
          anonymity: messageData.anonymity,
          content: messageData.content,
          x: messageData.x,
          y: messageData.y,
        },
        {
          onSuccess: () => resolve(),
          onError: (error) => reject(new Error(error)),
        }
      );
    });
  }, [createMessage]);

  // 메시지 삭제
  const handleDeleteMessage = useCallback((messageId: number) => {
    deleteMessage(
      { messageId, userName },
      {
        onSuccess: () => {
          setSelectedMessages(prev => prev.filter(id => id !== messageId));
        },
      }
    );
  }, [deleteMessage, userName]);

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
    isCreating,
    isDeleting
  };
}

// ===== ROLLING PAPER SEARCH =====
interface UseRollingPaperSearchReturn {
  /** 사용자가 입력 중인 원본 키워드 (디바운스 전) */
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  /** 디바운스가 적용되어 실제 API 호출에 사용되는 키워드 */
  effectiveKeyword: string;
  /** 검색 진행 여부 (디바운스 대기 또는 실제 fetch 중) */
  isSearching: boolean;
  /** Enter/돋보기 클릭 시 즉시 디바운스 우회하여 검색 (선택) */
  handleSearch: () => void;
}

/**
 * 롤링페이퍼 검색 Hook
 * - 입력값을 300ms 디바운스하여 effectiveKeyword 로 노출 (B-006 / 디바운스 도입)
 * - Enter/돋보기 클릭 시 즉시 effectiveKeyword 갱신 (디바운스 우회)
 * - isSearching 은 디바운스 대기 중일 때 true (사용자가 입력 직후 키워드 동기화 전)
 *   실제 fetch 로딩은 결과 컴포넌트(AllUsersList) 가 자체 isLoading 으로 표시
 */
export function useRollingPaperSearch(): UseRollingPaperSearchReturn {
  const [searchNickname, setSearchNickname] = useState("");
  const [immediateKeyword, setImmediateKeyword] = useState("");

  const trimmed = searchNickname.trim();
  const debouncedKeyword = useDebounce(trimmed, 300);
  // 즉시 검색(Enter/클릭) 키가 디바운스 결과보다 우선
  const effectiveKeyword = immediateKeyword || debouncedKeyword;

  // 디바운스가 immediate 를 따라잡으면 reset
  useEffect(() => {
    if (immediateKeyword && debouncedKeyword === immediateKeyword) {
      setImmediateKeyword("");
    }
  }, [debouncedKeyword, immediateKeyword]);

  const handleChangeNickname = useCallback((nickname: string) => {
    setSearchNickname(nickname);
    // 입력이 변경되면 immediate 우회 해제 → 다시 디바운스 흐름
    setImmediateKeyword((prev) => (prev ? "" : prev));
  }, []);

  const handleSearch = useCallback(() => {
    if (!trimmed) return;
    setImmediateKeyword(trimmed);
  }, [trimmed]);

  // 디바운스 대기 중 = 사용자가 방금 타이핑했고 effective 가 아직 따라잡지 못함
  const isDebouncePending = trimmed.length > 0 && trimmed !== effectiveKeyword;

  return useMemo(() => ({
    searchNickname,
    setSearchNickname: handleChangeNickname,
    effectiveKeyword,
    isSearching: isDebouncePending,
    handleSearch,
  }), [searchNickname, handleChangeNickname, effectiveKeyword, isDebouncePending, handleSearch]);
}


