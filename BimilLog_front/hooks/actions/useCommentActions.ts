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
 * 댓글 좋아요 Server Action 훅
 */
export function useLikeCommentAction(postId: number) {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const likeComment = (commentId: number) => {
    startTransition(async () => {
      const result = await likeCommentAction(commentId, postId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(postId) })

        showToast({ type: 'success', message: result.message || '추천 처리가 완료되었습니다.' })
      } else {
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
        queryClient.invalidateQueries({ queryKey: queryKeys.comment.list(data.postId) })

        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(data.postId) })
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
