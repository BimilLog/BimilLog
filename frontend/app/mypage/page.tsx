"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { userApi, rollingPaperApi } from "@/lib/api";
import { useRouter } from "next/navigation";
import { AuthHeader } from "@/components/organisms/auth-header";

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

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/login");
      return;
    }
    if (user) {
      // Fetch user stats
      Promise.all([
        userApi.getUserPosts(0, 1),
        userApi.getUserComments(0, 1),
        userApi.getUserLikedPosts(0, 1),
        userApi.getUserLikedComments(0, 1),
        rollingPaperApi.getMyRollingPaper(),
      ])
        .then(
          ([
            postsRes,
            commentsRes,
            likedPostsRes,
            likedCommentsRes,
            messagesRes,
          ]) => {
            setUserStats({
              totalPosts: postsRes.data?.totalElements || 0,
              totalComments: commentsRes.data?.totalElements || 0,
              totalLikedPosts: likedPostsRes.data?.totalElements || 0,
              totalLikedComments: likedCommentsRes.data?.totalElements || 0,
              totalMessages: messagesRes.data?.length || 0,
            });
          }
        )
        .catch((err) => console.error("Failed to fetch user stats:", err));
    }
  }, [isAuthenticated, isLoading, router, user]);

  const handleNicknameChange = async (newNickname: string) => {
    await updateUserName(newNickname);
    await refreshUser();
  };

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
        <UserStats stats={userStats} />
        <ActivityTabs />
      </main>
    </div>
  );
}
