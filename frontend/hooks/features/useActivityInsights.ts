"use client";

import { useState, useEffect, useCallback } from 'react';
import {
  trackActivityEvent,
  getActivityEvents,
  getActivityStats,
  getDailyActivity,
  getWeeklyInsights,
  clearActivityData,
  exportActivityData,
  type ActivityEvent,
  type ActivityStats,
  type DailyActivity,
  type WeeklyInsights,
} from '@/lib/utils/activity-insights';

export function useActivityInsights() {
  const [events, setEvents] = useState<ActivityEvent[]>([]);
  const [stats, setStats] = useState<ActivityStats | null>(null);
  const [dailyActivity, setDailyActivity] = useState<DailyActivity[]>([]);
  const [weeklyInsights, setWeeklyInsights] = useState<WeeklyInsights | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 데이터 로드
  const loadData = useCallback(() => {
    setIsLoading(true);
    try {
      setEvents(getActivityEvents());
      setStats(getActivityStats());
      setDailyActivity(getDailyActivity(30));
      setWeeklyInsights(getWeeklyInsights());
    } catch (error) {
      console.error('Failed to load activity insights:', error);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // 이벤트 추적
  const track = useCallback((
    type: ActivityEvent['type'],
    metadata?: ActivityEvent['metadata']
  ) => {
    trackActivityEvent(type, metadata);
    loadData(); // 데이터 리로드
  }, [loadData]);

  // 데이터 초기화
  const clearData = useCallback(() => {
    if (confirm('모든 활동 데이터를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
      clearActivityData();
      loadData();
    }
  }, [loadData]);

  // 데이터 내보내기
  const exportData = useCallback(() => {
    const data = exportActivityData();
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `activity-insights-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }, []);

  // 차트 데이터 생성
  const getChartData = useCallback(() => {
    if (!dailyActivity.length) return null;

    return {
      labels: dailyActivity.map(d => {
        const date = new Date(d.date);
        return `${date.getMonth() + 1}/${date.getDate()}`;
      }),
      datasets: [
        {
          label: '일별 활동',
          data: dailyActivity.map(d => d.eventCount),
          borderColor: 'rgb(236, 72, 153)',
          backgroundColor: 'rgba(236, 72, 153, 0.1)',
          tension: 0.3,
        },
      ],
    };
  }, [dailyActivity]);

  // 활동 타입별 통계
  const getTypeStats = useCallback(() => {
    if (!stats) return null;

    return [
      { type: '게시글 조회', count: stats.postsViewed, color: '#3b82f6' },
      { type: '게시글 작성', count: stats.postsCreated, color: '#10b981' },
      { type: '댓글 작성', count: stats.commentsCreated, color: '#8b5cf6' },
      { type: '좋아요', count: stats.likesGiven, color: '#ef4444' },
      { type: '롤링페이퍼', count: stats.papersWritten, color: '#f59e0b' },
      { type: '북마크', count: stats.bookmarksAdded, color: '#ec4899' },
      { type: '검색', count: stats.searches, color: '#6366f1' },
    ].filter(item => item.count > 0);
  }, [stats]);

  // 시간대별 활동 패턴
  const getHourlyPattern = useCallback(() => {
    const hourlyCount: Record<number, number> = {};

    events.forEach(event => {
      const hour = new Date(event.timestamp).getHours();
      hourlyCount[hour] = (hourlyCount[hour] || 0) + 1;
    });

    return Array.from({ length: 24 }, (_, hour) => ({
      hour,
      count: hourlyCount[hour] || 0,
      label: `${hour}시`,
    }));
  }, [events]);

  // 초기 로드
  useEffect(() => {
    loadData();
  }, [loadData]);

  return {
    // 데이터
    events,
    stats,
    dailyActivity,
    weeklyInsights,
    isLoading,

    // 메서드
    track,
    clearData,
    exportData,
    loadData,

    // 가공된 데이터
    chartData: getChartData(),
    typeStats: getTypeStats(),
    hourlyPattern: getHourlyPattern(),
  };
}