import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { commentCommand } from '@/lib/api';
import { useToast } from '@/hooks';
import type { Comment, ApiResponse } from '@/types';

/**
 * Comment 관련 Mutation hooks
 * 댓글 시스템에서 사용되는 데이터 변경 훅들
 */

/**
 * 댓글 작성
 */
export const useCreateComment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.comment.write,
    mutationFn: commentCommand.create,
    onSuccess: (response, variables) => {
      if (response.success) {
        // 댓글 목록 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(variables.postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.popular(variables.postId) });

        // 게시글 상세 정보도 갱신 (댓글 수 반영)
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });

        showToast({ type: 'success', message: '댓글이 작성되었습니다.' });
      }
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '댓글 작성에 실패했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};

/**
 * 댓글 수정
 */
export const useUpdateComment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.comment.update,
    mutationFn: commentCommand.update,
    onSuccess: (response, variables) => {
      if (response.success) {
        // 모든 댓글 관련 캐시 무효화 (postId를 정확히 알 수 없으므로)
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.all });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.details() });

        showToast({ type: 'success', message: '댓글이 수정되었습니다.' });
      }
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '댓글 수정에 실패했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};

/**
 * 댓글 삭제
 */
export const useDeleteComment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.comment.delete,
    mutationFn: commentCommand.delete,
    onMutate: async ({ commentId }) => {
      // 낙관적 업데이트를 위한 현재 댓글 데이터 확인
      // 실제 구현에서는 postId를 알아야 하므로 사용 시점에서 전달받아야 함
      return { commentId };
    },
    onSuccess: (response, variables, context) => {
      if (response.success) {
        // 모든 댓글 관련 캐시 무효화 (postId를 정확히 알 수 없으므로)
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.all });

        // 게시글 상세 정보도 갱신 (댓글 수 반영)
        queryClient.invalidateQueries({ queryKey: queryKeys.post.details() });

        showToast({ type: 'success', message: '댓글이 삭제되었습니다.' });
      }
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '댓글 삭제에 실패했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};

/**
 * 댓글 좋아요 토글 (낙관적 업데이트)
 */
export const useLikeComment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.comment.like,
    mutationFn: commentCommand.like,
    onMutate: async (commentId: number) => {
      // 현재 댓글 목록에서 해당 댓글을 찾아 낙관적 업데이트 수행
      // 하지만 댓글의 postId를 정확히 알기 어려우므로 간단한 처리만 수행
      return { commentId };
    },
    onSuccess: (response, commentId) => {
      if (response.success) {
        // 모든 댓글 관련 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.all });
      }
    },
    onError: (error, commentId) => {
      // 좋아요는 일반적으로 조용히 실패 (토스트 없음)
      // Silent fail for like operations
    },
    onSettled: (data, error, commentId) => {
      // 성공/실패 여부와 관계없이 해당 댓글의 캐시 갱신
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.all });
    },
  });
};

/**
 * 특정 게시글의 댓글 좋아요 (postId를 알고 있는 경우)
 */
export const useLikeCommentOptimized = (postId: number) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: [...mutationKeys.comment.like, postId],
    mutationFn: commentCommand.like,
    onMutate: async (commentId: number) => {
      // 진행 중인 refetch 취소 - 경합 상태(race condition) 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.comment.list(postId) });

      // 이전 값 스냅샷 - 에러 시 롤백용
      const previousComments = queryClient.getQueryData(queryKeys.comment.list(postId));

      // 낙관적 업데이트: 서버 응답 전에 UI를 먼저 업데이트하여 반응성 향상
      queryClient.setQueryData(queryKeys.comment.list(postId), (old: ApiResponse<Comment[]>) => {
        if (!old?.success || !old?.data) return old;

        const updatedComments = old.data.map(comment => {
          if (comment.id === commentId) {
            return {
              ...comment,
              userLike: !comment.userLike,
              likeCount: comment.userLike ? comment.likeCount - 1 : comment.likeCount + 1,
            };
          }
          return comment;
        });

        return {
          ...old,
          data: updatedComments,
        };
      });

      return { previousComments };
    },
    onError: (err, commentId, context) => {
      // 에러 시 이전 값으로 롤백 - 낙관적 업데이트 취소
      if (context?.previousComments) {
        queryClient.setQueryData(queryKeys.comment.list(postId), context.previousComments);
      }
    },
    onSettled: (data, error, commentId) => {
      // 성공/실패 여부와 관계없이 서버 데이터로 동기화 - 최종 일관성 보장
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.popular(postId) });
    },
  });
};