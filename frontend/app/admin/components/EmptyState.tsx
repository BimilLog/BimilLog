import React from "react";
import { AlertTriangle } from "@/components";

interface EmptyStateProps {
  hasSearchFilter?: boolean;
  searchTerm?: string;
  filterType?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  hasSearchFilter = false,
  searchTerm = "",
  filterType = "all",
}) => {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4">
      <div className="p-6 rounded-full bg-gray-100 mb-4">
        <AlertTriangle className="w-12 h-12 text-gray-400" />
      </div>
      <h3 className="text-lg font-medium text-gray-700 mb-2">
        신고 내역이 없습니다
      </h3>
      <p className="text-sm text-gray-500 text-center max-w-md">
        {hasSearchFilter && (searchTerm || filterType !== "all")
          ? "검색 조건에 맞는 신고가 없습니다. 다른 검색어나 필터를 시도해보세요."
          : "현재 처리 대기 중인 신고가 없습니다."}
      </p>
    </div>
  );
};