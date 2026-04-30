'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToastStore } from '@/stores/toast.store'
import type { ApiResponse, PageResponse } from '@/types/common'
import type {
  Friend,
  ReceivedFriendRequest,
  SentFriendRequest,
  RecommendedFriend,
} from '@/types/domains/friend'
import {
  sendFriendRequestAction,
  cancelFriendRequestAction,
  acceptFriendRequestAction,
  rejectFriendRequestAction,
  removeFriendAction,
} from '@/lib/actions/friend'

type ActionCallbacks = {
  onSuccess?: () => void
  onError?: (error: string) => void
}

/**
 * 라운드 9 B-009-F: 추천 친구 캐시에서 특정 ID 제거.
 * 모든 page/size 변형에서 한 번에 마스킹.
 */
function removeFromRecommendedCache(
  queryClient: ReturnType<typeof useQueryClient>,
  friendMemberId: number,
) {
  const queries = queryClient.getQueriesData<ApiResponse<PageResponse<RecommendedFriend>>>({
    queryKey: [...queryKeys.friend.all, 'recommended'],
  })

  queries.forEach(([key, data]) => {
    if (!data?.data) return
    const next = data.data.content.filter((f) => f.friendMemberId !== friendMemberId)
    if (next.length === data.data.content.length) return
    queryClient.setQueryData(key, {
      ...data,
      data: {
        ...data.data,
        content: next,
        numberOfElements: next.length,
        empty: next.length === 0,
      },
    })
  })
}

function removeFromSentCache(
  queryClient: ReturnType<typeof useQueryClient>,
  requestId: number,
) {
  const queries = queryClient.getQueriesData<ApiResponse<PageResponse<SentFriendRequest>>>({
    queryKey: [...queryKeys.friend.all, 'sent'],
  })

  queries.forEach(([key, data]) => {
    if (!data?.data) return
    const next = data.data.content.filter((r) => r.friendRequestId !== requestId)
    if (next.length === data.data.content.length) return
    queryClient.setQueryData(key, {
      ...data,
      data: {
        ...data.data,
        content: next,
        numberOfElements: next.length,
        empty: next.length === 0,
      },
    })
  })
}

function removeFromReceivedCache(
  queryClient: ReturnType<typeof useQueryClient>,
  requestId: number,
): ReceivedFriendRequest | null {
  const queries = queryClient.getQueriesData<ApiResponse<PageResponse<ReceivedFriendRequest>>>({
    queryKey: [...queryKeys.friend.all, 'received'],
  })

  let removed: ReceivedFriendRequest | null = null

  queries.forEach(([key, data]) => {
    if (!data?.data) return
    const idx = data.data.content.findIndex((r) => r.friendRequestId === requestId)
    if (idx === -1) return
    if (!removed) removed = data.data.content[idx]
    const next = data.data.content.filter((r) => r.friendRequestId !== requestId)
    queryClient.setQueryData(key, {
      ...data,
      data: {
        ...data.data,
        content: next,
        numberOfElements: next.length,
        empty: next.length === 0,
      },
    })
  })

  return removed
}

function removeFromFriendsCache(
  queryClient: ReturnType<typeof useQueryClient>,
  friendshipId: number,
): Friend | null {
  const queries = queryClient.getQueriesData<ApiResponse<PageResponse<Friend>>>({
    queryKey: [...queryKeys.friend.all, 'list'],
  })

  let removed: Friend | null = null

  queries.forEach(([key, data]) => {
    if (!data?.data) return
    const idx = data.data.content.findIndex((f) => f.friendshipId === friendshipId)
    if (idx === -1) return
    if (!removed) removed = data.data.content[idx]
    const next = data.data.content.filter((f) => f.friendshipId !== friendshipId)
    queryClient.setQueryData(key, {
      ...data,
      data: {
        ...data.data,
        content: next,
        numberOfElements: next.length,
        empty: next.length === 0,
      },
    })
  })

  return removed
}

/**
 * 친구 요청 보내기 Server Action 훅
 *
 * 라운드 9 B-009-F: 추천 카드 옵티미스틱 제거 → 사용자가 같은 카드를 다시 클릭해
 * 409 Conflict 토스트가 뜨는 것을 방지.
 * 라운드 5 패턴: 토스트에 "보낸 요청 보기" 액션.
 */
export function useSendFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const router = useRouter()
  const showAdvancedToast = useToastStore((state) => state.showAdvancedToast)
  const showToast = useToastStore((state) => state.showToast)

  const sendRequest = (
    receiverMemberId: number,
    callbacks?: ActionCallbacks,
  ) => {
    // 옵티미스틱: 추천 카드에서 즉시 제거
    const previousRecommended = queryClient.getQueriesData<ApiResponse<PageResponse<RecommendedFriend>>>({
      queryKey: [...queryKeys.friend.all, 'recommended'],
    })
    removeFromRecommendedCache(queryClient, receiverMemberId)

    startTransition(async () => {
      const result = await sendFriendRequestAction(receiverMemberId)

      if (result.success) {
        // 보낸 요청 캐시 갱신 (서버 진실 동기화)
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'sent'] })
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'recommended'] })

        showAdvancedToast({
          type: 'success',
          title: result.message || '친구 요청을 보냈어요',
          description: '보낸 요청에서 진행 상황을 확인할 수 있어요.',
          duration: 5000,
          action: {
            label: '보낸 요청 보기',
            onClick: () => router.push('/friends?tab=sent'),
          },
        })
        callbacks?.onSuccess?.()
      } else {
        // 롤백
        previousRecommended.forEach(([key, data]) => {
          queryClient.setQueryData(key, data)
        })
        showToast({ type: 'error', message: result.error || '친구 요청에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청에 실패했습니다.')
      }
    })
  }

  return { sendRequest, isPending }
}

