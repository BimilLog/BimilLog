/**
 * 활동 인사이트 유틸리티
 * 사용자 활동 데이터 수집 및 분석
 */

export interface ActivityEvent {
  id: string;
  type: 'post_view' | 'post_create' | 'comment_create' | 'like_give' | 'like_receive' | 'paper_write' | 'paper_receive' | 'bookmark_add' | 'search';
  timestamp: string;
  metadata?: {
    postId?: number;
    commentId?: number;
    paperId?: string;
    searchQuery?: string;
  } & Record<string, unknown>;
}

export interface ActivityStats {
  totalEvents: number;
  postsViewed: number;
  postsCreated: number;
  commentsCreated: number;
  likesGiven: number;
  likesReceived: number;
  papersWritten: number;
  papersReceived: number;
  bookmarksAdded: number;
  searches: number;
  activeStreak: number;
  longestStreak: number;
  mostActiveHour: number;
  mostActiveDay: string;
  lastActiveDate: string;
}

export interface DailyActivity {
  date: string;
  eventCount: number;
  types: Record<string, number>;
}

export interface WeeklyInsights {
  weekStart: string;
  weekEnd: string;
  totalEvents: number;
  dailyAverage: number;
  mostActiveDay: string;
  growthRate: number; // 전주 대비 성장률
}

const ACTIVITY_KEY = 'bimillog_activity_events';
const STATS_KEY = 'bimillog_activity_stats';
const MAX_EVENTS = 1000; // 최대 저장 이벤트 수
const RETENTION_DAYS = 90; // 이벤트 보관 기간

/**
 * 활동 이벤트 기록
 */
export function trackActivityEvent(
  type: ActivityEvent['type'],
  metadata?: ActivityEvent['metadata']
): void {
  if (typeof window === 'undefined') return;

  const event: ActivityEvent = {
    id: crypto.randomUUID(),
    type,
    timestamp: new Date().toISOString(),
    metadata,
  };

  const events = getActivityEvents();
  events.unshift(event);

  // 최대 이벤트 수 제한
  if (events.length > MAX_EVENTS) {
    events.splice(MAX_EVENTS);
  }

  localStorage.setItem(ACTIVITY_KEY, JSON.stringify(events));
  updateActivityStats();
}

/**
 * 활동 이벤트 목록 가져오기
 */
export function getActivityEvents(): ActivityEvent[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(ACTIVITY_KEY);
    if (stored) {
      const events: ActivityEvent[] = JSON.parse(stored);
      // 오래된 이벤트 필터링
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - RETENTION_DAYS);
      return events.filter(e => new Date(e.timestamp) > cutoffDate);
    }
  } catch (error) {
    console.error('Failed to get activity events:', error);
  }

  return [];
}

/**
 * 활동 통계 업데이트
 */
function updateActivityStats(): void {
  const events = getActivityEvents();
  const stats = calculateActivityStats(events);
  localStorage.setItem(STATS_KEY, JSON.stringify(stats));
}

/**
 * 활동 통계 계산
 */
export function calculateActivityStats(events: ActivityEvent[]): ActivityStats {
  const stats: ActivityStats = {
    totalEvents: events.length,
    postsViewed: 0,
    postsCreated: 0,
    commentsCreated: 0,
    likesGiven: 0,
    likesReceived: 0,
    papersWritten: 0,
    papersReceived: 0,
    bookmarksAdded: 0,
    searches: 0,
    activeStreak: 0,
    longestStreak: 0,
    mostActiveHour: 0,
    mostActiveDay: '',
    lastActiveDate: events[0]?.timestamp || '',
  };

  const hourCounts: Record<number, number> = {};
  const dayCounts: Record<string, number> = {};
  const uniqueDates = new Set<string>();

  events.forEach(event => {
    // 이벤트 타입별 카운트
    switch (event.type) {
      case 'post_view':
        stats.postsViewed++;
        break;
      case 'post_create':
        stats.postsCreated++;
        break;
      case 'comment_create':
        stats.commentsCreated++;
        break;
      case 'like_give':
        stats.likesGiven++;
        break;
      case 'like_receive':
        stats.likesReceived++;
        break;
      case 'paper_write':
        stats.papersWritten++;
        break;
      case 'paper_receive':
        stats.papersReceived++;
        break;
      case 'bookmark_add':
        stats.bookmarksAdded++;
        break;
      case 'search':
        stats.searches++;
        break;
    }

    const date = new Date(event.timestamp);
    const dateStr = date.toISOString().split('T')[0];
    const hour = date.getHours();
    const dayName = date.toLocaleDateString('ko-KR', { weekday: 'long' });

    uniqueDates.add(dateStr);
    hourCounts[hour] = (hourCounts[hour] || 0) + 1;
    dayCounts[dayName] = (dayCounts[dayName] || 0) + 1;
  });

  // 가장 활발한 시간대
  if (Object.keys(hourCounts).length > 0) {
    stats.mostActiveHour = Object.entries(hourCounts).reduce((a, b) =>
      a[1] > b[1] ? a : b
    )[0] as unknown as number;
  }

  // 가장 활발한 요일
  if (Object.keys(dayCounts).length > 0) {
    stats.mostActiveDay = Object.entries(dayCounts).reduce((a, b) =>
      a[1] > b[1] ? a : b
    )[0];
  }

  // 연속 활동일 계산
  const sortedDates = Array.from(uniqueDates).sort().reverse();
  if (sortedDates.length > 0) {
    let currentStreak = 1;
    let maxStreak = 1;
    const today = new Date().toISOString().split('T')[0];

    // 현재 연속 기록
    if (sortedDates[0] === today ||
        sortedDates[0] === new Date(Date.now() - 86400000).toISOString().split('T')[0]) {
      for (let i = 1; i < sortedDates.length; i++) {
        const prevDate = new Date(sortedDates[i - 1]);
        const currDate = new Date(sortedDates[i]);
        const diffDays = Math.floor((prevDate.getTime() - currDate.getTime()) / 86400000);

        if (diffDays === 1) {
          currentStreak++;
        } else {
          break;
        }
      }
    }

    // 최장 연속 기록
    let tempStreak = 1;
    for (let i = 1; i < sortedDates.length; i++) {
      const prevDate = new Date(sortedDates[i - 1]);
      const currDate = new Date(sortedDates[i]);
      const diffDays = Math.floor((prevDate.getTime() - currDate.getTime()) / 86400000);

      if (diffDays === 1) {
        tempStreak++;
        maxStreak = Math.max(maxStreak, tempStreak);
      } else {
        tempStreak = 1;
      }
    }

    stats.activeStreak = currentStreak;
    stats.longestStreak = maxStreak;
  }

  return stats;
}

