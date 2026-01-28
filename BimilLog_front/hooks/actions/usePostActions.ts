'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import { likePostAction } from '@/lib/actions/post'

/**
 * 게시글 좋아요 Server Action 훅
 * 기존 useLikePost를 대체
 */
export function useLikePostAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const likePost = (postId: number) => {
    startTransition(async () => {
      const result = await likePostAction(postId)

      if (result.success) {
        // 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.post.detail(postId) })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.lists() })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.realtimePopular() })
        queryClient.invalidateQueries({ queryKey: queryKeys.post.weeklyPopular() })

        showToast({ type: 'success', message: result.message || '추천 처리가 완료되었습니다.' })
      } else {
        showToast({ type: 'error', message: result.error || '추천 처리 중 오류가 발생했습니다.' })
      }
    })
  }

  return {
    likePost,
    isPending,
  }
}
