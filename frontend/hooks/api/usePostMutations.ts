import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { postCommand } from '@/lib/api';
import { useToast } from '@/hooks';
import { useRouter } from 'next/navigation';
import type { Post } from '@/types';

/**
 * 게시글 작성
 */
export const useCreatePost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.create,
    mutationFn: postCommand.create,
    onSuccess: (response) => {
      if (response.success && response.data) {
        // 게시글 목록 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
        showToast({ type: 'success', message: '게시글이 작성되었습니다.' });
        router.push(`/board/post/${response.data.id}`);
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '게시글 작성에 실패했습니다.' });
    },
  });
};

/**
 * 게시글 수정
 */
export const useUpdatePost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const router = useRouter();

  return useMutation({
    mutationKey: mutationKeys.post.update,
    mutationFn: ({ postId, ...data }: { postId: number } & Parameters<typeof postCommand.update>[0]) =>
      postCommand.update(data),
    onSuccess: (response, variables) => {
      if (response.success) {
        // 특정 게시글 캐시 무효화
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
export const useDeletePost = () => {
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
        showToast({ type: 'success', message: '게시글이 삭제되었습니다.' });
        router.push('/board');
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '게시글 삭제에 실패했습니다.' });
    },
  });
};

/**
 * 게시글 좋아요 토글 (낙관적 업데이트)
 */
export const useLikePost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.post.like,
    mutationFn: postCommand.like,
    onMutate: async (postId: number) => {
      // 진행 중인 refetch 취소
      await queryClient.cancelQueries({ queryKey: queryKeys.post.detail(postId) });

      // 이전 값 스냅샷
      const previousPost = queryClient.getQueryData(queryKeys.post.detail(postId));

      // 낙관적 업데이트
      queryClient.setQueryData(queryKeys.post.detail(postId), (old: any) => {
        if (!old?.success || !old?.data) return old;

        const post = old.data as Post;
        return {
          ...old,
          data: {
            ...post,
            isLiked: !post.isLiked,
            likeCount: post.isLiked ? post.likeCount - 1 : post.likeCount + 1,
          },
        };
      });

      return { previousPost };
    },
    onError: (err, postId, context) => {
      // 에러 시 이전 값으로 롤백
      if (context?.previousPost) {
        queryClient.setQueryData(queryKeys.post.detail(postId), context.previousPost);
      }
      showToast({ type: 'error', message: '좋아요 처리에 실패했습니다.' });
    },
    onSettled: (data, error, postId) => {
      // 성공/실패 여부와 관계없이 서버 데이터로 동기화
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) });
    },
  });
};

/**
 * 게시글 공지 설정/해제
 */
export const useToggleNotice = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.post.notice,
    mutationFn: postCommand.toggleNotice,
    onSuccess: (response, postId) => {
      if (response.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.post.notice() });
        showToast({
          type: 'success',
          message: response.data ? '공지사항으로 설정되었습니다.' : '공지사항이 해제되었습니다.'
        });
      }
    },
    onError: (error) => {
      showToast({ type: 'error', message: '공지 설정에 실패했습니다.' });
    },
  });
};