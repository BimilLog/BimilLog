"use client";

import React from "react";

interface ReportFiltersProps {
  filterType: string;
  setFilterType: (type: string) => void;
}

export const ReportFilters = React.memo<ReportFiltersProps>(({ filterType, setFilterType }) => {
  const filters = [
    { id: "all", label: "전체" },
    { id: "POST", label: "게시글" },
    { id: "COMMENT", label: "댓글" },
    { id: "ERROR", label: "오류" },
    { id: "IMPROVEMENT", label: "개선" }
  ];

  return (
    <div className="flex flex-wrap gap-2">
      {filters.map((filter) => (
        <button
          key={filter.id}
          onClick={() => setFilterType(filter.id)}
          className={`
            px-4 py-2 rounded-lg font-medium text-sm transition-all
            ${filterType === filter.id
              ? 'bg-purple-100 text-purple-700 ring-2 ring-purple-500 ring-opacity-50'
              : 'bg-gray-100 text-brand-muted hover:bg-gray-200'
            }
          `}
        >
          {filter.label}
        </button>
      ))}
    </div>
  );
});
ReportFilters.displayName = "ReportFilters";