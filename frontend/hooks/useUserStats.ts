import { useState, useCallback } from "react";
import { userQuery, paperQuery, User } from "@/lib/api";

interface UserStats {
  totalMessages: number;
  totalPosts: number;
  totalComments: number;
  totalLikedPosts: number;
  totalLikedComments: number;
}

export function useUserStats(user: User | null) {
  const [userStats, setUserStats] = useState<UserStats>({
    totalMessages: 0,
    totalPosts: 0,
    totalComments: 0,
    totalLikedPosts: 0,
    totalLikedComments: 0,
  });
  const [isLoadingStats, setIsLoadingStats] = useState(false);
  const [statsError, setStatsError] = useState<string | null>(null);
  const [partialErrors, setPartialErrors] = useState<string[]>([]);

  const fetchUserStats = useCallback(async () => {
    if (!user) return;

    setIsLoadingStats(true);
    setStatsError(null);
    setPartialErrors([]);

    try {
      const [postsRes, commentsRes, likedPostsRes, likedCommentsRes, messagesRes] =
        await Promise.allSettled([
          userQuery.getUserPosts(0, 1),
          userQuery.getUserComments(0, 1),
          userQuery.getUserLikedPosts(0, 1),
          userQuery.getUserLikedComments(0, 1),
          paperQuery.getMyRollingPaper(),
        ]);

      const errors: string[] = [];

      const totalPosts =
        postsRes.status === "fulfilled" && postsRes.value.success
          ? postsRes.value.data?.totalElements || 0
          : (() => {
              errors.push("작성한 글 정보를 불러오지 못했습니다");
              return 0;
            })();

      const totalComments =
        commentsRes.status === "fulfilled" && commentsRes.value.success
          ? commentsRes.value.data?.totalElements || 0
          : (() => {
              errors.push("작성한 댓글 정보를 불러오지 못했습니다");
              return 0;
            })();

      const totalLikedPosts =
        likedPostsRes.status === "fulfilled" && likedPostsRes.value.success
          ? likedPostsRes.value.data?.totalElements || 0
          : (() => {
              errors.push("추천한 글 정보를 불러오지 못했습니다");
              return 0;
            })();

      const totalLikedComments =
        likedCommentsRes.status === "fulfilled" && likedCommentsRes.value.success
          ? likedCommentsRes.value.data?.totalElements || 0
          : (() => {
              errors.push("추천한 댓글 정보를 불러오지 못했습니다");
              return 0;
            })();

      const totalMessages =
        messagesRes.status === "fulfilled" && messagesRes.value.success
          ? messagesRes.value.data?.length || 0
          : (() => {
              errors.push("롤링페이퍼 메시지 정보를 불러오지 못했습니다");
              return 0;
            })();

      const newStats = {
        totalPosts,
        totalComments,
        totalLikedPosts,
        totalLikedComments,
        totalMessages,
      };

      setUserStats(newStats);

      if (errors.length > 0) {
        setPartialErrors(errors);
      }

      const allFailed = [postsRes, commentsRes, likedPostsRes, likedCommentsRes, messagesRes].every(
        (res) => res.status === "rejected"
      );

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

  return {
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
  };
}