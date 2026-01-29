import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { commentCommand } from '@/lib/api';
import { useToast } from '@/hooks';
import { ErrorHandler } from '@/lib/api/helpers';
import type { Comment } from '@/types';
import type { CommentDTO } from '@/types/domains/comment';
import type { ApiResponse } from '@/types/common';
import type { InfiniteData } from '@tanstack/react-query';

/**
 * Comment 관련 Mutation hooks
 * 댓글 시스템에서 사용되는 데이터 변경 훅들
 */

const friendlyMessage = (msg?: string, context?: 'comment' | 'paper') => {
  if (!msg) return undefined;
  const trimmed = msg.trim();

  // JSON 형태 문자열이면 파싱 후 키워드 추출
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const parsed = JSON.parse(trimmed) as { message?: string; target?: string };
      const nestedMsg = parsed.message || parsed.target || '';
      const candidate = nestedMsg || trimmed;
      const lowerNested = candidate.toLowerCase();
      if (lowerNested.includes('차단')) {
        return context === 'comment'
          ? '차단된 사용자와는 상호작용할 수 없습니다.'
          : '차단된 상대의 롤링페이퍼는 볼 수 없습니다.';
      }
    } catch {
      // fall through
    }
    return context === 'comment'
      ? '댓글 작성에 실패했습니다. 잠시 후 다시 시도해주세요.'
      : '요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.';
  }

  const lower = msg.toLowerCase();
  if (lower.includes('차단')) {
    if (context === 'comment') {
      return '차단된 사용자와는 상호작용할 수 없습니다.';
    }
    if (lower.includes('롤링페이퍼')) {
      return '차단된 상대의 롤링페이퍼는 볼 수 없습니다.';
    }
    return '차단된 사용자와는 상호작용할 수 없습니다.';
  }
  return msg;
};

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
      if (!response.success) {
        const errorMessage = friendlyMessage(response.error, 'comment') || '댓글 작성에 실패했습니다.';
        showToast({ type: 'error', message: errorMessage });
        return;
      }

      // 댓글 목록 캐시 무효화
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(variables.postId) });


      // 게시글 상세 정보도 갱신 (댓글 수 반영)
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });

      showToast({ type: 'success', message: '댓글이 작성되었습니다.' });
    },
    onError: (error) => {
      const appError = ErrorHandler.mapApiError(error);
      const errorMessage = friendlyMessage(appError.userMessage || appError.message, 'comment') || '댓글 작성에 실패했습니다.';
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
  
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });

        showToast({ type: 'success', message: '댓글이 수정되었습니다.' });
      } else {
        // response.success가 false인 경우 백엔드 에러 메시지 표시
        showToast({ type: 'error', message: response.error || '댓글 수정에 실패했습니다.' });
      }
    },
    onError: (error: any) => {
      const appError = ErrorHandler.mapApiError(error);
      const message = friendlyMessage(appError.userMessage || appError.message, 'comment') || '댓글 수정에 실패했습니다.';
      showToast({ type: 'error', message });
    },
  });
};

/**
 * 댓글 삭제 (낙관적 업데이트)
 */
export const useDeleteComment = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.comment.delete,
    mutationFn: ({ commentId, password }: { commentId: number; postId: number; password?: number }) =>
      commentCommand.delete({ commentId, password }),
    onMutate: async ({ commentId, postId }) => {
      // 진행 중인 refetch 취소 - 경합 상태(race condition) 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.comment.list(postId) });

      // 이전 값 스냅샷 - 에러 시 롤백용
      const previousComments = queryClient.getQueryData(queryKeys.comment.list(postId));

      // 낙관적 업데이트: 서버 응답 전에 UI에서 댓글 제거 또는 소프트 삭제 표시
      const removeComment = (comments: Comment[]): Comment[] => {
        return comments.map(comment => {
          if (comment.id === commentId) {
            // 답글이 있는 경우 소프트 삭제 표시, 없으면 제거
            if (comment.replies && comment.replies.length > 0) {
              return { ...comment, deleted: true, content: '삭제된 댓글입니다' };
            }
            return null; // 제거 대상
          }
          // 재귀적으로 답글도 처리
          if (comment.replies && comment.replies.length > 0) {
            return { ...comment, replies: removeComment(comment.replies).filter(Boolean) as Comment[] };
          }
          return comment;
        }).filter(Boolean) as Comment[];
      };

      queryClient.setQueryData(
        queryKeys.comment.list(postId),
        (old: InfiniteData<ApiResponse<CommentDTO>> | undefined) => {
          if (!old) return old;

          return {
            ...old,
            pages: old.pages.map((page) => {
              if (!page.success || !page.data) return page;

              return {
                ...page,
                data: {
                  ...page.data,
                  popularCommentList: page.data.popularCommentList.filter(
                    (c) => c.id !== commentId
                  ),
                  commentInfoPage: {
                    ...page.data.commentInfoPage,
                    content: removeComment(page.data.commentInfoPage.content),
                  },
                },
              };
            }),
          };
        }
      );

      return { previousComments };
    },
    onSuccess: (response, variables, context) => {
      if (response.success) {
        // 특정 게시글의 댓글 캐시만 무효화 (서버 데이터와 동기화)
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(variables.postId) });
  
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });

        showToast({ type: 'success', message: '댓글이 삭제되었습니다.' });
      }
    },
    onError: (error: any, variables, context) => {
      // 에러 시 이전 값으로 롤백 - 낙관적 업데이트 취소
      if (context?.previousComments) {
        queryClient.setQueryData(queryKeys.comment.list(variables.postId), context.previousComments);
      }
      const appError = ErrorHandler.mapApiError(error);
      const message = friendlyMessage(appError.userMessage || appError.message, 'comment') || '댓글 삭제에 실패했습니다.';
      showToast({ type: 'error', message });
    },
  });
};

/**
 * 특정 게시글의 댓글 좋아요 (낙관적 업데이트)
 */
export const useLikeCommentOptimized = (postId: number) => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: [...mutationKeys.comment.like, postId],
    mutationFn: commentCommand.like,
    onSuccess: (data, commentId) => {
      // API가 실패 플래그를 반환하는 경우 (200 OK라도)
      if (!data?.success) {
        const message = friendlyMessage(data?.error, 'comment') || '추천 처리 중 오류가 발생했습니다.';
        showToast({ type: 'error', message });
        return;
      }

      // 서버 응답 후 캐시 무효화하여 최신 데이터 가져오기
      queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(postId) });

      showToast({ type: 'success', message: '추천 처리가 완료되었습니다.' });
    },
    onError: (err: any) => {
      // 에러 메시지 표시 (error-handler에서 변환된 메시지 사용)
      const appError = ErrorHandler.mapApiError(err);
      const errorMessage = friendlyMessage(appError.userMessage || appError.message, 'comment') || '추천 처리 중 오류가 발생했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};
