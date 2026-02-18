"use client";

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { mutationKeys, queryKeys } from '@/lib/tanstack-query/keys';
import { postCommand } from '@/lib/api';
import { useToast } from '@/hooks';
import { useRouter } from 'next/navigation';
import { ErrorHandler } from '@/lib/api/helpers';

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
 * 게시글 좋아요 토글
 */
export const useLikePost = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.post.like,
    mutationFn: postCommand.like,
    onSuccess: (data, postId) => {
      // 서버 응답 후 캐시 무효화하여 최신 데이터 가져오기
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() });

      showToast({ type: 'success', message: '추천 처리가 완료되었습니다.' });
    },
    onError: (err: any) => {
      const appError = ErrorHandler.mapApiError(err);
      const errorMessage = appError.userMessage || appError.message || '추천 처리 중 오류가 발생했습니다.';
      showToast({ type: 'error', message: errorMessage });
    },
  });
};

/**
 * 공지사항 토글 (관리자 전용)
 * 공지사항은 FeaturedPost(type=NOTICE) 테이블에서 관리됨
 */
export const useToggleNotice = () => {
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  return useMutation({
    mutationKey: mutationKeys.post.toggleNotice,
    mutationFn: ({ postId, notice }: { postId: number; notice: boolean }) =>
      postCommand.toggleNotice(postId, notice),
    onError: (err: any) => {
      // HTTP 상태 코드에 따른 구체적인 에러 메시지 표시
      let errorMessage = '공지사항 변경에 실패했습니다.';

      if (err?.response?.status === 403) {
        errorMessage = '권한이 없습니다. 관리자만 공지사항을 설정할 수 있습니다.';
      } else if (err?.response?.status === 404) {
        errorMessage = '게시글을 찾을 수 없습니다.';
      } else if (err?.error || err?.message) {
        // 백엔드에서 제공하는 에러 메시지가 있으면 사용
        errorMessage = err.error || err.message;
      }

      showToast({ type: 'error', message: errorMessage });
    },
    onSuccess: (response) => {
      if (response.success) {
        showToast({ type: 'success', message: '공지사항 설정이 변경되었습니다.' });
      }
    },
    onSettled: (data, error, { postId }) => {
      // 성공/실패 여부와 관계없이 서버 데이터로 동기화 - 최종 일관성 보장
      queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.notices() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() });
      queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() });
    },
  });
};
