"use client";

import React, { useEffect, useRef } from "react";
import { Button, Card, Input } from "@/components";
import { Search, AlertCircle, Clock, X } from "lucide-react";
import { Spinner as FlowbiteSpinner } from "flowbite-react";
import { useSearchHistory } from "@/hooks/features/useSearchHistory";

interface SearchSectionProps {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  searchError: string;
  onSearch: () => void;
}

export const SearchSection: React.FC<SearchSectionProps> = ({
  searchNickname,
  setSearchNickname,
  isSearching,
  searchError,
  onSearch,
}) => {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const {
    setSearchQuery,
    recentSearches,
    suggestions,
    showSuggestions,
    setShowSuggestions,
    handleSearch: saveToHistory,
    removeSearch,
    clearAll,
  } = useSearchHistory({ type: 'rolling-paper' });

  // 검색 실행 핸들러
  const executeSearch = () => {
    if (searchNickname.trim()) {
      saveToHistory(searchNickname); // 검색 히스토리에 저장
      onSearch(); // 실제 검색 실행
      setShowSuggestions(false);
    }
  };

  // 키보드 이벤트 핸들러 (기존 onKeyPress를 executeSearch로 대체)
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      executeSearch();
    }
  };

  // 검색어 변경 시 자동완성 업데이트
  useEffect(() => {
    setSearchQuery(searchNickname);
  }, [searchNickname, setSearchQuery]);

  // 외부 클릭 감지하여 자동완성 닫기
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchInputRef.current && !searchInputRef.current.contains(e.target as Node)) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [setShowSuggestions]);

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
          <div className="flex items-center border border-gray-300 rounded-lg bg-white overflow-hidden hover:border-brand-secondary/50 focus-within:border-brand-secondary focus-within:ring-2 focus-within:ring-brand-secondary/20 transition-all">
            <Input
              ref={searchInputRef}
              type="text"
              placeholder="닉네임을 입력하세요"
              className="flex-1 border-0 rounded-none focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent h-12 text-lg"
              value={searchNickname}
              onChange={(e) => setSearchNickname(e.target.value)}
              onKeyPress={handleKeyPress}
              onFocus={() => setShowSuggestions(true)}
              disabled={isSearching}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={executeSearch}
              className="border-0 rounded-none hover:bg-brand-secondary/10 border-l border-gray-200 h-12"
              disabled={!searchNickname.trim() || isSearching}
            >
              <Search className="w-5 h-5 text-brand-secondary" />
            </Button>
          </div>

          {/* 검색 히스토리 및 자동완성 드롭다운 */}
          {showSuggestions && (suggestions.length > 0 || recentSearches.length > 0) && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-lg border z-50 max-h-96 overflow-y-auto">
              {/* 검색어 자동완성 */}
              {suggestions.length > 0 && searchNickname && (
                <div className="p-2 border-b">
                  {suggestions.map((suggestion) => (
                    <button
                      key={suggestion.id}
                      onClick={() => {
                        setSearchNickname(suggestion.query);
                        saveToHistory(suggestion.query);
                        onSearch(); // 실제 검색 실행
                        setShowSuggestions(false);
                      }}
                      className="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-md flex items-center justify-between group"
                    >
                      <span className="flex items-center gap-2">
                        <Search className="w-4 h-4 text-gray-400" />
                        <span dangerouslySetInnerHTML={{
                          __html: suggestion.query.replace(
                            new RegExp(`(${searchNickname})`, 'gi'),
                            '<strong>$1</strong>'
                          )
                        }} />
                      </span>
                      <span className="text-xs text-gray-400">
                        {suggestion.count}회
                      </span>
                    </button>
                  ))}
                </div>
              )}

              {/* 최근 검색어 */}
              {recentSearches.length > 0 && (
                <div className="p-2">
                  <div className="flex items-center justify-between px-2 mb-1">
                    <span className="text-xs text-gray-500">
                    </span>
                    <button
                      onClick={clearAll}
                      className="text-xs text-gray-400 hover:text-red-500"
                    >
                      전체 삭제
                    </button>
                  </div>
                  {recentSearches.slice(0, 5).map((search) => (
                    <div
                      key={search.id}
                      className="flex items-center justify-between px-3 py-2 hover:bg-gray-50 rounded-md group"
                    >
                      <button
                        onClick={() => {
                          setSearchNickname(search.query);
                          saveToHistory(search.query);
                          onSearch(); // 실제 검색 실행
                          setShowSuggestions(false);
                        }}
                        className="flex-1 text-left flex items-center gap-2"
                      >
                        <Clock className="w-4 h-4 text-gray-400" />
                        <span>{search.query}</span>
                      </button>
                      <button
                        onClick={() => removeSearch(search.id)}
                        className="opacity-0 group-hover:opacity-100 p-1 hover:bg-gray-200 rounded"
                      >
                        <X className="w-3 h-3 text-gray-500" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
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