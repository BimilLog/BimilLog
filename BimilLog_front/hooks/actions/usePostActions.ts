'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import {
  likePostAction,
  createPostAction,
  updatePostAction,
  deletePostAction,
  toggleNoticeAction,
} from '@/lib/actions/post'

/**
 * 게시글 작성 Server Action 훅
 */
export function useCreatePostAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const router = useRouter()
  const { showToast } = useToast()

  const createPost = (data: { title: string; content: string; password?: number }) => {
    startTransition(async () => {
      const result = await createPostAction(data)

      if (result.success && result.postId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() })
        showToast({ type: 'success', message: result.message || '게시글이 작성되었습니다.' })
        router.push(`/board/post/${result.postId}`)
      } else {
        showToast({ type: 'error', message: result.error || '게시글 작성에 실패했습니다.' })
      }
    })
  }

  return { createPost, isPending }
}

/**
 * 게시글 수정 Server Action 훅
 */
export function useUpdatePostAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const router = useRouter()
  const { showToast } = useToast()

  const updatePost = (data: { postId: number; title: string; content: string; password?: number }) => {
    startTransition(async () => {
      const result = await updatePostAction(data)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(data.postId) })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() })
        showToast({ type: 'success', message: result.message || '게시글이 수정되었습니다.' })
        router.push(`/board/post/${data.postId}`)
      } else {
        showToast({ type: 'error', message: result.error || '게시글 수정에 실패했습니다.' })
      }
    })
  }

  return { updatePost, isPending }
}

/**
 * 게시글 삭제 Server Action 훅
 */
export function useDeletePostAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const router = useRouter()
  const { showToast } = useToast()

  const deletePost = (
    data: { postId: number; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ) => {
    startTransition(async () => {
      const result = await deletePostAction(data)

      if (result.success) {
        queryClient.removeQueries({ queryKey: queryKeys.post.detail(data.postId) })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() })
        showToast({ type: 'success', message: result.message || '게시글이 삭제되었습니다.' })
        callbacks?.onSuccess?.()
        router.push('/board')
      } else {
        showToast({ type: 'error', message: result.error || '게시글 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '게시글 삭제에 실패했습니다.')
      }
    })
  }

  return { deletePost, isPending }
}

/**
 * 공지사항 토글 Server Action 훅 (관리자 전용)
 */
export function useToggleNoticeAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const toggleNotice = (postId: number, notice: boolean) => {
    startTransition(async () => {
      const result = await toggleNoticeAction(postId, notice)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.notices() })
        showToast({ type: 'success', message: result.message || '공지사항 설정이 변경되었습니다.' })
      } else {
        showToast({ type: 'error', message: result.error || '공지사항 변경에 실패했습니다.' })
      }
    })
  }

  return { toggleNotice, isPending }
}

/**
 * 게시글 좋아요 Server Action 훅 (옵티미스틱 업데이트 적용)
 * 라운드 8 B-8-001: invalidate 단일 흐름 → onMutate 옵티미스틱 + onError 롤백.
 * Server Action 왕복(~150~400ms) 동안 UI가 즉시 반응하도록 한다.
 */
export function useLikePostAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const likePost = (postId: number) => {
    const detailKey = queryKeys.post.detail(postId)

    // 1) 진행 중인 refetch 취소 → 옵티미스틱 상태 보존
    queryClient.cancelQueries({ queryKey: detailKey })

    // 2) 스냅샷 저장
    const previousData = queryClient.getQueryData(detailKey)

    // 3) 옵티미스틱 토글: liked / likeCount
    let nextLiked: boolean | null = null
    queryClient.setQueryData(detailKey, (old: unknown) => {
      if (!old || typeof old !== 'object') return old
      const wrapper = old as { success?: boolean; data?: Record<string, unknown> }
      if (!wrapper.success || !wrapper.data) return old
      const post = wrapper.data as Record<string, unknown>
      const wasLiked = Boolean(post.liked)
      const currentCount = typeof post.likeCount === 'number' ? post.likeCount : 0
      nextLiked = !wasLiked
      return {
        ...wrapper,
        data: {
          ...post,
          liked: nextLiked,
          likeCount: wasLiked ? Math.max(0, currentCount - 1) : currentCount + 1,
        },
      }
    })

    startTransition(async () => {
      const result = await likePostAction(postId)

      if (result.success) {
        // 백그라운드 동기화: 정확한 카운트를 위해 invalidate
        queryClient.invalidateQueries({ queryKey: detailKey })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() })

        // 토글 결과에 따라 토스트 분기 (B-8-011)
        const successMessage =
          nextLiked === null
            ? '추천 처리가 완료되었습니다.'
            : nextLiked
              ? '추천했어요'
              : '추천을 취소했어요'
        showToast({ type: 'success', message: successMessage })
      } else {
        // 롤백
        if (previousData !== undefined) {
          queryClient.setQueryData(detailKey, previousData)
        } else {
          queryClient.invalidateQueries({ queryKey: detailKey })
        }
        showToast({ type: 'error', message: result.error || '추천 처리 중 오류가 발생했습니다.' })
      }
    })
  }

  return {
    likePost,
    isPending,
  }
}
