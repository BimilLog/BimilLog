"use client";

import { useEffect, useRef } from "react";
import { Input, Button, Card } from "@/components";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/molecules/select";
import { Search, ListFilter, Clock, TrendingUp, X } from "lucide-react";
import { useSearchHistory } from "@/hooks/features/useSearchHistory";
import { addSearchHistory } from "@/lib/utils/search-history";

interface BoardSearchProps {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  searchType: "TITLE" | "TITLE_CONTENT" | "AUTHOR";
  setSearchType: (type: "TITLE" | "TITLE_CONTENT" | "AUTHOR") => void;
  postsPerPage: string;
  setPostsPerPage: (value: string) => void;
  handleSearch: () => void;
}

export const BoardSearch = ({
  searchTerm,
  setSearchTerm,
  searchType,
  setSearchType,
  postsPerPage,
  setPostsPerPage,
  handleSearch,
}: BoardSearchProps) => {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const {
    searchQuery,
    setSearchQuery,
    recentSearches,
    topSearches,
    suggestions,
    showSuggestions,
    setShowSuggestions,
    handleSearch: saveToHistory,
    removeSearch,
    selectSuggestion,
    selectRecentSearch,
    clearAll,
  } = useSearchHistory({ type: 'post' });

  // 검색 실행 핸들러
  const executeSearch = () => {
    if (searchTerm.trim()) {
      saveToHistory(searchTerm); // 검색 히스토리에 저장
      handleSearch(); // 실제 검색 실행
      setShowSuggestions(false);
    }
  };

  // 검색어 변경 시 자동완성 업데이트
  useEffect(() => {
    setSearchQuery(searchTerm);
  }, [searchTerm, setSearchQuery]);

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
    <Card variant="default" className="mb-6 p-4 bg-white backdrop-blur-none">
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="flex-1 relative">
          <div className="flex items-center border border-gray-300 rounded-lg bg-white overflow-hidden hover:border-brand-secondary/50 focus-within:border-brand-secondary focus-within:ring-2 focus-within:ring-brand-secondary/20 transition-all">
            <Select
              value={searchType}
              onValueChange={(value: "TITLE" | "TITLE_CONTENT" | "AUTHOR") =>
                setSearchType(value)
              }
            >
              <SelectTrigger className="w-[120px] border-0 rounded-none focus:ring-0 focus-visible:ring-0 focus-visible:ring-offset-0 bg-gray-50 hover:bg-gray-100 border-r border-gray-200">
                <SelectValue placeholder="제목" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="TITLE">제목</SelectItem>
                <SelectItem value="TITLE_CONTENT">제목+내용</SelectItem>
                <SelectItem value="AUTHOR">작성자</SelectItem>
              </SelectContent>
            </Select>
            <Input
              ref={searchInputRef}
              type="text"
              placeholder="검색어를 입력하세요..."
              className="flex-1 border-0 rounded-none focus-visible:ring-0 focus-visible:ring-offset-0 bg-transparent"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyPress={(e) => e.key === "Enter" && executeSearch()}
              onFocus={() => setShowSuggestions(true)}
            />
            <Button
              variant="ghost"
              size="icon"
              onClick={executeSearch}
              className="border-0 rounded-none hover:bg-brand-secondary/10 border-l border-gray-200"
            >
              <Search className="w-5 h-5 text-brand-secondary" />
            </Button>
          </div>

          {/* 검색 히스토리 및 자동완성 드롭다운 */}
          {showSuggestions && (
            <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-lg border z-50 max-h-96 overflow-y-auto">
              {/* 검색어 자동완성 */}
              {suggestions.length > 0 && searchTerm && (
                <div className="p-2 border-b">
                  <div className="text-xs text-gray-500 mb-1 px-2">자동완성</div>
                  {suggestions.map((suggestion) => (
                    <button
                      key={suggestion.id}
                      onClick={() => {
                        setSearchTerm(suggestion.query);
                        selectSuggestion(suggestion);
                        executeSearch();
                      }}
                      className="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-md flex items-center justify-between group"
                    >
                      <span className="flex items-center gap-2">
                        <Search className="w-4 h-4 text-gray-400" />
                        <span dangerouslySetInnerHTML={{
                          __html: suggestion.query.replace(
                            new RegExp(`(${searchTerm})`, 'gi'),
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
                <div className="p-2 border-b">
                  <div className="flex items-center justify-between px-2 mb-1">
                    <span className="text-xs text-gray-500 flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      최근 검색어
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
                          setSearchTerm(search.query);
                          selectRecentSearch(search);
                          executeSearch();
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

              {/* 인기 검색어 */}
              {topSearches.length > 0 && (
                <div className="p-2">
                  <div className="text-xs text-gray-500 mb-1 px-2 flex items-center gap-1">
                    <TrendingUp className="w-3 h-3" />
                    인기 검색어
                  </div>
                  {topSearches.map((search, index) => (
                    <button
                      key={search.id}
                      onClick={() => {
                        setSearchTerm(search.query);
                        selectRecentSearch(search);
                        executeSearch();
                      }}
                      className="w-full text-left px-3 py-2 hover:bg-gray-50 rounded-md flex items-center justify-between"
                    >
                      <span className="flex items-center gap-2">
                        <span className="text-xs font-bold text-brand-primary">
                          {index + 1}
                        </span>
                        <span>{search.query}</span>
                      </span>
                      <span className="text-xs text-gray-400">
                        {search.count}회
                      </span>
                    </button>
                  ))}
                </div>
              )}

              {/* 검색 기록이 없을 때 */}
              {recentSearches.length === 0 && topSearches.length === 0 && suggestions.length === 0 && (
                <div className="p-4 text-center text-gray-500 text-sm">
                  검색 기록이 없습니다
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex items-center gap-2">
          <ListFilter className="w-5 h-5 text-brand-secondary" />
          <Select value={postsPerPage} onValueChange={setPostsPerPage}>
            <SelectTrigger className="w-[120px] bg-white border">
              <SelectValue placeholder="30개씩" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="15">15개씩</SelectItem>
              <SelectItem value="30">30개씩</SelectItem>
              <SelectItem value="50">50개씩</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </Card>
  );
};
