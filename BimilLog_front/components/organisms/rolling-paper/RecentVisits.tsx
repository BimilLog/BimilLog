"use client";

import { useState, useEffect, memo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components";
import { Button } from "@/components";
import { TimeBadge } from "@/components";
import { X, Trash2 } from "lucide-react";
import Link from "next/link";
import {
  getRecentVisits,
  removeRecentVisit,
  clearRecentVisits,
} from "@/lib/utils/storage";
import { useConfirmModal } from "@/components/molecules/modals/confirm-modal";

interface RecentVisit {
  nickname: string;
  visitedAt: string;
  displayName: string;
}

interface RecentVisitsProps {
  /** 검색 모드일 때 시각적 강도를 낮춰 결과에 집중하도록 (라운드 5 UX) */
  dimmed?: boolean;
}

/**
 * 라운드 5 변경:
 * - 다크 모드 토큰 일관화 (paper / ink / postal-navy) — B-007
 * - confirm() native dialog → useConfirmModal — B-008
 * - dimmed prop 으로 검색 모드 시 시각 우선순위 하향
 */
export const RecentVisits: React.FC<RecentVisitsProps> = memo(({ dimmed = false }) => {
  const [recentVisits, setRecentVisits] = useState<RecentVisit[]>([]);
  const { confirm, ConfirmModalComponent } = useConfirmModal();

  useEffect(() => {
    setRecentVisits(getRecentVisits());
  }, []);

  const handleRemoveVisit = (nickname: string) => {
    removeRecentVisit(nickname);
    setRecentVisits(getRecentVisits());
  };

  const handleClearAll = async () => {
    const confirmed = await confirm({
      title: "최근 방문 기록 삭제",
      message: "최근 방문한 롤링페이퍼 기록을 모두 삭제하시겠어요?\n삭제하면 되돌릴 수 없어요.",
      confirmText: "삭제",
      cancelText: "취소",
      confirmButtonVariant: "destructive",
      icon: <Trash2 className="h-8 w-8 stroke-stamp-red" />,
    });

    if (confirmed) {
      clearRecentVisits();
      setRecentVisits([]);
    }
  };

  if (recentVisits.length === 0) {
    return null;
  }

  return (
    <>
      <Card
        variant="elevated"
        aria-hidden={dimmed ? true : undefined}
        className={
          dimmed
            ? "opacity-50 pointer-events-none transition-opacity duration-200"
            : "transition-opacity duration-200"
        }
      >
        <CardHeader className="text-center">
          <div className="flex items-center justify-center">
            <CardTitle className="flex items-center space-x-2 text-lg text-postal-navy dark:text-postal-navy">
              <span className="font-display font-bold">최근 방문한 롤링페이퍼</span>
            </CardTitle>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClearAll}
              className="text-ink-soft dark:text-muted-foreground hover:text-stamp-red dark:hover:text-stamp-red hover:bg-stamp-red/10"
              aria-label="최근 방문 기록 전체 삭제"
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
                className="flex items-center justify-between p-3 rounded-lg bg-paper-aged/50 dark:bg-muted/30 border border-ink-soft/40 dark:border-border hover:bg-paper-aged dark:hover:bg-muted/50 hover:border-postal-navy/40 transition-all duration-200"
              >
                <Link
                  href={`/rolling-paper/${visit.nickname}`}
                  prefetch={false}
                  className="flex items-center justify-between flex-1 min-w-0 text-ink dark:text-foreground hover:text-postal-navy dark:hover:text-postal-navy transition-colors"
                  aria-label={`${visit.displayName}님의 롤링페이퍼로 다시 방문`}
                >
                  <p className="font-body font-semibold text-ink dark:text-foreground text-sm truncate">
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
                  className="text-ink-soft dark:text-muted-foreground hover:text-stamp-red dark:hover:text-stamp-red hover:bg-stamp-red/10 h-8 w-8 p-0 flex-shrink-0"
                  aria-label={`${visit.displayName}님의 방문 기록 삭제`}
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            ))}
          </div>

          {/* 로컬스토리지 저장 정책 안내 메시지 */}
          <div className="mt-4 pt-3 border-t border-ink-soft/30 dark:border-border">
            <p className="text-xs font-body text-ink-soft dark:text-muted-foreground text-center flex items-center justify-center space-x-1">
              <span>클릭하여 다시 방문하세요</span>
            </p>
          </div>
        </CardContent>
      </Card>
      <ConfirmModalComponent />
    </>
  );
});

RecentVisits.displayName = "RecentVisits";
