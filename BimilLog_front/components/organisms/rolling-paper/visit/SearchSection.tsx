"use client";

import React, { useId, useRef } from "react";
import { Button, Card } from "@/components";
import { Search, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface SearchSectionProps {
  searchNickname: string;
  setSearchNickname: (nickname: string) => void;
  isSearching: boolean;
  onSearch: () => void;
  children?: React.ReactNode;
}

/**
 * 라운드 5 변경:
 * - <form role="search"> 로 래핑하여 검색 영역 landmark 부여 (WAI-ARIA)
 * - onKeyPress → onKeyDown (React 19 권장)
 * - input aria-label / id-label 명시 (WCAG 3.3.2)
 * - 모바일 focus 시 scrollIntoView({block:'center'}) — sticky header 가림 방지
 * - isSearching 진행 중 spinner 아이콘으로 가시 피드백
 */
export const SearchSection: React.FC<SearchSectionProps> = React.memo(({
  searchNickname,
  setSearchNickname,
  isSearching,
  onSearch,
  children,
}) => {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);

  const executeSearch = () => {
    if (searchNickname.trim()) {
      onSearch();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      // 폼 submit 으로 처리되지만 연타 시 native 동작 방지
      e.preventDefault();
      executeSearch();
    }
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    executeSearch();
  };

  const handleFocus = () => {
    // 모바일 가상 키보드가 올라올 때 sticky header 가 input 을 가리지 않도록
    // 약간의 지연 후 가운데로 스크롤 (가상 키보드가 viewport 변경 후 실행되도록)
    if (typeof window === "undefined") return;
    window.setTimeout(() => {
      inputRef.current?.scrollIntoView({ block: "center", behavior: "smooth" });
    }, 250);
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
        <form
          role="search"
          aria-label="멤버 닉네임 검색"
          onSubmit={handleSubmit}
        >
          <label htmlFor={inputId} className="sr-only">
            닉네임 검색
          </label>
          <div className="flex items-center border border-ink-soft rounded-lg bg-card overflow-hidden hover:border-stamp-red/40 focus-within:border-stamp-red focus-within:ring-2 focus-within:ring-stamp-red/20 transition-all">
            <input
              id={inputId}
              ref={inputRef}
              type="search"
              placeholder="닉네임을 입력하세요"
              className={cn(
                "flex-1 border-0 rounded-none bg-transparent h-12 text-lg",
                "text-foreground placeholder:text-muted-foreground",
                "focus-visible:outline-none focus-visible:ring-0 focus-visible:ring-offset-0",
                "px-3 py-1 min-w-0 w-full"
              )}
              value={searchNickname}
              onChange={(e) => setSearchNickname(e.target.value)}
              onKeyDown={handleKeyDown}
              onFocus={handleFocus}
              aria-label="검색할 닉네임"
              aria-describedby={`${inputId}-hint`}
              autoComplete="off"
              enterKeyHint="search"
              inputMode="search"
            />
            <Button
              type="submit"
              variant="ghost"
              size="icon"
              className="border-0 rounded-none hover:bg-stamp-red/10 border-l border-ink-soft h-12"
              disabled={!searchNickname.trim()}
              aria-label="검색 실행"
            >
              {isSearching ? (
                <Loader2 className="w-5 h-5 text-ink-soft animate-spin" aria-hidden="true" />
              ) : (
                <Search className="w-5 h-5 text-ink-soft" aria-hidden="true" />
              )}
            </Button>
          </div>
          <p id={`${inputId}-hint`} className="sr-only">
            입력 후 잠시 기다리거나 Enter 키로 검색합니다.
          </p>
        </form>

        {/* AllUsersList가 여기에 렌더링됩니다 */}
        {children && <div className="mt-6">{children}</div>}
      </div>
    </Card>
  );
});

SearchSection.displayName = "SearchSection";
