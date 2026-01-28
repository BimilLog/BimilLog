'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import { addToBlacklistAction, removeFromBlacklistAction } from '@/lib/actions/blacklist'

/**
 * 블랙리스트 추가 Server Action 훅
 */
export function useAddToBlacklistAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const addToBlacklist = (
    memberName: string,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await addToBlacklistAction(memberName)

      if (result.success) {
        // 블랙리스트 캐시 무효화
        queryClient.invalidateQueries({
          queryKey: queryKeys.blacklist.all
        })

        showToast({ type: 'success', message: result.message || '블랙리스트에 추가되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '블랙리스트 추가에 실패했습니다.' })
        callbacks?.onError?.(result.error || '블랙리스트 추가에 실패했습니다.')
      }
    })
  }

  return { addToBlacklist, isPending }
}

/**
 * 블랙리스트 삭제 Server Action 훅
 */
export function useRemoveFromBlacklistAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const removeFromBlacklist = (
    id: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await removeFromBlacklistAction(id)

      if (result.success) {
        // 블랙리스트 캐시 무효화
        queryClient.invalidateQueries({
          queryKey: queryKeys.blacklist.all
        })

        showToast({ type: 'success', message: result.message || '블랙리스트에서 삭제되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '블랙리스트 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '블랙리스트 삭제에 실패했습니다.')
      }
    })
  }

  return { removeFromBlacklist, isPending }
}
