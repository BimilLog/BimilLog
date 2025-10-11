"use client";

import { useEffect, useCallback } from "react";
import { useAuth } from "@/hooks";
import { useUserStats } from "./useUserStats";
import { logger } from '@/lib/utils/logger';

export function useMyPage() {
  const { user, isLoading, updateUserName, logout } = useAuth();

  const {
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
  } = useUserStats(user);

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
        // ProfileCard에서 페이지 새로고침을 처리하므로 여기서는 추가 작업 불필요
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