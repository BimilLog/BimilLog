'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import {
  sendFriendRequestAction,
  cancelFriendRequestAction,
  acceptFriendRequestAction,
  rejectFriendRequestAction,
  removeFriendAction,
} from '@/lib/actions/friend'

/**
 * 친구 요청 보내기 Server Action 훅
 */
export function useSendFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const sendRequest = (
    receiverMemberId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await sendFriendRequestAction(receiverMemberId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.friend.all })
        showToast({ type: 'success', message: result.message || '친구 요청을 보냈습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '친구 요청에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청에 실패했습니다.')
      }
    })
  }

  return { sendRequest, isPending }
}

/**
 * 친구 요청 취소 Server Action 훅
 */
export function useCancelFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const cancelRequest = (
    requestId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await cancelFriendRequestAction(requestId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.friend.all })
        showToast({ type: 'success', message: result.message || '친구 요청을 취소했습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '친구 요청 취소에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청 취소에 실패했습니다.')
      }
    })
  }

  return { cancelRequest, isPending }
}

/**
 * 친구 요청 수락 Server Action 훅
 */
export function useAcceptFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const acceptRequest = (
    requestId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await acceptFriendRequestAction(requestId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.friend.all })
        showToast({ type: 'success', message: result.message || '친구 요청을 수락했습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '친구 요청 수락에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청 수락에 실패했습니다.')
      }
    })
  }

  return { acceptRequest, isPending }
}

/**
 * 친구 요청 거절 Server Action 훅
 */
export function useRejectFriendRequestAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const rejectRequest = (
    requestId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await rejectFriendRequestAction(requestId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.friend.all })
        showToast({ type: 'success', message: result.message || '친구 요청을 거절했습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '친구 요청 거절에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 요청 거절에 실패했습니다.')
      }
    })
  }

  return { rejectRequest, isPending }
}

/**
 * 친구 삭제 Server Action 훅
 */
export function useRemoveFriendAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const removeFriend = (
    friendshipId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await removeFriendAction(friendshipId)

      if (result.success) {
        queryClient.invalidateQueries({ queryKey: queryKeys.friend.all })
        showToast({ type: 'success', message: result.message || '친구를 삭제했습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '친구 삭제에 실패했습니다.' })
        callbacks?.onError?.(result.error || '친구 삭제에 실패했습니다.')
      }
    })
  }

  return { removeFriend, isPending }
}
