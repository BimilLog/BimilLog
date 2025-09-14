import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { postCommand } from '@/lib/api';
import { useToast, useAuth } from '@/hooks';
import { useRouter } from 'next/navigation';
import { stripHtml, validatePassword } from '@/lib/utils';
import type { ApiResponse, Post } from '@/types';

/**
 * Board 관련 Mutation hooks
 * 게시판에서 사용되는 데이터 변경 훅들
 */

/**
 * 게시글 작성 (Write Form용)
 */
export const useCreateBoardPost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { user, isAuthenticated } = useAuth();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.create,
    mutationFn: async (data: {
      title: string;
      content: string;
      password?: string;
    }) => {
      const plainContent = stripHtml(data.content).trim();

      if (!data.title.trim() || !plainContent) {
        throw new Error('제목과 내용을 모두 입력해주세요.');
      }

      let validatedPassword: number | undefined;
      try {
        validatedPassword = validatePassword(data.password || '', isAuthenticated);
      } catch (error) {
        throw error;
      }

      const postData = {
        userName: isAuthenticated ? user!.userName : null,
        title: data.title.trim(),
        content: plainContent,
        password: validatedPassword,
      };

      return postCommand.create(postData);
    },
    onSuccess: (response) => {
      if (response.success && response.data) {
        // 게시글 목록 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.popular() });

        showToast({ type: 'success', message: '게시글이 성공적으로 작성되었습니다!' });
        router.push(`/board/post/${response.data.id}`);
      }
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '게시글 작성 중 오류가 발생했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};

/**
 * 게시글 수정
 */
export const useUpdateBoardPost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.update,
    mutationFn: ({ postId, ...data }: { postId: number } & Parameters<typeof postCommand.update>[0]) =>
      postCommand.update(data),
    onSuccess: (response, variables) => {
      if (response.success) {
        // 특정 게시글 및 목록 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(variables.postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });

        showToast({ type: 'success', message: '게시글이 수정되었습니다.' });
        router.push(`/board/post/${variables.postId}`);
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '게시글 수정에 실패했습니다.' });
    },
  });
};

/**
 * 게시글 삭제
 */
export const useDeleteBoardPost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.delete,
    mutationFn: postCommand.delete,
    onSuccess: (response, postId) => {
      if (response.success) {
        // 캐시에서 게시글 제거
        queryClient.removeQueries({ queryKey: queryKeys.post.detail(postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.popular() });

        showToast({ type: 'success', message: '게시글이 삭제되었습니다.' });
        router.push('/board');
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '게시글 삭제에 실패했습니다.' });
    },
  });
};