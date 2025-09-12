"use client";

import { useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import { useUserStats } from "@/hooks/useUserStats";
import { useRouter } from "next/navigation";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import { ProfileCard } from "@/components/molecules";
import { UserStatsSection, UserActivitySection } from "@/components/organisms/user";

export default function MyPage() {
  const {
    user,
    isAuthenticated,
    isLoading,
    updateUserName,
    refreshUser,
    logout,
  } = useAuth();
  const router = useRouter();

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

  const handleNicknameChange = useCallback(async (newNickname: string) => {
    try {
      await updateUserName(newNickname);
      await refreshUser();
      await fetchUserStats();
    } catch (error) {
      console.error("Failed to update nickname:", error);
    }
  }, [updateUserName, refreshUser, fetchUserStats]);

  if (isLoading || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <p>사용자 정보를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />
      <main className="container mx-auto px-4 py-8">
        <ProfileCard
          user={user}
          onNicknameChange={handleNicknameChange}
          onLogout={logout}
        />
        <UserStatsSection 
          stats={userStats} 
          isLoading={isLoadingStats}
          error={statsError}
          partialErrors={partialErrors}
          onRetry={fetchUserStats}
        />
        <UserActivitySection />
      </main>
      <HomeFooter />
    </div>
  );
}
