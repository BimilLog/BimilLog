"use client";

import { useState, useCallback, useEffect } from 'react';
import { postQuery } from '@/lib/api';
import { useApiMutation } from '@/hooks/api/useApiMutation';
import { useDebounce } from '@/hooks/common/useDebounce';
import type { SimplePost } from '@/types/domains/post';

// ============ POST SEARCH HOOKS ============

// 게시글 검색 훅
export function usePostSearch() {
  const [searchResults, setSearchResults] = useState<SimplePost[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'AUTHOR'>('TITLE');

  const searchMutation = useApiMutation(
    async ({ searchType, term, page = 1, size = 10 }: {
      searchType: 'TITLE' | 'TITLE_CONTENT' | 'AUTHOR';
      term: string;
      page?: number;
      size?: number;
    }) => {
      return await postQuery.search(searchType, term, page, size);
    },
    {
      showErrorToast: true,
      onSuccess: (response) => {
        setSearchResults(response?.content || []);
      }
    }
  );

  const search = useCallback(async (
    term: string,
    type: 'TITLE' | 'TITLE_CONTENT' | 'AUTHOR' = searchType,
    page = 1,
    size = 10
  ) => {
    if (!term.trim()) {
      setSearchResults([]);
      return;
    }

    setIsSearching(true);
    try {
      await searchMutation.mutate({ searchType: type, term: term.trim(), page, size });
    } finally {
      setIsSearching(false);
    }
  }, [searchType, searchMutation]);

  const clearSearch = useCallback(() => {
    setSearchResults([]);
  }, []);

  return {
    searchResults,
    isSearching: isSearching || searchMutation.isLoading,
    searchType,
    setSearchType,
    search,
    clearSearch
  };
}

// 디바운스된 검색 훅
export function usePostSearchWithDebounce(debounceMs = 500) {
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, debounceMs);
  const postSearch = usePostSearch();

  // 디바운스된 검색어가 변경될 때마다 자동 검색
  useEffect(() => {
    if (debouncedSearchTerm.trim()) {
      postSearch.search(debouncedSearchTerm);
    } else {
      postSearch.clearSearch();
    }
  }, [debouncedSearchTerm, postSearch.search, postSearch.clearSearch]);

  return {
    ...postSearch,
    searchTerm,
    setSearchTerm,
    debouncedSearchTerm
  };
}