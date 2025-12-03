"use client";

import React from "react";
import { Button, Card, Input } from "@/components";
import { Search } from "lucide-react";

interface SearchSectionProps {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  onSearch: () => void;
  children?: React.ReactNode;
}

export const SearchSection: React.FC<SearchSectionProps> = ({
  searchNickname,
  setSearchNickname,
  isSearching,
  onSearch,
  children,
}) => {
  // 검색 실행 핸들러
  const executeSearch = () => {
    if (searchNickname.trim()) {
      onSearch(); // 실제 검색 실행
    }
  };

  // 키보드 이벤트 핸들러 (기존 onKeyPress를 executeSearch로 대체)
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      executeSearch();
    }
  };

  return (
    <Card variant="elevated" className="mb-8">
      <div className="text-center pb-4 p-6">
        <h2 className="text-2xl text-blue-600 dark:text-blue-400 font-bold whitespace-nowrap">
          누구의 롤링페이퍼를 방문할까요?
        </h2>
        <p className="text-gray-600 dark:text-gray-400 text-sm mt-2">
          닉네임을 입력하여 검색하거나 아래 목록에서 선택하세요
        </p>
      </div>
      <div className="space-y-4 p-6 pt-0">
        <div>
          <div className="flex items-center border border-gray-300 dark:border-slate-600 rounded-lg bg-white dark:bg-slate-800 overflow-hidden hover:border-brand-secondary/50 dark:hover:border-blue-500/50 focus-within:border-brand-secondary dark:focus-within:border-blue-500 focus-within:ring-2 focus-within:ring-brand-secondary/20 dark:focus-within:ring-blue-500/20 transition-all">
            <Input
              type="text"
              placeholder="닉네임을 입력하세요"
              className="flex-1 border-0 rounded-none focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent h-12 text-lg text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500"
              value={searchNickname}
              onChange={(e) => setSearchNickname(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={isSearching}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={executeSearch}
              className="border-0 rounded-none hover:bg-brand-secondary/10 dark:hover:bg-blue-500/10 border-l border-gray-200 dark:border-slate-600 h-12"
              disabled={!searchNickname.trim() || isSearching}
            >
              <Search className="w-5 h-5 stroke-slate-600 dark:stroke-slate-400" />
            </Button>
          </div>
        </div>

        {/* AllUsersList가 여기에 렌더링됩니다 */}
        {children && (
          <div className="mt-6">
            {children}
          </div>
        )}
      </div>
    </Card>
  );
};
