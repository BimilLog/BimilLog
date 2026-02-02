"use client";

import { useMyPage } from "@/hooks";
import { ProfileCard } from "@/components/molecules";
import { UserStatsSection, UserActivitySection, ProfileBadges } from "@/components/organisms/user";
import { CuteLoadingSpinner } from "@/components";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import type { MyPageDTO } from "@/types";
import type { RollingPaperMessage } from "@/types/domains/paper";

interface MyPageClientProps {
  initialMyPageData?: MyPageDTO | null;
  initialPaperData?: RollingPaperMessage[] | null;
}

export default function MyPageClient({ initialMyPageData, initialPaperData }: MyPageClientProps) {
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
  } = useMyPage({ initialMyPageData, initialPaperData });

  if (isLoading || !user) {
    return (
      <MainLayout
        className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
        containerClassName="container mx-auto px-4"
      >
        <div className="flex items-center justify-center py-16">
          <CuteLoadingSpinner message="사용자 정보를 불러오는 중..." />
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout
      className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50 dark:from-[#121327] dark:via-[#1a1030] dark:to-[#0b0c1c]"
      containerClassName="container mx-auto px-4"
    >
      <div className="py-8">
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

        <ProfileBadges userStats={userStats} />

        <UserActivitySection />
      </div>
    </MainLayout>
  );
}
