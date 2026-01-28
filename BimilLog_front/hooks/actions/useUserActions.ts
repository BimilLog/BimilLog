'use client'

import { useTransition } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { queryKeys } from '@/lib/tanstack-query/keys'
import { useToast } from '@/hooks'
import { useAuthStore } from '@/stores'
import {
  updateUserNameAction,
  updateSettingsAction,
  submitReportAction,
  withdrawAction,
} from '@/lib/actions/user'

interface Setting {
  messageNotification: boolean
  commentNotification: boolean
  postFeaturedNotification: boolean
  friendSendNotification: boolean
}

interface ReportData {
  reportType: 'POST' | 'COMMENT' | 'ERROR' | 'IMPROVEMENT'
  targetId?: number
  content: string
  reporterId: number | null
  reporterName: string
}

/**
 * 사용자명 변경 Server Action 훅
 */
export function useUpdateUserNameAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const { user, setUser } = useAuthStore()

  const updateUserName = (
    memberName: string,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await updateUserNameAction(memberName)

      if (result.success) {
        // 사용자 정보 업데이트
        if (user) {
          setUser({ ...user, memberName })
        }
        // 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.user.all })
        queryClient.invalidateQueries({ queryKey: queryKeys.auth.all })

        showToast({ type: 'success', message: result.message || '사용자명이 변경되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '사용자명 변경에 실패했습니다.' })
        callbacks?.onError?.(result.error || '사용자명 변경에 실패했습니다.')
      }
    })
  }

  return { updateUserName, isPending }
}

/**
 * 사용자 설정 업데이트 Server Action 훅
 */
export function useUpdateSettingsAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()

  const updateSettings = (
    settings: Setting,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await updateSettingsAction(settings)

      if (result.success) {
        // 설정 캐시 무효화
        queryClient.invalidateQueries({ queryKey: queryKeys.user.settings() })

        showToast({ type: 'success', message: result.message || '설정이 저장되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '설정 저장에 실패했습니다.' })
        callbacks?.onError?.(result.error || '설정 저장에 실패했습니다.')
      }
    })
  }

  return { updateSettings, isPending }
}

/**
 * 신고 제출 Server Action 훅
 */
export function useSubmitReportAction() {
  const [isPending, startTransition] = useTransition()
  const { showToast } = useToast()

  const submitReport = (
    report: ReportData,
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await submitReportAction(report)

      if (result.success) {
        showToast({ type: 'success', message: result.message || '신고가 접수되었습니다.' })
        callbacks?.onSuccess?.()
      } else {
        showToast({ type: 'error', message: result.error || '신고 처리에 실패했습니다.' })
        callbacks?.onError?.(result.error || '신고 처리에 실패했습니다.')
      }
    })
  }

  return { submitReport, isPending }
}

/**
 * 회원 탈퇴 Server Action 훅
 */
export function useWithdrawAction() {
  const [isPending, startTransition] = useTransition()
  const queryClient = useQueryClient()
  const { showToast } = useToast()
  const router = useRouter()
  const { logout } = useAuthStore()

  const withdraw = (
    callbacks?: {
      onSuccess?: () => void
      onError?: (error: string) => void
    }
  ) => {
    startTransition(async () => {
      const result = await withdrawAction()

      if (result.success) {
        // 모든 캐시 초기화
        queryClient.clear()
        // 인증 상태 초기화
        logout()

        showToast({ type: 'success', message: result.message || '회원 탈퇴가 완료되었습니다.' })
        callbacks?.onSuccess?.()
        router.push('/')
      } else {
        showToast({ type: 'error', message: result.error || '회원 탈퇴에 실패했습니다.' })
        callbacks?.onError?.(result.error || '회원 탈퇴에 실패했습니다.')
      }
    })
  }

  return { withdraw, isPending }
}
