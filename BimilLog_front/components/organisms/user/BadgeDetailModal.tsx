"use client";

import React from 'react';
import { Card } from '@/components';
import { getBadgeColor, type Badge } from '@/lib/utils/badges';
import { ICON_MAP, CATEGORY_NAMES } from './BadgeCard';
import { Award } from 'lucide-react';

interface BadgeDetailModalProps {
  badge: Badge | null;
  onClose: () => void;
}

const BadgeDetailModal = React.memo(({ badge, onClose }: BadgeDetailModalProps) => {
  if (!badge) return null;

  return (
    <div
      className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <Card
        className="max-w-md w-full p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <div
              className="w-14 h-14 rounded-full flex items-center justify-center"
              style={{
                backgroundColor: `${getBadgeColor(badge.tier)}20`,
              }}
            >
              <div style={{ color: getBadgeColor(badge.tier) }}>
                {React.createElement(ICON_MAP[badge.icon] || Award, {
                  className: "w-7 h-7",
                })}
              </div>
            </div>
            <div>
              <h3 className="font-semibold text-lg">{badge.name}</h3>
              <p className="text-sm text-gray-500">
                {CATEGORY_NAMES[badge.category]} · {badge.tier}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>

        <p className="text-gray-600 mb-4">{badge.description}</p>

        <div className="space-y-3">
          <div className="flex items-center justify-between py-2 border-t">
            <span className="text-sm text-gray-500">목표</span>
            <span className="text-sm font-medium">
              {badge.requirement.target}
            </span>
          </div>
          <div className="flex items-center justify-between py-2 border-t">
            <span className="text-sm text-gray-500">현재</span>
            <span className="text-sm font-medium">
              {badge.requirement.current || 0}
            </span>
          </div>
          {badge.unlockedAt && (
            <div className="flex items-center justify-between py-2 border-t">
              <span className="text-sm text-gray-500">획득일</span>
              <span className="text-sm font-medium">
                {new Date(badge.unlockedAt).toLocaleDateString('ko-KR')}
              </span>
            </div>
          )}
        </div>

        {badge.progress !== undefined && badge.progress < 100 && (
          <div className="mt-4">
            <div className="flex justify-between text-sm text-gray-500 mb-2">
              <span>진행도</span>
              <span>{Math.round(badge.progress)}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="h-2 rounded-full bg-gradient-to-r from-purple-500 to-pink-500"
                style={{ width: `${badge.progress}%` }}
              />
            </div>
          </div>
        )}
      </Card>
    </div>
  );
});

BadgeDetailModal.displayName = 'BadgeDetailModal';

export default BadgeDetailModal;
