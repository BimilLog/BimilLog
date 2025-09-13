"use client";

import { Badge } from "@/components";

interface ReportFiltersProps {
  filterType: string;
  setFilterType: (type: string) => void;
  reportCounts: {
    all: number;
    POST: number;
    COMMENT: number;
    ERROR: number;
    IMPROVEMENT: number;
  };
}

export function ReportFilters({ filterType, setFilterType, reportCounts }: ReportFiltersProps) {
  const filters = [
    { id: "all", label: "전체", count: reportCounts.all },
    { id: "POST", label: "게시글", count: reportCounts.POST },
    { id: "COMMENT", label: "댓글", count: reportCounts.COMMENT },
    { id: "ERROR", label: "오류", count: reportCounts.ERROR },
    { id: "IMPROVEMENT", label: "개선", count: reportCounts.IMPROVEMENT }
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
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }
          `}
        >
          {filter.label}
          <Badge 
            variant={filterType === filter.id ? "default" : "secondary"} 
            className="ml-2 text-xs"
          >
            {filter.count}
          </Badge>
        </button>
      ))}
    </div>
  );
}