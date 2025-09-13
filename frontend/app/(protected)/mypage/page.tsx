"use client";

import { useMyPage } from "@/hooks";
import { ProfileCard } from "@/components/molecules";
import { UserStatsSection, UserActivitySection } from "@/components/organisms/user";
import { LoadingSpinner } from "@/components/atoms";

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
      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-center py-16">
          <LoadingSpinner
            variant="gradient"
            message="사용자 정보를 불러오는 중..."
          />
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
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
    </div>
  );
}