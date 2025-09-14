"use client";

import { useState, useEffect, useCallback, useRef, useMemo } from "react"
import { sseManager, type Notification } from "@/lib/api"
import { useAuth } from "@/hooks"
import { logger } from "@/lib/utils"

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
  const [isSSEConnected, setIsSSEConnected] = useState(false)
  const [connectionState, setConnectionState] = useState<string>("DISCONNECTED")

  // SSE 연결 가능 여부 확인
  const canConnectSSE = useCallback(() => {
    if (!isAuthenticated || !user) {
      return false
    }

    if (!user.userName || user.userName.trim() === "") {
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

  // 브라우저 알림 권한 요청
  const requestNotificationPermission = useCallback(async () => {
    if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "default") {
      const permission = await Notification.requestPermission()
      logger.log("브라우저 알림 권한:", permission)
      return permission
    }
    return Notification.permission
  }, [])

  // SSE 이벤트 리스너 등록/해제
  useEffect(() => {
    if (canConnectSSE()) {
      logger.log(`사용자 인증 완료 (${user?.userName}) - 알림 리스너 등록`)

      // 기존 리스너 제거 후 새로 등록
      sseManager.removeEventListener("notification")

      // 새 알림 수신 리스너 등록
      sseManager.addEventListener("notification", (data) => {
        logger.log("새 알림 수신:", data)

        // 브라우저 알림 표시
        if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "granted") {
          new Notification(data.content, {
            body: data.url,
            icon: "/favicon.ico",
          })
        }
      })

      // 연결 상태 모니터링
      const intervalId = setInterval(updateConnectionState, 30000)

      // 초기 연결 상태 확인
      setTimeout(updateConnectionState, 1000)

      return () => {
        clearInterval(intervalId)
        sseManager.removeEventListener("notification")
      }
    } else {
      // 인증이 해제된 경우 리스너만 제거
      sseManager.removeEventListener("notification")
      setIsSSEConnected(false)
      setConnectionState("DISCONNECTED")
    }
  }, [canConnectSSE, user, updateConnectionState])

  return {
    isSSEConnected,
    connectionState,
    canConnectSSE,
    requestNotificationPermission,
  }
}
