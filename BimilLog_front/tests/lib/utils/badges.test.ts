import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/lib/utils/format', () => ({
  calculateActivityScore: vi.fn(
    (stats: { totalPosts?: number; totalComments?: number }) =>
      (stats.totalPosts || 0) + (stats.totalComments || 0)
  ),
}));

import {
  ALL_BADGES,
  getBadgeColor,
  updateBadgeProgress,
  unlockBadge,
  getUnlockedBadges,
  getBadgeProgress,
  clearBadgeData,
  getBadgeStats,
  groupBadgesByCategory,
  syncBadgeProgressFromBackendStats,
} from '@/lib/utils/badges';
import type { Badge, BadgeProgress } from '@/lib/utils/badges';

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();

  Object.defineProperty(window, 'Notification', {
    value: { permission: 'default' },
    writable: true,
    configurable: true,
  });
});

afterEach(() => {
  vi.restoreAllMocks();
});

// ─── getBadgeColor ──────────────────────────────────────────────────────────────

describe('getBadgeColor', () => {
  it('bronze 티어에 대해 올바른 색상을 반환한다', () => {
    expect(getBadgeColor('bronze')).toBe('#CD7F32');
  });

  it('silver 티어에 대해 올바른 색상을 반환한다', () => {
    expect(getBadgeColor('silver')).toBe('#C0C0C0');
  });

  it('gold 티어에 대해 올바른 색상을 반환한다', () => {
    expect(getBadgeColor('gold')).toBe('#FFD700');
  });

  it('platinum 티어에 대해 올바른 색상을 반환한다', () => {
    expect(getBadgeColor('platinum')).toBe('#E5E4E2');
  });

  it('diamond 티어에 대해 올바른 색상을 반환한다', () => {
    expect(getBadgeColor('diamond')).toBe('#B9F2FF');
  });

  it('알 수 없는 티어에 대해 기본 색상을 반환한다', () => {
    expect(getBadgeColor('unknown' as Badge['tier'])).toBe('#808080');
  });
});

// ─── updateBadgeProgress ────────────────────────────────────────────────────────

describe('updateBadgeProgress', () => {
  it('localStorage에 진행도를 저장한다', () => {
    updateBadgeProgress('posts_created', 3);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    // posts_created 타입의 모든 뱃지에 대한 진행도가 저장되어야 한다
    const postsCreatedBadges = ALL_BADGES.filter(
      (b) => b.requirement.type === 'posts_created'
    );
    expect(progress.length).toBe(postsCreatedBadges.length);
  });

  it('진행도 퍼센트를 올바르게 계산한다', () => {
    updateBadgeProgress('posts_created', 3);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    // first_step 뱃지: target 1, current 3 → 100% (capped)
    const firstStep = progress.find((p) => p.badgeId === 'first_step');
    expect(firstStep).toBeDefined();
    expect(firstStep!.percentage).toBe(100);
    expect(firstStep!.current).toBe(3);

    // writer_bronze 뱃지: target 5, current 3 → 60%
    const writerBronze = progress.find((p) => p.badgeId === 'writer_bronze');
    expect(writerBronze).toBeDefined();
    expect(writerBronze!.percentage).toBe(60);
  });

  it('100% 도달 시 뱃지를 잠금 해제한다', () => {
    updateBadgeProgress('posts_created', 1);

    const unlocked: Badge[] = JSON.parse(
      localStorage.getItem('bimillog_badges')!
    );
    expect(unlocked.some((b) => b.id === 'first_step')).toBe(true);
  });

  it('기존 진행도를 업데이트한다', () => {
    updateBadgeProgress('posts_created', 2);
    updateBadgeProgress('posts_created', 4);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    const writerBronze = progress.find((p) => p.badgeId === 'writer_bronze');
    expect(writerBronze!.current).toBe(4);
    expect(writerBronze!.percentage).toBe(80);
  });

  it('여러 관련 뱃지에 모두 업데이트한다', () => {
    updateBadgeProgress('posts_created', 50);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    // posts_created 타입의 뱃지가 모두 업데이트되어야 한다
    const postsCreatedBadges = ALL_BADGES.filter(
      (b) => b.requirement.type === 'posts_created'
    );
    postsCreatedBadges.forEach((badge) => {
      const p = progress.find((pr) => pr.badgeId === badge.id);
      expect(p).toBeDefined();
      expect(p!.current).toBe(50);
    });
  });

  it('퍼센트가 100을 초과하지 않는다', () => {
    updateBadgeProgress('posts_created', 999);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    progress.forEach((p) => {
      expect(p.percentage).toBeLessThanOrEqual(100);
    });
  });
});

