"use client";

import { useState, useCallback, useMemo } from 'react';
import { useRouter } from "next/navigation";
import { paperQuery } from '@/lib/api';
import { useAuth, useToast } from '@/hooks';
import { ErrorHandler } from "@/lib/api/helpers";
import { logger } from '@/lib/utils/logger';
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
  const { showError } = useToast();
  const [searchNickname, setSearchNickname] = useState("");
  const [isSearching, setIsSearching] = useState(false);
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
        // 사용자를 찾을 수 없는 경우: 토스트 메시지 표시
        showError(
          "닉네임이 존재하지 않습니다",
          "해당 닉네임의 롤링페이퍼를 찾을 수 없어요. 회원가입한 사용자의 롤링페이퍼만 존재합니다."
        );
      }
    } catch (error) {
      // 네트워크 에러 등 예상치 못한 에러 발생 시 처리
      logger.error("Search error:", error);
      showError("롤링페이퍼 조회 실패", "롤링페이퍼를 찾을 수 없어요. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsSearching(false); // 성공/실패 여부와 관계없이 로딩 상태 해제
    }
  }, [searchNickname, user, router, showError]);

  // 훅 반환값 메모이제이션 (불필요한 리렌더링 방지)
  const memoizedReturn = useMemo(() => ({
    searchNickname,
    setSearchNickname,
    isSearching,
    handleSearch,
    isOwnNickname,
    confirmOwnNicknameSearch,
    cancelOwnNicknameSearch,
  }), [searchNickname, isSearching, handleSearch, isOwnNickname, confirmOwnNicknameSearch, cancelOwnNicknameSearch]);

  return memoizedReturn;
}

