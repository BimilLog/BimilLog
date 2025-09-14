/**
 * 자동 복구 메커니즘 구현
 */

import { logger } from '@/lib/utils/logger';

/**
 * 임시 저장 매니저
 */
export class DraftManager {
  private static readonly DRAFT_PREFIX = 'draft_';
  private static readonly MAX_DRAFTS = 10;
  private static readonly DRAFT_EXPIRY = 24 * 60 * 60 * 1000; // 24시간

  /**
   * 게시글 임시저장
   */
  static savePostDraft(data: { title?: string; content?: string }): string {
    const draftKey = `${this.DRAFT_PREFIX}post_${Date.now()}`;
    const draftData = {
      ...data,
      savedAt: new Date().toISOString(),
      type: 'post'
    };

    localStorage.setItem(draftKey, JSON.stringify(draftData));
    this.cleanupOldDrafts('post');
    logger.info('Post draft saved:', draftKey);

    return draftKey;
  }

  /**
   * 댓글 임시저장
   */
  static saveCommentDraft(postId: string, content: string): void {
    const draftKey = `${this.DRAFT_PREFIX}comment_${postId}`;
    const draftData = {
      content,
      postId,
      savedAt: new Date().toISOString(),
      type: 'comment'
    };

    sessionStorage.setItem(draftKey, JSON.stringify(draftData));
    logger.info('Comment draft saved:', draftKey);
  }

  /**
   * 롤링페이퍼 메시지 임시저장
   */
  static savePaperMessageDraft(userName: string, data: {
    content?: string;
    decoType?: string;
    position?: { x: number; y: number };
  }): string {
    const draftKey = `${this.DRAFT_PREFIX}paper_${userName}`;
    const draftData = {
      ...data,
      userName,
      savedAt: new Date().toISOString(),
      type: 'paper'
    };

    localStorage.setItem(draftKey, JSON.stringify(draftData));
    logger.info('Paper message draft saved:', draftKey);

    return draftKey;
  }

  /**
   * 임시저장 데이터 복구
   */
  static getDraft<T = unknown>(key: string): T | null {
    try {
      const storage = key.includes('comment') ? sessionStorage : localStorage;
      const draftData = storage.getItem(key);

      if (!draftData) return null;

      const parsed = JSON.parse(draftData);

      // 만료 검사
      if (this.isDraftExpired(parsed.savedAt)) {
        storage.removeItem(key);
        return null;
      }

      return parsed as T;
    } catch (error) {
      logger.warn('Failed to get draft:', error);
      return null;
    }
  }

  /**
   * 게시글 임시저장 목록 조회
   */
  static getPostDrafts(): Array<{ key: string; data: unknown }> {
    return this.getDraftsByType('post', localStorage);
  }

  /**
   * 댓글 임시저장 데이터 조회
   */
  static getCommentDraft(postId: string): { content: string; savedAt: string } | null {
    const key = `${this.DRAFT_PREFIX}comment_${postId}`;
    return this.getDraft(key);
  }

  /**
   * 롤링페이퍼 메시지 임시저장 조회
   */
  static getPaperMessageDraft(userName: string): {
    content?: string;
    decoType?: string;
    position?: { x: number; y: number };
    savedAt: string;
  } | null {
    const key = `${this.DRAFT_PREFIX}paper_${userName}`;
    return this.getDraft(key);
  }

  /**
   * 임시저장 데이터 삭제
   */
  static removeDraft(key: string): void {
    const storage = key.includes('comment') ? sessionStorage : localStorage;
    storage.removeItem(key);
    logger.info('Draft removed:', key);
  }

  /**
   * 특정 타입의 임시저장 데이터 모두 삭제
   */
  static clearDraftsByType(type: string): void {
    const storage = type === 'comment' ? sessionStorage : localStorage;
    const keys = Object.keys(storage).filter(key =>
      key.startsWith(this.DRAFT_PREFIX) && key.includes(type)
    );

    keys.forEach(key => storage.removeItem(key));
    logger.info(`Cleared ${keys.length} drafts of type: ${type}`);
  }

