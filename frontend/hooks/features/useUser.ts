"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth, useToast } from "@/hooks";
import { userQuery, userCommand, paperQuery, authCommand, User, Setting } from "@/lib/api";
import { logger } from '@/lib/utils/logger';

// ===== USER STATS =====
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
interface PaginatedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

interface UseActivityDataOptions {
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

// ===== SETTINGS =====
export function useSettings() {
  const [settings, setSettings] = useState<Setting | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showSuccess, showError } = useToast();
  const router = useRouter();
  const isMounted = useRef(true);

  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await userQuery.getSettings();
      if (!isMounted.current) return;

      if (response.success && response.data) {
        setSettings(response.data);
      } else {
        const errorMessage = response.error || "설정을 불러오는 중 오류가 발생했습니다.";
        setError(errorMessage);
        showError("설정 로드 실패", errorMessage);
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("설정 로드 실패:", error);
      const errorMessage = "설정을 불러오는 중 오류가 발생했습니다. 페이지를 새로고침해주세요.";
      setError(errorMessage);
      showError("설정 로드 실패", errorMessage);
    } finally {
      if (isMounted.current) {
        setLoading(false);
      }
    }
  };

  const updateSettings = async (newSettings: Partial<Setting>) => {
    if (!settings || !isMounted.current) return;

    const fullSettings: Setting = {
      ...settings,
      ...newSettings,
    };

    try {
      setSaving(true);
      const response = await userCommand.updateSettings(fullSettings);
      if (!isMounted.current) return;

      if (response.success) {
        setSettings(fullSettings);
        showSuccess("설정 저장 완료", "알림 설정이 성공적으로 저장되었습니다.");
      } else {
        showError(
          "설정 저장 실패",
          response.error || "설정 저장 중 오류가 발생했습니다. 다시 시도해주세요."
        );
        setSettings(settings);
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("설정 저장 실패:", error);
      showError(
        "설정 저장 실패",
        "설정 저장 중 오류가 발생했습니다. 다시 시도해주세요."
      );
      setSettings(settings);
    } finally {
      if (isMounted.current) {
        setSaving(false);
      }
    }
  };

  const handleSingleToggle = (
    field: keyof Pick<
      Setting,
      "messageNotification" | "commentNotification" | "postFeaturedNotification"
    >,
    value: boolean
  ) => {
    updateSettings({ [field]: value });
  };

  const handleAllToggle = (enabled: boolean) => {
    updateSettings({
      messageNotification: enabled,
      commentNotification: enabled,
      postFeaturedNotification: enabled,
    });
  };

  const handleWithdraw = async () => {
    if (
      !window.confirm(
        "정말로 탈퇴하시겠습니까?\n\n탈퇴 시 모든 데이터가 삭제되며, 복구할 수 없습니다.\n작성한 게시글과 댓글, 롤링페이퍼 메시지가 모두 삭제됩니다.\n\n이 작업은 되돌릴 수 없습니다."
      )
    ) {
      return;
    }

    try {
      setWithdrawing(true);
      const response = await authCommand.withdraw();
      if (!isMounted.current) return;

      if (response.success) {
        showSuccess("회원탈퇴 완료", "회원탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사했습니다.");
        setTimeout(() => {
          if (isMounted.current) {
            router.push("/");
            window.location.reload();
          }
        }, 2000);
      } else {
        showError(
          "회원탈퇴 실패",
          response.error || "회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요."
        );
      }
    } catch (error) {
      if (!isMounted.current) return;
      logger.error("회원탈퇴 실패:", error);
      showError(
        "회원탈퇴 실패",
        error instanceof Error
          ? error.message
          : "회원탈퇴 중 오류가 발생했습니다. 다시 시도해주세요."
      );
    } finally {
      if (isMounted.current) {
        setWithdrawing(false);
      }
    }
  };

  const allEnabled = Boolean(
    settings &&
      settings.messageNotification === true &&
      settings.commentNotification === true &&
      settings.postFeaturedNotification === true
  );

  return {
    settings,
    loading,
    saving,
    withdrawing,
    error,
    allEnabled,
    updateSettings,
    handleSingleToggle,
    handleAllToggle,
    handleWithdraw,
    loadSettings,
  };
}

// ===== MY PAGE =====
export function useMyPage() {
  const { user, isLoading, updateUserName, refreshUser, logout } = useAuth();
  const { showSuccess } = useToast();

  const {
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
  } = useUserStats(user);

  useEffect(() => {
    if (user) {
      fetchUserStats();
    }
  }, [user, fetchUserStats]);

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
        logger.error("Failed to update nickname:", error);
      }
    },
    [updateUserName, refreshUser, fetchUserStats, showSuccess, logout]
  );

  return {
    user,
    isLoading,
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
    handleNicknameChange,
    logout,
  };
}