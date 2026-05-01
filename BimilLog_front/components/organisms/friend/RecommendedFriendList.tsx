"use client";

import React, { useEffect, useRef, useState } from "react";
import { Sparkles } from "lucide-react";
import { Button, Spinner } from "@/components";
import { EmptyView } from "@/components/molecules/feedback/EmptyView";
import { useRecommendedFriends } from "@/hooks/api/useFriendQueries";
import { RecommendedFriendItem } from "./RecommendedFriendItem";
import type { PageResponse } from "@/types/common";
import type { RecommendedFriend } from "@/types/domains/friend";

interface RecommendedFriendListProps {
  initialData?: PageResponse<RecommendedFriend> | null;
}

/**
 * 추천 친구 목록 컴포넌트
 * 2촌, 3촌 친구를 추천 점수별로 표시
 *
 * 라운드 9: paper/ink 토큰, 편지 메타포 카피, totalElements,
 * 페이지 변경 시 list 포커스 (B-009-E).
 */
export const RecommendedFriendList: React.FC<RecommendedFriendListProps> = React.memo(({ initialData }) => {
  const [page, setPage] = useState(0);
  const size = 10;
  const listRef = useRef<HTMLUListElement | null>(null);
  const lastPageRef = useRef(0);

  const { data, isLoading, error } = useRecommendedFriends(page, size, page === 0 ? initialData : undefined);

  const recommendedData = data?.data;
  const friends = recommendedData?.content || [];
  const totalPages = recommendedData?.totalPages || 0;
  const totalElements = recommendedData?.totalElements ?? 0;
  const isEmpty = recommendedData?.empty ?? true;

  useEffect(() => {
    if (lastPageRef.current !== page) {
      lastPageRef.current = page;
      listRef.current?.focus();
    }
  }, [page]);

  if (isLoading && !data) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner message="추천 친구를 불러오는 중..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 text-center" role="alert">
        <Sparkles className="w-16 h-16 mx-auto mb-4 text-ink-soft" aria-hidden="true" />
        <p className="text-ink break-keep">추천 친구를 불러올 수 없어요.</p>
        <p className="text-sm text-ink-soft mt-2 break-keep">
          {error instanceof Error ? error.message : '잠시 후 다시 시도해 주세요'}
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold font-display text-ink flex items-center gap-2 break-keep">
          <Sparkles className="w-5 h-5 text-postal-navy" aria-hidden="true" />
          알 수도 있는 친구
        </h2>
        {!isEmpty && (
          <span className="text-sm text-ink-soft" aria-label={`총 ${totalElements}명 추천`}>
            {totalElements}명 추천
          </span>
        )}
      </div>

      {/* 리스트 — 라운드 17 F-17-BUG-10: EmptyView 통일 */}
      {isEmpty ? (
        <EmptyView
          title="지금은 추천할 친구가 없어요"
          description="친구가 한 명만 늘어도 새로운 인연이 도착해요"
          icon={<Sparkles className="w-9 h-9" strokeWidth={1.6} aria-hidden="true" />}
          compact
        />
      ) : (
        <>
          <ul
            ref={listRef}
            tabIndex={-1}
            aria-label="추천 친구 목록"
            className="bg-paper-card border border-postal-navy/20 rounded-lg overflow-hidden focus:outline-none focus:ring-2 focus:ring-postal-navy/40"
          >
            {friends.map((friend) => (
              <RecommendedFriendItem key={friend.friendMemberId} friend={friend} />
            ))}
          </ul>

          {totalPages > 1 && (
            <nav
              className="flex justify-center items-center gap-2 mt-6"
              aria-label="추천 친구 페이지"
            >
              <Button
                color="light"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="min-h-[44px]"
              >
                이전
              </Button>
              <span className="text-sm text-ink-soft px-3" aria-live="polite">
                {page + 1} / {totalPages}
              </span>
              <Button
                color="light"
                size="sm"
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="min-h-[44px]"
              >
                다음
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
});

RecommendedFriendList.displayName = "RecommendedFriendList";