  private static getDraftsByType(type: string, storage: Storage): Array<{ key: string; data: unknown }> {
    const drafts: Array<{ key: string; data: unknown }> = [];

    try {
      Object.keys(storage)
        .filter(key => key.startsWith(this.DRAFT_PREFIX) && key.includes(type))
        .forEach(key => {
          const draftData = storage.getItem(key);
          if (draftData) {
            const parsed = JSON.parse(draftData);
            if (!this.isDraftExpired(parsed.savedAt)) {
              drafts.push({ key, data: parsed });
            } else {
              storage.removeItem(key);
            }
          }
        });
    } catch (error) {
      logger.warn('Failed to get drafts by type:', error);
    }

    return drafts;
  }

  private static isDraftExpired(savedAt: string): boolean {
    const now = Date.now();
    const savedTime = new Date(savedAt).getTime();
    return now - savedTime > this.DRAFT_EXPIRY;
  }

  private static cleanupOldDrafts(type: string): void {
    try {
      const storage = localStorage;
      const drafts = this.getDraftsByType(type, storage);

      if (drafts.length > this.MAX_DRAFTS) {
        // 오래된 draft 정리 로직: savedAt 기준으로 오래된 순 정렬하여 최대 개수 초과분 삭제
        const sortedDrafts = drafts
          .sort((a, b) => {
            const timeA = new Date((a.data as { savedAt: string }).savedAt).getTime();
            const timeB = new Date((b.data as { savedAt: string }).savedAt).getTime();
            return timeA - timeB; // 오래된 것이 앞에 오도록 정렬
          });

        // 초과분만 삭제하여 최대 개수 유지 (가장 오래된 것부터 제거)
        const toRemove = sortedDrafts.slice(0, drafts.length - this.MAX_DRAFTS);
        toRemove.forEach(({ key }) => storage.removeItem(key));

        logger.info(`Cleaned up ${toRemove.length} old ${type} drafts`);
      }
    } catch (error) {
      logger.warn('Failed to cleanup old drafts:', error);
    }
  }
}

/**
 * 재연결 매니저
 */
export class ReconnectionManager {
  private static retryTimers = new Map<string, NodeJS.Timeout>();
  private static reconnectCounts = new Map<string, number>();

  /**
   * 지수 백오프 재연결 시도
   */
  static scheduleReconnection(
    key: string,
    reconnectFn: () => Promise<void>,
    options: {
      maxRetries?: number;
      baseDelay?: number;
      maxDelay?: number;
    } = {}
  ): void {
    const {
      maxRetries = 5,
      baseDelay = 1000,
      maxDelay = 30000
    } = options;

    const currentCount = this.reconnectCounts.get(key) || 0;

    if (currentCount >= maxRetries) {
      logger.warn(`Max reconnection attempts reached for ${key}`);
      return;
    }

    // 지수 백오프 알고리즘: 시도 횟수에 따라 대기 시간을 지수적으로 증가
    // 2^currentCount를 곱하여 1초 -> 2초 -> 4초 -> 8초... 순으로 증가, maxDelay로 상한 설정
    const delay = Math.min(baseDelay * Math.pow(2, currentCount), maxDelay);

    // 기존 타이머 정리하여 중복 실행 방지
    const existingTimer = this.retryTimers.get(key);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    // 새 타이머 설정
    const timer = setTimeout(async () => {
      try {
        await reconnectFn();
        // 성공시 카운터 리셋
        this.reconnectCounts.delete(key);
        this.retryTimers.delete(key);
        logger.info(`Reconnection successful for ${key}`);
      } catch (error) {
        // 실패시 카운터 증가하고 재귀적으로 재시도 스케줄링
        this.reconnectCounts.set(key, currentCount + 1);
        this.scheduleReconnection(key, reconnectFn, options);
        logger.warn(`Reconnection failed for ${key}:`, error);
      }
    }, delay);

    this.retryTimers.set(key, timer);
    this.reconnectCounts.set(key, currentCount + 1);

    logger.info(`Scheduled reconnection for ${key} in ${delay}ms (attempt ${currentCount + 1})`);
  }

  /**
   * 재연결 시도 취소
   */
  static cancelReconnection(key: string): void {
    const timer = this.retryTimers.get(key);
    if (timer) {
      clearTimeout(timer);
      this.retryTimers.delete(key);
      this.reconnectCounts.delete(key);
      logger.info(`Cancelled reconnection for ${key}`);
    }
  }

  /**
   * 모든 재연결 시도 취소
   */
  static cancelAllReconnections(): void {
    this.retryTimers.forEach((timer, key) => {
      clearTimeout(timer);
      logger.info(`Cancelled reconnection for ${key}`);
    });

    this.retryTimers.clear();
    this.reconnectCounts.clear();
  }

