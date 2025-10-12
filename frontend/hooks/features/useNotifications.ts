"use client";

import { useState, useEffect, useCallback } from "react"
import { sseManager, type Notification } from "@/lib/api"
import { useAuth } from "@/hooks"
import { logger } from "@/lib/utils"
import { useToastStore } from "@/stores/toast.store"
import { useQueryClient } from "@tanstack/react-query"
import { queryKeys } from "@/lib/tanstack-query/keys"

// Notification-related TanStack Query hooks are exported from hooks/api

/**
 * SSE 연결과 실시간 알림 처리를 통합한 Hook (TanStack Query + SSE)
 */
export function useNotifications() {
  const { isAuthenticated, user } = useAuth()
  const { showSuccess, showError, showInfo } = useToastStore()
  const queryClient = useQueryClient()
  const [isSSEConnected, setIsSSEConnected] = useState(false)
  const [connectionState, setConnectionState] = useState<string>("DISCONNECTED")

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

  // SSE 상태 변경 리스너: 연결 상태 로깅
  useEffect(() => {
    if (!canConnectSSE()) return

    const statusListener = (status: 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting') => {
      logger.log(`SSE 상태 변경: ${status}`)

      switch (status) {
        case 'connecting':
          logger.log("SSE 연결 시도 중...")
          break
        case 'connected':
          logger.log("SSE 연결 완료")
          if (!sseManager.hasShownConnectedToast()) {
            sseManager.markConnectedToastShown()
          }
          break
        case 'reconnecting':
          logger.log("SSE 재연결 중...")
          break
        case 'error':
          logger.log("SSE 연결 실패 - 재시도 중...")
          break
        case 'disconnected':
          logger.log("SSE 연결 종료")
          break
      }
    }

    sseManager.addStatusListener(statusListener)

    return () => {
      sseManager.removeStatusListener(statusListener)
    }
  }, [canConnectSSE])

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
    }
  }, [canConnectSSE, user, updateConnectionState])

  return {
    isSSEConnected,
    connectionState,
    canConnectSSE,
  }
}
