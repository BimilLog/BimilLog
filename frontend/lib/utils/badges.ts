/**
 * 프로필 뱃지 시스템
 * 사용자 활동에 따른 업적 뱃지 관리
 */

import type { UserStats } from '@/hooks/features/user/useUserStats';
import { calculateActivityScore } from './format';

export interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string; // Lucide icon name
  category: 'post' | 'comment' | 'like' | 'paper' | 'milestone';
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
  // 게시글 작성 뱃지
  {
    id: 'first_step',
    name: '첫 발걸음',
    description: '첫 게시글 작성',
    icon: 'Footprints',
    category: 'post',
    tier: 'bronze',
    requirement: { type: 'posts_created', target: 1 },
  },
  {
    id: 'writer_bronze',
    name: '초보 작가',
    description: '게시글 5개 작성',
    icon: 'PenTool',
    category: 'post',
    tier: 'bronze',
    requirement: { type: 'posts_created', target: 5 },
  },
  {
    id: 'writer_silver',
    name: '열정 작가',
    description: '게시글 20개 작성',
    icon: 'PenTool',
    category: 'post',
    tier: 'silver',
    requirement: { type: 'posts_created', target: 20 },
  },
  {
    id: 'writer_gold',
    name: '프로 작가',
    description: '게시글 50개 작성',
    icon: 'PenTool',
    category: 'post',
    tier: 'gold',
    requirement: { type: 'posts_created', target: 50 },
  },
  {
    id: 'writer_platinum',
    name: '베테랑 작가',
    description: '게시글 100개 작성',
    icon: 'PenTool',
    category: 'post',
    tier: 'platinum',
    requirement: { type: 'posts_created', target: 100 },
  },
  {
    id: 'writer_diamond',
    name: '전설의 작가',
    description: '게시글 200개 작성',
    icon: 'PenTool',
    category: 'post',
    tier: 'diamond',
    requirement: { type: 'posts_created', target: 200 },
  },

  // 댓글 뱃지
  {
    id: 'commenter_bronze',
    name: '대화 시작',
    description: '댓글 10개 작성',
    icon: 'MessageCircle',
    category: 'comment',
    tier: 'bronze',
    requirement: { type: 'comments_created', target: 10 },
  },
  {
    id: 'commenter_silver',
    name: '활발한 토론가',
    description: '댓글 50개 작성',
    icon: 'MessageCircle',
    category: 'comment',
    tier: 'silver',
    requirement: { type: 'comments_created', target: 50 },
  },
  {
    id: 'commenter_gold',
    name: '토론 마스터',
    description: '댓글 200개 작성',
    icon: 'MessageCircle',
    category: 'comment',
    tier: 'gold',
    requirement: { type: 'comments_created', target: 200 },
  },
  {
    id: 'commenter_platinum',
    name: '토론의 달인',
    description: '댓글 500개 작성',
    icon: 'MessageCircle',
    category: 'comment',
    tier: 'platinum',
    requirement: { type: 'comments_created', target: 500 },
  },
  {
    id: 'commenter_diamond',
    name: '전설의 토론가',
    description: '댓글 1000개 작성',
    icon: 'MessageCircle',
    category: 'comment',
    tier: 'diamond',
    requirement: { type: 'comments_created', target: 1000 },
  },

  // 추천 뱃지 - 총 추천 (글+댓글)
  {
    id: 'total_liker_bronze',
    name: '응원 시작',
    description: '추천 30개 누르기',
    icon: 'Heart',
    category: 'like',
    tier: 'bronze',
    requirement: { type: 'total_likes_given', target: 30 },
  },
  {
    id: 'total_liker_silver',
    name: '열정 추천러',
    description: '총 추천 100개',
    icon: 'Heart',
    category: 'like',
    tier: 'silver',
    requirement: { type: 'total_likes_given', target: 100 },
  },
  {
    id: 'total_liker_gold',
    name: '응원의 아이콘',
    description: '총 추천 300개',
    icon: 'Heart',
    category: 'like',
    tier: 'gold',
    requirement: { type: 'total_likes_given', target: 300 },
  },
  {
    id: 'total_liker_platinum',
    name: '추천의 달인',
    description: '총 추천 600개',
    icon: 'Heart',
    category: 'like',
    tier: 'platinum',
    requirement: { type: 'total_likes_given', target: 600 },
  },
  {
    id: 'total_liker_diamond',
    name: '전설의 서포터',
    description: '총 추천 1000개',
    icon: 'Heart',
    category: 'like',
    tier: 'diamond',
    requirement: { type: 'total_likes_given', target: 1000 },
  },

  // 추천 뱃지 - 글 추천
  {
    id: 'post_liker_bronze',
    name: '글 추천 입문',
    description: '게시글 추천 10개',
    icon: 'ThumbsUp',
    category: 'like',
    tier: 'bronze',
    requirement: { type: 'posts_liked', target: 10 },
  },
  {
    id: 'post_liker_silver',
    name: '글 추천러',
    description: '게시글 추천 30개',
    icon: 'ThumbsUp',
    category: 'like',
    tier: 'silver',
    requirement: { type: 'posts_liked', target: 30 },
  },
  {
    id: 'post_liker_gold',
    name: '글 추천 마스터',
    description: '게시글 추천 100개',
    icon: 'ThumbsUp',
    category: 'like',
    tier: 'gold',
    requirement: { type: 'posts_liked', target: 100 },
  },
  {
    id: 'post_liker_platinum',
    name: '글 추천의 달인',
    description: '게시글 추천 200개',
    icon: 'ThumbsUp',
    category: 'like',
    tier: 'platinum',
    requirement: { type: 'posts_liked', target: 200 },
  },
  {
    id: 'post_liker_diamond',
    name: '전설의 글 추천러',
    description: '게시글 추천 500개',
    icon: 'ThumbsUp',
    category: 'like',
    tier: 'diamond',
    requirement: { type: 'posts_liked', target: 500 },
  },

  // 추천 뱃지 - 댓글 추천
  {
    id: 'comment_liker_bronze',
    name: '댓글 추천 입문',
    description: '댓글 추천 20개',
    icon: 'MessageCircle',
    category: 'like',
    tier: 'bronze',
    requirement: { type: 'comments_liked', target: 20 },
  },
  {
    id: 'comment_liker_silver',
    name: '댓글 추천러',
    description: '댓글 추천 50개',
    icon: 'MessageCircle',
    category: 'like',
    tier: 'silver',
    requirement: { type: 'comments_liked', target: 50 },
  },
  {
    id: 'comment_liker_gold',
    name: '댓글 추천 마스터',
    description: '댓글 추천 150개',
    icon: 'MessageCircle',
    category: 'like',
    tier: 'gold',
    requirement: { type: 'comments_liked', target: 150 },
  },
  {
    id: 'comment_liker_platinum',
    name: '댓글 추천의 달인',
    description: '댓글 추천 300개',
    icon: 'MessageCircle',
    category: 'like',
    tier: 'platinum',
    requirement: { type: 'comments_liked', target: 300 },
  },
  {
    id: 'comment_liker_diamond',
    name: '전설의 댓글 추천러',
    description: '댓글 추천 600개',
    icon: 'MessageCircle',
    category: 'like',
    tier: 'diamond',
    requirement: { type: 'comments_liked', target: 600 },
  },

  // 롤링페이퍼 받기 뱃지
  {
    id: 'paper_receiver_bronze',
    name: '편지함',
    description: '롤링페이퍼 20개 받기',
    icon: 'Mail',
    category: 'paper',
    tier: 'bronze',
    requirement: { type: 'papers_received', target: 20 },
  },
  {
    id: 'paper_receiver_silver_1',
    name: '인기 우체통',
    description: '롤링페이퍼 40개 받기',
    icon: 'Inbox',
    category: 'paper',
    tier: 'silver',
    requirement: { type: 'papers_received', target: 40 },
  },
  {
    id: 'paper_receiver_silver_2',
    name: '사랑받는 편지함',
    description: '롤링페이퍼 60개 받기',
    icon: 'Mail',
    category: 'paper',
    tier: 'gold',
    requirement: { type: 'papers_received', target: 60 },
  },
  {
    id: 'paper_receiver_gold_1',
    name: '편지 왕',
    description: '롤링페이퍼 80개 받기',
    icon: 'Inbox',
    category: 'paper',
    tier: 'platinum',
    requirement: { type: 'papers_received', target: 80 },
  },
  {
    id: 'paper_receiver_gold_2',
    name: '편지 대왕',
    description: '롤링페이퍼 100개 받기',
    icon: 'Mail',
    category: 'paper',
    tier: 'platinum',
    requirement: { type: 'papers_received', target: 100 },
  },
  {
    id: 'paper_receiver_platinum',
    name: '전설의 편지함',
    description: '롤링페이퍼 120개 받기',
    icon: 'Inbox',
    category: 'paper',
    tier: 'diamond',
    requirement: { type: 'papers_received', target: 120 },
  },

  // 마일스톤 뱃지
  {
    id: 'activity_100',
    name: '시작의 발걸음',
    description: '총 활동 100회',
    icon: 'Footprints',
    category: 'milestone',
    tier: 'bronze',
    requirement: { type: 'total_events', target: 100 },
  },
  {
    id: 'activity_500',
    name: '활동의 빛',
    description: '총 활동 500회',
    icon: 'Star',
    category: 'milestone',
    tier: 'silver',
    requirement: { type: 'total_events', target: 500 },
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
    id: 'activity_5000',
    name: '오천의 기록',
    description: '총 활동 5000회',
    icon: 'TrendingUp',
    category: 'milestone',
    tier: 'platinum',
    requirement: { type: 'total_events', target: 5000 },
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
 * 뱃지 데이터 초기화
 */
export function clearBadgeData(): void {
  if (typeof window === 'undefined') return;

  localStorage.removeItem(BADGES_KEY);
  localStorage.removeItem(BADGE_PROGRESS_KEY);
}

/**
 * 백엔드 통계로부터 뱃지 진행도 동기화
 * 추가 API 호출 없이 이미 가져온 UserStats를 활용
 */
export function syncBadgeProgressFromBackendStats(
  stats: UserStats | null | undefined,
  receivedPaperCount?: number
): void {
  if (!stats) return;

  // 게시글 작성 뱃지
  updateBadgeProgress('posts_created', stats.totalPosts || 0);

  // 댓글 작성 뱃지
  updateBadgeProgress('comments_created', stats.totalComments || 0);

  // 추천한 게시글
  updateBadgeProgress('posts_liked', stats.totalLikedPosts || 0);

  // 추천한 댓글
  updateBadgeProgress('comments_liked', stats.totalLikedComments || 0);

  // 총 추천 (글+댓글)
  const totalLikesGiven = (stats.totalLikedPosts || 0) + (stats.totalLikedComments || 0);
  updateBadgeProgress('total_likes_given', totalLikesGiven);

  // 받은 롤링페이퍼 (다른 사람이 내게 쓴 메시지)
  if (receivedPaperCount !== undefined) {
    updateBadgeProgress('papers_received', receivedPaperCount);
  }

  // 총 활동 점수 (프론트엔드에서 계산)
  const totalActivityScore = calculateActivityScore(stats);
  updateBadgeProgress('total_events', totalActivityScore);
}