  /**
   * 현재 재연결 상태 조회
   */
  static getReconnectionStatus(): Array<{ key: string; attempt: number }> {
    return Array.from(this.reconnectCounts.entries())
      .map(([key, count]) => ({ key, attempt: count }));
  }
}

/**
 * 캐시 복구 매니저
 */
export class CacheRecoveryManager {
  private static readonly CACHE_PREFIX = 'cache_';
  private static readonly CACHE_EXPIRY = 5 * 60 * 1000; // 5분

  /**
   * 데이터 캐시 저장
   */
  static setCacheData<T>(key: string, data: T): void {
    try {
      const cacheData = {
        data,
        timestamp: Date.now(),
        expires: Date.now() + this.CACHE_EXPIRY
      };

      sessionStorage.setItem(`${this.CACHE_PREFIX}${key}`, JSON.stringify(cacheData));
    } catch (error) {
      logger.warn('Failed to set cache data:', error);
    }
  }

  /**
   * 캐시 데이터 조회
   */
  static getCacheData<T>(key: string): T | null {
    try {
      const cachedItem = sessionStorage.getItem(`${this.CACHE_PREFIX}${key}`);
      if (!cachedItem) return null;

      const parsed = JSON.parse(cachedItem);

      // 캐시 만료 검사: 현재 시간과 expires 비교하여 만료된 캐시 자동 삭제
      if (Date.now() > parsed.expires) {
        sessionStorage.removeItem(`${this.CACHE_PREFIX}${key}`);
        return null;
      }

      return parsed.data as T;
    } catch (error) {
      logger.warn('Failed to get cache data:', error);
      return null;
    }
  }

  /**
   * 캐시 데이터 삭제
   */
  static removeCacheData(key: string): void {
    sessionStorage.removeItem(`${this.CACHE_PREFIX}${key}`);
  }

  /**
   * 모든 캐시 데이터 정리
   */
  static clearAllCache(): void {
    try {
      Object.keys(sessionStorage)
        .filter(key => key.startsWith(this.CACHE_PREFIX))
        .forEach(key => sessionStorage.removeItem(key));

      logger.info('All cache data cleared');
    } catch (error) {
      logger.warn('Failed to clear cache:', error);
    }
  }

  /**
   * 만료된 캐시 데이터 정리
   */
  static cleanupExpiredCache(): void {
    try {
      const now = Date.now();
      const expiredKeys: string[] = [];

      Object.keys(sessionStorage)
        .filter(key => key.startsWith(this.CACHE_PREFIX))
        .forEach(key => {
          try {
            const item = sessionStorage.getItem(key);
            if (item) {
              const parsed = JSON.parse(item);
              if (now > parsed.expires) {
                expiredKeys.push(key);
              }
            }
          } catch {
            // 파싱 실패한 캐시는 삭제
            expiredKeys.push(key);
          }
        });

      expiredKeys.forEach(key => sessionStorage.removeItem(key));

      if (expiredKeys.length > 0) {
        logger.info(`Cleaned up ${expiredKeys.length} expired cache entries`);
      }
    } catch (error) {
      logger.warn('Failed to cleanup expired cache:', error);
    }
  }
}

/**
 * 복구 유틸리티 초기화
 */
export function initializeRecoveryMechanisms(): void {
  // 페이지 로드시 만료된 캐시 정리
  CacheRecoveryManager.cleanupExpiredCache();

  // 주기적으로 만료된 캐시 정리 (5분마다)
  setInterval(() => {
    CacheRecoveryManager.cleanupExpiredCache();
  }, 5 * 60 * 1000);

  // 페이지 언로드시 모든 재연결 시도 취소
  if (typeof window !== 'undefined') {
    window.addEventListener('beforeunload', () => {
      ReconnectionManager.cancelAllReconnections();
    });
  }

  logger.info('Recovery mechanisms initialized');
}

/**
 * 복구 상태 조회
 */
export function getRecoveryStatus() {
  return {
    drafts: {
      posts: DraftManager.getPostDrafts().length,
      comments: Object.keys(sessionStorage).filter(key => key.includes('draft_comment')).length,
      papers: Object.keys(localStorage).filter(key => key.includes('draft_paper')).length
    },
    reconnections: ReconnectionManager.getReconnectionStatus(),
    cache: Object.keys(sessionStorage).filter(key => key.startsWith('cache_')).length
  };
}