// ─── unlockBadge ────────────────────────────────────────────────────────────────

describe('unlockBadge', () => {
  it('localStorage에 뱃지를 저장한다', () => {
    unlockBadge('first_step');

    const unlocked: Badge[] = JSON.parse(
      localStorage.getItem('bimillog_badges')!
    );
    expect(unlocked.length).toBe(1);
    expect(unlocked[0].id).toBe('first_step');
  });

  it('unlockedAt 필드를 추가한다', () => {
    unlockBadge('first_step');

    const unlocked: Badge[] = JSON.parse(
      localStorage.getItem('bimillog_badges')!
    );
    expect(unlocked[0].unlockedAt).toBeDefined();
    expect(typeof unlocked[0].unlockedAt).toBe('string');
  });

  it('이미 잠금 해제된 뱃지는 중복 추가하지 않는다', () => {
    unlockBadge('first_step');
    unlockBadge('first_step');

    const unlocked: Badge[] = JSON.parse(
      localStorage.getItem('bimillog_badges')!
    );
    expect(unlocked.length).toBe(1);
  });

  it('존재하지 않는 뱃지 ID는 무시한다', () => {
    unlockBadge('nonexistent_badge');

    const stored = localStorage.getItem('bimillog_badges');
    // 뱃지를 찾을 수 없으면 저장하지 않는다
    expect(stored).toBeNull();
  });

  it('여러 뱃지를 순서대로 잠금 해제할 수 있다', () => {
    unlockBadge('first_step');
    unlockBadge('writer_bronze');

    const unlocked: Badge[] = JSON.parse(
      localStorage.getItem('bimillog_badges')!
    );
    expect(unlocked.length).toBe(2);
    expect(unlocked[0].id).toBe('first_step');
    expect(unlocked[1].id).toBe('writer_bronze');
  });

  it('Notification 권한이 granted이면 알림을 표시한다', () => {
    const notificationSpy = vi.fn();
    Object.defineProperty(window, 'Notification', {
      value: class {
        constructor(title: string, options: object) {
          notificationSpy(title, options);
        }
        static permission = 'granted';
      },
      writable: true,
      configurable: true,
    });

    unlockBadge('first_step');

    expect(notificationSpy).toHaveBeenCalledTimes(1);
  });

  it('Notification 권한이 default이면 알림을 표시하지 않는다', () => {
    const notificationSpy = vi.fn();
    Object.defineProperty(window, 'Notification', {
      value: class {
        constructor(title: string, options: object) {
          notificationSpy(title, options);
        }
        static permission = 'default';
      },
      writable: true,
      configurable: true,
    });

    unlockBadge('first_step');

    expect(notificationSpy).not.toHaveBeenCalled();
  });
});

// ─── getUnlockedBadges ──────────────────────────────────────────────────────────

describe('getUnlockedBadges', () => {
  it('잠금 해제된 뱃지 목록을 반환한다', () => {
    unlockBadge('first_step');
    unlockBadge('writer_bronze');

    const unlocked = getUnlockedBadges();
    expect(unlocked.length).toBe(2);
    expect(unlocked[0].id).toBe('first_step');
    expect(unlocked[1].id).toBe('writer_bronze');
  });

  it('저장된 뱃지가 없으면 빈 배열을 반환한다', () => {
    const unlocked = getUnlockedBadges();
    expect(unlocked).toEqual([]);
  });

  it('JSON 파싱 실패 시 빈 배열을 반환한다', () => {
    localStorage.setItem('bimillog_badges', 'invalid json');

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const unlocked = getUnlockedBadges();

    expect(unlocked).toEqual([]);
    consoleErrorSpy.mockRestore();
  });
});

// ─── getBadgeProgress ───────────────────────────────────────────────────────────

describe('getBadgeProgress', () => {
  it('진행도 목록을 반환한다', () => {
    updateBadgeProgress('posts_created', 3);

    const progress = getBadgeProgress();
    expect(progress.length).toBeGreaterThan(0);
  });

  it('저장된 진행도가 없으면 빈 배열을 반환한다', () => {
    const progress = getBadgeProgress();
    expect(progress).toEqual([]);
  });

  it('JSON 파싱 실패 시 빈 배열을 반환한다', () => {
    localStorage.setItem('bimillog_badge_progress', 'invalid json');

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const progress = getBadgeProgress();

    expect(progress).toEqual([]);
    consoleErrorSpy.mockRestore();
  });
});

// ─── clearBadgeData ─────────────────────────────────────────────────────────────

