"use client";

import { useMyPage } from "@/hooks";
import { ProfileCard } from "@/components/molecules";
import { UserStatsSection, UserActivitySection, BookmarkSection, ActivityInsights, ProfileBadges } from "@/components/organisms/user";
import { CuteLoadingSpinner } from "@/components";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";

export default function MyPage() {
  const {
    user,
    isLoading,
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
    handleNicknameChange,
    logout,
  } = useMyPage();

  if (isLoading || !user) {
    return (
      <MainLayout containerClassName="container mx-auto px-4">
        <div className="flex items-center justify-center py-16">
          <CuteLoadingSpinner message="사용자 정보를 불러오는 중..." />
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout containerClassName="container mx-auto px-4 py-8">
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

      <BookmarkSection />

      <ProfileBadges />

      <ActivityInsights />

      <UserActivitySection />
    </MainLayout>
  );
}