"use client";

import React from "react";
import { Button, Card, Input } from "@/components";
import { Search } from "lucide-react";
import { Spinner as FlowbiteSpinner } from "flowbite-react";

interface SearchSectionProps {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  onSearch: () => void;
}

export const SearchSection: React.FC<SearchSectionProps> = ({
  searchNickname,
  setSearchNickname,
  isSearching,
  onSearch,
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
        <h2 className="text-2xl bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent font-bold whitespace-nowrap">
          누구의 롤링페이퍼를 방문할까요?
        </h2>
        <p className="text-brand-muted text-sm mt-2">
          닉네임을 입력하여 롤링페이퍼를 찾아보세요
        </p>
      </div>
      <div className="space-y-4 p-6 pt-0">
        <div>
          <div className="flex items-center border border-gray-300 rounded-lg bg-white overflow-hidden hover:border-brand-secondary/50 focus-within:border-brand-secondary focus-within:ring-2 focus-within:ring-brand-secondary/20 transition-all">
            <Input
              type="text"
              placeholder="닉네임을 입력하세요"
              className="flex-1 border-0 rounded-none focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent h-12 text-lg"
              value={searchNickname}
              onChange={(e) => setSearchNickname(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={isSearching}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={executeSearch}
              className="border-0 rounded-none hover:bg-brand-secondary/10 border-l border-gray-200 h-12"
              disabled={!searchNickname.trim() || isSearching}
            >
              <Search className="w-5 h-5 stroke-slate-600" />
            </Button>
          </div>
        </div>

        <Button
          onClick={executeSearch}
          className="w-full h-12 bg-gradient-to-r from-blue-500 to-cyan-600 hover:from-blue-600 hover:to-cyan-700 text-lg font-semibold disabled:opacity-50"
          // 닉네임이 비어있거나 검색 중일 때 버튼 비활성화
          disabled={!searchNickname.trim() || isSearching}
        >
          {/* 검색 중일 때 로딩 스피너와 텍스트 표시 */}
          {isSearching ? (
            <div className="flex items-center justify-center space-x-2">
              <FlowbiteSpinner color="white" size="sm" aria-label="검색 중..." />
              <span>검색 중...</span>
            </div>
          ) : (
            "롤링페이퍼 방문하기"
          )}
        </Button>
      </div>
    </Card>
  );
};