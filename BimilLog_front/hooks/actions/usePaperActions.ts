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
      // 백엔드 무응답 등으로 Server Action 이 응답하지 않을 때 사용자에게 빠르게 피드백을 주기 위해
      // 8초 타임아웃을 걸어 강제로 에러 토스트를 노출한다.
      // F-005: 4초는 모바일 네트워크 환경에서 짧아 false-negative 후 사용자가 재시도하면
      // unique_member_x_y 충돌이 빈번. 8초로 확장하고 안내 문구도 "이미 등록되었을 수 있어요" 로 보정.
      const TIMEOUT_MS = 8000
      const timeoutPromise = new Promise<{ success: false; error: string }>((resolve) => {
        setTimeout(
          () =>
            resolve({
              success: false,
              error:
                '응답이 늦어지고 있어요. 이미 등록되었을 수 있으니 잠시 후 새로고침해 확인해주세요.',
            }),
          TIMEOUT_MS,
        )
      })
      const result = await Promise.race([createMessageAction(data), timeoutPromise])

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
        // raw 좌표 노출 (예: "x는 0~11 사이의 값이어야 합니다.") 은 사용자에게 친화적이지 않으므로
        // 위치 관련 검증 에러는 일반화된 메시지로 치환한다.
        const rawError = result.error || '메시지 작성에 실패했습니다.'
        const isCoordError =
          /x\s*는?\s*0\s*~\s*\d+/.test(rawError) ||
          /y\s*는?\s*0\s*~\s*\d+/.test(rawError) ||
          /0\s*~\s*\d+\s*사이/.test(rawError)
        const friendlyError = isCoordError
          ? '선택한 위치에 메시지를 작성할 수 없습니다. 다른 위치를 선택해주세요.'
          : rawError

        showToast({ type: 'error', message: friendlyError })
        callbacks?.onError?.(friendlyError)
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
