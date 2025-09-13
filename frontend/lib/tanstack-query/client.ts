import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // 기본 stale time을 5분으로 설정
      staleTime: 5 * 60 * 1000,
      // 기본 캐시 시간을 10분으로 설정
      gcTime: 10 * 60 * 1000,
      // 백그라운드에서 refetch
      refetchOnWindowFocus: false,
      // 마운트 시 refetch 비활성화 (필요한 경우만 활성화)
      refetchOnMount: true,
      // 재시도 로직
      retry: (failureCount, error: any) => {
        // 401, 403 에러는 재시도하지 않음
        if (error?.status === 401 || error?.status === 403) {
          return false;
        }
        // 최대 2번까지 재시도
        return failureCount < 2;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
    mutations: {
      // mutation 에러 시 재시도 하지 않음
      retry: false,
    },
  },
});

// SSR을 위한 기본 설정
export const defaultQueryOptions = {
  staleTime: 5 * 60 * 1000,
  gcTime: 10 * 60 * 1000,
};