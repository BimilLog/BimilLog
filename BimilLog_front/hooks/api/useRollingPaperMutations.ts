import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys, mutationKeys } from '@/lib/tanstack-query/keys';
import { paperCommand } from '@/lib/api';
import type { DecoType } from '@/types/domains/paper';

/**
 * 롤링페이퍼 메시지 작성
 *
 * Note: 토스트 메시지는 컴포넌트에서 직접 처리합니다.
 * 이는 더 세밀한 에러 메시지 제어와 중복 방지를 위함입니다.
 */
export const useCreateRollingPaperMessage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: mutationKeys.paper.write,
    mutationFn: ({ ownerName, message }: {
      ownerName: string;
      message: {
        ownerId: number;
        decoType: DecoType;
        anonymity: string;
        content: string;
        x: number;
        y: number;
      }
    }) => paperCommand.createMessage(message),
    onSuccess: (data, variables) => {
      // 해당 사용자의 롤링페이퍼 캐시 무효화 - 새 메시지 반영
      queryClient.invalidateQueries({
        queryKey: queryKeys.paper.detail(variables.ownerName)
      });

      // 내 롤링페이퍼 캐시도 무효화 (자신에게 메시지 작성 시 마이페이지 통계 실시간 업데이트)
      queryClient.invalidateQueries({
        queryKey: queryKeys.paper.my
      });
    }
  });
};

/**
 * 롤링페이퍼 메시지 삭제
 *
 * Note: 토스트 메시지는 컴포넌트에서 직접 처리합니다.
 * 이는 더 세밀한 에러 메시지 제어와 중복 방지를 위함입니다.
 */
export const useDeleteRollingPaperMessage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: mutationKeys.paper.delete,
    mutationFn: (messageId: number) => paperCommand.deleteMessage(messageId),
    onSuccess: () => {
      // 모든 롤링페이퍼 관련 캐시 무효화 - 메시지가 어느 사용자 것인지 모르므로 전체 갱신
      queryClient.invalidateQueries({
        queryKey: queryKeys.paper.all
      });
    }
  });
};