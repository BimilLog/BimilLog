"use client";

import { useMutation } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { notificationCommand } from '@/lib/api';
import {
  getPendingUpdatesForAPI,
  clearPendingUpdates,
  hasPendingUpdates
} from '@/lib/utils/notification-sync';
import { logger } from '@/lib/utils/logger';
import { useAuth } from '@/hooks';
import { useToastStore } from '@/stores';

/**
 * 알림 동기화 훅
 * 30초마다 로컬스토리지의 대기 중인 알림 업데이트를 서버로 일괄 전송
 */
export const useNotificationSync = () => {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToastStore();
  const intervalRef = useRef<NodeJS.Timeout | undefined>(undefined);

  // 일괄 업데이트 뮤테이션
  const syncMutation = useMutation({
    mutationKey: ['notification', 'sync'],
    mutationFn: async () => {
      if (!hasPendingUpdates()) {
        logger.log('동기화할 알림 업데이트 없음');
        return null;
      }

      const updates = getPendingUpdatesForAPI();
      logger.log('알림 동기화 시작:', updates);

      // API 호출
      const response = await notificationCommand.batchUpdate(updates);

      // 성공 시 로컬스토리지 클리어
      clearPendingUpdates();
      logger.log('알림 동기화 완료');

      return response;
    },
    onSuccess: (response) => {
      if (!response) return; // 동기화할 내용이 없었던 경우

      if (response.success) {
        logger.log('✅ 알림 동기화 성공');
      }
    },
    onError: (error) => {
      logger.error('알림 동기화 실패:', error);
      showToast({
        type: 'error',
        message: '알림 동기화에 실패했습니다. 잠시 후 다시 시도됩니다.',
      });
      // 실패해도 로컬스토리지는 유지하여 다음 동기화 시 재시도
    }
  });

  // 수동 동기화 함수
  const syncNow = () => {
    if (isAuthenticated && hasPendingUpdates()) {
      syncMutation.mutate();
    }
  };

  // 5분마다 자동 동기화
  useEffect(() => {
    if (!isAuthenticated) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
      return;
    }

    // 초기 동기화 체크 (페이지 로드 시)
    if (hasPendingUpdates()) {
      syncMutation.mutate();
    }

    // 30초 간격 타이머 설정
    intervalRef.current = setInterval(() => {
      if (hasPendingUpdates()) {
        syncMutation.mutate();
      }
    }, 30 * 1000); // 30초

    // 클린업
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [isAuthenticated]);

  // 페이지 언로드 시 동기화
  useEffect(() => {
    const handleUnload = () => {
      if (hasPendingUpdates()) {
        // 동기적으로 실행되어야 하므로 sendBeacon 사용 고려
        const updates = getPendingUpdatesForAPI();
        const blob = new Blob([JSON.stringify(updates)], { type: 'application/json' });
        const url = `${process.env.NEXT_PUBLIC_API_URL}/api/notification/update`;
        navigator.sendBeacon(url, blob);
      }
    };

    window.addEventListener('beforeunload', handleUnload);
    return () => window.removeEventListener('beforeunload', handleUnload);
  }, []);

  return {
    syncNow,
    isSyncing: syncMutation.isPending,
    lastSyncError: syncMutation.error,
  };
};