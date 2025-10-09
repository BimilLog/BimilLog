"use client";

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { postCommand } from '@/lib/api';
import { useToast } from '@/hooks';
import { useRouter } from 'next/navigation';
import type { Post, ApiResponse } from '@/types';

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
    onError: (error: any) => {
      // 백엔드 에러 메시지를 파싱하여 사용자에게 표시
      const message = error?.error || error?.message || '게시글 작성에 실패했습니다.';
      showToast({ type: 'error', message });
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
    onError: (error: any) => {
      const message = error?.error || error?.message || '게시글 수정에 실패했습니다.';
      showToast({ type: 'error', message });
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
    mutationFn: ({ postId, password }: { postId: number; password?: number }) =>
      postCommand.delete(postId, password),
    onSuccess: (response, { postId }) => {
      if (response.success) {
        // 캐시에서 게시글 제거 - 삭제된 게시글은 더 이상 필요 없음
        queryClient.removeQueries({ queryKey: queryKeys.post.detail(postId) });
        // 게시글 목록 캐시 무효화 - 목록에서도 제거 반영
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
        showToast({ type: 'success', message: '게시글이 삭제되었습니다.' });
        router.push('/board');
      }
    },
    onError: (error: any) => {
      const message = error?.error || error?.message || '게시글 삭제에 실패했습니다.';
      showToast({ type: 'error', message });
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
      // 진행 중인 refetch 취소 - 경합 상태(race condition) 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.post.detail(postId) });

      // 이전 값 스냅샷 - 에러 시 롤백용
      const previousPost = queryClient.getQueryData(queryKeys.post.detail(postId));

      // 낙관적 업데이트: 서버 응답 전에 UI를 먼저 업데이트하여 반응성 향상
      queryClient.setQueryData(queryKeys.post.detail(postId), (old: ApiResponse<Post>) => {
        if (!old?.success || !old?.data) return old;

        const post = old.data;
        return {
          ...old,
          data: {
            ...post,
            liked: !post.liked,
            likeCount: post.liked ? post.likeCount - 1 : post.likeCount + 1,
          },
        };
      });

      return { previousPost };
    },
    onError: (err, postId, context) => {
      // 에러 시 이전 값으로 롤백 - 낙관적 업데이트 취소
      if (context?.previousPost) {
        queryClient.setQueryData(queryKeys.post.detail(postId), context.previousPost);
      }
      showToast({ type: 'error', message: '좋아요 처리에 실패했습니다.' });
    },
    onSettled: (data, error, postId) => {
      // 성공/실패 여부와 관계없이 서버 데이터로 동기화 - 최종 일관성 보장
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() });
    },
  });
};

/**
 * 공지사항 토글 (관리자 전용, 낙관적 업데이트)
 */
export const useToggleNotice = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.post.toggleNotice,
    mutationFn: postCommand.toggleNotice,
    onMutate: async (postId: number) => {
      // 진행 중인 refetch 취소 - 경합 상태(race condition) 방지
      await queryClient.cancelQueries({ queryKey: queryKeys.post.detail(postId) });

      // 이전 값 스냅샷 - 에러 시 롤백용
      const previousPost = queryClient.getQueryData(queryKeys.post.detail(postId));

      // 낙관적 업데이트: 서버 응답 전에 UI를 먼저 업데이트하여 반응성 향상
      queryClient.setQueryData(queryKeys.post.detail(postId), (old: ApiResponse<Post>) => {
        if (!old?.success || !old?.data) return old;

        const post = old.data;
        const newIsNotice = !post.isNotice;

        return {
          ...old,
          data: {
            ...post,
            isNotice: newIsNotice,
          },
        };
      });

      return { previousPost, postId };
    },
    onError: (err, postId, context) => {
      // 에러 시 이전 값으로 롤백 - 낙관적 업데이트 취소
      if (context?.previousPost) {
        queryClient.setQueryData(queryKeys.post.detail(postId), context.previousPost);
      }
      showToast({ type: 'error', message: '공지사항 변경에 실패했습니다.' });
    },
    onSuccess: (response, postId, context) => {
      if (response.success) {
        // 성공 시 변경 전 상태를 기반으로 구체적인 메시지 표시
        const previousPost = context?.previousPost as ApiResponse<Post> | undefined;
        const wasNotice = previousPost?.data?.isNotice ?? false;

        const message = wasNotice
          ? '공지사항이 해제되었습니다.'
          : '공지사항으로 등록되었습니다.';

        showToast({ type: 'success', message });
      }
    },
    onSettled: (data, error, postId) => {
      // 성공/실패 여부와 관계없이 서버 데이터로 동기화 - 최종 일관성 보장
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.notices() });
    },
  });
};