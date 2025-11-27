/**
 * 친구 관련 Mutation Hooks
 * TanStack Query를 사용한 데이터 변경 훅
 */

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { friendCommand } from '@/lib/api/friend/command';
import { queryKeys, mutationKeys } from '@/lib/tanstack-query/keys';
import { useToastStore } from '@/stores/toast.store';
import { SendFriendRequestDTO } from '@/types/domains/friend';

/**
 * 친구 요청 보내기
 */
export const useSendFriendRequest = () => {
  const queryClient = useQueryClient();
  const showToast = useToastStore((state) => state.showToast);

  return useMutation({
    mutationKey: mutationKeys.friend.sendRequest,
    mutationFn: (data: SendFriendRequestDTO) => friendCommand.sendRequest(data),
    onSuccess: () => {
      // 모든 friend 관련 쿼리 무효화 (페이지 사이즈 무관)
      queryClient.invalidateQueries({ queryKey: queryKeys.friend.all });

      showToast({
        type: 'success',
        message: '친구 요청을 보냈습니다',
      });
    },
    onError: (error: any) => {
      const message = error?.message || '친구 요청에 실패했습니다';
      showToast({
        type: 'error',
        message,
      });
    },
  });
};

/**
 * 친구 요청 수락
 */
export const useAcceptFriendRequest = () => {
  const queryClient = useQueryClient();
  const showToast = useToastStore((state) => state.showToast);

  return useMutation({
    mutationKey: mutationKeys.friend.acceptRequest,
    mutationFn: (requestId: number) => friendCommand.acceptRequest(requestId),
    onSuccess: () => {
      // 모든 friend 관련 쿼리 무효화 (페이지 사이즈 무관)
      queryClient.invalidateQueries({ queryKey: queryKeys.friend.all });

      showToast({
        type: 'success',
        message: '친구 요청을 수락했습니다',
      });
    },
    onError: (error: any) => {
      const message = error?.message || '친구 요청 수락에 실패했습니다';
      showToast({
        type: 'error',
        message,
      });
    },
  });
};

/**
 * 친구 요청 거절
 */
export const useRejectFriendRequest = () => {
  const queryClient = useQueryClient();
  const showToast = useToastStore((state) => state.showToast);

  return useMutation({
    mutationKey: mutationKeys.friend.rejectRequest,
    mutationFn: (requestId: number) => friendCommand.rejectRequest(requestId),
    onSuccess: () => {
      // 모든 friend 관련 쿼리 무효화 (페이지 사이즈 무관)
      queryClient.invalidateQueries({ queryKey: queryKeys.friend.all });

      showToast({
        type: 'success',
        message: '친구 요청을 거절했습니다',
      });
    },
    onError: (error: any) => {
      const message = error?.message || '친구 요청 거절에 실패했습니다';
      showToast({
        type: 'error',
        message,
      });
    },
  });
};

/**
 * 친구 요청 취소 (보낸 요청)
 */
export const useCancelFriendRequest = () => {
  const queryClient = useQueryClient();
  const showToast = useToastStore((state) => state.showToast);

  return useMutation({
    mutationKey: mutationKeys.friend.cancelRequest,
    mutationFn: (requestId: number) => friendCommand.cancelRequest(requestId),
    onSuccess: () => {
      // 모든 friend 관련 쿼리 무효화 (페이지 사이즈 무관)
      queryClient.invalidateQueries({ queryKey: queryKeys.friend.all });

      showToast({
        type: 'success',
        message: '친구 요청을 취소했습니다',
      });
    },
    onError: (error: any) => {
      const message = error?.message || '친구 요청 취소에 실패했습니다';
      showToast({
        type: 'error',
        message,
      });
    },
  });
};

/**
 * 친구 삭제 (친구 관계 끊기)
 */
export const useRemoveFriend = () => {
  const queryClient = useQueryClient();
  const showToast = useToastStore((state) => state.showToast);

  return useMutation({
    mutationKey: mutationKeys.friend.removeFriend,
    mutationFn: (friendshipId: number) => friendCommand.removeFriend(friendshipId),
    onSuccess: () => {
      // 모든 friend 관련 쿼리 무효화 (페이지 사이즈 무관)
      queryClient.invalidateQueries({ queryKey: queryKeys.friend.all });

      showToast({
        type: 'success',
        message: '친구를 삭제했습니다',
      });
    },
    onError: (error: any) => {
      const message = error?.message || '친구 삭제에 실패했습니다';
      showToast({
        type: 'error',
        message,
      });
    },
  });
};
