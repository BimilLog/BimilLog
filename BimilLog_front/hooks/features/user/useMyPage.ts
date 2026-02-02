"use client";

import { useEffect, useCallback } from "react";
import { useAuth } from "@/hooks";
import { useUserStats } from "./useUserStats";
import { logger } from '@/lib/utils/logger';
import type { MyPageDTO } from "@/types";
import type { RollingPaperMessage } from "@/types/domains/paper";

interface UseMyPageOptions {
  initialMyPageData?: MyPageDTO | null;
  initialPaperData?: RollingPaperMessage[] | null;
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

  // 닉네임 변경 처리: 닉네임 업데이트 (ProfileCard에서 성공 메시지와 새로고침 처리)
  const handleNicknameChange = useCallback(
    async (newNickname: string) => {
      try {
        await updateUserName(newNickname);
      } catch (error) {
        logger.error("Failed to update nickname:", error);
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
