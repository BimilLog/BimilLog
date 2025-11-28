"use client";

import { useMemo } from "react";
import { Member } from "@/lib/api";
import { useMyPageInfo } from "@/hooks/api/useMyPageQueries";
import { useMyRollingPaper } from "@/hooks/api/useMyRollingPaper";

// ===== USER STATS =====
interface UserStats {
  totalMessages: number;
  totalPosts: number;
  totalComments: number;
  totalLikedPosts: number;
  totalLikedComments: number;
}

export function useUserStats(user: Member | null) {
  // 마이페이지 통합 API 호출 (1번의 호출로 모든 활동 데이터 조회)
  const {
    data: mypageData,
    isLoading: isLoadingMypage,
    error: mypageError,
    refetch: fetchUserStats,
  } = useMyPageInfo(0, 1); // page=0, size=1로 totalElements만 조회

  // 롤링페이퍼 데이터 (마이페이지 API와 별도)
  const { data: myPaperData, isLoading: isLoadingPaper, isError: isPaperError } = useMyRollingPaper();

  // 통계 계산 (메모이제이션)
  const userStats = useMemo<UserStats>(() => {
    if (!mypageData?.data) {
      return {
        totalPosts: 0,
        totalComments: 0,
        totalLikedPosts: 0,
        totalLikedComments: 0,
        totalMessages: 0,
      };
    }

    const { memberActivityPost, memberActivityComment } = mypageData.data;

    return {
      totalPosts: memberActivityPost.writePosts.totalElements,
      totalComments: memberActivityComment.writeComments.totalElements,
      totalLikedPosts: memberActivityPost.likedPosts.totalElements,
      totalLikedComments: memberActivityComment.likedComments.totalElements,
      totalMessages: myPaperData?.success ? (myPaperData.data?.length || 0) : 0,
    };
  }, [mypageData, myPaperData]);

  // 로딩 상태
  const isLoadingStats = isLoadingMypage || isLoadingPaper;

  // 에러 상태
  const statsError = mypageError ? "통계 정보를 불러오는데 실패했습니다. 새로고침 후 다시 시도해주세요." : null;

  // 부분 에러 (롤링페이퍼만 실패한 경우)
  const partialErrors: string[] = [];
  if (isPaperError) {
    partialErrors.push("롤링페이퍼 메시지 정보를 불러오지 못했습니다");
  }

  return {
    userStats,
    isLoadingStats,
    statsError,
    partialErrors,
    fetchUserStats,
  };
}

export type { UserStats };
