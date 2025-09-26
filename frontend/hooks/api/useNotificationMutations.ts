import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys, mutationKeys } from '@/lib/tanstack-query/keys';
import { notificationCommand } from '@/lib/api';
import { useToastStore } from '@/stores';
import { logger } from '@/lib/utils/logger';
import { addPendingRead, addPendingDelete } from '@/lib/utils/notification-sync';

/**
 * 개별 알림 읽음 처리 (로컬스토리지 저장 + 낙관적 업데이트)
 */
export const useMarkNotificationAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: mutationKeys.notification.markAsRead,
    mutationFn: async (notificationId: number) => {
      // 로컬스토리지에 저장 (5분 후 일괄 동기화)
      addPendingRead(notificationId);
      // 서버 API 호출하지 않음
      return { success: true };
    },
    onMutate: async (notificationId) => {
      // 쿼리 취소 - 경합 상태 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.notification.list() });

      // 이전 데이터 백업 - 에러 시 롤백용
      const previousNotifications = queryClient.getQueryData(queryKeys.notification.list());

      // 낙관적 업데이트: UI에서 즉시 읽음 처리
      queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
        if (!old?.success || !old?.data) return old;

        return {
          ...old,
          data: old.data.map((notification: any) =>
            notification.id === notificationId
              ? { ...notification, read: true }
              : notification
          ),
        };
      });

      logger.log(`알림 ${notificationId} 읽음 처리 - 로컬스토리지 저장`);

      return { previousNotifications };
    },
    onError: (err, notificationId, context) => {
      // 오류 시 롤백
      queryClient.setQueryData(queryKeys.notification.list(), context?.previousNotifications);
      logger.error(`알림 ${notificationId} 읽음 처리 실패:`, err);
    },
  });
};

/**
 * 개별 알림 삭제 (로컬스토리지 저장 + 낙관적 업데이트)
 */
export const useDeleteNotification = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: ['notification', 'delete'],
    mutationFn: async (notificationId: number) => {
      // 로컬스토리지에 저장 (5분 후 일괄 동기화)
      addPendingDelete(notificationId);
      // 서버 API 호출하지 않음
      return { success: true };
    },
    onMutate: async (notificationId) => {
      // 쿼리 취소 - 경합 상태 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.notification.list() });

      // 이전 데이터 백업 - 에러 시 롤백용
      const previousNotifications = queryClient.getQueryData(queryKeys.notification.list());

      // 낙관적 업데이트: UI에서 즉시 삭제
      queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
        if (!old?.success || !old?.data) return old;

        return {
          ...old,
          data: old.data.filter((notification: any) => notification.id !== notificationId),
        };
      });

      logger.log(`알림 ${notificationId} 삭제 - 로컬스토리지 저장`);

      return { previousNotifications };
    },
    onError: (err, notificationId, context) => {
      // 오류 시 롤백
      queryClient.setQueryData(queryKeys.notification.list(), context?.previousNotifications);
      logger.error(`알림 ${notificationId} 삭제 실패:`, err);
    },
  });
};

/**
 * 모든 알림 읽음 처리 (즉시 서버 반영)
 */
export const useMarkAllNotificationsAsRead = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToastStore();

  return useMutation({
    mutationKey: mutationKeys.notification.markAllAsRead,
    mutationFn: async (unreadIds?: number[]) => {
      const ids = unreadIds ?? [];

      if (ids.length === 0) {
        logger.log('📭 읽음 처리할 알림이 없어 API 호출을 생략합니다.');
        return { success: true };
      }

      logger.log(`📤 모든 알림 읽음 처리 API 호출 - ${ids.length}개 알림`);
      const result = await notificationCommand.batchUpdate({
        readIds: ids,
        deletedIds: [],
      });
      logger.log('📥 모든 알림 읽음 처리 API 응답:', result);
      return result;
    },
    onMutate: async (unreadIds) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.notification.list() });

      const previousNotifications = queryClient.getQueryData(queryKeys.notification.list());

      queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
        if (!old?.success || !old?.data) return old;

        return {
          ...old,
          data: old.data.map((notification: any) => ({
            ...notification,
            read: true,
          })),
        };
      });

      logger.log('모든 알림 읽음 처리 - 낙관적 업데이트');

      return { previousNotifications, unreadIds };
    },
    onSuccess: (response, unreadIds) => {
      const ids = unreadIds ?? [];

      if (!response?.success) {
        showToast({
          type: 'error',
          message: '알림 읽음 처리에 실패했습니다.',
        });
        logger.error('❌ 모든 알림 읽음 처리 응답 실패:', response);
        return;
      }

      showToast({
        type: 'success',
        message: '모든 알림을 읽음 처리했습니다.',
      });
      logger.log(`모든 알림 읽음 처리 완료 (${ids.length}개)`);
    },
    onError: (err, unreadIds, context) => {
      queryClient.setQueryData(queryKeys.notification.list(), context?.previousNotifications);
      showToast({
        type: 'error',
        message: '알림 읽음 처리에 실패했습니다.',
      });
      logger.error('❌ 모든 알림 읽음 처리 실패:', err);
      logger.error('❌ 요청 변수:', unreadIds);
      logger.error('❌ 오류 상세:', (err as any)?.response || (err as any)?.message || err);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() });
    },
  });
};

/**
 * 모든 알림 삭제
 */
export const useDeleteAllNotifications = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToastStore();

  return useMutation({
    mutationKey: mutationKeys.notification.deleteAll,
    mutationFn: async (deleteIds?: number[]) => {
      const ids = deleteIds ?? [];

      if (ids.length === 0) {
        logger.log('📭 삭제할 알림이 없어 API 호출을 생략합니다.');
        return { success: true };
      }

      logger.log(`📤 모든 알림 삭제 API 호출 - ${ids.length}개 알림`);
      const result = await notificationCommand.batchUpdate({
        readIds: [],
        deletedIds: ids,
      });
      logger.log('📥 모든 알림 삭제 API 응답:', result);

      return result;
    },
    onMutate: async (deleteIds) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.notification.list() });

      const previousNotifications = queryClient.getQueryData(queryKeys.notification.list());

      queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
        if (!old?.success) return old;

        return {
          ...old,
          data: [],
        };
      });

      logger.log('모든 알림 삭제 - 낙관적 업데이트');

      return { previousNotifications, deleteIds };
    },
    onSuccess: (response, deleteIds) => {
      const ids = deleteIds ?? [];

      if (!response?.success) {
        showToast({
          type: 'error',
          message: '알림 삭제에 실패했습니다.',
        });
        logger.error('❌ 모든 알림 삭제 응답 실패:', response);
        return;
      }

      showToast({
        type: 'success',
        message: `${ids.length}개의 알림을 삭제했습니다.`,
      });
      logger.log(`모든 알림 삭제 완료 (${ids.length}개)`);
    },
    onError: (err, deleteIds, context) => {
      queryClient.setQueryData(queryKeys.notification.list(), context?.previousNotifications);
      showToast({
        type: 'error',
        message: '알림 삭제에 실패했습니다.',
      });
      logger.error('❌ 모든 알림 삭제 실패:', err);
      logger.error('❌ 요청 변수:', deleteIds);
      logger.error('❌ 오류 상세:', (err as any)?.response || (err as any)?.message || err);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() });
    },
  });
};