/**
 * @deprecated 이 파일은 더 이상 사용되지 않습니다. @/lib/utils/storage를 사용하세요.
 * 하위 호환성을 위해 storage.ts로 리다이렉트됩니다.
 */

import { storage, type RecentVisit } from '@/lib/utils/storage';
import { formatRelativeDate } from '@/lib/date-utils';

// 하위 호환성을 위한 래퍼 함수들
export const addRecentVisit = (nickname: string): void => {
  storage.local.addRecentVisit(nickname);
};

export const getRecentVisits = (): RecentVisit[] => {
  return storage.local.getRecentVisits();
};

export const removeRecentVisit = (nickname: string): void => {
  storage.local.removeRecentVisit(nickname);
};

export const clearRecentVisits = (): void => {
  storage.local.clearRecentVisits();
};

export const getRelativeTimeString = (dateString: string): string => {
  return formatRelativeDate(dateString);
};

// 타입도 함께 내보내기
export type { RecentVisit }; 