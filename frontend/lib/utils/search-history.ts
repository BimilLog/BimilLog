/**
 * 검색 히스토리 관리 유틸리티
 * localStorage를 활용한 검색어 저장 및 자동완성 기능
 */

export interface SearchHistory {
  id: string;
  query: string;
  searchedAt: string;
  count: number; // 검색 횟수
  lastSearched: string;
  type: 'post' | 'user' | 'rolling-paper'; // 검색 타입
}

export interface SearchStats {
  total: number;
  topSearches: SearchHistory[];
  recentSearches: SearchHistory[];
}

const SEARCH_HISTORY_KEY = 'bimillog_search_history';
const MAX_HISTORY = 20; // 최대 검색어 저장 개수
const TOP_SEARCHES_LIMIT = 5; // 인기 검색어 개수

/**
 * 모든 검색 히스토리 가져오기
 */
export function getSearchHistory(): SearchHistory[] {
  if (typeof window === 'undefined') return [];

  try {
    const history = localStorage.getItem(SEARCH_HISTORY_KEY);
    return history ? JSON.parse(history) : [];
  } catch (error) {
    console.error('Failed to get search history:', error);
    return [];
  }
}

/**
 * 타입별 검색 히스토리 가져오기
 */
export function getSearchHistoryByType(type: SearchHistory['type']): SearchHistory[] {
  return getSearchHistory().filter(h => h.type === type);
}

/**
 * 최근 검색어 가져오기
 */
export function getRecentSearches(limit: number = 10): SearchHistory[] {
  return getSearchHistory()
    .sort((a, b) => new Date(b.lastSearched).getTime() - new Date(a.lastSearched).getTime())
    .slice(0, limit);
}

/**
 * 인기 검색어 가져오기 (검색 횟수 기준)
 */
export function getTopSearches(limit: number = TOP_SEARCHES_LIMIT): SearchHistory[] {
  return getSearchHistory()
    .sort((a, b) => b.count - a.count)
    .slice(0, limit);
}

/**
 * 검색어 추가/업데이트
 */
export function addSearchHistory(
  query: string,
  type: SearchHistory['type'] = 'post'
): void {
  if (typeof window === 'undefined' || !query.trim()) return;

  try {
    const history = getSearchHistory();
    const normalizedQuery = query.toLowerCase().trim();

    // 기존 검색어 찾기
    const existingIndex = history.findIndex(
      h => h.query.toLowerCase() === normalizedQuery && h.type === type
    );

    const now = new Date().toISOString();

    if (existingIndex !== -1) {
      // 기존 검색어 업데이트
      history[existingIndex].count++;
      history[existingIndex].lastSearched = now;

      // 최신 검색어를 앞으로 이동
      const [existing] = history.splice(existingIndex, 1);
      history.unshift(existing);
    } else {
      // 새 검색어 추가
      const newHistory: SearchHistory = {
        id: `search_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
        query: query.trim(),
        searchedAt: now,
        count: 1,
        lastSearched: now,
        type,
      };

      history.unshift(newHistory);

      // 최대 개수 제한
      if (history.length > MAX_HISTORY) {
        history.pop();
      }
    }

    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(history));
  } catch (error) {
    console.error('Failed to add search history:', error);
  }
}

/**
 * 특정 검색어 삭제
 */
export function removeSearchHistory(id: string): boolean {
  if (typeof window === 'undefined') return false;

  try {
    const history = getSearchHistory();
    const filtered = history.filter(h => h.id !== id);

    if (filtered.length === history.length) return false;

    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(filtered));
    return true;
  } catch (error) {
    console.error('Failed to remove search history:', error);
    return false;
  }
}

/**
 * 타입별 검색 히스토리 삭제
 */
export function removeSearchHistoryByType(type: SearchHistory['type']): number {
  if (typeof window === 'undefined') return 0;

  try {
    const history = getSearchHistory();
    const filtered = history.filter(h => h.type !== type);
    const removedCount = history.length - filtered.length;

    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(filtered));
    return removedCount;
  } catch (error) {
    console.error('Failed to remove search history:', error);
    return 0;
  }
}

/**
 * 모든 검색 히스토리 삭제
 */
export function clearSearchHistory(): void {
  if (typeof window === 'undefined') return;

  try {
    localStorage.removeItem(SEARCH_HISTORY_KEY);
  } catch (error) {
    console.error('Failed to clear search history:', error);
  }
}

/**
 * 검색어 자동완성 제안
 */
export function getSearchSuggestions(
  query: string,
  type?: SearchHistory['type'],
  limit: number = 5
): SearchHistory[] {
  if (!query.trim()) return [];

  const normalizedQuery = query.toLowerCase().trim();
  const history = type ? getSearchHistoryByType(type) : getSearchHistory();

  return history
    .filter(h => h.query.toLowerCase().includes(normalizedQuery))
    .sort((a, b) => {
      // 정확히 일치하는 검색어 우선
      const aExact = a.query.toLowerCase() === normalizedQuery;
      const bExact = b.query.toLowerCase() === normalizedQuery;
      if (aExact && !bExact) return -1;
      if (!aExact && bExact) return 1;

      // 시작 부분이 일치하는 검색어 우선
      const aStarts = a.query.toLowerCase().startsWith(normalizedQuery);
      const bStarts = b.query.toLowerCase().startsWith(normalizedQuery);
      if (aStarts && !bStarts) return -1;
      if (!aStarts && bStarts) return 1;

      // 검색 횟수로 정렬
      return b.count - a.count;
    })
    .slice(0, limit);
}

/**
 * 검색 통계 가져오기
 */
export function getSearchStats(): SearchStats {
  const history = getSearchHistory();

  return {
    total: history.length,
    topSearches: getTopSearches(),
    recentSearches: getRecentSearches(),
  };
}

/**
 * 오래된 검색 기록 정리 (30일 이상)
 */
export function cleanupOldSearchHistory(): number {
  if (typeof window === 'undefined') return 0;

  try {
    const history = getSearchHistory();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const filtered = history.filter(h => {
      try {
        return new Date(h.lastSearched) > thirtyDaysAgo;
      } catch {
        return false;
      }
    });

    const removedCount = history.length - filtered.length;
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(filtered));
    return removedCount;
  } catch (error) {
    console.error('Failed to cleanup old search history:', error);
    return 0;
  }
}

/**
 * 검색어 하이라이트 처리용 헬퍼
 */
export function highlightSearchQuery(text: string, query: string): string {
  if (!query.trim()) return text;

  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
  return text.replace(regex, '<mark>$1</mark>');
}