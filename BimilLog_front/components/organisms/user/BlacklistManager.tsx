"use client";

import React, { useEffect, useRef, useState } from "react";
import { MailX, ShieldOff } from "lucide-react";
import { Button, Spinner } from "@/components";
import { EmptyView } from "@/components/molecules/feedback/EmptyView";
import { useBlacklist } from "@/hooks/api/useBlacklistQueries";
import { BlacklistListItem } from "@/components/molecules/user/BlacklistListItem";

interface BlacklistManagerProps {
  className?: string;
}

/**
 * 블랙리스트 관리 오거니즘.
 *
 * 라운드 15 (F-15-BUG-1~18): 라운드 9 친구 패턴(옵티미스틱 + 페이지 변경 후 focus 이동 +
 * tablist 보다 단일 list aria-label) + 라운드 13 EmptyView + 라운드 5 undo 토스트 + 라운드 1
 * paper 토큰 일괄 차용. window.confirm → useConfirmModal 은 BlacklistListItem 으로 위임.
 */
export const BlacklistManager: React.FC<BlacklistManagerProps> = React.memo(({ className }) => {
  const [page, setPage] = useState(0);
  const size = 20;
  const listRef = useRef<HTMLUListElement | null>(null);
  const lastPageRef = useRef(0);

  const { data: blacklistResponse, isLoading, error, refetch } = useBlacklist(page, size);

  // 블랙리스트 데이터
  const blacklistData = blacklistResponse?.data;
  const blacklistItems = blacklistData?.content || [];
  const totalPages = blacklistData?.totalPages || 0;
  const totalElements = blacklistData?.totalElements ?? 0;
  const isEmpty = blacklistData?.empty ?? true;

  // 라운드 15 F-15-BUG-13: 페이지 변경 후 list 영역에 focus 이동
  useEffect(() => {
    if (lastPageRef.current !== page) {
      lastPageRef.current = page;
      listRef.current?.focus();
    }
  }, [page]);

  // 라운드 15 F-15-BUG-1: 마지막 항목 삭제로 현재 페이지가 비면 한 페이지 후퇴
  useEffect(() => {
    if (!isLoading && isEmpty && page > 0) {
      setPage((p) => Math.max(0, p - 1));
    }
  }, [isLoading, isEmpty, page]);

  // 로딩 상태
  if (isLoading && !blacklistData) {
    return (
      <div className={`flex justify-center items-center min-h-[400px] ${className || ""}`}>
        <Spinner color="failure" message="블랙리스트를 불러오는 중..." />
      </div>
    );
  }

  // 에러 상태 (라운드 15 F-15-BUG-7: EmptyView assertive)
  if (error) {
    return (
      <div className={className}>
        <EmptyView
          assertive
          title="블랙리스트를 불러올 수 없어요"
          description="잠시 후 다시 시도해주세요."
          icon={<ShieldOff className="w-9 h-9" strokeWidth={1.6} aria-hidden="true" />}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  return (
    <div className={className}>
      {/* 헤더 (라운드 15 F-15-BUG-9: 총 N명 카운트) */}
      <div className="mb-6 flex items-center justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-2xl font-bold font-display text-ink dark:text-foreground flex items-center gap-2 break-keep">
            <ShieldOff className="w-7 h-7 text-stamp-red" aria-hidden="true" />
            받지 않는 발신인
          </h1>
          <p className="text-sm text-ink-soft dark:text-muted-foreground mt-1 break-keep">
            차단한 사람은 회원님의 롤링페이퍼에 메시지를 남길 수 없어요.
          </p>
        </div>
        {!isEmpty && (
          <span
            className="text-sm text-ink-soft dark:text-muted-foreground shrink-0"
            aria-label={`총 ${totalElements}명 차단`}
          >
            총 {totalElements}명
          </span>
        )}
      </div>

      {/* 블랙리스트 목록 */}
      {isEmpty ? (
        <EmptyView
          title="차단한 발신인이 없어요"
          description={
            <>
              롤링페이퍼에서 받기 싫은 편지가 오면
              <br />
              메시지의 [⋮] 메뉴에서 발신인을 차단할 수 있어요.
            </>
          }
          icon={<MailX className="w-9 h-9" strokeWidth={1.6} aria-hidden="true" />}
        />
      ) : (
        <>
          <ul
            ref={listRef}
            tabIndex={-1}
            aria-label="블랙리스트 목록"
            className="bg-paper-card border border-postal-navy/20 rounded-lg overflow-hidden focus:outline-none focus:ring-2 focus:ring-postal-navy/40"
          >
            {blacklistItems.map((item) => (
              <BlacklistListItem key={item.id} item={item} />
            ))}
          </ul>

          {/* 페이지네이션 (라운드 15 F-15-BUG-8: 44px + nav aria-label + aria-live) */}
          {totalPages > 1 && (
            <nav
              className="flex justify-center items-center gap-2 mt-6"
              aria-label="블랙리스트 페이지"
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
              <span
                className="text-sm text-ink-soft dark:text-muted-foreground px-3"
                aria-live="polite"
                aria-label={`현재 ${page + 1}페이지, 총 ${totalPages}페이지`}
              >
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

BlacklistManager.displayName = "BlacklistManager";
