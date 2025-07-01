"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Clock, Heart, X, Trash2 } from "lucide-react";
import Link from "next/link";
import {
  getRecentVisits,
  removeRecentVisit,
  clearRecentVisits,
  getRelativeTimeString,
} from "@/lib/cookies";

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
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl">
      <CardHeader className="text-center pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center space-x-2 text-lg bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent">
            <Clock className="w-5 h-5 text-blue-600" />
            <span className="font-bold">최근 방문한 롤링페이퍼</span>
          </CardTitle>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleClearAll}
            className="text-gray-400 hover:text-red-500 hover:bg-red-50"
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
        <p className="text-gray-600 text-xs mt-2">클릭하여 다시 방문하세요</p>
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
                className="flex items-center space-x-3 flex-1 min-w-0 hover:text-blue-600 transition-colors"
              >
                <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-cyan-600 rounded-full flex items-center justify-center flex-shrink-0">
                  <Heart className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-gray-800 text-sm truncate">
                    {visit.displayName}님의 롤링페이퍼
                  </p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {getRelativeTimeString(visit.visitedAt)}
                  </p>
                </div>
              </Link>
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.preventDefault();
                  handleRemoveVisit(visit.nickname);
                }}
                className="text-gray-400 hover:text-red-500 hover:bg-red-50 h-8 w-8 p-0 flex-shrink-0"
              >
                <X className="w-4 h-4" />
              </Button>
            </div>
          ))}
        </div>

        <div className="mt-4 pt-3 border-t border-gray-200">
          <p className="text-xs text-gray-500 text-center">
            💡 최근 {recentVisits.length}개의 방문 기록 (최대 5개까지
            저장됩니다)
          </p>
        </div>
      </CardContent>
    </Card>
  );
};
