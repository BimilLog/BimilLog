import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/lib/tanstack-query/keys';
import { notificationQuery } from '@/lib/api';

/**
 * 알림 목록 조회
 * 알림 패널이 열렸을 때만 수동으로 호출됨
 */
export const useNotificationList = () => {
  return useQuery({
    queryKey: queryKeys.notification.list(),
    queryFn: notificationQuery.getAll,
    staleTime: 5 * 60 * 1000, // 5분
    gcTime: 10 * 60 * 1000, // 10분
    enabled: false, // 자동 호출 비활성화 - 패널 열 때만 수동 refetch
  });
};