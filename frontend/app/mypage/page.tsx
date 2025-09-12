"use client";

import { useMyPage } from "@/hooks/useMyPage";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
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
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
        <LoadingSpinner
          variant="gradient"
          message="사용자 정보를 불러오는 중..."
        />
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