/**
 * 친구 요청 취소 Server Action 훅 (옵티미스틱)
 */
export function useCancelFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const showToast = useToastStore((state) => state.showToast)

  const cancelRequest = (
    requestId: number,
    callbacks?: ActionCallbacks,
  ) => {
    const previousSent = queryClient.getQueriesData<ApiResponse<PageResponse<SentFriendRequest>>>({
      queryKey: [...queryKeys.friend.all, 'sent'],
    })
    removeFromSentCache(queryClient, requestId)

    startTransition(async () => {
      const result = await cancelFriendRequestAction(requestId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'sent'] })
        showToast({ type: 'success', message: result.message || '친구 요청을 취소했어요.' })
        callbacks?.onSuccess?.()
      } else {
        previousSent.forEach(([key, data]) => queryClient.setQueryData(key, data))
        showToast({ type: 'error', message: result.error || '친구 요청 취소에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청 취소에 실패했습니다.')
      }
    })
  }

  return { cancelRequest, isPending }
}

/**
 * 친구 요청 수락 Server Action 훅 (옵티미스틱 + 토스트 액션)
 *
 * 라운드 9 B-009-C: 수락 직후 "페이퍼 보러 가기" 토스트 액션을 노출하여
 * 4단계(탭 전환→친구 찾기→버튼→이동) 동선을 1단계로 단축.
 */
export function useAcceptFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const router = useRouter()
  const showAdvancedToast = useToastStore((state) => state.showAdvancedToast)
  const showToast = useToastStore((state) => state.showToast)

  const acceptRequest = (
    requestId: number,
    callbacks?: ActionCallbacks,
  ) => {
    const previousReceived = queryClient.getQueriesData<ApiResponse<PageResponse<ReceivedFriendRequest>>>({
      queryKey: [...queryKeys.friend.all, 'received'],
    })
    const removed = removeFromReceivedCache(queryClient, requestId)

    startTransition(async () => {
      const result = await acceptFriendRequestAction(requestId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'received'] })
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'list'] })
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'recommended'] })

        const senderName = removed?.senderMemberName
        showAdvancedToast({
          type: 'success',
          title: senderName ? `${senderName}님과 친구가 되었어요` : '친구가 되었어요',
          description: '편지로 인사를 건네보세요.',
          duration: 6000,
          action: senderName
            ? {
                label: '페이퍼 보러 가기',
                onClick: () =>
                  router.push(`/rolling-paper/${encodeURIComponent(senderName)}`),
              }
            : undefined,
        })
        callbacks?.onSuccess?.()
      } else {
        previousReceived.forEach(([key, data]) => queryClient.setQueryData(key, data))
        showToast({ type: 'error', message: result.error || '친구 요청 수락에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청 수락에 실패했습니다.')
      }
    })
  }

  return { acceptRequest, isPending }
}

/**
 * 친구 요청 거절 Server Action 훅 (옵티미스틱)
 */
export function useRejectFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const showToast = useToastStore((state) => state.showToast)

  const rejectRequest = (
    requestId: number,
    callbacks?: ActionCallbacks,
  ) => {
    const previousReceived = queryClient.getQueriesData<ApiResponse<PageResponse<ReceivedFriendRequest>>>({
      queryKey: [...queryKeys.friend.all, 'received'],
    })
    removeFromReceivedCache(queryClient, requestId)

    startTransition(async () => {
      const result = await rejectFriendRequestAction(requestId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'received'] })
        showToast({ type: 'success', message: result.message || '친구 요청을 거절했어요.' })
        callbacks?.onSuccess?.()
      } else {
        previousReceived.forEach(([key, data]) => queryClient.setQueryData(key, data))
        showToast({ type: 'error', message: result.error || '친구 요청 거절에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청 거절에 실패했습니다.')
      }
    })
  }

  return { rejectRequest, isPending }
}

/**
 * 친구 삭제 Server Action 훅 (옵티미스틱)
 *
 * 라운드 9 B-009-G: 친구 삭제 시 페이퍼 detail 캐시를 invalidate 해
 * 5분간 stale 권한 정보 (FriendActionButtons) 가 남는 것을 방지.
 */
export function useRemoveFriendAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const showToast = useToastStore((state) => state.showToast)

  const removeFriend = (
    friendshipId: number,
    callbacks?: ActionCallbacks,
  ) => {
    const previousFriends = queryClient.getQueriesData<ApiResponse<PageResponse<Friend>>>({
      queryKey: [...queryKeys.friend.all, 'list'],
    })
    const removed = removeFromFriendsCache(queryClient, friendshipId)

    startTransition(async () => {
      const result = await removeFriendAction(friendshipId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: [...queryKeys.friend.all, 'list'] })
        // 페이퍼 detail 캐시 무효화 (B-009-G: 5분 stale 방지)
        if (removed) {
          queryClient.invalidateQueries({
            queryKey: queryKeys.paper.detail(removed.memberName),
          })
        }
        showToast({ type: 'success', message: result.message || '친구를 삭제했어요.' })
        callbacks?.onSuccess?.()
      } else {
        previousFriends.forEach(([key, data]) => queryClient.setQueryData(key, data))
        showToast({ type: 'error', message: result.error || '친구 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 삭제에 실패했습니다.')
      }
    })
  }

  return { removeFriend, isPending }
}
