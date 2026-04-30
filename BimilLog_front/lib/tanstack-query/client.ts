import { QueryCache, QueryClient } from '@tanstack/react-query';
import { errorLogger } from '@/lib/error-logger';

/**
 * 글로벌 QueryCache onError — 라운드 13 F-13-BUG-10:
 * 모든 query 실패를 백엔드로 로깅 (페이지 단 토스트는 그대로 페이지 책임).
 * 401/403 은 useErrorHandler 가 별도 처리 (needsRelogin / 권한 토스트) 하므로
 * 여기서는 로깅에서 제외 (중복 로그 방지).
 */
const queryCache = new QueryCache({
  onError: (error, query) => {
    if (error && typeof error === 'object' && 'status' in error) {
      const status = (error as { status: number }).status;
      if (status === 401 || status === 403) {
        return;
      }
    }

    errorLogger.logError(error instanceof Error ? error : String(error), {
      type: 'TanStackQuery',
      queryKey: JSON.stringify(query.queryKey),
    });
  },
});

export const queryClient = new QueryClient({
  queryCache,
  defaultOptions: {
    queries: {
      // 기본 stale time을 5분으로 설정
      staleTime: 5 * 60 * 1000,
      // 기본 캐시 시간을 10분으로 설정
      gcTime: 10 * 60 * 1000,
      // 백그라운드에서 refetch
      refetchOnWindowFocus: false,
      // stale 상태일 때만 마운트 시 refetch (staleTime 5분과 연동)
      refetchOnMount: true,
      // 재시도 로직
      retry: (failureCount, error: unknown) => {
        // 401, 403 에러는 재시도하지 않음
        if (error && typeof error === 'object' && 'status' in error) {
          const status = (error as { status: number }).status;
          if (status === 401 || status === 403) {
            return false;
          }
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
