"use client";

import { useState, useEffect, useCallback } from "react";
import { userQuery, paperQuery, User } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

// ===== USER STATS =====
export interface UserStats {
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
          paperQuery.getMy(),
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
      logger.error("Failed to fetch user stats:", error);
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

// ===== ACTIVITY DATA =====
export interface PaginatedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export interface UseActivityDataOptions {
  fetchData: (page?: number, size?: number) => Promise<PaginatedData<any>>;
}

export function useActivityData({ fetchData }: UseActivityDataOptions) {
  const [items, setItems] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  const loadData = async (page = 0, append = false) => {
    try {
      if (!append) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }
      setError(null);

      const result = await fetchData(page, 10);

      if (append) {
        setItems((prev) => [...prev, ...result.content]);
      } else {
        setItems(result.content);
      }

      setCurrentPage(result.currentPage);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      logger.error("Failed to fetch activity data:", err);
      setError("데이터를 불러오는 중 오류가 발생했습니다.");
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  };

  useEffect(() => {
    loadData(0);
  }, [fetchData]);

  const handleLoadMore = () => {
    if (currentPage < totalPages - 1) {
      loadData(currentPage + 1, true);
    }
  };

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      loadData(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const retry = () => {
    loadData(currentPage);
  };

  return {
    items,
    isLoading,
    error,
    currentPage,
    totalPages,
    totalElements,
    isLoadingMore,
    handleLoadMore,
    handlePageChange,
    retry,
  };
}

