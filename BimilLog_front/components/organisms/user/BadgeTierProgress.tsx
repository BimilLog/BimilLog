"use client";

import React from 'react';
import { Card } from '@/components';
import { Medal } from 'lucide-react';
import { getBadgeColor, type Badge } from '@/lib/utils/badges';

type BadgeTier = Badge['tier'];

interface BadgeTierProgressProps {
  tierCounts: Array<{ tier: BadgeTier; total: number; unlocked: number }>;
}

const BadgeTierProgress = React.memo(({ tierCounts }: BadgeTierProgressProps) => {
  return (
    <Card className="p-4">
      <h3 className="font-medium mb-4 flex items-center gap-2">
        <Medal className="w-4 h-4 stroke-yellow-500 fill-yellow-100" />
        티어별 진행도
      </h3>
      <div className="space-y-3">
        {tierCounts.map(({ tier, total, unlocked }) => (
          <div key={tier} className="flex items-center gap-3">
            <div
              className="w-8 h-8 rounded-full flex items-center justify-center"
              style={{ backgroundColor: `${getBadgeColor(tier)}20` }}
            >
              <Medal className="w-4 h-4" style={{ color: getBadgeColor(tier) }} />
            </div>
            <div className="flex-1">
              <div className="flex items-center justify-between mb-1">
                <span className="text-sm font-medium capitalize">{tier}</span>
                <span className="text-sm text-gray-500">
                  {unlocked} / {total}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="h-2 rounded-full transition-all"
                  style={{
                    backgroundColor: getBadgeColor(tier),
                    width: `${total > 0 ? (unlocked / total) * 100 : 0}%`,
                  }}
                />
              </div>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
});

BadgeTierProgress.displayName = 'BadgeTierProgress';

export default BadgeTierProgress;
