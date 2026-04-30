'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import {
  likeCommentAction,
  createCommentAction,
  updateCommentAction,
  deleteCommentAction,
} from '@/lib/actions/comment'

/**
 * 댓글 좋아요 Server Action 훅 (옵티미스틱 업데이트 적용)
 * 라운드 8 B-8-001 / B-8-015:
 *  - 무한쿼리(`pages` 배열)의 모든 페이지를 setQueriesData로 순회하며
 *    해당 commentId 의 userLike/likeCount 만 토글한다.
 *  - 인기 댓글 리스트(popularCommentList)도 동일 댓글이면 함께 갱신.
 *  - 실패 시 스냅샷 복원 → 페이지 전체 재요청 회피.
 */
export function useLikeCommentAction(postId: number) {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const likeComment = (commentId: number) => {
    const listKey = queryKeys.comment.list(postId)

    // 진행 중인 refetch 취소
    queryClient.cancelQueries({ queryKey: listKey })

    // 스냅샷
    const snapshot = queryClient.getQueriesData({ queryKey: listKey })

    // 옵티미스틱: 무한쿼리 모든 페이지 + popularCommentList 순회
    let nextUserLike: boolean | null = null
    queryClient.setQueriesData({ queryKey: listKey }, (old: unknown) => {
      if (!old || typeof old !== 'object') return old
      const data = old as { pages?: unknown[]; pageParams?: unknown[] }
      if (!Array.isArray(data.pages)) return old

      const togglePopularList = (list: unknown[]): unknown[] =>
        list.map((c) => {
          if (!c || typeof c !== 'object') return c
          const comment = c as Record<string, unknown>
          if (comment.id !== commentId) return c
          const wasLiked = Boolean(comment.userLike)
          if (nextUserLike === null) nextUserLike = !wasLiked
          const currentCount = typeof comment.likeCount === 'number' ? comment.likeCount : 0
          return {
            ...comment,
            userLike: !wasLiked,
            likeCount: wasLiked ? Math.max(0, currentCount - 1) : currentCount + 1,
          }
        })

      return {
        ...data,
        pages: data.pages.map((page) => {
          if (!page || typeof page !== 'object') return page
          const p = page as {
            success?: boolean
            data?: {
              popularCommentList?: unknown[]
              commentInfoPage?: { content?: unknown[] }
            }
          }
          if (!p.success || !p.data) return page
          const popular = Array.isArray(p.data.popularCommentList)
            ? togglePopularList(p.data.popularCommentList)
            : p.data.popularCommentList
          const content = Array.isArray(p.data.commentInfoPage?.content)
            ? togglePopularList(p.data.commentInfoPage!.content!)
            : p.data.commentInfoPage?.content
          return {
            ...p,
            data: {
              ...p.data,
              popularCommentList: popular,
              commentInfoPage: p.data.commentInfoPage
                ? { ...p.data.commentInfoPage, content }
                : p.data.commentInfoPage,
            },
          }
        }),
      }
    })

    startTransition(async () => {
      const result = await likeCommentAction(commentId, postId)

      if (result.success) {
        // 추가 동기화는 의도적으로 생략 — 옵티미스틱 결과를 신뢰.
        // (라운드 8 B-8-015: 모든 페이지 invalidate 회피)
        const successMessage =
          nextUserLike === null
            ? '추천 처리가 완료되었습니다.'
            : nextUserLike
              ? '추천했어요'
              : '추천을 취소했어요'
        showToast({ type: 'success', message: successMessage })
      } else {
        // 롤백: 모든 페이지 스냅샷 복원
        snapshot.forEach(([key, value]) => {
          queryClient.setQueryData(key, value)
        })
        showToast({ type: 'error', message: result.error || '추천 처리 중 오류가 발생했습니다.' })
      }
    })
  }

  return { likeComment, isPending }
}

/**
 * 댓글 작성 Server Action 훅
 */
export function useCreateCommentAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const createComment = (
    data: { postId: number; content: string; parentId?: number; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ) => {
    startTransition(async () => {
      const result = await createCommentAction(data)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(data.postId) })

        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(data.postId) })
        showToast({ type: 'success', message: result.message || '댓글이 작성되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '댓글 작성에 실패했습니다.' })
        callbacks?.onError?.(result.error || '댓글 작성에 실패했습니다.')
      }
    })
  }

  return { createComment, isPending }
}

/**
 * 댓글 수정 Server Action 훅
 */
export function useUpdateCommentAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const updateComment = (
    data: { commentId: number; postId: number; content: string; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ) => {
    startTransition(async () => {
      const result = await updateCommentAction(data)

      if (result.success) {
        // 댓글 수정은 댓글 수 변동 없으므로 comment.list만 갱신
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(data.postId) })
        showToast({ type: 'success', message: result.message || '댓글이 수정되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '댓글 수정에 실패했습니다.' })
        callbacks?.onError?.(result.error || '댓글 수정에 실패했습니다.')
      }
    })
  }

  return { updateComment, isPending }
}

/**
 * 댓글 삭제 Server Action 훅
 */
export function useDeleteCommentAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const deleteComment = (
    data: { commentId: number; postId: number; password?: number },
    callbacks?: { onSuccess?: () => void; onError?: (error: string) => void }
  ) => {
    startTransition(async () => {
      const result = await deleteCommentAction(data)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(data.postId) })

        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(data.postId) })
        showToast({ type: 'success', message: result.message || '댓글이 삭제되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '댓글 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '댓글 삭제에 실패했습니다.')
      }
    })
  }

  return { deleteComment, isPending }
}
