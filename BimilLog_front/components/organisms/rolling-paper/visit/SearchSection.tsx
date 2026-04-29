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

export const SearchSection: React.FC<SearchSectionProps> = React.memo(({
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
        <h2 className="font-display text-2xl text-ink dark:text-foreground font-bold whitespace-nowrap tracking-tight">
          누구의 롤링페이퍼를 방문할까요?
        </h2>
        <p className="font-body text-ink-soft dark:text-muted-foreground text-sm mt-2">
          닉네임을 입력하여 검색하거나 아래 목록에서 선택하세요
        </p>
      </div>
      <div className="space-y-4 p-6 pt-0">
        <div>
          <div className="flex items-center border border-ink-soft rounded-lg bg-card overflow-hidden hover:border-stamp-red/40 focus-within:border-stamp-red focus-within:ring-2 focus-within:ring-stamp-red/20 transition-all">
            <Input
              type="text"
              placeholder="닉네임을 입력하세요"
              className="flex-1 border-0 rounded-none focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent h-12 text-lg text-foreground placeholder:text-muted-foreground"
              value={searchNickname}
              onChange={(e) => setSearchNickname(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={isSearching}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={executeSearch}
              className="border-0 rounded-none hover:bg-stamp-red/10 border-l border-ink-soft h-12"
              disabled={!searchNickname.trim() || isSearching}
            >
              <Search className="w-5 h-5 text-ink-soft" />
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
});

SearchSection.displayName = "SearchSection";
