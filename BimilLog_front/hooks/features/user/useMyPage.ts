"use client";

import { useEffect, useCallback } from "react";
import { useAuth } from "@/hooks";
import { useUserStats } from "./useUserStats";
import { logger } from '@/lib/utils/logger';
import type { MyPageDTO } from "@/types";
import type { MyPaperDTO } from "@/types/domains/paper";

interface UseMyPageOptions {
  initialMyPageData?: MyPageDTO | null;
  initialPaperData?: MyPaperDTO | null;
}

export function useMyPage(options?: UseMyPageOptions) {
  const { user, isLoading, updateUserName, logout } = useAuth();

  const {
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
  } = useUserStats(user, {
    initialMyPageData: options?.initialMyPageData,
    initialPaperData: options?.initialPaperData,
  });

  useEffect(() => {
    if (user) {
      fetchUserStats();
    }
  }, [user, fetchUserStats]);

  // 닉네임 변경 처리: B-301 — auth store(useAuthStore.updateUserName) 가 단일 경로로
  // updateUserNameAction 을 호출하고 사용자 상태를 갱신한다. ProfileCard 의 직접 호출과
  // 콜백이 분리되어 발생하던 더블 POST 를 차단.
  const handleNicknameChange = useCallback(
    async (newNickname: string): Promise<boolean> => {
      try {
        return await updateUserName(newNickname);
      } catch (error) {
        logger.error("Failed to update nickname:", error);
        return false;
      }
    },
    [updateUserName]
  );

  return {
    user,
    isLoading,
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
    handleNicknameChange,
    logout,
  };
}
