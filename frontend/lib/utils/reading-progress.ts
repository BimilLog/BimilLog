/**
 * 읽음 표시 & 진행률 트래킹 유틸리티
 * 게시글 읽기 진행률 및 읽음 상태 관리
 */

export interface ReadingProgress {
  postId: number;
  progress: number; // 0-100
  scrollPosition: number;
  totalHeight: number;
  readTime: number; // seconds
  startedAt: string;
  updatedAt: string;
  completedAt?: string;
  isCompleted: boolean;
}

export interface ReadingSession {
  postId: number;
  startTime: number;
  lastActiveTime: number;
  totalReadTime: number;
  isActive: boolean;
}

export interface ReadingStats {
  totalPostsRead: number;
  totalReadTime: number; // seconds
  averageReadTime: number;
  completionRate: number;
  recentlyRead: number[];
  mostRead: { postId: number; count: number }[];
}

const PROGRESS_KEY = 'bimillog_reading_progress';
const STATS_KEY = 'bimillog_reading_stats';
const SESSION_KEY = 'bimillog_reading_session';
const READ_POSTS_KEY = 'bimillog_read_posts';
const RETENTION_DAYS = 180; // 6개월 보관
const INACTIVITY_TIMEOUT = 30000; // 30초 비활동 시 세션 종료

/**
 * 읽기 진행률 저장
 */
export function saveReadingProgress(
  postId: number,
  scrollPosition: number,
  totalHeight: number
): void {
  if (typeof window === 'undefined') return;

  const progress = calculateProgress(scrollPosition, totalHeight);
  const now = new Date().toISOString();

  const allProgress = getReadingProgress();
  const existing = allProgress.find(p => p.postId === postId);

  // 세션에서 읽은 시간 가져오기
  const session = getActiveSession();
  const readTime = session?.postId === postId ? session.totalReadTime : 0;

  if (existing) {
    existing.progress = progress;
    existing.scrollPosition = scrollPosition;
    existing.totalHeight = totalHeight;
    existing.readTime = readTime;
    existing.updatedAt = now;

    // 90% 이상 읽으면 완료 처리
    if (progress >= 90 && !existing.isCompleted) {
      existing.isCompleted = true;
      existing.completedAt = now;
      markAsRead(postId);
    }
  } else {
    allProgress.push({
      postId,
      progress,
      scrollPosition,
      totalHeight,
      readTime,
      startedAt: now,
      updatedAt: now,
      isCompleted: progress >= 90,
      completedAt: progress >= 90 ? now : undefined,
    });

    if (progress >= 90) {
      markAsRead(postId);
    }
  }

  // 오래된 진행률 제거
  const cutoffDate = new Date();
  cutoffDate.setDate(cutoffDate.getDate() - RETENTION_DAYS);
  const filtered = allProgress.filter(p =>
    new Date(p.updatedAt) > cutoffDate
  );

  localStorage.setItem(PROGRESS_KEY, JSON.stringify(filtered));
  updateReadingStats();
}

/**
 * 진행률 계산
 */
function calculateProgress(scrollPosition: number, totalHeight: number): number {
  if (totalHeight === 0) return 0;
  const viewportHeight = window.innerHeight;
  const progress = ((scrollPosition + viewportHeight) / totalHeight) * 100;
  return Math.min(100, Math.max(0, progress));
}

/**
 * 특정 게시글의 읽기 진행률 가져오기
 */
export function getPostProgress(postId: number): ReadingProgress | null {
  const allProgress = getReadingProgress();
  return allProgress.find(p => p.postId === postId) || null;
}

/**
 * 모든 읽기 진행률 가져오기
 */
export function getReadingProgress(): ReadingProgress[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(PROGRESS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get reading progress:', error);
  }

  return [];
}

/**
 * 읽음으로 표시
 */
export function markAsRead(postId: number): void {
  if (typeof window === 'undefined') return;

  const readPosts = getReadPosts();
  if (!readPosts.includes(postId)) {
    readPosts.push(postId);

    // 최대 1000개까지만 저장
    if (readPosts.length > 1000) {
      readPosts.shift();
    }

    localStorage.setItem(READ_POSTS_KEY, JSON.stringify(readPosts));
  }
}

/**
 * 읽은 게시글 목록 가져오기
 */
export function getReadPosts(): number[] {
  if (typeof window === 'undefined') return [];

  try {
    const stored = localStorage.getItem(READ_POSTS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get read posts:', error);
  }

  return [];
}

/**
 * 게시글 읽음 여부 확인
 */
export function isPostRead(postId: number): boolean {
  return getReadPosts().includes(postId);
}

/**
 * 읽기 세션 시작
 */
export function startReadingSession(postId: number): void {
  if (typeof window === 'undefined') return;

  const now = Date.now();
  const session: ReadingSession = {
    postId,
    startTime: now,
    lastActiveTime: now,
    totalReadTime: 0,
    isActive: true,
  };

  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  startSessionTimer();
}

/**
 * 읽기 세션 업데이트
 */
export function updateReadingSession(): void {
  if (typeof window === 'undefined') return;

  const session = getActiveSession();
  if (!session || !session.isActive) return;

  const now = Date.now();
  const timeDiff = now - session.lastActiveTime;

  // 비활동 시간이 타임아웃보다 길면 세션 종료
  if (timeDiff > INACTIVITY_TIMEOUT) {
    endReadingSession();
    return;
  }

  // 읽은 시간 업데이트
  session.totalReadTime += Math.floor(timeDiff / 1000);
  session.lastActiveTime = now;

  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

/**
 * 읽기 세션 종료
 */
export function endReadingSession(): void {
  if (typeof window === 'undefined') return;

  const session = getActiveSession();
  if (!session) return;

  session.isActive = false;
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));

  // 읽은 시간을 진행률에 반영
  const progress = getPostProgress(session.postId);
  if (progress) {
    progress.readTime = session.totalReadTime;
    const allProgress = getReadingProgress();
    const index = allProgress.findIndex(p => p.postId === session.postId);
    if (index !== -1) {
      allProgress[index] = progress;
      localStorage.setItem(PROGRESS_KEY, JSON.stringify(allProgress));
    }
  }

  updateReadingStats();
}

