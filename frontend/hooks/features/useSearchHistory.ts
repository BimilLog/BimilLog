"use client";

import { useState, useEffect, useCallback } from 'react';
import {
  getSearchHistory,
  getRecentSearches,
  getTopSearches,
  addSearchHistory,
  removeSearchHistory,
  clearSearchHistory,
  getSearchSuggestions,
  cleanupOldSearchHistory,
  type SearchHistory,
} from '@/lib/utils/search-history';

interface UseSearchHistoryOptions {
  type?: SearchHistory['type'];
  autoCleanup?: boolean; // 오래된 검색어 자동 정리
}

export function useSearchHistory(options: UseSearchHistoryOptions = {}) {
  const { type = 'post', autoCleanup = true } = options;

  const [searchQuery, setSearchQuery] = useState('');
  const [recentSearches, setRecentSearches] = useState<SearchHistory[]>([]);
  const [topSearches, setTopSearches] = useState<SearchHistory[]>([]);
  const [suggestions, setSuggestions] = useState<SearchHistory[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // 검색 히스토리 로드
  const loadHistory = useCallback(() => {
    const allHistory = getSearchHistory();
    const typeFiltered = allHistory.filter(h => {
      // type이 없으면 기본적으로 'post'로 간주
      if (!h.type && type === 'post') return true;
      return h.type === type;
    });

    setRecentSearches(
      typeFiltered
        .sort((a, b) => new Date(b.lastSearched).getTime() - new Date(a.lastSearched).getTime())
        .slice(0, 10)
    );
    setTopSearches(
      typeFiltered
        .sort((a, b) => b.count - a.count)
        .slice(0, 5)
    );
  }, [type]);

  // 초기 로드 및 자동 정리
  useEffect(() => {
    loadHistory();

    if (autoCleanup) {
      const removed = cleanupOldSearchHistory();
      if (removed > 0) {
        console.log(`Cleaned up ${removed} old search entries`);
      }
    }
  }, [loadHistory, autoCleanup]);

  // 검색어 변경 시 자동완성 제안 업데이트
  useEffect(() => {
    if (searchQuery.trim()) {
      const newSuggestions = getSearchSuggestions(searchQuery, type, 5);
      setSuggestions(newSuggestions);
      setShowSuggestions(newSuggestions.length > 0);
    } else {
      setSuggestions([]);
      setShowSuggestions(false);
    }
  }, [searchQuery, type]);

  // storage 이벤트 리스너 (다른 탭에서 변경 감지)
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'bimillog_search_history') {
        loadHistory();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [loadHistory]);

  // 검색 실행
  const handleSearch = useCallback((query: string) => {
    const trimmedQuery = query.trim();
    if (!trimmedQuery) return;

    // 검색 히스토리에 추가
    addSearchHistory(trimmedQuery, type);

    // 히스토리 재로드
    loadHistory();

    // 자동완성 숨기기
    setShowSuggestions(false);
  }, [type, loadHistory]);

  // 검색어 제거
  const removeSearch = useCallback((id: string) => {
    const success = removeSearchHistory(id);
    if (success) {
      loadHistory();
    }
    return success;
  }, [loadHistory]);

  // 전체 검색 기록 삭제 (현재 타입만)
  const clearAll = useCallback(() => {
    // 현재 타입의 검색 기록만 삭제
    const allHistory = getSearchHistory();
    const otherTypeHistory = allHistory.filter(h => {
      // type이 없으면 'post'로 간주
      if (!h.type && type === 'post') return false; // 'post' 타입으로 삭제
      if (!h.type && type !== 'post') return true;  // 다른 타입이면 유지
      return h.type !== type;
    });

    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem('bimillog_search_history', JSON.stringify(otherTypeHistory));
      } catch (error) {
        console.error('Failed to clear search history:', error);
      }
    }

    setRecentSearches([]);
    setTopSearches([]);
    setSuggestions([]);
    setShowSuggestions(false);
  }, [type]);

  // 자동완성 선택
  const selectSuggestion = useCallback((suggestion: SearchHistory) => {
    setSearchQuery(suggestion.query);
    setShowSuggestions(false);
    handleSearch(suggestion.query);
  }, [handleSearch]);

  // 최근 검색어 클릭
  const selectRecentSearch = useCallback((search: SearchHistory) => {
    setSearchQuery(search.query);
    handleSearch(search.query);
  }, [handleSearch]);

  return {
    // 상태
    searchQuery,
    setSearchQuery,
    recentSearches,
    topSearches,
    suggestions,
    showSuggestions,
    setShowSuggestions,

    // 액션
    handleSearch,
    removeSearch,
    clearAll,
    selectSuggestion,
    selectRecentSearch,
    reload: loadHistory,
  };
}