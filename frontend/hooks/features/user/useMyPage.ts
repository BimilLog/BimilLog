"use client";

import { useEffect, useCallback } from "react";
import { useAuth, useToast } from "@/hooks";
import { useUserStats } from "../useUser";
import { logger } from '@/lib/utils/logger';

export function useMyPage() {
  const { user, isLoading, updateUserName, refreshUser, logout } = useAuth();
  const { showSuccess } = useToast();

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

  const handleNicknameChange = useCallback(
    async (newNickname: string) => {
      try {
        await updateUserName(newNickname);
        await refreshUser();
        await fetchUserStats();

        showSuccess(
          "닉네임 변경 완료",
          "닉네임이 성공적으로 변경되었습니다. 3초 후 재로그인 페이지로 이동합니다."
        );

        setTimeout(async () => {
          await logout();
        }, 3000);
      } catch (error) {
        logger.error("Failed to update nickname:", error);
      }
    },
    [updateUserName, refreshUser, fetchUserStats, showSuccess, logout]
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