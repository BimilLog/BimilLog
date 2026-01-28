'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import { batchUpdateNotificationAction, registerFcmTokenAction } from '@/lib/actions/notification'
import { logger } from '@/lib/utils/logger'

interface BatchUpdateRequest {
  readIds: number[]
  deletedIds: number[]
}

/**
 * 개별 알림 읽음 처리 Server Action 훅
 */
export function useMarkNotificationAsReadAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()

  const markAsRead = (
    notificationId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    // 낙관적 업데이트
    queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
      if (!old?.success || !old?.data) return old
      return {
        ...old,
        data: old.data.map((notification: any) =>
          notification.id === notificationId
            ? { ...notification, read: true }
            : notification
        ),
      }
    })

    startTransition(async () => {
      logger.log(`📤 알림 ${notificationId} 읽음 처리 Server Action 호출`)
      const result = await batchUpdateNotificationAction({
        readIds: [notificationId],
        deletedIds: [],
      })

      if (result.success) {
        logger.log(`알림 ${notificationId} 읽음 처리 완료`)
        callbacks?.onSuccess?.()
      } else {
        // 롤백
        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
        logger.error(`알림 ${notificationId} 읽음 처리 실패:`, result.error)
        callbacks?.onError?.(result.error || '알림 읽음 처리에 실패했습니다.')
      }
    })
  }

  return { markAsRead, isPending }
}

/**
 * 개별 알림 삭제 Server Action 훅
 */
export function useDeleteNotificationAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const deleteNotification = (
    notificationId: number,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    // 낙관적 업데이트
    queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
      if (!old?.success || !old?.data) return old
      return {
        ...old,
        data: old.data.filter((notification: any) => notification.id !== notificationId),
      }
    })

    startTransition(async () => {
      logger.log(`📤 알림 ${notificationId} 삭제 Server Action 호출`)
      const result = await batchUpdateNotificationAction({
        readIds: [],
        deletedIds: [notificationId],
      })

      if (result.success) {
        showToast({ type: 'success', message: '알림을 삭제했습니다.' })
        logger.log(`알림 ${notificationId} 삭제 완료`)
        callbacks?.onSuccess?.()
      } else {
        // 롤백
        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
        showToast({ type: 'error', message: '알림 삭제에 실패했습니다.' })
        logger.error(`알림 ${notificationId} 삭제 실패:`, result.error)
        callbacks?.onError?.(result.error || '알림 삭제에 실패했습니다.')
      }
    })
  }

  return { deleteNotification, isPending }
}

/**
 * 모든 알림 읽음 처리 Server Action 훅
 */
export function useMarkAllNotificationsAsReadAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const markAllAsRead = (
    unreadIds: number[],
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    if (unreadIds.length === 0) {
      logger.log('📭 읽음 처리할 알림이 없어 Server Action 호출을 생략합니다.')
      return
    }

    // 낙관적 업데이트
    queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
      if (!old?.success || !old?.data) return old
      return {
        ...old,
        data: old.data.map((notification: any) => ({
          ...notification,
          read: true,
        })),
      }
    })

    startTransition(async () => {
      logger.log(`📤 모든 알림 읽음 처리 Server Action 호출 - ${unreadIds.length}개 알림`)
      const result = await batchUpdateNotificationAction({
        readIds: unreadIds,
        deletedIds: [],
      })

      if (result.success) {
        showToast({ type: 'success', message: '모든 알림을 읽음 처리했습니다.' })
        logger.log(`모든 알림 읽음 처리 완료 (${unreadIds.length}개)`)
        callbacks?.onSuccess?.()
      } else {
        // 롤백
        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
        showToast({ type: 'error', message: '알림 읽음 처리에 실패했습니다.' })
        logger.error('모든 알림 읽음 처리 실패:', result.error)
        callbacks?.onError?.(result.error || '알림 읽음 처리에 실패했습니다.')
      }

      // 항상 캐시 무효화
      queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
    })
  }

  return { markAllAsRead, isPending }
}

/**
 * 모든 알림 삭제 Server Action 훅
 */
export function useDeleteAllNotificationsAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const deleteAllNotifications = (
    deleteIds: number[],
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    if (deleteIds.length === 0) {
      logger.log('📭 삭제할 알림이 없어 Server Action 호출을 생략합니다.')
      return
    }

    // 낙관적 업데이트
    queryClient.setQueryData(queryKeys.notification.list(), (old: any) => {
      if (!old?.success) return old
      return {
        ...old,
        data: [],
      }
    })

    startTransition(async () => {
      logger.log(`📤 모든 알림 삭제 Server Action 호출 - ${deleteIds.length}개 알림`)
      const result = await batchUpdateNotificationAction({
        readIds: [],
        deletedIds: deleteIds,
      })

      if (result.success) {
        showToast({ type: 'success', message: `${deleteIds.length}개의 알림을 삭제했습니다.` })
        logger.log(`모든 알림 삭제 완료 (${deleteIds.length}개)`)
        callbacks?.onSuccess?.()
      } else {
        // 롤백
        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
        showToast({ type: 'error', message: '알림 삭제에 실패했습니다.' })
        logger.error('모든 알림 삭제 실패:', result.error)
        callbacks?.onError?.(result.error || '알림 삭제에 실패했습니다.')
      }

      // 항상 캐시 무효화
      queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
    })
  }

  return { deleteAllNotifications, isPending }
}

/**
 * FCM 토큰 등록 Server Action 훅
 */
export function useRegisterFcmTokenAction() {
  const [isPending, startTransition] = useTransition()

  const registerFcmToken = (
    fcmToken: string,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      logger.log('📤 FCM 토큰 등록 Server Action 호출')
      const result = await registerFcmTokenAction(fcmToken)

      if (result.success) {
        logger.log('FCM 토큰 등록 완료')
        callbacks?.onSuccess?.()
      } else {
        logger.warn('FCM 토큰 등록 실패:', result.error)
        callbacks?.onError?.(result.error || 'FCM 토큰 등록에 실패했습니다.')
      }
    })
  }

  return { registerFcmToken, isPending }
}
