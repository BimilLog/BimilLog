/**
 * 알림 동기화 유틸리티
 * 로컬스토리지를 사용하여 알림 읽음/삭제 요청을 일괄 처리
 */

const STORAGE_KEY = 'notification_pending_updates';

interface PendingUpdates {
  readIds: Set<number>;
  deleteIds: Set<number>;
  lastSyncTime: number;
}

/**
 * 로컬스토리지에서 대기 중인 업데이트 가져오기
 */
export const getPendingUpdates = (): PendingUpdates => {
  if (typeof window === 'undefined') {
    return {
      readIds: new Set(),
      deleteIds: new Set(),
      lastSyncTime: Date.now(),
    };
  }

  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        readIds: new Set(parsed.readIds || []),
        deleteIds: new Set(parsed.deleteIds || []),
        lastSyncTime: parsed.lastSyncTime || Date.now(),
      };
    }
  } catch (error) {
    console.error('Failed to parse pending updates:', error);
  }

  return {
    readIds: new Set(),
    deleteIds: new Set(),
    lastSyncTime: Date.now(),
  };
};

/**
 * 로컬스토리지에 대기 중인 업데이트 저장
 */
const savePendingUpdates = (updates: PendingUpdates) => {
  if (typeof window === 'undefined') return;

  try {
    const toStore = {
      readIds: Array.from(updates.readIds),
      deleteIds: Array.from(updates.deleteIds),
      lastSyncTime: updates.lastSyncTime,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(toStore));
  } catch (error) {
    console.error('Failed to save pending updates:', error);
  }
};

/**
 * 읽음 처리할 알림 ID 추가
 */
export const addPendingRead = (notificationId: number) => {
  const updates = getPendingUpdates();

  // 삭제 목록에 있으면 추가하지 않음 (삭제가 우선)
  if (updates.deleteIds.has(notificationId)) {
    return;
  }

  updates.readIds.add(notificationId);
  savePendingUpdates(updates);
};

/**
 * 삭제할 알림 ID 추가
 */
export const addPendingDelete = (notificationId: number) => {
  const updates = getPendingUpdates();

  // 읽음 목록에 있으면 제거 (삭제가 우선)
  updates.readIds.delete(notificationId);
  updates.deleteIds.add(notificationId);

  savePendingUpdates(updates);
};

/**
 * 대기 중인 업데이트 초기화
 */
export const clearPendingUpdates = () => {
  if (typeof window === 'undefined') return;

  localStorage.removeItem(STORAGE_KEY);
};

/**
 * 동기화할 업데이트가 있는지 확인
 */
export const hasPendingUpdates = (): boolean => {
  const updates = getPendingUpdates();
  return updates.readIds.size > 0 || updates.deleteIds.size > 0;
};

/**
 * 대기 중인 업데이트를 API 요청 형식으로 변환
 */
export const getPendingUpdatesForAPI = () => {
  const updates = getPendingUpdates();

  return {
    readIds: Array.from(updates.readIds),
    deletedIds: Array.from(updates.deleteIds),
  };
};