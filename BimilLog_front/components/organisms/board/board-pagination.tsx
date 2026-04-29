"use client";

import { memo } from "react";
import { Pagination } from "flowbite-react";

interface BoardPaginationProps {
  currentPage: number;
  totalPages: number;
  setCurrentPage: (page: number) => void;
}

export const BoardPagination = memo(({
  currentPage,
  totalPages,
  setCurrentPage,
}: BoardPaginationProps) => {
  if (totalPages <= 1) return null;

  // Convert from 0-based to 1-based page numbering for Flowbite
  const flowbiteCurrentPage = currentPage + 1;

  const handlePageChange = (page: number) => {
    // Convert back to 0-based for your internal state
    setCurrentPage(page - 1);
  };

  return (
    <div
      data-testid="board-pagination"
      className="flex items-center justify-center mt-8"
    >
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
