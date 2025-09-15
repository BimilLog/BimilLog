/**
 * 프로필 뱃지 시스템
 * 사용자 활동에 따른 업적 뱃지 관리
 */

export interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string; // Lucide icon name
  category: 'activity' | 'social' | 'content' | 'special' | 'milestone';
  tier: 'bronze' | 'silver' | 'gold' | 'platinum' | 'diamond';
  unlockedAt?: string;
  progress?: number; // 0-100
  requirement: {
    type: string;
    target: number;
    current?: number;
  };
}

export interface BadgeProgress {
  badgeId: string;
  current: number;
  target: number;
  percentage: number;
  lastUpdated: string;
}

const BADGES_KEY = 'bimillog_badges';
const BADGE_PROGRESS_KEY = 'bimillog_badge_progress';

/**
 * 모든 가능한 뱃지 정의
 */
export const ALL_BADGES: Badge[] = [
  // 활동 뱃지
  {
    id: 'first_step',
    name: '첫 발걸음',
    description: '첫 게시글 작성',
    icon: 'Footprints',
    category: 'activity',
    tier: 'bronze',
    requirement: { type: 'posts_created', target: 1 },
  },
  {
    id: 'writer_bronze',
    name: '초보 작가',
    description: '게시글 5개 작성',
    icon: 'PenTool',
    category: 'content',
    tier: 'bronze',
    requirement: { type: 'posts_created', target: 5 },
  },
  {
    id: 'writer_silver',
    name: '열정 작가',
    description: '게시글 20개 작성',
    icon: 'PenTool',
    category: 'content',
    tier: 'silver',
    requirement: { type: 'posts_created', target: 20 },
  },
  {
    id: 'writer_gold',
    name: '프로 작가',
    description: '게시글 50개 작성',
    icon: 'PenTool',
    category: 'content',
    tier: 'gold',
    requirement: { type: 'posts_created', target: 50 },
  },

  // 댓글 뱃지
  {
    id: 'commenter_bronze',
    name: '대화 시작',
    description: '댓글 10개 작성',
    icon: 'MessageCircle',
    category: 'social',
    tier: 'bronze',
    requirement: { type: 'comments_created', target: 10 },
  },
  {
    id: 'commenter_silver',
    name: '활발한 토론가',
    description: '댓글 50개 작성',
    icon: 'MessageCircle',
    category: 'social',
    tier: 'silver',
    requirement: { type: 'comments_created', target: 50 },
  },
  {
    id: 'commenter_gold',
    name: '토론 마스터',
    description: '댓글 200개 작성',
    icon: 'MessageCircle',
    category: 'social',
    tier: 'gold',
    requirement: { type: 'comments_created', target: 200 },
  },

  // 좋아요 뱃지
  {
    id: 'liker_bronze',
    name: '응원 시작',
    description: '좋아요 30개 누르기',
    icon: 'Heart',
    category: 'social',
    tier: 'bronze',
    requirement: { type: 'likes_given', target: 30 },
  },
  {
    id: 'popular_bronze',
    name: '인기 상승',
    description: '좋아요 50개 받기',
    icon: 'TrendingUp',
    category: 'social',
    tier: 'bronze',
    requirement: { type: 'likes_received', target: 50 },
  },
  {
    id: 'popular_silver',
    name: '인기 스타',
    description: '좋아요 200개 받기',
    icon: 'Star',
    category: 'social',
    tier: 'silver',
    requirement: { type: 'likes_received', target: 200 },
  },
  {
    id: 'popular_gold',
    name: '인플루언서',
    description: '좋아요 1000개 받기',
    icon: 'Award',
    category: 'social',
    tier: 'gold',
    requirement: { type: 'likes_received', target: 1000 },
  },

  // 연속 활동 뱃지
  {
    id: 'streak_week',
    name: '주간 러너',
    description: '7일 연속 활동',
    icon: 'Flame',
    category: 'activity',
    tier: 'bronze',
    requirement: { type: 'active_streak', target: 7 },
  },
  {
    id: 'streak_month',
    name: '월간 챔피언',
    description: '30일 연속 활동',
    icon: 'Flame',
    category: 'activity',
    tier: 'silver',
    requirement: { type: 'active_streak', target: 30 },
  },
  {
    id: 'streak_100',
    name: '100일의 약속',
    description: '100일 연속 활동',
    icon: 'Flame',
    category: 'activity',
    tier: 'gold',
    requirement: { type: 'active_streak', target: 100 },
  },
  {
    id: 'streak_year',
    name: '연간 마스터',
    description: '365일 연속 활동',
    icon: 'Flame',
    category: 'activity',
    tier: 'platinum',
    requirement: { type: 'active_streak', target: 365 },
  },

  // 롤링페이퍼 뱃지
  {
    id: 'paper_writer',
    name: '편지 작가',
    description: '롤링페이퍼 10개 작성',
    icon: 'Mail',
    category: 'social',
    tier: 'bronze',
    requirement: { type: 'papers_written', target: 10 },
  },
  {
    id: 'paper_receiver',
    name: '인기 우체통',
    description: '롤링페이퍼 20개 받기',
    icon: 'Inbox',
    category: 'social',
    tier: 'silver',
    requirement: { type: 'papers_received', target: 20 },
  },

  // 검색 뱃지
  {
    id: 'explorer',
    name: '탐험가',
    description: '검색 100회 수행',
    icon: 'Search',
    category: 'activity',
    tier: 'bronze',
    requirement: { type: 'searches', target: 100 },
  },

  // 북마크 뱃지
  {
    id: 'collector',
    name: '수집가',
    description: '북마크 50개 수집',
    icon: 'Bookmark',
    category: 'activity',
    tier: 'bronze',
    requirement: { type: 'bookmarks_added', target: 50 },
  },

  // 특별 뱃지
  {
    id: 'early_bird',
    name: '얼리버드',
    description: '오전 6시 이전 활동',
    icon: 'Sunrise',
    category: 'special',
    tier: 'bronze',
    requirement: { type: 'special_time', target: 1 },
  },
  {
    id: 'night_owl',
    name: '올빼미',
    description: '새벽 2-4시 활동',
    icon: 'Moon',
    category: 'special',
    tier: 'bronze',
    requirement: { type: 'special_time', target: 1 },
  },
  {
    id: 'weekend_warrior',
    name: '주말 전사',
    description: '주말 연속 4주 활동',
    icon: 'Calendar',
    category: 'special',
    tier: 'silver',
    requirement: { type: 'weekend_streak', target: 4 },
  },

  // 마일스톤 뱃지
  {
    id: 'veteran_100',
    name: '100일 베테랑',
    description: '가입 후 100일',
    icon: 'Shield',
    category: 'milestone',
    tier: 'silver',
    requirement: { type: 'days_since_join', target: 100 },
  },
  {
    id: 'veteran_365',
    name: '1년 베테랑',
    description: '가입 후 1년',
    icon: 'Shield',
    category: 'milestone',
    tier: 'gold',
    requirement: { type: 'days_since_join', target: 365 },
  },
  {
    id: 'activity_1000',
    name: '천 개의 발자국',
    description: '총 활동 1000회',
    icon: 'Zap',
    category: 'milestone',
    tier: 'gold',
    requirement: { type: 'total_events', target: 1000 },
  },
  {
    id: 'activity_10000',
    name: '만 개의 별',
    description: '총 활동 10000회',
    icon: 'Sparkles',
    category: 'milestone',
    tier: 'diamond',
    requirement: { type: 'total_events', target: 10000 },
  },
];

