import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { notificationQuery } from '@/lib/api';

type UseNotificationListOptions = {
  enabled?: boolean;
};

/**
 * 알림 목록 조회
 * enabled 플래그를 통해 쿼리 실행 시점을 제어한다.
 */
export const useNotificationList = (options?: UseNotificationListOptions) => {
  const enabled = options?.enabled ?? false;

  return useQuery({
    queryKey: queryKeys.notification.list(),
    queryFn: notificationQuery.getAll,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
    enabled,
  });
};
