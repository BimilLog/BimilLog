"use client";

import { useState, useEffect, useCallback } from 'react';
import {
  getBookmarks,
  getBookmarksByType,
  getBookmarksByCategory,
  addBookmark,
  updateBookmark,
  removeBookmark,
  togglePostBookmark,
  toggleRollingPaperBookmark,
  isPostBookmarked,
  isRollingPaperBookmarked,
  searchBookmarks,
  sortBookmarks,
  getBookmarkStats,
  clearBookmarks,
  type Bookmark,
  type BookmarkCategory,
  type BookmarkStats,
} from '@/lib/utils/bookmark';
import { useToast } from '@/hooks';

interface UseBookmarkOptions {
  type?: Bookmark['type'];
  category?: BookmarkCategory;
  sortBy?: 'createdAt' | 'visitCount' | 'title';
  sortOrder?: 'asc' | 'desc';
}

export function useBookmark(options: UseBookmarkOptions = {}) {
  const { type, category, sortBy = 'createdAt', sortOrder = 'desc' } = options;
  const { showSuccess, showError, showInfo } = useToast();

  const [bookmarks, setBookmarks] = useState<Bookmark[]>([]);
  const [stats, setStats] = useState<BookmarkStats>({
    total: 0,
    byType: { 'post': 0, 'rolling-paper': 0, 'comment': 0 },
    byCategory: { 'favorite': 0, 'read-later': 0, 'important': 0, 'friends': 0, 'birthday': 0, 'other': 0 },
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  // 북마크 로드
  const loadBookmarks = useCallback(() => {
    try {
      let loaded = getBookmarks();

      // 타입 필터링
      if (type) {
        loaded = loaded.filter(b => b.type === type);
      }

      // 카테고리 필터링
      if (category) {
        loaded = loaded.filter(b => b.category === category);
      }

      // 검색 필터링
      if (searchQuery) {
        loaded = searchBookmarks(searchQuery);
      }

      // 정렬
      loaded = sortBookmarks(loaded, sortBy, sortOrder);

      setBookmarks(loaded);
      setStats(getBookmarkStats());
    } catch (error) {
      console.error('Failed to load bookmarks:', error);
      showError('북마크 로드 실패', '북마크를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [type, category, searchQuery, sortBy, sortOrder, showError]);

  // 초기 로드 및 필터 변경 시 재로드
  useEffect(() => {
    loadBookmarks();
  }, [loadBookmarks]);

  // storage 이벤트 리스너 (다른 탭에서 변경 감지)
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === 'bimillog_bookmarks') {
        loadBookmarks();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, [loadBookmarks]);

  // 북마크 추가
  const addNewBookmark = useCallback((bookmark: Omit<Bookmark, 'id' | 'createdAt' | 'visitCount'>) => {
    const id = addBookmark(bookmark);
    if (id) {
      showSuccess('북마크 추가', '북마크가 추가되었습니다.');
      loadBookmarks();
      return true;
    } else {
      showError('북마크 추가 실패', '이미 북마크된 항목이거나 제한을 초과했습니다.');
      return false;
    }
  }, [showSuccess, showError, loadBookmarks]);

  // 게시글 북마크 토글
  const togglePost = useCallback((postId: number, title: string, category?: BookmarkCategory) => {
    const success = togglePostBookmark(postId, title, category);
    if (success) {
      const isBookmarked = isPostBookmarked(postId);
      if (isBookmarked) {
        showSuccess('북마크 추가', '게시글이 북마크되었습니다.');
      } else {
        showInfo('북마크 제거', '게시글 북마크가 제거되었습니다.');
      }
      loadBookmarks();
    }
    return success;
  }, [showSuccess, showInfo, loadBookmarks]);

  // 롤링페이퍼 북마크 토글
  const toggleRollingPaper = useCallback((nickname: string, category?: BookmarkCategory) => {
    const success = toggleRollingPaperBookmark(nickname, category);
    if (success) {
      const isBookmarked = isRollingPaperBookmarked(nickname);
      if (isBookmarked) {
        showSuccess('북마크 추가', '롤링페이퍼가 북마크되었습니다.');
      } else {
        showInfo('북마크 제거', '롤링페이퍼 북마크가 제거되었습니다.');
      }
      loadBookmarks();
    }
    return success;
  }, [showSuccess, showInfo, loadBookmarks]);

  // 북마크 업데이트
  const updateBookmarkItem = useCallback((id: string, updates: Partial<Bookmark>) => {
    const success = updateBookmark(id, updates);
    if (success) {
      showSuccess('북마크 수정', '북마크가 수정되었습니다.');
      loadBookmarks();
    } else {
      showError('북마크 수정 실패', '북마크를 수정할 수 없습니다.');
    }
    return success;
  }, [showSuccess, showError, loadBookmarks]);

  // 북마크 삭제
  const removeBookmarkItem = useCallback((id: string) => {
    const success = removeBookmark(id);
    if (success) {
      showInfo('북마크 삭제', '북마크가 삭제되었습니다.');
      loadBookmarks();
    } else {
      showError('북마크 삭제 실패', '북마크를 삭제할 수 없습니다.');
    }
    return success;
  }, [showInfo, showError, loadBookmarks]);

  // 모든 북마크 삭제
  const clearAllBookmarks = useCallback(() => {
    clearBookmarks();
    showInfo('북마크 초기화', '모든 북마크가 삭제되었습니다.');
    loadBookmarks();
  }, [showInfo, loadBookmarks]);

  return {
    // 상태
    bookmarks,
    stats,
    searchQuery,
    isLoading,

    // 액션
    setSearchQuery,
    addNewBookmark,
    togglePost,
    toggleRollingPaper,
    updateBookmarkItem,
    removeBookmarkItem,
    clearAllBookmarks,
    reload: loadBookmarks,

    // 체크 함수
    isPostBookmarked,
    isRollingPaperBookmarked,
  };
}