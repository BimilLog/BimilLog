"use client";

import { useState, useEffect, useCallback } from 'react';
import {
  createGoal,
  updateGoal,
  updateGoalProgress,
  deleteGoal,
  getGoals,
  getActiveGoals,
  getCompletedGoals,
  getGoalsByCategory,
  getDailyLogs,
  getDailyLog,
  addDailyReflection,
  getStreaks,
  calculateGrowthStats,
  clearGrowthData,
  exportGrowthData,
  type GrowthGoal,
  type DailyLog,
  type GrowthStats,
  type GrowthStreak,
} from '@/lib/utils/growth-tracker';

export function useGrowthTracker() {
  const [goals, setGoals] = useState<GrowthGoal[]>([]);
  const [activeGoals, setActiveGoals] = useState<GrowthGoal[]>([]);
  const [completedGoals, setCompletedGoals] = useState<GrowthGoal[]>([]);
  const [dailyLogs, setDailyLogs] = useState<DailyLog[]>([]);
  const [todayLog, setTodayLog] = useState<DailyLog | null>(null);
  const [streaks, setStreaks] = useState<GrowthStreak[]>([]);
  const [stats, setStats] = useState<GrowthStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 데이터 로드
  const loadData = useCallback(() => {
    setIsLoading(true);
    try {
      const allGoals = getGoals();
      const active = getActiveGoals();
      const completed = getCompletedGoals();
      const logs = getDailyLogs();
      const today = new Date().toISOString().split('T')[0];
      const todayLogData = getDailyLog(today);
      const streakData = getStreaks();
      const statsData = calculateGrowthStats();

      setGoals(allGoals);
      setActiveGoals(active);
      setCompletedGoals(completed);
      setDailyLogs(logs);
      setTodayLog(todayLogData);
      setStreaks(streakData);
      setStats(statsData);
    } catch (error) {
      console.error('Failed to load growth tracker data:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 새 목표 생성
  const addGoal = useCallback((goalData: Omit<GrowthGoal, 'id' | 'createdAt' | 'updatedAt' | 'currentValue' | 'isCompleted' | 'milestones'>) => {
    const newGoal = createGoal(goalData);
    loadData();
    return newGoal;
  }, [loadData]);

  // 목표 수정
  const editGoal = useCallback((goalId: string, updates: Partial<GrowthGoal>) => {
    updateGoal(goalId, updates);
    loadData();
  }, [loadData]);

  // 진행도 업데이트
  const updateProgress = useCallback((goalId: string, progress: number, note?: string) => {
    updateGoalProgress(goalId, progress, note);
    loadData();
  }, [loadData]);

  // 목표 삭제
  const removeGoal = useCallback((goalId: string) => {
    if (confirm('이 목표를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      deleteGoal(goalId);
      loadData();
    }
  }, [loadData]);

  // 일일 반성 추가
  const addReflection = useCallback((reflection: string, mood?: DailyLog['mood']) => {
    const today = new Date().toISOString().split('T')[0];
    addDailyReflection(today, reflection, mood);
    loadData();
  }, [loadData]);

  // 카테고리별 목표 가져오기
  const getByCategory = useCallback((category: GrowthGoal['category']) => {
    return getGoalsByCategory(category);
  }, []);

  // 진행률 계산
  const getGoalProgress = useCallback((goal: GrowthGoal) => {
    if (goal.targetValue === 0) return 0;
    return Math.min(100, (goal.currentValue / goal.targetValue) * 100);
  }, []);

  // 다음 마일스톤 가져오기
  const getNextMilestone = useCallback((goal: GrowthGoal) => {
    return goal.milestones.find(m => !m.achievedAt);
  }, []);

  // 최근 달성한 마일스톤
  const getRecentAchievements = useCallback((limit: number = 5) => {
    const achievements: { goal: GrowthGoal; milestone: any; achievedAt: string }[] = [];

    goals.forEach(goal => {
      goal.milestones.forEach(milestone => {
        if (milestone.achievedAt) {
          achievements.push({
            goal,
            milestone,
            achievedAt: milestone.achievedAt,
          });
        }
      });
    });

    return achievements
      .sort((a, b) => new Date(b.achievedAt).getTime() - new Date(a.achievedAt).getTime())
      .slice(0, limit);
  }, [goals]);

  // 데이터 내보내기
  const exportData = useCallback(() => {
    const data = exportGrowthData();
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `growth-tracker-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }, []);

  // 데이터 초기화
  const clearData = useCallback(() => {
    if (confirm('모든 성장 목표 데이터를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      clearGrowthData();
      loadData();
    }
  }, [loadData]);

  // 초기 로드
  useEffect(() => {
    loadData();
  }, [loadData]);

  return {
    // 데이터
    goals,
    activeGoals,
    completedGoals,
    dailyLogs,
    todayLog,
    streaks,
    stats,
    isLoading,

    // 메서드
    addGoal,
    editGoal,
    updateProgress,
    removeGoal,
    addReflection,
    getByCategory,
    getGoalProgress,
    getNextMilestone,
    getRecentAchievements,
    exportData,
    clearData,
    loadData,
  };
}