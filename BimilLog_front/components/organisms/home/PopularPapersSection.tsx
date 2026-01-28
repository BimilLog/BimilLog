"use client";

import { Card, Button, Spinner } from "flowbite-react";
import { TrendingUp, MessageSquare, MessageCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { usePopularPapers } from "@/hooks/api/useRollingPaperQueries";
import type { PageResponse } from "@/types/common";
import type { PopularPaperInfo } from "@/types/domains/paper";

interface PopularPapersSectionProps {
  initialData?: PageResponse<PopularPaperInfo> | null;
}

export const PopularPapersSection: React.FC<PopularPapersSectionProps> = ({ initialData }) => {
  const router = useRouter();

  // SSR 데이터가 없으면 클라이언트에서 fetch (fallback)
  const { data: clientData, isLoading, isError } = usePopularPapers(0, 10, {
    enabled: !initialData, // SSR 데이터가 없을 때만 활성화
  });

  // SSR 데이터 우선, 없으면 클라이언트 데이터 사용
  const data = initialData || clientData;

  const handlePaperClick = (memberName: string) => {
    router.push(`/rolling-paper/${encodeURIComponent(memberName)}`);
  };

  return (
    <section className="w-full lg:w-[400px] lg:min-w-[400px]">
      <Card className="h-full">
        <div className="flex items-center gap-2 mb-4">
          <div className="w-8 h-8 bg-gradient-to-r from-pink-500 to-purple-500 rounded-full flex items-center justify-center">
            <TrendingUp className="w-5 h-5 text-white" />
          </div>
          <h2 className="text-xl font-bold text-foreground">
            실시간 인기 롤링페이퍼
          </h2>
        </div>

        {/* 로딩 상태 (클라이언트 fallback 시) */}
        {!initialData && isLoading && (
          <div className="flex flex-col items-center justify-center py-10 gap-3">
            <Spinner size="xl" color="purple" />
            <p className="text-sm text-muted-foreground">
              실시간 인기 롤링페이퍼를 불러오는 중입니다
            </p>
          </div>
        )}

        {/* 에러 상태 */}
        {!data && !isLoading && (
          <div className="text-center py-8">
            <p className="text-muted-foreground">
              인기 롤링페이퍼를 불러올 수 없습니다
            </p>
          </div>
        )}

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
                <div key={paper.memberId} className="space-y-2">
                  <div className="w-full flex items-center gap-3 p-3 rounded-lg hover:bg-accent transition-colors">
                    <div
                      className={cn(
                        "w-10 h-10 rounded-full flex items-center justify-center text-base font-bold flex-shrink-0",
                        paper.rank === 1 && "bg-gradient-to-br from-yellow-300 via-yellow-400 to-yellow-500 text-black shadow-lg",
                        paper.rank === 2 && "bg-gradient-to-br from-gray-200 via-gray-300 to-gray-400 text-black shadow-lg",
                        paper.rank === 3 && "bg-gradient-to-br from-orange-300 via-orange-400 to-orange-500 text-black shadow-lg",
                        paper.rank > 3 && "bg-white text-gray-900 font-semibold border border-gray-300"
                      )}
                    >
                      {paper.rank}
                    </div>

                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-foreground truncate">
                        {paper.memberName}님의 롤링페이퍼
                      </p>
                    </div>

                    <Button
                      type="button"
                      size="sm"
                      onClick={() => handlePaperClick(paper.memberName)}
                      className="bg-gradient-to-r from-purple-600 to-pink-600 text-white hover:from-purple-700 hover:to-pink-700 flex-shrink-0"
                    >
                      <MessageCircle className="h-4 w-4" />
                      <span className="ml-1 text-sm">롤링페이퍼</span>
                    </Button>
                  </div>

                  <div className="flex items-center gap-2 pl-3">
                    <div className="flex items-center gap-1 px-1.5 py-1 bg-muted rounded text-xs text-foreground">
                      <MessageSquare className="w-3.5 h-3.5" />
                      <span>최근 메시지 수: {paper.recentMessageCount}개</span>
                    </div>
                    <div className="flex items-center gap-1 px-1.5 py-1 bg-purple-100 dark:bg-purple-900 rounded text-xs">
                      <TrendingUp className="w-3.5 h-3.5 text-purple-600 dark:text-purple-300" />
                      <span className="font-medium text-purple-600 dark:text-purple-300">
                        {paper.popularityScore.toFixed(1)}점
                      </span>
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
};