/**
 * 활성 세션 가져오기
 */
export function getActiveSession(): ReadingSession | null {
  if (typeof window === 'undefined') return null;

  try {
    const stored = sessionStorage.getItem(SESSION_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get active session:', error);
  }

  return null;
}

/**
 * 세션 타이머 시작
 */
let sessionTimer: NodeJS.Timeout | null = null;

function startSessionTimer(): void {
  if (sessionTimer) {
    clearInterval(sessionTimer);
  }

  sessionTimer = setInterval(() => {
    updateReadingSession();
  }, 5000); // 5초마다 업데이트
}

/**
 * 읽기 통계 업데이트
 */
function updateReadingStats(): void {
  const progress = getReadingProgress();
  const readPosts = getReadPosts();

  const completedProgress = progress.filter(p => p.isCompleted);
  const totalReadTime = progress.reduce((sum, p) => sum + p.readTime, 0);
  const averageReadTime = completedProgress.length > 0
    ? totalReadTime / completedProgress.length
    : 0;

  // 가장 많이 읽은 게시글 (중복 읽기 트래킹)
  const readCounts = new Map<number, number>();
  progress.forEach(p => {
    readCounts.set(p.postId, (readCounts.get(p.postId) || 0) + 1);
  });

  const mostRead = Array.from(readCounts.entries())
    .map(([postId, count]) => ({ postId, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);

  const stats: ReadingStats = {
    totalPostsRead: readPosts.length,
    totalReadTime,
    averageReadTime,
    completionRate: progress.length > 0
      ? (completedProgress.length / progress.length) * 100
      : 0,
    recentlyRead: readPosts.slice(-10).reverse(),
    mostRead,
  };

  localStorage.setItem(STATS_KEY, JSON.stringify(stats));
}

/**
 * 읽기 통계 가져오기
 */
export function getReadingStats(): ReadingStats | null {
  if (typeof window === 'undefined') return null;

  try {
    const stored = localStorage.getItem(STATS_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (error) {
    console.error('Failed to get reading stats:', error);
  }

  // 통계가 없으면 생성
  updateReadingStats();
  const newStats = localStorage.getItem(STATS_KEY);
  return newStats ? JSON.parse(newStats) : null;
}

/**
 * 읽기 시간 포맷팅
 */
export function formatReadTime(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}초`;
  } else if (seconds < 3600) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return remainingSeconds > 0
      ? `${minutes}분 ${remainingSeconds}초`
      : `${minutes}분`;
  } else {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return minutes > 0
      ? `${hours}시간 ${minutes}분`
      : `${hours}시간`;
  }
}

/**
 * 진행 중인 읽기 목록 가져오기
 */
export function getInProgressReading(limit: number = 10): ReadingProgress[] {
  const progress = getReadingProgress();
  return progress
    .filter(p => !p.isCompleted && p.progress > 0)
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, limit);
}

/**
 * 최근 완료한 읽기 목록 가져오기
 */
export function getRecentlyCompleted(limit: number = 10): ReadingProgress[] {
  const progress = getReadingProgress();
  return progress
    .filter(p => p.isCompleted && p.completedAt)
    .sort((a, b) => new Date(b.completedAt!).getTime() - new Date(a.completedAt!).getTime())
    .slice(0, limit);
}

/**
 * 읽기 데이터 초기화
 */
export function clearReadingData(): void {
  if (typeof window === 'undefined') return;

  localStorage.removeItem(PROGRESS_KEY);
  localStorage.removeItem(STATS_KEY);
  localStorage.removeItem(READ_POSTS_KEY);
  sessionStorage.removeItem(SESSION_KEY);

  if (sessionTimer) {
    clearInterval(sessionTimer);
    sessionTimer = null;
  }
}

/**
 * 읽기 진행률 제거
 */
export function removeReadingProgress(postId: number): void {
  const allProgress = getReadingProgress();
  const filtered = allProgress.filter(p => p.postId !== postId);
  localStorage.setItem(PROGRESS_KEY, JSON.stringify(filtered));

  // 읽은 목록에서도 제거
  const readPosts = getReadPosts();
  const filteredPosts = readPosts.filter(id => id !== postId);
  localStorage.setItem(READ_POSTS_KEY, JSON.stringify(filteredPosts));

  updateReadingStats();
}