"use client";

import { useState, useCallback, useMemo } from 'react';
import { useRouter } from "next/navigation";
import { paperQuery } from '@/lib/api';
import { useAuth } from '@/hooks';
import { ErrorHandler } from "@/lib/api/helpers";
import { logger } from '@/lib/utils/logger';
import type { DecoType } from '@/types/domains/paper';

// TanStack Query Hooks re-exports
export { useRollingPaper } from '@/hooks/api/useRollingPaperQueries';
export {
  useCreateRollingPaperMessage,
  useDeleteRollingPaperMessage
} from '@/hooks/api/useRollingPaperMutations';

// Import for local usage
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
      console.log('[useRollingPaper] 메시지 작성 요청:', messageData);

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
          console.log('[useRollingPaper] 메시지 작성 성공');
          resolve();
        },
        onError: (error) => {
          console.error('[useRollingPaper] 메시지 작성 실패:', error);
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
  searchError: string;
  handleSearch: () => Promise<void>;
  isOwnNickname: boolean;
  confirmOwnNicknameSearch: () => void;
  cancelOwnNicknameSearch: () => void;
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
  const [isOwnNickname, setIsOwnNickname] = useState(false);

  // 자신의 닉네임 검색 확인 후 리다이렉트
  const confirmOwnNicknameSearch = useCallback(() => {
    setIsOwnNickname(false);
    router.push("/rolling-paper");
  }, [router]);

  // 자신의 닉네임 검색 취소 (다른 롤링페이퍼 찾기)
  const cancelOwnNicknameSearch = useCallback(() => {
    setIsOwnNickname(false);
  }, []);

  const handleSearch = useCallback(async () => {
    // 빈 문자열 검색 방지
    if (!searchNickname.trim()) return;

    const trimmedNickname = searchNickname.trim();
    setIsSearching(true); // 검색 상태 활성화 (UI에서 로딩 표시)
    setSearchError(""); // 기존 에러 메시지 초기화
    setIsOwnNickname(false); // 이전 상태 초기화

    try {
      // 자신의 롤링페이퍼 검색 시 확인 다이얼로그 표시
      if (user && user.memberName === trimmedNickname) {
        setIsOwnNickname(true);
        setIsSearching(false);
        return;
      }

      // API 호출로 다른 사용자의 롤링페이퍼 조회 시도
      const response = await paperQuery.getByUserName(trimmedNickname);

      if (response.success) {
        // 성공 시 해당 사용자의 롤링페이지 페이지로 라우팅
        router.push(`/rolling-paper/${trimmedNickname}`);
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
    isOwnNickname,
    confirmOwnNicknameSearch,
    cancelOwnNicknameSearch,
  }), [searchNickname, setSearchNicknameWithClear, isSearching, searchError, handleSearch, isOwnNickname, confirmOwnNicknameSearch, cancelOwnNicknameSearch]);

  return memoizedReturn;
}

