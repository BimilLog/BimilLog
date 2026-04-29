"use client";

import { memo } from "react";
import { Card } from "flowbite-react";
import { TrendingUp, MessageSquare } from "lucide-react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { usePopularPapers } from "@/hooks/api/useRollingPaperQueries";
import type { CursorPageResponse } from "@/types/common";
import type { PopularPaperInfo } from "@/types/domains/paper";

interface PopularPapersSectionProps {
  initialData?: CursorPageResponse<PopularPaperInfo> | null;
}

export const PopularPapersSection: React.FC<PopularPapersSectionProps> = memo(({ initialData }) => {
  const router = useRouter();

  // SSR 데이터가 없으면 클라이언트에서 fetch (fallback)
  const {
    data: clientData,
    isLoading,
    isError,
    refetch,
  } = usePopularPapers(10, {
    enabled: !initialData, // SSR 데이터가 없을 때만 활성화
  });

  // SSR 데이터 우선, 없으면 클라이언트 데이터 사용
  const data = initialData || clientData;

  const handlePaperClick = (memberName: string) => {
    router.push(`/rolling-paper/${encodeURIComponent(memberName)}`);
  };

  const handleRowKeyDown = (e: React.KeyboardEvent, memberName: string) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handlePaperClick(memberName);
    }
  };

  return (
    <section
      data-testid="popular-papers-section"
      className="lg:w-[400px] lg:flex-shrink-0 h-full"
    >
      <Card className="h-full bg-paper-card border border-ink-soft washi-tape">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-8 h-8 bg-paper-button rounded-full flex items-center justify-center shadow-brand-sm">
            <TrendingUp className="w-5 h-5 text-white" />
          </div>
          <h2 className="font-display text-xl font-bold text-ink dark:text-foreground">
            실시간 인기 롤링페이퍼
          </h2>
        </div>

        {/* 로딩 상태 (클라이언트 fallback 시) - Skeleton 8행 */}
        {!initialData && isLoading && (
          <div
            data-testid="popular-papers-skeleton"
            className="space-y-2 animate-pulse"
            aria-busy="true"
            aria-label="실시간 인기 롤링페이퍼를 불러오는 중"
          >
            {Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                className="bg-muted h-14 rounded-lg"
              />
            ))}
          </div>
        )}

        {/* 에러 상태 (fetch 실패) - 재시도 버튼 노출 */}
        {!initialData && !isLoading && isError && (
          <div className="text-center py-8 space-y-3">
            <p className="text-muted-foreground text-sm">
              인기 롤링페이퍼를 불러오지 못했습니다
            </p>
            <button
              type="button"
              data-testid="popular-papers-retry"
              onClick={() => refetch()}
              className="inline-flex items-center justify-center min-h-touch px-4 py-2 rounded-md border border-border bg-background text-foreground hover:bg-accent transition-colors focus-visible:ring-2 focus-visible:ring-purple-400 focus-visible:ring-offset-2"
            >
              다시 불러오기
            </button>
          </div>
        )}

        {/* 데이터 도착 후 표시 */}
        {data && (
          <div className="space-y-2">
            {data.content.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-muted-foreground">
                  아직 인기 롤링페이퍼가 없습니다
                </p>
              </div>
            ) : (
              data.content.map((paper) => (
                <div
                  key={paper.memberId}
                  role="button"
                  tabIndex={0}
                  data-testid="popular-paper-row"
                  aria-label={`${paper.memberName}님의 롤링페이퍼 방문하기 (최근 24시간 메시지 ${paper.recentMessageCount}개)`}
                  onClick={() => handlePaperClick(paper.memberName)}
                  onKeyDown={(e) => handleRowKeyDown(e, paper.memberName)}
                  className="flex items-center gap-3 p-3 rounded-lg cursor-pointer hover:bg-accent transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-400"
                >
                  <div
                    className={cn(
                      "w-10 h-10 rounded-full flex items-center justify-center text-base font-bold flex-shrink-0",
                      paper.rank === 1 && "bg-gradient-to-br from-yellow-300 via-yellow-400 to-yellow-500 text-black shadow-lg",
                      paper.rank === 2 && "bg-gradient-to-br from-gray-200 via-gray-300 to-gray-400 text-black shadow-lg",
                      paper.rank === 3 && "bg-gradient-to-br from-orange-300 via-orange-400 to-orange-500 text-black shadow-lg",
                      paper.rank > 3 && "bg-card text-card-foreground border border-border"
                    )}
                  >
                    {paper.rank}
                  </div>

                  <div className="flex-1 min-w-0">
                    <p
                      className="text-sm font-semibold text-foreground truncate"
                      title={paper.memberName}
                    >
                      {paper.memberName}님의 롤링페이퍼
                    </p>
                    <div className="flex items-center gap-2 mt-1 flex-wrap">
                      <div className="flex items-center gap-1 px-1.5 py-0.5 bg-muted rounded text-xs text-foreground">
                        <MessageSquare className="w-3 h-3" />
                        <span>최근 24시간 메시지 {paper.recentMessageCount}개</span>
                      </div>
                      <div className="flex items-center gap-1 px-1.5 py-0.5 bg-secondary text-secondary-foreground rounded text-xs">
                        <TrendingUp className="w-3 h-3" />
                        <span className="font-medium">
                          {paper.popularityScore.toFixed(1)}점
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </Card>
    </section>
  );
});

PopularPapersSection.displayName = "PopularPapersSection";
