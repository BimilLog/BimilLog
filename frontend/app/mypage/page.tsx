"use client";

import { useState, useEffect, useCallback } from "react";
import { useAuth } from "@/hooks/useAuth";
import { userApi, rollingPaperApi } from "@/lib/api";
import { useRouter } from "next/navigation";
import { AuthHeader } from "@/components/organisms/auth-header";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";

// 분리된 컴포넌트들 import
import { UserProfile } from "./components/UserProfile";
import { UserStats } from "./components/UserStats";
import { ActivityTabs } from "./components/ActivityTabs";

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

  const [userStats, setUserStats] = useState({
    totalMessages: 0,
    totalPosts: 0,
    totalComments: 0,
    totalLikedPosts: 0,
    totalLikedComments: 0,
  });
  const [isLoadingStats, setIsLoadingStats] = useState(false);
  const [statsError, setStatsError] = useState<string | null>(null);

  // 사용자 통계 가져오기
  const fetchUserStats = useCallback(async () => {
    if (!user) return;

    setIsLoadingStats(true);
    setStatsError(null);

    try {
      // API 호출을 개선하여 오류에 더 관대하게 처리
      const [
        postsRes,
        commentsRes, 
        likedPostsRes,
        likedCommentsRes,
        messagesRes,
      ] = await Promise.allSettled([
        userApi.getUserPosts(0, 1),
        userApi.getUserComments(0, 1), 
        userApi.getUserLikedPosts(0, 1),
        userApi.getUserLikedComments(0, 1),
        rollingPaperApi.getMyRollingPaper(),
      ]);

      const newStats = {
        totalPosts: 
          postsRes.status === 'fulfilled' && postsRes.value.success
            ? postsRes.value.data?.totalElements || 0
            : 0,
        totalComments:
          commentsRes.status === 'fulfilled' && commentsRes.value.success
            ? commentsRes.value.data?.totalElements || 0
            : 0,
        totalLikedPosts:
          likedPostsRes.status === 'fulfilled' && likedPostsRes.value.success
            ? likedPostsRes.value.data?.totalElements || 0
            : 0,
        totalLikedComments:
          likedCommentsRes.status === 'fulfilled' && likedCommentsRes.value.success
            ? likedCommentsRes.value.data?.totalElements || 0
            : 0,
        totalMessages:
          messagesRes.status === 'fulfilled' && messagesRes.value.success
            ? messagesRes.value.data?.length || 0
            : 0,
      };

      setUserStats(newStats);

      // 모든 API가 실패한 경우에만 에러로 처리
      const allFailed = [postsRes, commentsRes, likedPostsRes, likedCommentsRes, messagesRes]
        .every(res => res.status === 'rejected');
      
      if (allFailed) {
        setStatsError("통계 정보를 불러오는데 실패했습니다. 새로고침 후 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("Failed to fetch user stats:", error);
      setStatsError("통계 정보를 불러오는데 실패했습니다. 새로고침 후 다시 시도해주세요.");
    } finally {
      setIsLoadingStats(false);
    }
  }, [user]);

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
      // 닉네임 변경 후 통계도 다시 가져오기
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
        <UserProfile
          user={user}
          onNicknameChange={handleNicknameChange}
          onLogout={logout}
        />
        <UserStats 
          stats={userStats} 
          isLoading={isLoadingStats}
          error={statsError}
          onRetry={fetchUserStats}
        />
        <ActivityTabs />
      </main>

      {/* Footer */}
      <HomeFooter />
    </div>
  );
}