/**
 * 뱃지 색상 가져오기
 */
export function getBadgeColor(tier: Badge['tier']): string {
  switch (tier) {
    case 'bronze':
      return '#CD7F32'; // 브론즈
    case 'silver':
      return '#C0C0C0'; // 실버
    case 'gold':
      return '#FFD700'; // 골드
    case 'platinum':
      return '#E5E4E2'; // 플래티넘
    case 'diamond':
      return '#B9F2FF'; // 다이아몬드
    default:
      return '#808080';
  }
}

/**
 * 뱃지 진행도 업데이트
 */
export function updateBadgeProgress(
  type: string,
  current: number
): void {
  if (typeof window === 'undefined') return;

  const progress = getBadgeProgress();
  const relevantBadges = ALL_BADGES.filter(b => b.requirement.type === type);

  relevantBadges.forEach(badge => {
    const existing = progress.find(p => p.badgeId === badge.id);
    const percentage = Math.min(100, (current / badge.requirement.target) * 100);

    if (existing) {
      existing.current = current;
      existing.percentage = percentage;
      existing.lastUpdated = new Date().toISOString();
    } else {
      progress.push({
        badgeId: badge.id,
        current,
        target: badge.requirement.target,
        percentage,
        lastUpdated: new Date().toISOString(),
      });
    }

    // 뱃지 달성 확인
    if (percentage >= 100) {
      unlockBadge(badge.id);
    }
  });

  localStorage.setItem(BADGE_PROGRESS_KEY, JSON.stringify(progress));
}

/**
 * 뱃지 잠금 해제
 */