/**
 * 활동 통계 가져오기
 */
export function getActivityStats(): ActivityStats | null {
  if (typeof window === 'undefined') return null;

  try {
    const stored = localStorage.getItem(STATS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get activity stats:', error);
  }

  // 저장된 통계가 없으면 계산
  const events = getActivityEvents();
  if (events.length > 0) {
    const stats = calculateActivityStats(events);
    localStorage.setItem(STATS_KEY, JSON.stringify(stats));
    return stats;
  }

  return null;
}

/**
 * 일별 활동 데이터 가져오기
 */
export function getDailyActivity(days: number = 30): DailyActivity[] {
  const events = getActivityEvents();
  const dailyMap = new Map<string, DailyActivity>();

  // 최근 N일 날짜 생성
  for (let i = 0; i < days; i++) {
    const date = new Date();
    date.setDate(date.getDate() - i);
    const dateStr = date.toISOString().split('T')[0];
    dailyMap.set(dateStr, {
      date: dateStr,
      eventCount: 0,
      types: {},
    });
  }

  // 이벤트 집계
  events.forEach(event => {
    const dateStr = event.timestamp.split('T')[0];
    if (dailyMap.has(dateStr)) {
      const daily = dailyMap.get(dateStr)!;
      daily.eventCount++;
      daily.types[event.type] = (daily.types[event.type] || 0) + 1;
    }
  });

  return Array.from(dailyMap.values()).reverse();
}

/**
 * 주간 인사이트 가져오기
 */
export function getWeeklyInsights(): WeeklyInsights {
  const events = getActivityEvents();
  const now = new Date();
  const weekStart = new Date(now);
  weekStart.setDate(weekStart.getDate() - weekStart.getDay());
  weekStart.setHours(0, 0, 0, 0);

  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekEnd.getDate() + 6);
  weekEnd.setHours(23, 59, 59, 999);

  const prevWeekStart = new Date(weekStart);
  prevWeekStart.setDate(prevWeekStart.getDate() - 7);
  const prevWeekEnd = new Date(prevWeekStart);
  prevWeekEnd.setDate(prevWeekEnd.getDate() + 6);

  // 이번 주 이벤트
  const thisWeekEvents = events.filter(e => {
    const date = new Date(e.timestamp);
    return date >= weekStart && date <= weekEnd;
  });

  // 지난 주 이벤트
  const lastWeekEvents = events.filter(e => {
    const date = new Date(e.timestamp);
    return date >= prevWeekStart && date <= prevWeekEnd;
  });

  // 요일별 집계
  const dayStats: Record<string, number> = {};
  thisWeekEvents.forEach(e => {
    const day = new Date(e.timestamp).toLocaleDateString('ko-KR', { weekday: 'long' });
    dayStats[day] = (dayStats[day] || 0) + 1;
  });

  const mostActiveDay = Object.keys(dayStats).length > 0
    ? Object.entries(dayStats).reduce((a, b) => a[1] > b[1] ? a : b)[0]
    : '활동 없음';

  const growthRate = lastWeekEvents.length > 0
    ? ((thisWeekEvents.length - lastWeekEvents.length) / lastWeekEvents.length) * 100
    : 0;

  return {
    weekStart: weekStart.toISOString().split('T')[0],
    weekEnd: weekEnd.toISOString().split('T')[0],
    totalEvents: thisWeekEvents.length,
    dailyAverage: thisWeekEvents.length / 7,
    mostActiveDay,
    growthRate,
  };
}

/**
 * 활동 데이터 초기화
 */
export function clearActivityData(): void {
  if (typeof window === 'undefined') return;

  localStorage.removeItem(ACTIVITY_KEY);
  localStorage.removeItem(STATS_KEY);
}

/**
 * 활동 데이터 내보내기
 */
export function exportActivityData(): string {
  const events = getActivityEvents();
  const stats = getActivityStats();
  const daily = getDailyActivity();
  const weekly = getWeeklyInsights();

  return JSON.stringify({
    events,
    stats,
    daily,
    weekly,
    exportedAt: new Date().toISOString(),
  }, null, 2);
}