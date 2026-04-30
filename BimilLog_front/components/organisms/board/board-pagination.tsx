"use client";

import { memo } from "react";
import { Pagination } from "flowbite-react";

interface BoardPaginationProps {
  currentPage: number;
  totalPages: number;
  setCurrentPage: (page: number) => void;
  /**
   * 총 항목 수. 제공되면 totalPages<=1 인 경우에도 "총 N건" 캡션을 표시한다.
   * (B-001 보강: 백엔드 캐시 totalElements 누락 방어 + size=N exact match 케이스)
   */
  totalElements?: number;
  /**
   * 캡션 단위 (예: "건", "명", "개"). 기본값 "건".
   */
  itemLabel?: string;
}

export const BoardPagination = memo(({
  currentPage,
  totalPages,
  setCurrentPage,
  totalElements,
  itemLabel = "건",
}: BoardPaginationProps) => {
  // totalPages<=1 이지만 totalElements 가 제공되면 캡션이라도 표시 (B-001)
  if (totalPages <= 1) {
    if (typeof totalElements === "number" && totalElements > 0) {
      return (
        <div
          data-testid="board-pagination"
          data-pagination-mode="caption-only"
          className="flex items-center justify-center mt-8"
        >
          <p className="text-sm text-muted-foreground">
            총 <span className="font-semibold text-foreground">{totalElements.toLocaleString()}</span>
            {itemLabel} · 페이지 1 / 1
          </p>
        </div>
      );
    }
    return null;
  }

  // Convert from 0-based to 1-based page numbering for Flowbite
  const flowbiteCurrentPage = currentPage + 1;

  const handlePageChange = (page: number) => {
    // Convert back to 0-based for your internal state
    setCurrentPage(page - 1);
  };

  return (
    <div
      data-testid="board-pagination"
      data-pagination-mode="full"
      className="flex flex-col items-center justify-center mt-8 gap-2"
    >
      {typeof totalElements === "number" && totalElements > 0 && (
        <p className="text-xs text-muted-foreground">
          총 <span className="font-semibold text-foreground">{totalElements.toLocaleString()}</span>
          {itemLabel} · 페이지 {flowbiteCurrentPage} / {totalPages}
        </p>
      )}
      <Pagination
        currentPage={flowbiteCurrentPage}
        totalPages={totalPages}
        onPageChange={handlePageChange}
        showIcons
        previousLabel="이전"
        nextLabel="다음"
        className="text-sm"
        theme={{
          pages: {
            base: "xs:mt-0 mt-2 inline-flex items-center gap-1",
            showIcon: "inline-flex",
            previous: {
              base: "ml-0 flex min-h-[44px] min-w-[44px] items-center justify-center gap-1 rounded-l-lg border border-border bg-card px-4 py-3 text-muted-foreground leading-tight hover:bg-accent hover:text-foreground",
              icon: "h-5 w-5"
            },
            next: {
              base: "flex min-h-[44px] min-w-[44px] items-center justify-center gap-1 rounded-r-lg border border-border bg-card px-4 py-3 text-muted-foreground leading-tight hover:bg-accent hover:text-foreground",
              icon: "h-5 w-5"
            },
            selector: {
              base: "flex min-h-[44px] min-w-[44px] items-center justify-center border border-border bg-card px-4 py-3 text-muted-foreground leading-tight hover:bg-accent hover:text-foreground",
              active:
                "border-stamp-red bg-stamp-red text-paper-50 hover:bg-stamp-red hover:text-paper-50 dark:border-stamp-red dark:bg-stamp-red",
              disabled: "cursor-not-allowed text-muted-foreground/60"
            }
          }
        }}
      />
    </div>
  );
});

BoardPagination.displayName = "BoardPagination";