export function unlockBadge(badgeId: string): void {
  if (typeof window === 'undefined') return;

  const unlockedBadges = getUnlockedBadges();
  if (unlockedBadges.find(b => b.id === badgeId)) return; // 이미 잠금 해제됨

  const badge = ALL_BADGES.find(b => b.id === badgeId);
  if (!badge) return;

  const unlocked: Badge = {
    ...badge,
    unlockedAt: new Date().toISOString(),
  };

  unlockedBadges.push(unlocked);
  localStorage.setItem(BADGES_KEY, JSON.stringify(unlockedBadges));

  // 알림 표시 (선택적)
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('🎉 새로운 뱃지 획득!', {
      body: `${badge.name} - ${badge.description}`,
      icon: '/log.png',
    });
  }
}

/**
 * 잠금 해제된 뱃지 가져오기
 */
export function getUnlockedBadges(): Badge[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(BADGES_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get unlocked badges:', error);
  }

  return [];
}

/**
 * 뱃지 진행도 가져오기
 */
export function getBadgeProgress(): BadgeProgress[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(BADGE_PROGRESS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get badge progress:', error);
  }

  return [];
}

/**
 * 뱃지 카테고리별 그룹화
 */
export function groupBadgesByCategory(badges: Badge[]): Record<Badge['category'], Badge[]> {
  return badges.reduce((acc, badge) => {
    if (!acc[badge.category]) {
      acc[badge.category] = [];
    }
    acc[badge.category].push(badge);
    return acc;
  }, {} as Record<Badge['category'], Badge[]>);
}

/**
 * 다음 달성 가능한 뱃지 가져오기
 */
export function getNextAchievableBadges(limit: number = 3): Badge[] {
  const unlockedIds = getUnlockedBadges().map(b => b.id);
  const progress = getBadgeProgress();

  const achievable = ALL_BADGES
    .filter(badge => !unlockedIds.includes(badge.id))
    .map(badge => {
      const badgeProgress = progress.find(p => p.badgeId === badge.id);
      return {
        ...badge,
        progress: badgeProgress?.percentage || 0,
      };
    })
    .sort((a, b) => (b.progress || 0) - (a.progress || 0));

  return achievable.slice(0, limit);
}

/**
 * 뱃지 통계 가져오기
 */
export function getBadgeStats() {
  const unlocked = getUnlockedBadges();
  const total = ALL_BADGES.length;
  const byTier = unlocked.reduce((acc, badge) => {
    acc[badge.tier] = (acc[badge.tier] || 0) + 1;
    return acc;
  }, {} as Record<Badge['tier'], number>);

  const byCategory = unlocked.reduce((acc, badge) => {
    acc[badge.category] = (acc[badge.category] || 0) + 1;
    return acc;
  }, {} as Record<Badge['category'], number>);

  return {
    unlocked: unlocked.length,
    total,
    percentage: Math.round((unlocked.length / total) * 100),
    byTier,
    byCategory,
    recentUnlocks: unlocked
      .sort((a, b) => new Date(b.unlockedAt!).getTime() - new Date(a.unlockedAt!).getTime())
      .slice(0, 5),
  };
}

/**
 * 활동 데이터로부터 뱃지 진행도 동기화
 */
export function syncBadgeProgressFromActivity(stats: any): void {
  if (!stats) return;

  // 각 통계 타입에 대해 진행도 업데이트
  updateBadgeProgress('posts_created', stats.postsCreated || 0);
  updateBadgeProgress('comments_created', stats.commentsCreated || 0);
  updateBadgeProgress('likes_given', stats.likesGiven || 0);
  updateBadgeProgress('likes_received', stats.likesReceived || 0);
  updateBadgeProgress('papers_written', stats.papersWritten || 0);
  updateBadgeProgress('papers_received', stats.papersReceived || 0);
  updateBadgeProgress('bookmarks_added', stats.bookmarksAdded || 0);
  updateBadgeProgress('searches', stats.searches || 0);
  updateBadgeProgress('active_streak', stats.activeStreak || 0);
  updateBadgeProgress('total_events', stats.totalEvents || 0);

  // 특별 시간대 체크
  if (stats.mostActiveHour !== undefined) {
    if (stats.mostActiveHour < 6) {
      updateBadgeProgress('special_time', 1); // 얼리버드
    }
    if (stats.mostActiveHour >= 2 && stats.mostActiveHour <= 4) {
      updateBadgeProgress('special_time', 1); // 올빼미
    }
  }
}

/**
 * 뱃지 데이터 초기화
 */
export function clearBadgeData(): void {
  if (typeof window === 'undefined') return;

  localStorage.removeItem(BADGES_KEY);
  localStorage.removeItem(BADGE_PROGRESS_KEY);
}