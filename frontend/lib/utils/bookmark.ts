/**
 * 개인 북마크 시스템 유틸리티
 * localStorage를 활용한 게시글, 롤링페이퍼, 댓글 북마크 관리
 */

export interface Bookmark {
  id: string;
  type: 'post' | 'rolling-paper' | 'comment';
  title: string;
  url: string;
  category: BookmarkCategory;
  memo?: string;

  // 타입별 추가 정보
  postId?: number;
  nickname?: string; // 롤링페이퍼용
  commentId?: number;

  createdAt: string;
  lastVisited?: string;
  visitCount: number;
}

export type BookmarkCategory =
  | 'favorite'      // 즐겨찾기
  | 'read-later'    // 나중에 읽기
  | 'important'     // 중요
  | 'friends'       // 친구 롤링페이퍼
  | 'birthday'      // 생일/기념일용
  | 'other';        // 기타

export interface BookmarkStats {
  total: number;
  byType: Record<Bookmark['type'], number>;
  byCategory: Record<BookmarkCategory, number>;
}

const BOOKMARK_KEY = 'bimillog_bookmarks';
const BOOKMARK_LIMIT = 500; // 최대 북마크 개수

/**
 * 모든 북마크 가져오기
 */
export function getBookmarks(): Bookmark[] {
  if (typeof window === 'undefined') return [];

  try {
    const bookmarks = localStorage.getItem(BOOKMARK_KEY);
    return bookmarks ? JSON.parse(bookmarks) : [];
  } catch (error) {
    console.error('Failed to get bookmarks:', error);
    return [];
  }
}

/**
 * 타입별 북마크 가져오기
 */
export function getBookmarksByType(type: Bookmark['type']): Bookmark[] {
  return getBookmarks().filter(b => b.type === type);
}

/**
 * 카테고리별 북마크 가져오기
 */
export function getBookmarksByCategory(category: BookmarkCategory): Bookmark[] {
  return getBookmarks().filter(b => b.category === category);
}

/**
 * 특정 북마크 가져오기
 */
export function getBookmark(id: string): Bookmark | null {
  const bookmarks = getBookmarks();
  return bookmarks.find(b => b.id === id) || null;
}

/**
 * 게시글 북마크 확인
 */
export function isPostBookmarked(postId: number): boolean {
  const bookmarks = getBookmarks();
  return bookmarks.some(b => b.type === 'post' && b.postId === postId);
}

/**
 * 롤링페이퍼 북마크 확인
 */
export function isRollingPaperBookmarked(nickname: string): boolean {
  const bookmarks = getBookmarks();
  return bookmarks.some(b => b.type === 'rolling-paper' && b.nickname === nickname);
}

/**
 * 북마크 추가
 */
