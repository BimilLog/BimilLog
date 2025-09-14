import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { notificationQuery } from '@/lib/api';

/**
 * 알림 목록 조회
 */
export const useNotificationList = () => {
  return useQuery({
    queryKey: queryKeys.notification.list(),
    queryFn: notificationQuery.getAll,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
    refetchInterval: 5 * 60 * 1000, // 5분마다 자동 새로고침
    refetchIntervalInBackground: false, // 백그라운드에서는 새로고침 안함
  });
};