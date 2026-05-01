"use client";

import React, { useEffect, useRef, useState } from "react";
import { MailOpen, Users } from "lucide-react";
import { Button, Spinner } from "@/components";
import { EmptyView } from "@/components/molecules/feedback/EmptyView";
import { useMyFriends } from "@/hooks/api/useFriendQueries";
import { FriendListItem } from "./FriendListItem";
import type { PageResponse } from "@/types/common";
import type { Friend } from "@/types/domains/friend";

interface FriendListProps {
  initialData?: PageResponse<Friend> | null;
}

/**
 * 내 친구 목록 컴포넌트
 *
 * 라운드 9: paper/ink 토큰 일괄 적용, totalElements 표시,
 * 페이지 변경 후 list 영역에 포커스 이동(B-009-E).
 */
export const FriendList: React.FC<FriendListProps> = React.memo(({ initialData }) => {
  const [page, setPage] = useState(0);
  const size = 20;
  const listRef = useRef<HTMLUListElement | null>(null);
  const lastPageRef = useRef(0);

  const { data, isLoading, error } = useMyFriends(page, size, true, page === 0 ? initialData : undefined);

  const friendData = data?.data;
  const friends = friendData?.content || [];
  const totalPages = friendData?.totalPages || 0;
  const totalElements = friendData?.totalElements ?? 0;
  const isEmpty = friendData?.empty ?? true;

  // 페이지 변경 후 포커스 → list 영역
  useEffect(() => {
    if (lastPageRef.current !== page) {
      lastPageRef.current = page;
      listRef.current?.focus();
    }
  }, [page]);

  if (isLoading && !data) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner message="친구 목록을 불러오는 중..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 text-center" role="alert">
        <Users className="w-16 h-16 mx-auto mb-4 text-ink-soft" aria-hidden="true" />
        <p className="text-ink break-keep">친구 목록을 불러올 수 없어요.</p>
      </div>
    );
  }

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold font-display text-ink flex items-center gap-2 break-keep">
          <Users className="w-5 h-5 text-postal-navy" aria-hidden="true" />
          내 친구
        </h2>
        {!isEmpty && (
          <span className="text-sm text-ink-soft" aria-label={`총 ${totalElements}명`}>
            {totalElements}명
          </span>
        )}
      </div>

      {/* 리스트 — 라운드 17 F-17-BUG-10: ad-hoc 빈 상태 → EmptyView 통일 */}
      {isEmpty ? (
        <EmptyView
          title="아직 같이 편지를 주고받을 친구가 없어요"
          description="추천 탭에서 새로운 인연을 찾아보세요"
          icon={<MailOpen className="w-9 h-9" strokeWidth={1.6} aria-hidden="true" />}
          compact
        />
      ) : (
        <>
          <ul
            ref={listRef}
            tabIndex={-1}
            aria-label="내 친구 목록"
            className="bg-paper-card border border-postal-navy/20 rounded-lg overflow-hidden focus:outline-none focus:ring-2 focus:ring-postal-navy/40"
          >
            {friends.map((friend) => (
              <FriendListItem key={friend.friendMemberId} friend={friend} />
            ))}
          </ul>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <nav
              className="flex justify-center items-center gap-2 mt-6"
              aria-label="친구 목록 페이지"
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

FriendList.displayName = "FriendList";
