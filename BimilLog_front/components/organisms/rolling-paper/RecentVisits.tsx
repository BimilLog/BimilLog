"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Button } from "@/components";
import { TimeBadge } from "@/components";
import { X, Trash2} from "lucide-react";
import Link from "next/link";
import {
  getRecentVisits,
  removeRecentVisit,
  clearRecentVisits,
} from "@/lib/utils/storage";

interface RecentVisit {
  nickname: string;
  visitedAt: string;
  displayName: string;
}

export const RecentVisits: React.FC = () => {
  const [recentVisits, setRecentVisits] = useState<RecentVisit[]>([]);

  useEffect(() => {
    setRecentVisits(getRecentVisits());
  }, []);

  const handleRemoveVisit = (nickname: string) => {
    removeRecentVisit(nickname);
    setRecentVisits(getRecentVisits());
  };

  const handleClearAll = () => {
    if (confirm("최근 방문 기록을 모두 삭제하시겠습니까?")) {
      clearRecentVisits();
      setRecentVisits([]);
    }
  };

  if (recentVisits.length === 0) {
    return null;
  }

  return (
    <Card variant="elevated">
      <CardHeader className="text-center">
          <div className="flex items-center justify-center">
            <CardTitle className="flex items-center space-x-2 text-lg text-blue-600">
              <span className="font-bold">최근 방문한 롤링페이퍼</span>
            </CardTitle>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleClearAll}
            className="text-brand-secondary hover:text-red-500 hover:bg-red-50"
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent className="p-4 space-y-4">
        <div className="space-y-3">
          {recentVisits.map((visit) => (
            <div
              key={visit.nickname}
              className="flex items-center justify-between p-3 rounded-lg bg-gray-50 border border-gray-200 hover:bg-gray-100 hover:border-blue-300 transition-all duration-200"
            >
              <Link
                href={`/rolling-paper/${visit.nickname}`}
                className="flex items-center justify-between flex-1 min-w-0 text-gray-900 hover:text-blue-600 transition-colors"
              >
                <p className="font-semibold text-gray-900 text-sm truncate">
                  {visit.displayName}님의 롤링페이퍼
                </p>
                <TimeBadge dateString={visit.visitedAt} size="xs" />
              </Link>
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.preventDefault(); // Link 컴포넌트의 기본 네비게이션 방지
                  handleRemoveVisit(visit.nickname);
                }}
                className="text-brand-secondary hover:text-red-500 hover:bg-red-50 h-8 w-8 p-0 flex-shrink-0"
              >
                <X className="w-4 h-4" />
              </Button>
            </div>
          ))}
        </div>

        {/* 로컬스토리지 저장 정책 안내 메시지 */}
        <div className="mt-4 pt-3 border-t border-gray-200">
          <p className="text-xs text-brand-secondary text-center flex items-center justify-center space-x-1">
            <span>클릭하여 다시 방문하세요</span>
          </p>
        </div>
      </CardContent>
    </Card>
  );
};
