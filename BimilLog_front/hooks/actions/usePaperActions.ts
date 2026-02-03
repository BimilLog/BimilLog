'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import { createMessageAction, deleteMessageAction } from '@/lib/actions/paper'
import type { DecoType } from '@/types/domains/paper'

/**
 * 롤링페이퍼 메시지 작성 Server Action 훅
 */
export function useCreateMessageAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const createMessage = (
    data: {
      ownerId: number
      ownerName?: string // 캐시 무효화용 (선택)
      decoType: DecoType
      anonymity: string
      content: string
      x: number
      y: number
    },
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await createMessageAction(data)

      if (result.success) {
        // 캐시 무효화
        if (data.ownerName) {
          queryClient.invalidateQueries({
            queryKey: queryKeys.paper.detail(data.ownerName)
          })
        }
        queryClient.invalidateQueries({
          queryKey: queryKeys.paper.my
        })

        showToast({ type: 'success', message: result.message || '메시지가 작성되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '메시지 작성에 실패했습니다.' })
        callbacks?.onError?.(result.error || '메시지 작성에 실패했습니다.')
      }
    })
  }

  return { createMessage, isPending }
}

/**
 * 롤링페이퍼 메시지 삭제 Server Action 훅
 */
export function useDeleteMessageAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const deleteMessage = (
    data: {
      messageId: number
      userName?: string
    },
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await deleteMessageAction(data)

      if (result.success) {
        // 모든 롤링페이퍼 관련 캐시 무효화
        queryClient.invalidateQueries({
          queryKey: queryKeys.paper.all
        })

        showToast({ type: 'success', message: result.message || '메시지가 삭제되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '메시지 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '메시지 삭제에 실패했습니다.')
      }
    })
  }

  return { deleteMessage, isPending }
}
