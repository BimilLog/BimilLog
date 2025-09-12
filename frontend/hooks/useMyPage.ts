import { useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useUserStats } from "@/hooks/useUserStats";
import { useToast } from "@/hooks/useToast";

export function useMyPage() {
  const { user, isAuthenticated, isLoading, updateUserName, refreshUser, logout } = useAuth();
  const router = useRouter();
  const { showSuccess } = useToast();

  const {
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
  } = useUserStats(user);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
      return;
    }
    if (user) {
      fetchUserStats();
    }
  }, [isAuthenticated, isLoading, router, user, fetchUserStats]);

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
        console.error("Failed to update nickname:", error);
      }
    },
    [updateUserName, refreshUser, fetchUserStats, showSuccess, logout]
  );

  return {
    user,
    isLoading,
    isAuthenticated,
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
    handleNicknameChange,
    logout,
  };
}