export function addBookmark(bookmark: Omit<Bookmark, 'id' | 'createdAt' | 'visitCount'>): string | null {
  if (typeof window === 'undefined') return null;

  try {
    const bookmarks = getBookmarks();

    // 중복 체크
    if (bookmark.type === 'post' && bookmark.postId) {
      if (isPostBookmarked(bookmark.postId)) {
        console.warn('Post already bookmarked');
        return null;
      }
    }

    if (bookmark.type === 'rolling-paper' && bookmark.nickname) {
      if (isRollingPaperBookmarked(bookmark.nickname)) {
        console.warn('Rolling paper already bookmarked');
        return null;
      }
    }

    // 제한 체크
    if (bookmarks.length >= BOOKMARK_LIMIT) {
      console.warn('Bookmark limit reached');
      return null;
    }

    const newBookmark: Bookmark = {
      ...bookmark,
      id: `bookmark_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
      createdAt: new Date().toISOString(),
      visitCount: 0,
    };

    bookmarks.unshift(newBookmark); // 최신 북마크를 앞에 추가
    localStorage.setItem(BOOKMARK_KEY, JSON.stringify(bookmarks));

    return newBookmark.id;
  } catch (error) {
    console.error('Failed to add bookmark:', error);
    return null;
  }
}

/**
 * 게시글 북마크 토글
 */
export function togglePostBookmark(
  postId: number,
  title: string,
  category: BookmarkCategory = 'favorite'
): boolean {
  if (isPostBookmarked(postId)) {
    return removeBookmarkByPostId(postId);
  } else {
    const id = addBookmark({
      type: 'post',
      title,
      url: `/board/post/${postId}`,
      category,
      postId,
    });
    return !!id;
  }
}

/**
 * 롤링페이퍼 북마크 토글
 */
export function toggleRollingPaperBookmark(
  nickname: string,
  category: BookmarkCategory = 'friends'
): boolean {
  if (isRollingPaperBookmarked(nickname)) {
    return removeBookmarkByNickname(nickname);
  } else {
    const id = addBookmark({
      type: 'rolling-paper',
      title: `${decodeURIComponent(nickname)}님의 롤링페이퍼`,
      url: `/rolling-paper/${nickname}`,
      category,
      nickname,
    });
    return !!id;
  }
}

/**
 * 북마크 업데이트
 */
export function updateBookmark(id: string, updates: Partial<Bookmark>): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const bookmarks = getBookmarks();
    const index = bookmarks.findIndex(b => b.id === id);

    if (index === -1) return false;

    bookmarks[index] = {
      ...bookmarks[index],
      ...updates,
      id: bookmarks[index].id, // ID는 변경 불가
      type: bookmarks[index].type, // 타입은 변경 불가
    };

    localStorage.setItem(BOOKMARK_KEY, JSON.stringify(bookmarks));
    return true;
  } catch (error) {
    console.error('Failed to update bookmark:', error);
    return false;
  }
}

/**
 * 북마크 방문 기록 업데이트
 */
export function updateBookmarkVisit(id: string): void {
  const bookmark = getBookmark(id);
  if (bookmark) {
    updateBookmark(id, {
      lastVisited: new Date().toISOString(),
      visitCount: bookmark.visitCount + 1,
    });
  }
}

/**
 * 북마크 삭제
 */
export function removeBookmark(id: string): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const bookmarks = getBookmarks();
    const filtered = bookmarks.filter(b => b.id !== id);

    if (filtered.length === bookmarks.length) return false;

    localStorage.setItem(BOOKMARK_KEY, JSON.stringify(filtered));
    return true;
  } catch (error) {
    console.error('Failed to remove bookmark:', error);
    return false;
  }
}

/**
 * 게시글 ID로 북마크 삭제
 */
function removeBookmarkByPostId(postId: number): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const bookmarks = getBookmarks();
    const filtered = bookmarks.filter(b => !(b.type === 'post' && b.postId === postId));

    if (filtered.length === bookmarks.length) return false;

    localStorage.setItem(BOOKMARK_KEY, JSON.stringify(filtered));
    return true;
  } catch (error) {
    console.error('Failed to remove bookmark:', error);
    return false;
  }
}

/**
 * 닉네임으로 롤링페이퍼 북마크 삭제
 */
function removeBookmarkByNickname(nickname: string): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const bookmarks = getBookmarks();
    const filtered = bookmarks.filter(b => !(b.type === 'rolling-paper' && b.nickname === nickname));

    if (filtered.length === bookmarks.length) return false;

    localStorage.setItem(BOOKMARK_KEY, JSON.stringify(filtered));
    return true;
  } catch (error) {
    console.error('Failed to remove bookmark:', error);
    return false;
  }
}

/**
 * 카테고리별 북마크 삭제
 */
export function removeBookmarksByCategory(category: BookmarkCategory): number {
  if (typeof window === 'undefined') return 0;

  try {
    const bookmarks = getBookmarks();
    const filtered = bookmarks.filter(b => b.category !== category);
    const removedCount = bookmarks.length - filtered.length;

    localStorage.setItem(BOOKMARK_KEY, JSON.stringify(filtered));
    return removedCount;
  } catch (error) {
    console.error('Failed to remove bookmarks:', error);
    return 0;
  }
}

/**
 * 모든 북마크 삭제
 */
export function clearBookmarks(): void {
  if (typeof window === 'undefined') return;

  try {
    localStorage.removeItem(BOOKMARK_KEY);
  } catch (error) {
    console.error('Failed to clear bookmarks:', error);
  }
}

/**
 * 북마크 통계 가져오기
 */
export function getBookmarkStats(): BookmarkStats {
  const bookmarks = getBookmarks();

  const stats: BookmarkStats = {
    total: bookmarks.length,
    byType: {
      'post': 0,
      'rolling-paper': 0,
      'comment': 0,
    },
    byCategory: {
      'favorite': 0,
      'read-later': 0,
      'important': 0,
      'friends': 0,
      'birthday': 0,
      'other': 0,
    },
  };

  bookmarks.forEach(bookmark => {
    stats.byType[bookmark.type]++;
    stats.byCategory[bookmark.category]++;
  });

  return stats;
}

/**
 * 북마크 검색
 */
export function searchBookmarks(query: string): Bookmark[] {
  const lowercaseQuery = query.toLowerCase();
  return getBookmarks().filter(b =>
    b.title.toLowerCase().includes(lowercaseQuery) ||
    b.memo?.toLowerCase().includes(lowercaseQuery) ||
    b.url.toLowerCase().includes(lowercaseQuery)
  );
}

/**
 * 북마크 정렬
 */
export function sortBookmarks(
  bookmarks: Bookmark[],
  sortBy: 'createdAt' | 'visitCount' | 'title' = 'createdAt',
  order: 'asc' | 'desc' = 'desc'
): Bookmark[] {
  const sorted = [...bookmarks].sort((a, b) => {
    switch (sortBy) {
      case 'createdAt':
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      case 'visitCount':
        return a.visitCount - b.visitCount;
      case 'title':
        return a.title.localeCompare(b.title);
      default:
        return 0;
    }
  });

  return order === 'desc' ? sorted.reverse() : sorted;
}

/**
 * 카테고리 한글 라벨
 */
export const CATEGORY_LABELS: Record<BookmarkCategory, string> = {
  'favorite': '즐겨찾기',
  'read-later': '나중에 읽기',
  'important': '중요',
  'friends': '친구 롤링페이퍼',
  'birthday': '생일/기념일',
  'other': '기타',
};

/**
 * 카테고리 아이콘 이름 (Lucide React)
 */
export const CATEGORY_ICONS: Record<BookmarkCategory, string> = {
  'favorite': 'Star',
  'read-later': 'Clock',
  'important': 'AlertCircle',
  'friends': 'Users',
  'birthday': 'Gift',
  'other': 'Folder',
};