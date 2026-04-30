"use client";

import React, { useState, useEffect } from 'react';
import { Card } from '@/components';
import { Button } from '@/components';
import { useBadges } from '@/hooks/features/useBadges';
import { useMyRollingPaper } from '@/hooks/api/useMyRollingPaper';
import { getBadgeColor, type Badge } from '@/lib/utils/badges';
import type { UserStats } from '@/hooks/features/user/useUserStats';
import {
  Award,
  Trophy,
  Target,
  Sparkles,
  ChevronDown,
} from 'lucide-react';
import BadgeCard, { ICON_MAP, CATEGORY_NAMES } from './BadgeCard';
import BadgeDetailModal from './BadgeDetailModal';
import BadgeTierProgress from './BadgeTierProgress';

interface ProfileBadgesProps {
  userStats?: UserStats | null;
}

export const ProfileBadges = React.memo(({ userStats }: ProfileBadgesProps) => {
  // 받은 롤링페이퍼 조회
  const { data: myPaperData } = useMyRollingPaper();
  const receivedPaperCount = myPaperData?.success ? myPaperData.data?.myMessageDTOList?.length || 0 : 0;

  const {
    allBadges,
    unlockedBadges,
    nextBadges,
    tierCounts,
    recentBadges,
    completionRate,
    isLoading,
  } = useBadges(userStats, receivedPaperCount);

  const [selectedBadge, setSelectedBadge] = useState<Badge | null>(null);
  const [activeCategory, setActiveCategory] = useState<Badge['category'] | 'all'>('all');
  const [visibleCount, setVisibleCount] = useState(10);
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth < 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);

    return () => window.removeEventListener('resize', checkMobile);
  }, []);

  useEffect(() => {
    setVisibleCount(10);
  }, [activeCategory]);

  if (isLoading) {
    return (
      <div className="space-y-4 mb-8">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2 text-ink-900 dark:text-ink-100">
          <Trophy className="w-5 h-5 text-seal-gold" aria-hidden="true" />
          프로필 뱃지
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(8)].map((_, i) => (
            <Card key={i} className="p-4 animate-pulse bg-paper-50 dark:bg-paper-900">
              <div className="h-24 bg-postal-navy/10 dark:bg-postal-navy/20 rounded" />
            </Card>
          ))}
        </div>
      </div>
    );
  }

  const filteredBadges = activeCategory === 'all'
    ? allBadges
    : allBadges.filter(b => b.category === activeCategory);

  const displayedBadges = isMobile ? filteredBadges.slice(0, visibleCount) : filteredBadges;
  const hasMore = isMobile && visibleCount < filteredBadges.length;

  const handleLoadMore = () => {
    setVisibleCount(prev => prev + 10);
  };

  return (
    <div className="space-y-6 mb-8">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold flex items-center gap-2 text-ink-900 dark:text-ink-100">
          <Trophy className="w-5 h-5 text-seal-gold" aria-hidden="true" />
          프로필 뱃지
        </h2>
        <div className="text-sm text-ink-soft dark:text-ink-300">
          {unlockedBadges.length} / {allBadges.length} 획득 ({completionRate}%)
        </div>
      </div>

      {/* 통계 카드 — paper 토큰 + ring 액센트 (다크 일관성) */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        <Card className="p-4 bg-paper-50 dark:bg-paper-900 ring-1 ring-seal-gold/30 dark:ring-seal-gold/40">
          <div className="flex items-center justify-between mb-2">
            <Trophy className="w-5 h-5 text-seal-gold" aria-hidden="true" />
            <span className="text-2xl font-bold text-ink-900 dark:text-ink-100">{unlockedBadges.length}</span>
          </div>
          <p className="text-sm text-ink-soft dark:text-ink-300">획득한 뱃지</p>
        </Card>

        <Card className="p-4 bg-paper-50 dark:bg-paper-900 ring-1 ring-postal-navy/30 dark:ring-postal-navy/40">
          <div className="flex items-center justify-between mb-2">
            <Target className="w-5 h-5 text-postal-navy" aria-hidden="true" />
            <span className="text-2xl font-bold text-ink-900 dark:text-ink-100">{completionRate}%</span>
          </div>
          <p className="text-sm text-ink-soft dark:text-ink-300">달성률</p>
        </Card>

        <Card className="p-4 bg-paper-50 dark:bg-paper-900 ring-1 ring-stamp-red/30 dark:ring-stamp-red/40">
          <div className="flex items-center justify-between mb-2">
            <Sparkles className="w-5 h-5 text-stamp-red" aria-hidden="true" />
            <span className="text-2xl font-bold text-ink-900 dark:text-ink-100">{recentBadges.length}</span>
          </div>
          <p className="text-sm text-ink-soft dark:text-ink-300">최근 획득</p>
        </Card>
      </div>

      {/* 다음 달성 가능한 뱃지 */}
      {nextBadges.length > 0 && (
        <Card className="p-4 bg-paper-50 dark:bg-paper-900">
          <h3 className="font-medium mb-3 flex items-center gap-2 text-ink-900 dark:text-ink-100">
            <Target className="w-4 h-4 text-postal-navy" aria-hidden="true" />
            곧 달성 가능한 뱃지
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {nextBadges.slice(0, 3).map(badge => {
              const Icon = ICON_MAP[badge.icon] || Award;
              const progress = badge.progress || 0;

              return (
                <div
                  key={badge.id}
                  className="flex items-center gap-3 p-3 bg-paper-100 dark:bg-paper-200 rounded-lg"
                >
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center"
                    style={{ backgroundColor: `${getBadgeColor(badge.tier)}20` }}
                  >
                    <div style={{ color: getBadgeColor(badge.tier) }}>
                      <Icon className="w-5 h-5" aria-hidden="true" />
                    </div>
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-ink-900 dark:text-ink-100">{badge.name}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <div className="flex-1 bg-postal-navy/15 dark:bg-postal-navy/30 rounded-full h-1.5">
                        <div
                          className="h-1.5 rounded-full bg-postal-navy"
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                      <span className="text-xs text-ink-soft dark:text-ink-300">{Math.round(progress)}%</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      )}

      {/* 카테고리 필터 — 라운드 9 friend 탭 패턴 (postal-navy 토큰) */}
      <div
        className="flex gap-2 overflow-x-auto pb-2"
        role="tablist"
        aria-label="뱃지 카테고리"
      >
        <button
          type="button"
          role="tab"
          aria-selected={activeCategory === 'all'}
          onClick={() => setActiveCategory('all')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap break-keep min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy focus-visible:ring-offset-1 focus-visible:ring-offset-paper-50 ${
            activeCategory === 'all'
              ? 'bg-postal-navy text-paper-50'
              : 'bg-postal-navy/15 text-postal-navy hover:bg-postal-navy/25 dark:bg-postal-navy/30 dark:text-ink-900 dark:hover:bg-postal-navy/40'
          }`}
        >
          전체 ({allBadges.length})
        </button>
        {Object.entries(CATEGORY_NAMES).map(([key, name]) => {
          const count = allBadges.filter(b => b.category === key).length;
          const unlockedCount = unlockedBadges.filter(b => b.category === key).length;

          return (
            <button
              type="button"
              role="tab"
              aria-selected={activeCategory === key}
              key={key}
              onClick={() => setActiveCategory(key as Badge['category'])}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap break-keep min-h-[44px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy focus-visible:ring-offset-1 focus-visible:ring-offset-paper-50 ${
                activeCategory === key
                  ? 'bg-postal-navy text-paper-50'
                  : 'bg-postal-navy/15 text-postal-navy hover:bg-postal-navy/25 dark:bg-postal-navy/30 dark:text-ink-900 dark:hover:bg-postal-navy/40'
              }`}
            >
              {name} ({unlockedCount}/{count})
            </button>
          );
        })}
      </div>

      {/* 뱃지 그리드 */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
        {displayedBadges.map(badge => {
          const isUnlocked = unlockedBadges.some(u => u.id === badge.id);

          return (
            <BadgeCard
              key={badge.id}
              badge={badge}
              isUnlocked={isUnlocked}
              progress={badge.progress}
              onClick={() => setSelectedBadge(badge)}
            />
          );
        })}
      </div>

      {/* 더보기 버튼 (모바일) */}
      {hasMore && (
        <div className="flex justify-center">
          <Button
            onClick={handleLoadMore}
            variant="outline"
            size="default"
            className="w-full md:w-auto"
          >
            <ChevronDown className="w-4 h-4 mr-2" />
            더보기 ({filteredBadges.length - visibleCount}개 남음)
          </Button>
        </div>
      )}

      {/* 티어별 진행도 */}
      <BadgeTierProgress tierCounts={tierCounts} />

      {/* 뱃지 상세 모달 */}
      <BadgeDetailModal
        badge={selectedBadge}
        onClose={() => setSelectedBadge(null)}
      />
    </div>
  );
});

ProfileBadges.displayName = 'ProfileBadges';
