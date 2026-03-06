import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  getBookmarks,
  getBookmarksByType,
  getBookmarksByCategory,
  getBookmark,
  isPostBookmarked,
  isRollingPaperBookmarked,
  addBookmark,
  togglePostBookmark,
  toggleRollingPaperBookmark,
  updateBookmark,
  updateBookmarkVisit,
  removeBookmark,
  removeBookmarksByCategory,
  clearBookmarks,
  getBookmarkStats,
  searchBookmarks,
  sortBookmarks,
  CATEGORY_LABELS,
  type Bookmark,
} from '@/lib/utils/bookmark';

describe('bookmark utils', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('getBookmarks', () => {
    it('빈 상태에서 빈 배열 반환', () => {
      expect(getBookmarks()).toEqual([]);
    });

    it('저장된 북마크를 반환', () => {
      const bookmarks = [{ id: 'b1', type: 'post', title: 'Test', url: '/test', category: 'favorite', createdAt: new Date().toISOString(), visitCount: 0 }];
      localStorage.setItem('bimillog_bookmarks', JSON.stringify(bookmarks));
      expect(getBookmarks()).toHaveLength(1);
    });
  });

  describe('addBookmark', () => {
    it('북마크를 추가하고 ID를 반환', () => {
      const id = addBookmark({
        type: 'post',
        title: '테스트 게시글',
        url: '/board/post/1',
        category: 'favorite',
        postId: 1,
      });
      expect(id).toBeTruthy();
      expect(getBookmarks()).toHaveLength(1);
    });

    it('중복 게시글 북마크는 null 반환', () => {
      addBookmark({ type: 'post', title: '게시글', url: '/post/1', category: 'favorite', postId: 1 });
      const id = addBookmark({ type: 'post', title: '게시글', url: '/post/1', category: 'favorite', postId: 1 });
      expect(id).toBeNull();
      expect(getBookmarks()).toHaveLength(1);
    });

    it('중복 롤링페이퍼 북마크는 null 반환', () => {
      addBookmark({ type: 'rolling-paper', title: 'RP', url: '/rp/nick', category: 'friends', nickname: 'nick' });
      const id = addBookmark({ type: 'rolling-paper', title: 'RP', url: '/rp/nick', category: 'friends', nickname: 'nick' });
      expect(id).toBeNull();
    });

    it('최신 북마크가 앞에 추가됨', () => {
      addBookmark({ type: 'post', title: '첫번째', url: '/1', category: 'favorite', postId: 1 });
      addBookmark({ type: 'post', title: '두번째', url: '/2', category: 'favorite', postId: 2 });
      expect(getBookmarks()[0].title).toBe('두번째');
    });
  });

  describe('isPostBookmarked / isRollingPaperBookmarked', () => {
    it('게시글 북마크 확인', () => {
      expect(isPostBookmarked(1)).toBe(false);
      addBookmark({ type: 'post', title: 'T', url: '/1', category: 'favorite', postId: 1 });
      expect(isPostBookmarked(1)).toBe(true);
    });

    it('롤링페이퍼 북마크 확인', () => {
      expect(isRollingPaperBookmarked('nick')).toBe(false);
      addBookmark({ type: 'rolling-paper', title: 'RP', url: '/rp', category: 'friends', nickname: 'nick' });
      expect(isRollingPaperBookmarked('nick')).toBe(true);
    });
  });

  describe('togglePostBookmark', () => {
    it('없으면 추가하고 true 반환', () => {
      expect(togglePostBookmark(1, '게시글')).toBe(true);
      expect(isPostBookmarked(1)).toBe(true);
    });

    it('있으면 삭제하고 true 반환', () => {
      togglePostBookmark(1, '게시글');
      expect(togglePostBookmark(1, '게시글')).toBe(true);
      expect(isPostBookmarked(1)).toBe(false);
    });
  });

  describe('toggleRollingPaperBookmark', () => {
    it('없으면 추가', () => {
      expect(toggleRollingPaperBookmark('nick')).toBe(true);
      expect(isRollingPaperBookmarked('nick')).toBe(true);
    });

    it('있으면 삭제', () => {
      toggleRollingPaperBookmark('nick');
      toggleRollingPaperBookmark('nick');
      expect(isRollingPaperBookmarked('nick')).toBe(false);
    });
  });

  describe('getBookmark', () => {
    it('존재하는 북마크를 반환', () => {
      const id = addBookmark({ type: 'post', title: 'T', url: '/', category: 'favorite', postId: 1 });
      expect(getBookmark(id!)).toBeTruthy();
      expect(getBookmark(id!)!.title).toBe('T');
    });

    it('없으면 null', () => {
      expect(getBookmark('nonexistent')).toBeNull();
    });
  });

  describe('getBookmarksByType / getBookmarksByCategory', () => {
    beforeEach(() => {
      addBookmark({ type: 'post', title: 'P1', url: '/1', category: 'favorite', postId: 1 });
      addBookmark({ type: 'post', title: 'P2', url: '/2', category: 'important', postId: 2 });
      addBookmark({ type: 'rolling-paper', title: 'RP', url: '/rp', category: 'friends', nickname: 'a' });
    });

    it('타입별 필터', () => {
      expect(getBookmarksByType('post')).toHaveLength(2);
      expect(getBookmarksByType('rolling-paper')).toHaveLength(1);
      expect(getBookmarksByType('comment')).toHaveLength(0);
    });

    it('카테고리별 필터', () => {
      expect(getBookmarksByCategory('favorite')).toHaveLength(1);
      expect(getBookmarksByCategory('friends')).toHaveLength(1);
    });
  });

  describe('updateBookmark', () => {
    it('메모를 업데이트', () => {
      const id = addBookmark({ type: 'post', title: 'T', url: '/', category: 'favorite', postId: 1 });
      expect(updateBookmark(id!, { memo: '메모 추가' })).toBe(true);
      expect(getBookmark(id!)!.memo).toBe('메모 추가');
    });

    it('ID와 type은 변경 불가', () => {
      const id = addBookmark({ type: 'post', title: 'T', url: '/', category: 'favorite', postId: 1 });
      updateBookmark(id!, { type: 'comment' as any });
      expect(getBookmark(id!)!.type).toBe('post');
    });

    it('존재하지 않는 ID는 false', () => {
      expect(updateBookmark('no', { memo: 'x' })).toBe(false);
    });
  });

  describe('updateBookmarkVisit', () => {
    it('방문 횟수와 시간을 업데이트', () => {
      const id = addBookmark({ type: 'post', title: 'T', url: '/', category: 'favorite', postId: 1 });
      updateBookmarkVisit(id!);
      const bookmark = getBookmark(id!);
      expect(bookmark!.visitCount).toBe(1);
      expect(bookmark!.lastVisited).toBeTruthy();
    });
  });

  describe('removeBookmark / removeBookmarksByCategory / clearBookmarks', () => {
    it('ID로 삭제', () => {
      const id = addBookmark({ type: 'post', title: 'T', url: '/', category: 'favorite', postId: 1 });
      expect(removeBookmark(id!)).toBe(true);
      expect(getBookmarks()).toHaveLength(0);
    });

    it('존재하지 않는 ID는 false', () => {
      expect(removeBookmark('nope')).toBe(false);
    });

    it('카테고리별 삭제', () => {
      addBookmark({ type: 'post', title: 'P1', url: '/1', category: 'favorite', postId: 1 });
      addBookmark({ type: 'post', title: 'P2', url: '/2', category: 'favorite', postId: 2 });
      addBookmark({ type: 'post', title: 'P3', url: '/3', category: 'important', postId: 3 });
      expect(removeBookmarksByCategory('favorite')).toBe(2);
      expect(getBookmarks()).toHaveLength(1);
    });

    it('전체 삭제', () => {
      addBookmark({ type: 'post', title: 'T', url: '/', category: 'favorite', postId: 1 });
      clearBookmarks();
      expect(getBookmarks()).toHaveLength(0);
    });
  });

  describe('getBookmarkStats', () => {
    it('통계를 올바르게 계산', () => {
      addBookmark({ type: 'post', title: 'P1', url: '/1', category: 'favorite', postId: 1 });
      addBookmark({ type: 'rolling-paper', title: 'RP', url: '/rp', category: 'friends', nickname: 'a' });

      const stats = getBookmarkStats();
      expect(stats.total).toBe(2);
      expect(stats.byType['post']).toBe(1);
      expect(stats.byType['rolling-paper']).toBe(1);
      expect(stats.byCategory['favorite']).toBe(1);
      expect(stats.byCategory['friends']).toBe(1);
    });
  });

  describe('searchBookmarks', () => {
    beforeEach(() => {
      addBookmark({ type: 'post', title: '맛집 추천', url: '/1', category: 'favorite', postId: 1 });
      addBookmark({ type: 'post', title: '여행 후기', url: '/2', category: 'favorite', postId: 2 });
    });

    it('제목으로 검색', () => {
      expect(searchBookmarks('맛집')).toHaveLength(1);
    });

    it('대소문자 무시', () => {
      addBookmark({ type: 'post', title: 'Hello World', url: '/3', category: 'other', postId: 3 });
      expect(searchBookmarks('hello')).toHaveLength(1);
    });

    it('결과 없으면 빈 배열', () => {
      expect(searchBookmarks('존재하지않는')).toHaveLength(0);
    });
  });

  describe('sortBookmarks', () => {
    const bookmarks: Bookmark[] = [
      { id: '1', type: 'post', title: 'B', url: '/', category: 'favorite', createdAt: '2026-01-02T00:00:00Z', visitCount: 5 },
      { id: '2', type: 'post', title: 'A', url: '/', category: 'favorite', createdAt: '2026-01-01T00:00:00Z', visitCount: 10 },
      { id: '3', type: 'post', title: 'C', url: '/', category: 'favorite', createdAt: '2026-01-03T00:00:00Z', visitCount: 1 },
    ];

    it('createdAt 내림차순 (기본값)', () => {
      const sorted = sortBookmarks(bookmarks);
      expect(sorted[0].id).toBe('3');
    });

    it('createdAt 오름차순', () => {
      const sorted = sortBookmarks(bookmarks, 'createdAt', 'asc');
      expect(sorted[0].id).toBe('2');
    });

    it('visitCount 내림차순', () => {
      const sorted = sortBookmarks(bookmarks, 'visitCount', 'desc');
      expect(sorted[0].visitCount).toBe(10);
    });

    it('title 오름차순', () => {
      const sorted = sortBookmarks(bookmarks, 'title', 'asc');
      expect(sorted[0].title).toBe('A');
    });

    it('원본 배열을 변경하지 않음', () => {
      const original = [...bookmarks];
      sortBookmarks(bookmarks, 'title', 'asc');
      expect(bookmarks).toEqual(original);
    });
  });

  describe('CATEGORY_LABELS', () => {
    it('모든 카테고리에 한글 라벨이 존재', () => {
      expect(CATEGORY_LABELS['favorite']).toBe('즐겨찾기');
      expect(CATEGORY_LABELS['read-later']).toBe('나중에 읽기');
      expect(CATEGORY_LABELS['important']).toBe('중요');
      expect(CATEGORY_LABELS['friends']).toBe('친구 롤링페이퍼');
      expect(CATEGORY_LABELS['birthday']).toBe('생일/기념일');
      expect(CATEGORY_LABELS['other']).toBe('기타');
    });
  });
});
