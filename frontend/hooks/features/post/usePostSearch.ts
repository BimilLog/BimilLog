"use client";

import { useState, useCallback, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { postQuery } from '@/lib/api';
import { useDebounce } from '@/hooks/common/useDebounce';
import { useToast } from '@/hooks';
import type { SimplePost } from '@/types/domains/post';

// ============ POST SEARCH HOOKS ============

// 게시글 검색 훅
export function usePostSearch() {
  const [searchResults, setSearchResults] = useState<SimplePost[]>([]);
  const [searchType, setSearchType] = useState<'TITLE' | 'TITLE_CONTENT' | 'WRITER'>('TITLE');
  const { showToast } = useToast();

  const searchMutation = useMutation({
    mutationFn: async ({ searchType, term, page = 1, size = 10 }: {
      searchType: 'TITLE' | 'TITLE_CONTENT' | 'WRITER';
      term: string;
      page?: number;
      size?: number;
    }) => {
      return await postQuery.search(searchType, term, page, size);
    },
    onSuccess: (response) => {
      if (response.success && response.data) {
        setSearchResults(response.data.content || []);
      }
    },
    onError: () => {
      showToast({ type: 'error', message: '검색에 실패했습니다.' });
    }
  });

  const search = useCallback(async (
    term: string,
    type: 'TITLE' | 'TITLE_CONTENT' | 'WRITER' = searchType,
    page = 1,
    size = 10
  ) => {
    if (!term.trim()) {
      setSearchResults([]);
      return;
    }

    searchMutation.mutate({ searchType: type, term: term.trim(), page, size });
  }, [searchType, searchMutation]);

  const clearSearch = useCallback(() => {
    setSearchResults([]);
  }, []);

  return {
    searchResults,
    isSearching: searchMutation.isPending,
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

  // 디바운스된 검색어가 변경될 때마다 자동 검색: 500ms 지연 후 실행하여 과도한 API 호출 방지
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