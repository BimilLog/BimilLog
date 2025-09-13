"use client";

import React from "react";
import { Button, Card, Input } from "@/components";
import { Search, AlertCircle } from "lucide-react";

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
    <Card className="bg-white/80 backdrop-blur-sm border-0 shadow-xl mb-8">
      <div className="text-center pb-4 p-6">
        <h2 className="text-2xl bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent font-bold">
          누구의 롤링페이퍼를 방문할까요?
        </h2>
        <p className="text-gray-600 text-sm mt-2">
          닉네임을 입력하여 롤링페이퍼를 찾아보세요
        </p>
      </div>
      <div className="space-y-4 p-6 pt-0">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <Input
            placeholder="닉네임을 입력하세요"
            value={searchNickname}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchNickname(e.target.value)}
            onKeyPress={onKeyPress}
            className="pl-10 h-12 text-lg bg-white border-2 border-gray-200 focus:border-purple-400"
            disabled={isSearching}
          />
        </div>

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
          disabled={!searchNickname.trim() || isSearching}
        >
          {isSearching ? (
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
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