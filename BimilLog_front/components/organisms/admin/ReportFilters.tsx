"use client";

import React from "react";

interface ReportFiltersProps {
  filterType: string;
  setFilterType: (type: string) => void;
}

/**
 * 신고 종류 필터 칩 그룹.
 *
 * 라운드 16 F-16-015/016: WAI-ARIA toggle button 패턴 (`aria-pressed`) +
 * `role="group" aria-label`. 활성 색은 stamp-red 토큰 (paper 메타포 일관).
 */
export const ReportFilters = React.memo<ReportFiltersProps>(({ filterType, setFilterType }) => {
  const filters = [
    { id: "all", label: "전체" },
    { id: "POST", label: "게시글" },
    { id: "COMMENT", label: "댓글" },
    { id: "ERROR", label: "오류" },
    { id: "IMPROVEMENT", label: "개선" },
  ];

  return (
    <div
      role="group"
      aria-label="신고 종류 필터"
      className="flex flex-wrap gap-2"
    >
      {filters.map((filter) => {
        const isActive = filterType === filter.id;
        return (
          <button
            key={filter.id}
            type="button"
            onClick={() => setFilterType(filter.id)}
            aria-pressed={isActive}
            className={`
              px-4 py-2 rounded-lg font-medium text-sm transition-all min-h-[44px]
              focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-postal-navy focus-visible:ring-offset-1
              ${isActive
                ? 'bg-stamp-red/15 text-stamp-red ring-2 ring-stamp-red/40 dark:bg-stamp-red/25 dark:text-paper-50'
                : 'bg-paper-soft text-ink-soft hover:bg-paper-aged hover:text-postal-navy dark:bg-postal-navy/10 dark:text-muted-foreground dark:hover:bg-postal-navy/20'
              }
            `}
          >
            {filter.label}
          </button>
        );
      })}
    </div>
  );
});
ReportFilters.displayName = "ReportFilters";
