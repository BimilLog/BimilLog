import React from "react";
import { 
  Input, 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue
} from "@/components";
import { Search } from "lucide-react";

interface ReportFiltersProps {
  searchTerm: string;
  onSearchChange: (value: string) => void;
  filterType: string;
  onFilterChange: (value: string) => void;
}

const FILTER_OPTIONS = [
  { value: "all", label: "전체 유형" },
  { value: "POST", label: "게시글" },
  { value: "COMMENT", label: "댓글" },
  { value: "ERROR", label: "오류" },
  { value: "IMPROVEMENT", label: "개선사항" },
] as const;

export const ReportFilters = React.memo<ReportFiltersProps>(({
  searchTerm,
  onSearchChange,
  filterType,
  onFilterChange,
}) => {
  return (
    <div className="flex flex-col sm:flex-row gap-4">
      <div className="flex-1 relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
        <Input
          placeholder="신고 내용, 제목, 신고자명으로 검색..."
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-10 min-h-[48px] bg-white border-gray-200 focus:border-purple-400 focus:ring-purple-400/20 touch-manipulation"
        />
      </div>
      <div className="flex-shrink-0">
        <Select value={filterType} onValueChange={onFilterChange}>
          <SelectTrigger className="w-full sm:w-44 min-h-[48px] bg-white border-gray-200 focus:border-purple-400 focus:ring-purple-400/20 touch-manipulation">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {FILTER_OPTIONS.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
});

ReportFilters.displayName = "ReportFilters";