describe('clearBadgeData', () => {
  it('localStorage에서 뱃지 관련 데이터를 모두 삭제한다', () => {
    unlockBadge('first_step');
    updateBadgeProgress('posts_created', 3);

    // 데이터가 있는지 확인
    expect(localStorage.getItem('bimillog_badges')).not.toBeNull();
    expect(localStorage.getItem('bimillog_badge_progress')).not.toBeNull();

    clearBadgeData();

    expect(localStorage.getItem('bimillog_badges')).toBeNull();
    expect(localStorage.getItem('bimillog_badge_progress')).toBeNull();
  });

  it('데이터가 없어도 에러가 발생하지 않는다', () => {
    expect(() => clearBadgeData()).not.toThrow();
  });
});

// ─── getBadgeStats ──────────────────────────────────────────────────────────────

describe('getBadgeStats', () => {
  it('전체 뱃지 수를 반환한다', () => {
    const stats = getBadgeStats();
    expect(stats.total).toBe(ALL_BADGES.length);
  });

  it('잠금 해제 수를 반환한다', () => {
    unlockBadge('first_step');
    unlockBadge('writer_bronze');

    const stats = getBadgeStats();
    expect(stats.unlocked).toBe(2);
  });

  it('퍼센트를 올바르게 계산한다', () => {
    unlockBadge('first_step');

    const stats = getBadgeStats();
    expect(stats.percentage).toBe(Math.round((1 / ALL_BADGES.length) * 100));
  });

  it('잠금 해제가 없으면 0%를 반환한다', () => {
    const stats = getBadgeStats();
    expect(stats.unlocked).toBe(0);
    expect(stats.percentage).toBe(0);
  });

  it('티어별 통계를 반환한다', () => {
    unlockBadge('first_step'); // bronze
    unlockBadge('writer_silver'); // silver

    const stats = getBadgeStats();
    expect(stats.byTier.bronze).toBe(1);
    expect(stats.byTier.silver).toBe(1);
  });

  it('카테고리별 통계를 반환한다', () => {
    unlockBadge('first_step'); // post
    unlockBadge('commenter_bronze'); // comment

    const stats = getBadgeStats();
    expect(stats.byCategory.post).toBe(1);
    expect(stats.byCategory.comment).toBe(1);
  });

  it('최근 잠금 해제 목록을 반환한다', () => {
    unlockBadge('first_step');
    unlockBadge('writer_bronze');

    const stats = getBadgeStats();
    expect(stats.recentUnlocks.length).toBe(2);
  });

  it('최근 잠금 해제는 최대 5개까지 반환한다', () => {
    unlockBadge('first_step');
    unlockBadge('writer_bronze');
    unlockBadge('writer_silver');
    unlockBadge('writer_gold');
    unlockBadge('writer_platinum');
    unlockBadge('writer_diamond');
    unlockBadge('commenter_bronze');

    const stats = getBadgeStats();
    expect(stats.recentUnlocks.length).toBe(5);
  });
});

// ─── groupBadgesByCategory ──────────────────────────────────────────────────────

describe('groupBadgesByCategory', () => {
  it('카테고리별로 뱃지를 분류한다', () => {
    const grouped = groupBadgesByCategory(ALL_BADGES);

    expect(grouped.post).toBeDefined();
    expect(grouped.comment).toBeDefined();
    expect(grouped.like).toBeDefined();
    expect(grouped.paper).toBeDefined();
    expect(grouped.milestone).toBeDefined();
  });

  it('각 카테고리의 뱃지 수가 올바르다', () => {
    const grouped = groupBadgesByCategory(ALL_BADGES);

    const postCount = ALL_BADGES.filter((b) => b.category === 'post').length;
    const commentCount = ALL_BADGES.filter((b) => b.category === 'comment').length;
    const likeCount = ALL_BADGES.filter((b) => b.category === 'like').length;
    const paperCount = ALL_BADGES.filter((b) => b.category === 'paper').length;
    const milestoneCount = ALL_BADGES.filter((b) => b.category === 'milestone').length;

    expect(grouped.post.length).toBe(postCount);
    expect(grouped.comment.length).toBe(commentCount);
    expect(grouped.like.length).toBe(likeCount);
    expect(grouped.paper.length).toBe(paperCount);
    expect(grouped.milestone.length).toBe(milestoneCount);
  });

  it('빈 배열이 주어지면 빈 객체를 반환한다', () => {
    const grouped = groupBadgesByCategory([]);
    expect(Object.keys(grouped).length).toBe(0);
  });

  it('단일 카테고리의 뱃지만 주어지면 해당 카테고리만 반환한다', () => {
    const postBadges = ALL_BADGES.filter((b) => b.category === 'post');
    const grouped = groupBadgesByCategory(postBadges);

    expect(grouped.post).toBeDefined();
    expect(grouped.post.length).toBe(postBadges.length);
    expect(grouped.comment).toBeUndefined();
  });
});

