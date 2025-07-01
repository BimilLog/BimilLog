interface RecentVisit {
  nickname: string;
  visitedAt: string;
  displayName: string;
}

const RECENT_VISITS_KEY = 'recent_rolling_papers';
const MAX_RECENT_VISITS = 5;

export const addRecentVisit = (nickname: string): void => {
  if (typeof window === 'undefined') return;
  
  const displayName = decodeURIComponent(nickname);
  const newVisit: RecentVisit = {
    nickname,
    visitedAt: new Date().toISOString(),
    displayName,
  };

  const existing = getRecentVisits();
  
  // 이미 방문한 곳이면 제거하고 맨 앞에 추가
  const filtered = existing.filter(visit => visit.nickname !== nickname);
  const updated = [newVisit, ...filtered].slice(0, MAX_RECENT_VISITS);
  
  try {
    localStorage.setItem(RECENT_VISITS_KEY, JSON.stringify(updated));
  } catch (error) {
    console.error('Failed to save recent visits:', error);
  }
};

export const getRecentVisits = (): RecentVisit[] => {
  if (typeof window === 'undefined') return [];
  
  try {
    const stored = localStorage.getItem(RECENT_VISITS_KEY);
    if (!stored) return [];
    
    const visits: RecentVisit[] = JSON.parse(stored);
    
    // 30일이 지난 방문 기록은 제거
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    
    const filtered = visits.filter(visit => 
      new Date(visit.visitedAt) > thirtyDaysAgo
    );
    
    // 필터링된 결과가 기존과 다르면 업데이트
    if (filtered.length !== visits.length) {
      localStorage.setItem(RECENT_VISITS_KEY, JSON.stringify(filtered));
    }
    
    return filtered;
  } catch (error) {
    console.error('Failed to get recent visits:', error);
    return [];
  }
};

export const removeRecentVisit = (nickname: string): void => {
  if (typeof window === 'undefined') return;
  
  const existing = getRecentVisits();
  const filtered = existing.filter(visit => visit.nickname !== nickname);
  
  try {
    localStorage.setItem(RECENT_VISITS_KEY, JSON.stringify(filtered));
  } catch (error) {
    console.error('Failed to remove recent visit:', error);
  }
};

export const clearRecentVisits = (): void => {
  if (typeof window === 'undefined') return;
  
  try {
    localStorage.removeItem(RECENT_VISITS_KEY);
  } catch (error) {
    console.error('Failed to clear recent visits:', error);
  }
};

export const getRelativeTimeString = (dateString: string): string => {
  const now = new Date();
  const date = new Date(dateString);
  const diffInMs = now.getTime() - date.getTime();
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);

  if (diffInMinutes < 1) return '방금 전';
  if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
  if (diffInHours < 24) return `${diffInHours}시간 전`;
  if (diffInDays < 7) return `${diffInDays}일 전`;
  return date.toLocaleDateString('ko-KR');
}; 