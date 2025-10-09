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
    mutationFn: ({ commentId, content, password }: { commentId: number; postId: number; content: string; password?: number }) =>
      commentCommand.update({ commentId, content, password }),
    onSuccess: (response, variables) => {
      if (response.success) {
        // 특정 게시글의 댓글 캐시만 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(variables.postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.popular(variables.postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });

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
    mutationFn: ({ commentId, password }: { commentId: number; postId: number; password?: number }) =>
      commentCommand.delete({ commentId, password }),
    onMutate: async ({ commentId }) => {
      // 낙관적 업데이트를 위한 현재 댓글 데이터 확인
      return { commentId };
    },
    onSuccess: (response, variables, context) => {
      if (response.success) {
        // 특정 게시글의 댓글 캐시만 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(variables.postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.popular(variables.postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });

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
 * 특정 게시글의 댓글 좋아요 (낙관적 업데이트)
 */
export const useLikeCommentOptimized = (postId: number) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationKey: [...mutationKeys.comment.like, postId],
    mutationFn: commentCommand.like,
    onMutate: async (commentId: number) => {
      // 진행 중인 refetch 취소 - 경합 상태(race condition) 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.comment.list(postId) });
      await queryClient.cancelQueries({ queryKey: queryKeys.comment.popular(postId) });

      // 이전 값 스냅샷 - 에러 시 롤백용
      const previousComments = queryClient.getQueryData(queryKeys.comment.list(postId));
      const previousPopularComments = queryClient.getQueryData(queryKeys.comment.popular(postId));

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

      // 인기 댓글도 낙관적 업데이트 - 일반 댓글과 동일한 반응성 제공
      queryClient.setQueryData(queryKeys.comment.popular(postId), (old: ApiResponse<Comment[]>) => {
        if (!old?.success || !old?.data) return old;

        const updatedPopularComments = old.data.map(comment => {
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
          data: updatedPopularComments,
        };
      });

      return { previousComments, previousPopularComments };
    },
    onError: (err, commentId, context) => {
      // 에러 시 이전 값으로 롤백 - 낙관적 업데이트 취소
      if (context?.previousComments) {
        queryClient.setQueryData(queryKeys.comment.list(postId), context.previousComments);
      }
      if (context?.previousPopularComments) {
        queryClient.setQueryData(queryKeys.comment.popular(postId), context.previousPopularComments);
      }
    },
    onSettled: (data, error, commentId) => {
      // 성공/실패 여부와 관계없이 서버 데이터로 동기화 - 최종 일관성 보장
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.popular(postId) });
    },
  });
};