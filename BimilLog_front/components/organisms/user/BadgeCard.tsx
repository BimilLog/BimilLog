"use client";

import React from 'react';
import { Popover } from 'flowbite-react';
import {
  Award,
  Trophy,
  Medal,
  Star,
  Zap,
  Lock,
  Shield,
  Flame,
  Heart,
  MessageCircle,
  PenTool,
  ThumbsUp,
  Search,
  Bookmark,
  Mail,
  Calendar,
  Moon,
  Sunrise,
  TrendingUp,
  Footprints,
  Inbox,
  Sparkles,
} from 'lucide-react';
import { getBadgeColor, type Badge } from '@/lib/utils/badges';

// 아이콘 매핑
export const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  Award,
  Trophy,
  Medal,
  Star,
  Zap,
  Shield,
  Flame,
  Heart,
  MessageCircle,
  PenTool,
  ThumbsUp,
  Search,
  Bookmark,
  Mail,
  Calendar,
  Moon,
  Sunrise,
  TrendingUp,
  Footprints,
  Inbox,
  Sparkles,
};

export const CATEGORY_NAMES: Record<Badge['category'], string> = {
  post: '글',
  comment: '댓글',
  like: '추천',
  paper: '롤링페이퍼',
  milestone: '마일스톤',
};

export interface BadgeCardProps {
  badge: Badge;
  isUnlocked: boolean;
  progress?: number;
  onClick: () => void;
}

const BadgeCard = React.memo(({ badge, isUnlocked, progress = 0, onClick }: BadgeCardProps) => {
  const Icon = ICON_MAP[badge.icon] || Award;
  const color = isUnlocked ? getBadgeColor(badge.tier) : '#9ca3af';
  const tierName = badge.tier.charAt(0).toUpperCase() + badge.tier.slice(1);
  const categoryName = CATEGORY_NAMES[badge.category];

  const popoverContent = (
    <div className="p-3 space-y-2 max-w-xs">
      <div className="flex items-center gap-2 mb-2">
        <div
          className="w-8 h-8 rounded-full flex items-center justify-center"
          style={{ backgroundColor: `${color}20` }}
        >
          <div style={{ color }}>
            <Icon className="w-4 h-4" />
          </div>
        </div>
        <div>
          <p className="font-semibold text-sm">{badge.name}</p>
          <p className="text-xs text-gray-500">{categoryName} · {tierName}</p>
        </div>
      </div>
      <p className="text-xs text-gray-600">{badge.description}</p>
      <div className="pt-2 border-t space-y-1">
        <div className="flex justify-between text-xs">
          <span className="text-gray-500">목표:</span>
          <span className="font-medium">{badge.requirement.target}</span>
        </div>
        <div className="flex justify-between text-xs">
          <span className="text-gray-500">현재:</span>
          <span className="font-medium">{badge.requirement.current || 0}</span>
        </div>
        {isUnlocked && badge.unlockedAt && (
          <div className="flex justify-between text-xs">
            <span className="text-gray-500">획득일:</span>
            <span className="font-medium">
              {new Date(badge.unlockedAt).toLocaleDateString('ko-KR')}
            </span>
          </div>
        )}
      </div>
      {!isUnlocked && progress > 0 && (
        <div className="pt-2">
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>진행도</span>
            <span className="font-medium">{Math.round(progress)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-1.5">
            <div
              className="h-1.5 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 transition-all"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );

  return (
    <Popover
      trigger="hover"
      placement="top"
      content={popoverContent}
    >
      <button
        onClick={onClick}
        className={`relative p-4 rounded-xl border transition-all ${
          isUnlocked
            ? 'bg-white hover:shadow-lg hover:scale-105 border-gray-200'
            : 'bg-gray-50 hover:bg-gray-100 border-gray-300 opacity-75'
        }`}
      >
        <div className="flex flex-col items-center gap-2">
          <div
            className={`w-12 h-12 rounded-full flex items-center justify-center ${
              isUnlocked ? '' : 'relative'
            }`}
            style={{ backgroundColor: `${color}20` }}
          >
            <div style={{ color }}>
              <Icon className="w-6 h-6" />
            </div>
            {!isUnlocked && (
              <div className="absolute inset-0 rounded-full bg-gray-900/20 flex items-center justify-center">
                <Lock className="w-4 h-4 stroke-white fill-white" />
              </div>
            )}
          </div>

          <div className="text-center">
            <p className={`text-sm font-medium ${isUnlocked ? 'text-gray-900' : 'text-gray-500'}`}>
              {badge.name}
            </p>
            <p className="text-xs text-gray-500 mt-0.5">{badge.description}</p>
          </div>

          {!isUnlocked && progress > 0 && (
            <div className="w-full mt-2">
              <div className="flex justify-between text-xs text-gray-500 mb-1">
                <span>진행도</span>
                <span>{Math.round(progress)}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-1.5">
                <div
                  className="h-1.5 rounded-full bg-gradient-to-r from-purple-500 to-pink-500 transition-all"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          )}

          {isUnlocked && badge.unlockedAt && (
            <p className="text-xs text-gray-400 mt-1">
              {new Date(badge.unlockedAt).toLocaleDateString('ko-KR')}
            </p>
          )}
        </div>
      </button>
    </Popover>
  );
});

BadgeCard.displayName = 'BadgeCard';

export default BadgeCard;
