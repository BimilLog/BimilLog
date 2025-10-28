'use client';

import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ReactNode, useState } from 'react';
import { queryClient as defaultQueryClient } from '@/lib/tanstack-query/client';

interface QueryProviderProps {
  children: ReactNode;
}

export function QueryProvider({ children }: QueryProviderProps) {
  // useState를 사용해 클라이언트별 QueryClient 인스턴스 생성
  // SSR 환경에서 요청 간 데이터 공유 방지
  const [queryClient] = useState(() => defaultQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {process.env.NODE_ENV === 'development' && (
        <ReactQueryDevtools
          initialIsOpen={false}
          position="bottom"
          buttonPosition="bottom-left"
        />
      )}
    </QueryClientProvider>
  );
}