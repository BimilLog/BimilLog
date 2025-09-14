import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys, mutationKeys } from '@/lib/tanstack-query/keys';
import { paperCommand } from '@/lib/api';
import { useToastStore } from '@/stores';
import type { DecoType } from '@/types/domains/paper';

/**
 * 롤링페이퍼 메시지 작성
 */
export const useCreateRollingPaperMessage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToastStore();

  return useMutation({
    mutationKey: mutationKeys.paper.write,
    mutationFn: ({ userName, message }: {
      userName: string;
      message: {
        decoType: DecoType;
        anonymity: string;
        content: string;
        x: number;
        y: number;
      }
    }) => paperCommand.createMessage(userName, message),
    onSuccess: (data, variables) => {
      // 해당 사용자의 롤링페이퍼 캐시 무효화 - 새 메시지 반영
      queryClient.invalidateQueries({
        queryKey: queryKeys.paper.detail(variables.userName)
      });

      showToast({
        type: 'success',
        message: '메시지가 작성되었습니다.'
      });
    },
    onError: (error) => {
      showToast({
        type: 'error',
        message: '메시지 작성에 실패했습니다.'
      });
    }
  });
};

/**
 * 롤링페이퍼 메시지 삭제
 */
export const useDeleteRollingPaperMessage = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToastStore();

  return useMutation({
    mutationKey: mutationKeys.paper.delete,
    mutationFn: (messageId: number) => paperCommand.deleteMessage(messageId),
    onSuccess: () => {
      // 모든 롤링페이퍼 관련 캐시 무효화 - 메시지가 어느 사용자 것인지 모르므로 전체 갱신
      queryClient.invalidateQueries({
        queryKey: queryKeys.paper.all
      });

      showToast({
        type: 'success',
        message: '메시지가 삭제되었습니다.'
      });
    },
    onError: (error) => {
      showToast({
        type: 'error',
        message: '메시지 삭제에 실패했습니다.'
      });
    }
  });
};