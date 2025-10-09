"use client";

import { useState, useEffect, useCallback, useRef, useMemo } from "react"
import { sseManager, type Notification } from "@/lib/api"
import { useAuth } from "@/hooks"
import { logger } from "@/lib/utils"
import { useToastStore } from "@/stores/toast.store"
import { useQueryClient } from "@tanstack/react-query"
import { queryKeys } from "@/lib/tanstack-query/keys"

// TanStack Query Hooks re-exports
export { useNotificationList } from '@/hooks/api/useNotificationQueries';
export {
  useMarkNotificationAsRead,
  useDeleteNotification,
  useMarkAllNotificationsAsRead,
  useDeleteAllNotifications
} from '@/hooks/api/useNotificationMutations';

/**
 * SSE 연결과 실시간 알림 처리를 통합한 Hook (TanStack Query + SSE)
 */
export function useNotifications() {
  const { isAuthenticated, user } = useAuth()
  const { showSuccess, showError, showInfo } = useToastStore()
  const queryClient = useQueryClient()
  const [isSSEConnected, setIsSSEConnected] = useState(false)
  const [connectionState, setConnectionState] = useState<string>("DISCONNECTED")
  const hasShownConnectedToastRef = useRef(false)

  // SSE 연결 가능 여부 확인: 사용자 인증 및 닉네임 설정 완료 상태 체크
  const canConnectSSE = useCallback(() => {
    if (!isAuthenticated || !user) {
      return false
    }

    // 닉네임이 설정되지 않은 경우 SSE 연결 불가
    if (!user.memberName || user.memberName.trim() === "") {
      return false
    }

    return true
  }, [isAuthenticated, user])

  // SSE 연결 상태 업데이트
  const updateConnectionState = useCallback(() => {
    const state = sseManager.getConnectionState()
    const connected = sseManager.isConnected()

    logger.log(`SSE 연결 상태: ${state}, 연결됨: ${connected}`)
    setConnectionState(state)
    setIsSSEConnected(connected)
  }, [])

  // SSE 상태 변경 리스너: 연결 상태에 따라 토스트 알림 표시
  useEffect(() => {
    if (!canConnectSSE()) return

    const statusListener = (status: 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting') => {
      logger.log(`SSE 상태 변경: ${status}`)

      switch (status) {
        case 'connecting':
          logger.log("SSE 연결 시도 중...")
          break
        case 'connected':
          // 최초 연결 시에만 토스트 표시 (재연결 시에는 표시하지 않음)
          if (!hasShownConnectedToastRef.current) {
            showSuccess("실시간 알림 활성화", "새로운 알림을 실시간으로 받을 수 있습니다", 3000)
            hasShownConnectedToastRef.current = true
          } else {
            showInfo("실시간 알림 복구", "알림 연결이 복구되었습니다", 2000)
          }
          break
        case 'reconnecting':
          showInfo("재연결 중", "실시간 알림 연결을 복구하고 있습니다...", 2000)
          break
        case 'error':
          showError("연결 실패", "실시간 알림 연결에 실패했습니다. 재시도 중...", 3000)
          break
        case 'disconnected':
          hasShownConnectedToastRef.current = false
          break
      }
    }

    sseManager.addStatusListener(statusListener)

    return () => {
      sseManager.removeStatusListener(statusListener)
    }
  }, [canConnectSSE, showSuccess, showError, showInfo])

  // SSE 이벤트 리스너 등록/해제: 실시간 알림 처리와 연결 상태 관리
  useEffect(() => {
    if (canConnectSSE()) {
      logger.log(`사용자 인증 완료 (${user?.memberName}) - 알림 리스너 등록`)

      // 중복 리스너 방지: 기존 리스너 제거 후 새로 등록
      sseManager.removeEventListener("notification")

      // 새 알림 수신 리스너 등록
      sseManager.addEventListener("notification", (data) => {
        logger.log("새 알림 수신:", data)

        // 알림 목록 자동 갱신 (TanStack Query invalidation)
        queryClient.invalidateQueries({ queryKey: queryKeys.notification.list() })
        logger.log("알림 목록 자동 갱신 완료")

        // 브라우저 알림 권한 확인 후 푸시 알림 표시
        if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "granted") {
          new Notification(data.content, {
            body: data.url,
            icon: "/favicon.ico",
          })
        }
      })

      // SSE 연결 상태 모니터링: 30초마다 연결 상태 확인하여 자동 재연결
      const intervalId = setInterval(updateConnectionState, 30000)

      // 초기 연결 상태 확인 (1초 후 실행)
      setTimeout(updateConnectionState, 1000)

      return () => {
        clearInterval(intervalId)
        sseManager.removeEventListener("notification")
      }
    } else {
      // 인증 해제 시 리스너 정리 및 상태 초기화
      sseManager.removeEventListener("notification")
      setIsSSEConnected(false)
      setConnectionState("DISCONNECTED")
      hasShownConnectedToastRef.current = false
    }
  }, [canConnectSSE, user, updateConnectionState])

  return {
    isSSEConnected,
    connectionState,
    canConnectSSE,
  }
}
