"use client";

import React, { useEffect, useRef, useState } from "react";
import { Mail, UserPlus } from "lucide-react";
import { Button, Spinner } from "@/components";
import { useReceivedFriendRequests } from "@/hooks/api/useFriendQueries";
import { ReceivedRequestItem } from "./ReceivedRequestItem";
import type { PageResponse } from "@/types/common";
import type { ReceivedFriendRequest } from "@/types/domains/friend";

interface ReceivedRequestListProps {
  initialData?: PageResponse<ReceivedFriendRequest> | null;
}

/**
 * 받은 친구 요청 목록 컴포넌트
 *
 * 라운드 9: paper/ink 토큰, 편지 메타포 카피, totalElements 표시,
 * 페이지 변경 시 list 포커스 (B-009-E),
 * keepPreviousData 로 깜박임 방지 (B-009-A — useReceivedFriendRequests 내부).
 */
export const ReceivedRequestList: React.FC<ReceivedRequestListProps> = React.memo(({ initialData }) => {
  const [page, setPage] = useState(0);
  const size = 20;
  const listRef = useRef<HTMLUListElement | null>(null);
  const lastPageRef = useRef(0);

  const { data, isLoading, error } = useReceivedFriendRequests(page, size, true, page === 0 ? initialData : undefined);

  const requestData = data?.data;
  const requests = requestData?.content || [];
  const totalPages = requestData?.totalPages || 0;
  const totalElements = requestData?.totalElements ?? 0;
  const isEmpty = requestData?.empty ?? true;

  useEffect(() => {
    if (lastPageRef.current !== page) {
      lastPageRef.current = page;
      listRef.current?.focus();
    }
  }, [page]);

  if (isLoading && !data) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Spinner message="받은 요청을 불러오는 중..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 text-center" role="alert">
        <UserPlus className="w-16 h-16 mx-auto mb-4 text-ink-soft" aria-hidden="true" />
        <p className="text-ink break-keep">받은 요청을 불러올 수 없어요.</p>
      </div>
    );
  }

  return (
    <div>
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold font-display text-ink flex items-center gap-2 break-keep">
          <UserPlus className="w-5 h-5 text-postal-navy" aria-hidden="true" />
          받은 친구 요청
        </h2>
        {!isEmpty && (
          <span className="text-sm text-ink-soft" aria-label={`총 ${totalElements}건`}>
            {totalElements}건
          </span>
        )}
      </div>

      {/* 리스트 */}
      {isEmpty ? (
        <div
          className="text-center py-16 bg-paper-100 border border-postal-navy/20 rounded-lg"
          role="status"
        >
          <Mail className="w-16 h-16 mx-auto mb-4 text-postal-navy/60" aria-hidden="true" />
          <p className="text-ink font-medium break-keep">
            도착한 친구 요청이 없어요
          </p>
          <p className="text-sm text-ink-soft mt-2 break-keep">
            새 편지를 기다려볼까요?
          </p>
        </div>
      ) : (
        <>
          <ul
            ref={listRef}
            tabIndex={-1}
            aria-label="받은 친구 요청 목록"
            className="bg-paper-card border border-postal-navy/20 rounded-lg overflow-hidden focus:outline-none focus:ring-2 focus:ring-postal-navy/40"
          >
            {requests.map((request) => (
              <ReceivedRequestItem key={request.friendRequestId} request={request} />
            ))}
          </ul>

          {totalPages > 1 && (
            <nav
              className="flex justify-center items-center gap-2 mt-6"
              aria-label="받은 요청 페이지"
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

ReceivedRequestList.displayName = "ReceivedRequestList";
