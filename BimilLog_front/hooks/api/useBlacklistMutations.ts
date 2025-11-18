"use client";

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { blacklistCommand } from '@/lib/api/user/blacklist/command';
import { useToast } from '@/hooks/common/useToast';

/**
 * 블랙리스트 변경 hooks (추가, 삭제)
 */

/**
 * 블랙리스트에 사용자 추가
 */
export const useAddToBlacklist = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: ['user', 'blacklist', 'add'],
    mutationFn: (memberName: string) => blacklistCommand.addToBlacklist(memberName),
    onSuccess: (response) => {
      if (response.success) {
        // 블랙리스트 캐시 무효화
        queryClient.invalidateQueries({ queryKey: ['user', 'blacklist'] });
        showToast({ type: 'success', message: '블랙리스트에 추가되었습니다.' });
      }
    },
    onError: (error: Error) => {
      const errorMessage = error.message || '사용자 추가에 실패했습니다.';

      // 백엔드 에러 메시지에 따른 처리
      if (errorMessage.includes('사용자를 찾을 수 없습니다')) {
        showToast({ type: 'error', message: '사용자를 찾을 수 없습니다.' });
      } else {
        showToast({ type: 'error', message: errorMessage });
      }
    },
  });
};

/**
 * 블랙리스트에서 사용자 삭제
 */
export const useRemoveFromBlacklist = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: ['user', 'blacklist', 'remove'],
    mutationFn: ({ id, page, size }: { id: number; page: number; size: number }) =>
      blacklistCommand.removeFromBlacklist(id, page, size),
    onSuccess: (response, variables) => {
      if (response.success) {
        // 삭제 후 서버가 반환한 업데이트된 목록으로 캐시 업데이트
        queryClient.setQueryData(['user', 'blacklist', variables.page, variables.size], response);
        // 전체 블랙리스트 캐시 무효화 (다른 페이지도 업데이트)
        queryClient.invalidateQueries({ queryKey: ['user', 'blacklist'] });
        showToast({ type: 'success', message: '블랙리스트에서 삭제되었습니다.' });
      }
    },
    onError: (error: Error) => {
      const errorMessage = error.message || '삭제에 실패했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};
