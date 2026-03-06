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
  const receivedPaperCount = myPaperData?.success ? myPaperData.data?.length || 0 : 0;

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
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Trophy className="w-5 h-5 stroke-yellow-500 fill-yellow-100" />
          프로필 뱃지
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(8)].map((_, i) => (
            <Card key={i} className="p-4 animate-pulse">
              <div className="h-24 bg-gray-200 rounded" />
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
        <h2 className="text-lg font-semibold flex items-center gap-2">
          <Trophy className="w-5 h-5 stroke-yellow-500 fill-yellow-100" />
          프로필 뱃지
        </h2>
        <div className="text-sm text-gray-500">
          {unlockedBadges.length} / {allBadges.length} 획득 ({completionRate}%)
        </div>
      </div>

      {/* 통계 카드 */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        <Card className="p-4 bg-gradient-to-br from-yellow-50 to-orange-50">
          <div className="flex items-center justify-between mb-2">
            <Trophy className="w-5 h-5 stroke-yellow-500 fill-yellow-100" />
            <span className="text-2xl font-bold">{unlockedBadges.length}</span>
          </div>
          <p className="text-sm text-gray-600">획득한 뱃지</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-purple-50 to-pink-50">
          <div className="flex items-center justify-between mb-2">
            <Target className="w-5 h-5 stroke-purple-600 fill-purple-100" />
            <span className="text-2xl font-bold">{completionRate}%</span>
          </div>
          <p className="text-sm text-gray-600">달성률</p>
        </Card>

        <Card className="p-4 bg-gradient-to-br from-green-50 to-emerald-50">
          <div className="flex items-center justify-between mb-2">
            <Sparkles className="w-5 h-5 stroke-green-600 fill-green-100" />
            <span className="text-2xl font-bold">{recentBadges.length}</span>
          </div>
          <p className="text-sm text-gray-600">최근 획득</p>
        </Card>
      </div>

      {/* 다음 달성 가능한 뱃지 */}
      {nextBadges.length > 0 && (
        <Card className="p-4">
          <h3 className="font-medium mb-3 flex items-center gap-2">
            <Target className="w-4 h-4 stroke-purple-600 fill-purple-100" />
            곧 달성 가능한 뱃지
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {nextBadges.slice(0, 3).map(badge => {
              const Icon = ICON_MAP[badge.icon] || Award;
              const progress = badge.progress || 0;

              return (
                <div
                  key={badge.id}
                  className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg"
                >
                  <div
                    className="w-10 h-10 rounded-full flex items-center justify-center"
                    style={{ backgroundColor: `${getBadgeColor(badge.tier)}20` }}
                  >
                    <div style={{ color: getBadgeColor(badge.tier) }}>
                      <Icon className="w-5 h-5" />
                    </div>
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium">{badge.name}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <div className="flex-1 bg-gray-200 rounded-full h-1.5">
                        <div
                          className="h-1.5 rounded-full bg-gradient-to-r from-purple-500 to-pink-500"
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                      <span className="text-xs text-gray-500">{Math.round(progress)}%</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </Card>
      )}

      {/* 카테고리 필터 */}
      <div className="flex gap-2 overflow-x-auto pb-2">
        <button
          onClick={() => setActiveCategory('all')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${
            activeCategory === 'all'
              ? 'bg-purple-100 text-purple-700'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          전체 ({allBadges.length})
        </button>
        {Object.entries(CATEGORY_NAMES).map(([key, name]) => {
          const count = allBadges.filter(b => b.category === key).length;
          const unlockedCount = unlockedBadges.filter(b => b.category === key).length;

          return (
            <button
              key={key}
              onClick={() => setActiveCategory(key as Badge['category'])}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${
                activeCategory === key
                  ? 'bg-purple-100 text-purple-700'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
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
