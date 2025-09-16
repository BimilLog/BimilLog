"use client";

import React from "react";
import { Button, Card, Input } from "@/components";
import { Search, AlertCircle } from "lucide-react";
import { Spinner as FlowbiteSpinner } from "flowbite-react";

interface SearchSectionProps {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  searchError: string;
  onSearch: () => void;
  onKeyPress: (e: React.KeyboardEvent) => void;
}

export const SearchSection: React.FC<SearchSectionProps> = ({
  searchNickname,
  setSearchNickname,
  isSearching,
  searchError,
  onSearch,
  onKeyPress,
}) => {
  return (
    <Card variant="elevated" className="mb-8">
      <div className="text-center pb-4 p-6">
        <h2 className="text-2xl bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent font-bold">
          누구의 롤링페이퍼를 방문할까요?
        </h2>
        <p className="text-brand-muted text-sm mt-2">
          닉네임을 입력하여 롤링페이퍼를 찾아보세요
        </p>
      </div>
      <div className="space-y-4 p-6 pt-0">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-brand-secondary w-5 h-5" />
          <Input
            placeholder="닉네임을 입력하세요"
            value={searchNickname}
            // 입력값 변경 시 기존 에러 메시지 자동 제거 (useRollingPaperSearch 훅에서 처리)
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchNickname(e.target.value)}
            onKeyPress={onKeyPress} // Enter 키 누를 시 검색 실행
            className="pl-10 h-12 text-lg bg-white border-2 border-gray-200 focus:border-purple-400"
            disabled={isSearching}
          />
        </div>

        {/* 검색 에러 메시지 표시 (닉네임을 찾을 수 없거나 네트워크 오류 시) */}
        {searchError && (
          <div className="flex items-start space-x-2 p-3 bg-red-50 border border-red-200 rounded-lg">
            <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
            <div className="text-sm text-red-800">
              <p>{searchError}</p>
            </div>
          </div>
        )}

        <Button
          onClick={onSearch}
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