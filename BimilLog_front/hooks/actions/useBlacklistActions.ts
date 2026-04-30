'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToastStore } from '@/stores/toast.store'
import type { ApiResponse, PageResponse } from '@/types/common'
import type { BlacklistDTO } from '@/types/domains/blacklist'
import { addToBlacklistAction, removeFromBlacklistAction } from '@/lib/actions/blacklist'

type ActionCallbacks = {
  onSuccess?: () => void
  onError?: (error: string) => void
}

/**
 * 라운드 15 F-15-BUG-2: 모든 page/size 변형 캐시에서 한 번에 차단 항목을 마스킹.
 * 라운드 9 friend 의 removeFromFriendsCache 패턴 차용.
 *
 * 반환값: 첫 번째로 발견된 항목 (undo 토스트에서 활용).
 */
function removeFromBlacklistCache(
  queryClient: ReturnType<typeof useQueryClient>,
  id: number,
): BlacklistDTO | null {
  const queries = queryClient.getQueriesData<ApiResponse<PageResponse<BlacklistDTO>>>({
    queryKey: queryKeys.blacklist.lists(),
  })

  let removed: BlacklistDTO | null = null

  queries.forEach(([key, data]) => {
    if (!data?.data) return
    const idx = data.data.content.findIndex((b) => b.id === id)
    if (idx === -1) return
    if (!removed) removed = data.data.content[idx]
    const next = data.data.content.filter((b) => b.id !== id)
    queryClient.setQueryData(key, {
      ...data,
      data: {
        ...data.data,
        content: next,
        numberOfElements: next.length,
        totalElements: Math.max(0, data.data.totalElements - 1),
        empty: next.length === 0,
      },
    })
  })

  return removed
}

/**
 * 블랙리스트 추가 Server Action 훅
 *
 * 라운드 15 F-15-BUG-15: 409(이미 차단) 응답 시 친화적 카피 + "블랙리스트 열기" 액션.
 * 추가는 응답 본문이 비어있어 새 id 를 모르므로 옵티미스틱 추가는 하지 않고
 * invalidate 로 서버 진실 동기화 후 토스트.
 */
export function useAddToBlacklistAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const router = useRouter()
  const showAdvancedToast = useToastStore((state) => state.showAdvancedToast)
  const showToast = useToastStore((state) => state.showToast)

  const addToBlacklist = (memberName: string, callbacks?: ActionCallbacks) => {
    startTransition(async () => {
      const result = await addToBlacklistAction(memberName)

      if (result.success) {
        // 모든 페이지 캐시 갱신 (라운드 15 F-15-BUG-1)
        queryClient.invalidateQueries({ queryKey: queryKeys.blacklist.all })

        showAdvancedToast({
          type: 'success',
          title: `${memberName}님을 차단했어요`,
          description: '회원님의 롤링페이퍼에 메시지를 남길 수 없어요.',
          duration: 5000,
          action: {
            label: '블랙리스트 보기',
            onClick: () => router.push('/blacklist'),
          },
        })
        callbacks?.onSuccess?.()
      } else {
        // 라운드 15 F-15-BUG-15: 이미 차단된 경우 친화 카피 + 동선 단축
        if (result.code === 'DUPLICATE') {
          showAdvancedToast({
            type: 'info',
            title: '이미 차단한 사용자예요',
            description: '블랙리스트에서 확인해보세요.',
            duration: 5000,
            action: {
              label: '블랙리스트 열기',
              onClick: () => router.push('/blacklist'),
            },
          })
        } else {
          showToast({ type: 'error', message: result.error || '블랙리스트 추가에 실패했습니다.' })
        }
        callbacks?.onError?.(result.error || '블랙리스트 추가에 실패했습니다.')
      }
    })
  }

  return { addToBlacklist, isPending }
}

/**
 * 블랙리스트 삭제 Server Action 훅 (옵티미스틱 + undo 토스트)
 *
 * 라운드 15 F-15-BUG-2/3:
 * - 옵티미스틱 캐시 제거 (라운드 9 useRemoveFriendAction 패턴).
 * - 성공 시: setQueryData 로 첫 페이지 캐시 백엔드 응답으로 동기화 + 다른 페이지 invalidate.
 * - undo 토스트: "다시 차단" 액션 (라운드 5 패턴).
 * - 실패 시: previous 복원 + error 토스트.
 */
export function useRemoveFromBlacklistAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const showAdvancedToast = useToastStore((state) => state.showAdvancedToast)
  const showToast = useToastStore((state) => state.showToast)

  const removeFromBlacklist = (id: number, callbacks?: ActionCallbacks) => {
    // 옵티미스틱: 모든 page/size 캐시에서 즉시 제거
    const previous = queryClient.getQueriesData<ApiResponse<PageResponse<BlacklistDTO>>>({
      queryKey: queryKeys.blacklist.lists(),
    })
    const removed = removeFromBlacklistCache(queryClient, id)

    startTransition(async () => {
      const result = await removeFromBlacklistAction(id)

      if (result.success) {
        // 백엔드 응답으로 첫 페이지 캐시 직접 갱신 (라운드 15 F-15-BUG-1)
        if (result.data) {
          queryClient.setQueryData<ApiResponse<PageResponse<BlacklistDTO>>>(
            queryKeys.blacklist.list(0, 20),
            { success: true, data: result.data },
          )
        }
        // 다른 페이지 캐시는 stale 마킹 (다음 진입 시 재요청)
        queryClient.invalidateQueries({ queryKey: queryKeys.blacklist.all })

        // 라운드 5 패턴: undo 토스트 — 새 id 가 발급되지만 memberName 으로 다시 추가 가능
        if (removed?.memberName) {
          showAdvancedToast({
            type: 'success',
            title: `${removed.memberName}님 차단을 풀었어요`,
            description: '다시 편지를 받을 수 있어요.',
            duration: 6000,
            action: {
              label: '다시 차단',
              onClick: () => {
                if (!removed.memberName) return
                // 동일 startTransition 컨텍스트가 아니지만 useAddToBlacklistAction 의
                // startTransition 이 별도로 동작 — 사용자가 토스트 클릭 시 새 추가 흐름.
                addToBlacklistAction(removed.memberName).then((r) => {
                  if (r.success) {
                    queryClient.invalidateQueries({ queryKey: queryKeys.blacklist.all })
                    showToast({
                      type: 'success',
                      message: `${removed.memberName}님을 다시 차단했어요`,
                    })
                  } else {
                    showToast({
                      type: 'error',
                      message: r.error || '다시 차단에 실패했어요. 잠시 후 다시 시도해주세요.',
                    })
                  }
                })
              },
            },
          })
        } else {
          showToast({ type: 'success', message: result.message || '차단을 풀었어요.' })
        }
        callbacks?.onSuccess?.()
      } else {
        // 롤백
        previous.forEach(([key, data]) => queryClient.setQueryData(key, data))
        showToast({ type: 'error', message: result.error || '블랙리스트 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '블랙리스트 삭제에 실패했습니다.')
      }
    })
  }

  return { removeFromBlacklist, isPending }
}
