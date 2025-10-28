"use client";

import { useState, useEffect, useCallback } from 'react';
import {
  getUnlockedBadges,
  getBadgeProgress,
  getNextAchievableBadges,
  getBadgeStats,
  groupBadgesByCategory,
  syncBadgeProgressFromBackendStats,
  clearBadgeData,
  ALL_BADGES,
  type Badge,
  type BadgeProgress,
} from '@/lib/utils/badges';
import type { UserStats } from './user/useUserStats';

export function useBadges(userStats?: UserStats | null, receivedPaperCount?: number) {
  const [unlockedBadges, setUnlockedBadges] = useState<Badge[]>([]);
  const [allBadges, setAllBadges] = useState<Badge[]>(ALL_BADGES);
  const [badgeProgress, setBadgeProgress] = useState<BadgeProgress[]>([]);
  const [nextBadges, setNextBadges] = useState<Badge[]>([]);
  const [stats, setStats] = useState<ReturnType<typeof getBadgeStats> | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 데이터 로드
  const loadBadgeData = useCallback(() => {
    setIsLoading(true);
    try {
      // 백엔드 통계로부터 진행도 동기화
      if (userStats) {
        syncBadgeProgressFromBackendStats(userStats, receivedPaperCount);
      }

      // 뱃지 데이터 로드
      const unlocked = getUnlockedBadges();
      const progress = getBadgeProgress();
      const next = getNextAchievableBadges(5);
      const badgeStats = getBadgeStats();

      // 전체 뱃지에 진행도 정보 추가
      const badgesWithProgress = ALL_BADGES.map(badge => {
        const unlockedBadge = unlocked.find(u => u.id === badge.id);
        const progressData = progress.find(p => p.badgeId === badge.id);

        return {
          ...badge,
          unlockedAt: unlockedBadge?.unlockedAt,
          progress: progressData?.percentage || 0,
          requirement: {
            ...badge.requirement,
            current: progressData?.current || 0,
          },
        };
      });

      setUnlockedBadges(unlocked);
      setAllBadges(badgesWithProgress);
      setBadgeProgress(progress);
      setNextBadges(next);
      setStats(badgeStats);
    } catch (error) {
      console.error('Failed to load badge data:', error);
    } finally {
      setIsLoading(false);
    }
  }, [userStats, receivedPaperCount]);

  // 카테고리별 그룹화된 뱃지
  const groupedBadges = useCallback(() => {
    return groupBadgesByCategory(allBadges);
  }, [allBadges]);

  // 카테고리별 잠금 해제된 뱃지
  const groupedUnlockedBadges = useCallback(() => {
    return groupBadgesByCategory(unlockedBadges);
  }, [unlockedBadges]);

  // 특정 뱃지 정보 가져오기
  const getBadgeById = useCallback((badgeId: string) => {
    return allBadges.find(b => b.id === badgeId);
  }, [allBadges]);

  // 뱃지 달성률 계산
  const getCompletionRate = useCallback(() => {
    if (allBadges.length === 0) return 0;
    return Math.round((unlockedBadges.length / allBadges.length) * 100);
  }, [unlockedBadges, allBadges]);

  // 티어별 뱃지 수 계산
  const getTierCounts = useCallback(() => {
    const tiers: Badge['tier'][] = ['bronze', 'silver', 'gold', 'platinum', 'diamond'];

    return tiers.map(tier => ({
      tier,
      total: allBadges.filter(b => b.tier === tier).length,
      unlocked: unlockedBadges.filter(b => b.tier === tier).length,
    }));
  }, [allBadges, unlockedBadges]);

  // 최근 획득 뱃지
  const getRecentBadges = useCallback((limit: number = 3) => {
    return [...unlockedBadges]
      .sort((a, b) => {
        if (!a.unlockedAt || !b.unlockedAt) return 0;
        return new Date(b.unlockedAt).getTime() - new Date(a.unlockedAt).getTime();
      })
      .slice(0, limit);
  }, [unlockedBadges]);

  // 데이터 초기화
  const clearData = useCallback(() => {
    if (confirm('모든 뱃지 데이터를 초기화하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      clearBadgeData();
      loadBadgeData();
    }
  }, [loadBadgeData]);

  // 초기 로드
  useEffect(() => {
    loadBadgeData();
  }, [loadBadgeData]);

  return {
    // 데이터
    unlockedBadges,
    allBadges,
    badgeProgress,
    nextBadges,
    stats,
    isLoading,

    // 메서드
    loadBadgeData,
    clearData,
    getBadgeById,

    // 계산된 값
    groupedBadges: groupedBadges(),
    groupedUnlockedBadges: groupedUnlockedBadges(),
    completionRate: getCompletionRate(),
    tierCounts: getTierCounts(),
    recentBadges: getRecentBadges(),
  };
}