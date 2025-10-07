import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { postCommand } from '@/lib/api';
import { useToast, useAuth } from '@/hooks';
import { useGlobalToast } from '@/stores/toastStore';
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
  const globalToast = useGlobalToast();
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
        userName: isAuthenticated ? user!.memberName : null,
        title: data.title.trim(),
        content: plainContent,
        password: validatedPassword,
      };

      const response = await postCommand.create(postData);

      // API 에러 처리
      if (!response.success) {
        throw new Error(response.error || '게시글 작성에 실패했습니다.');
      }

      return response;
    },
    onSuccess: (response) => {
      // 201 Created 또는 성공 응답 처리
      if (response.data && response.data.id) {
        // 게시글 목록 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() });

        globalToast.success('게시글이 성공적으로 작성되었습니다!');
        router.push(`/board/post/${response.data.id}`);
      }
    },
    onError: (error) => {
      // 백엔드 에러 메시지를 정확히 표시
      const errorMessage = error instanceof Error ? error.message : '게시글 작성 중 오류가 발생했습니다.';
      globalToast.error(errorMessage, '게시글 작성 실패');
    },
  });
};

/**
 * 게시글 수정
 */
export const useUpdateBoardPost = () => {
  const queryClient = useQueryClient();
  const globalToast = useGlobalToast();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.update,
    mutationFn: async ({ postId, ...data }: { postId: number } & Parameters<typeof postCommand.update>[0]) => {
      const response = await postCommand.update(data);

      // API 에러 처리
      if (!response.success) {
        throw new Error(response.error || '게시글 수정에 실패했습니다.');
      }

      return { ...response, postId };
    },
    onSuccess: (response) => {
      // 특정 게시글 및 목록 캐시 무효화
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(response.postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });

      globalToast.success('게시글이 수정되었습니다.');
      router.push(`/board/post/${response.postId}`);
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '게시글 수정에 실패했습니다.';
      globalToast.error(errorMessage, '게시글 수정 실패');
    },
  });
};

/**
 * 게시글 삭제
 */
export const useDeleteBoardPost = () => {
  const queryClient = useQueryClient();
  const globalToast = useGlobalToast();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.delete,
    mutationFn: async (postId: number) => {
      const response = await postCommand.delete(postId);

      // API 에러 처리
      if (!response.success) {
        throw new Error(response.error || '게시글 삭제에 실패했습니다.');
      }

      return { ...response, postId };
    },
    onSuccess: (response) => {
      // 캐시에서 게시글 제거
      queryClient.removeQueries({ queryKey: queryKeys.post.detail(response.postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() });

      globalToast.success('게시글이 삭제되었습니다.');
      router.push('/board');
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : '게시글 삭제에 실패했습니다.';
      globalToast.error(errorMessage, '게시글 삭제 실패');
    },
  });
};