// ─── syncBadgeProgressFromBackendStats ──────────────────────────────────────────

describe('syncBadgeProgressFromBackendStats', () => {
  it('null stats는 아무것도 하지 않는다', () => {
    syncBadgeProgressFromBackendStats(null);

    const progress = localStorage.getItem('bimillog_badge_progress');
    expect(progress).toBeNull();
  });

  it('undefined stats는 아무것도 하지 않는다', () => {
    syncBadgeProgressFromBackendStats(undefined);

    const progress = localStorage.getItem('bimillog_badge_progress');
    expect(progress).toBeNull();
  });

  it('각 필드별로 진행도를 업데이트한다', () => {
    const stats = {
      totalMessages: 10,
      totalPosts: 5,
      totalComments: 20,
      totalLikedPosts: 15,
      totalLikedComments: 10,
    };

    syncBadgeProgressFromBackendStats(stats);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    // posts_created 진행도 확인
    const writerBronze = progress.find((p) => p.badgeId === 'writer_bronze');
    expect(writerBronze).toBeDefined();
    expect(writerBronze!.current).toBe(5);

    // comments_created 진행도 확인
    const commenterBronze = progress.find((p) => p.badgeId === 'commenter_bronze');
    expect(commenterBronze).toBeDefined();
    expect(commenterBronze!.current).toBe(20);

    // posts_liked 진행도 확인
    const postLikerBronze = progress.find((p) => p.badgeId === 'post_liker_bronze');
    expect(postLikerBronze).toBeDefined();
    expect(postLikerBronze!.current).toBe(15);

    // comments_liked 진행도 확인
    const commentLikerBronze = progress.find((p) => p.badgeId === 'comment_liker_bronze');
    expect(commentLikerBronze).toBeDefined();
    expect(commentLikerBronze!.current).toBe(10);

    // total_likes_given 진행도 확인 (15 + 10 = 25)
    const totalLikerBronze = progress.find((p) => p.badgeId === 'total_liker_bronze');
    expect(totalLikerBronze).toBeDefined();
    expect(totalLikerBronze!.current).toBe(25);
  });

  it('receivedPaperCount가 주어지면 papers_received 진행도를 업데이트한다', () => {
    const stats = {
      totalMessages: 0,
      totalPosts: 0,
      totalComments: 0,
      totalLikedPosts: 0,
      totalLikedComments: 0,
    };

    syncBadgeProgressFromBackendStats(stats, 25);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    const paperReceiver = progress.find((p) => p.badgeId === 'paper_receiver_bronze');
    expect(paperReceiver).toBeDefined();
    expect(paperReceiver!.current).toBe(25);
  });

  it('receivedPaperCount가 없으면 papers_received는 업데이트하지 않는다', () => {
    const stats = {
      totalMessages: 0,
      totalPosts: 0,
      totalComments: 0,
      totalLikedPosts: 0,
      totalLikedComments: 0,
    };

    syncBadgeProgressFromBackendStats(stats);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    const paperReceiver = progress.find((p) => p.badgeId === 'paper_receiver_bronze');
    expect(paperReceiver).toBeUndefined();
  });

  it('total_events에 calculateActivityScore 결과를 사용한다', () => {
    const stats = {
      totalMessages: 0,
      totalPosts: 10,
      totalComments: 20,
      totalLikedPosts: 0,
      totalLikedComments: 0,
    };

    syncBadgeProgressFromBackendStats(stats);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    // 모킹된 calculateActivityScore: totalPosts + totalComments = 30
    const activity100 = progress.find((p) => p.badgeId === 'activity_100');
    expect(activity100).toBeDefined();
    expect(activity100!.current).toBe(30);
  });

  it('필드가 없으면 0으로 처리한다', () => {
    const stats = {
      totalMessages: 0,
      totalPosts: 0,
      totalComments: 0,
      totalLikedPosts: 0,
      totalLikedComments: 0,
    };

    syncBadgeProgressFromBackendStats(stats);

    const progress: BadgeProgress[] = JSON.parse(
      localStorage.getItem('bimillog_badge_progress')!
    );

    const writerBronze = progress.find((p) => p.badgeId === 'writer_bronze');
    expect(writerBronze).toBeDefined();
    expect(writerBronze!.current).toBe(0);
    expect(writerBronze!.percentage).toBe(0);
  });
});
