"use client";

import { useState, useEffect, useCallback, useRef } from "react"
import { notificationQuery, notificationCommand, sseManager, type Notification } from "@/lib/api"
import { useAuth } from "@/hooks"
import { logger } from "@/lib/utils"

export function useNotifications() {
  const { isAuthenticated, user } = useAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [isSSEConnected, setIsSSEConnected] = useState(false)
  const [connectionState, setConnectionState] = useState<string>("DISCONNECTED")

  // 배치 처리용 상태
  const [pendingReadIds, setPendingReadIds] = useState<Set<number>>(new Set())
  const [pendingDeleteIds, setPendingDeleteIds] = useState<Set<number>>(new Set())
  const batchTimerRef = useRef<NodeJS.Timeout | null>(null)

  // SSE 연결 가능 여부 확인
  const canConnectSSE = useCallback(() => {
    // 인증되지 않은 경우 연결 불가
    if (!isAuthenticated || !user) {
      return false
    }

    // 닉네임이 없는 신규회원은 회원가입 완료 후 연결
    if (!user.userName || user.userName.trim() === "") {
      return false
    }

    return true
  }, [isAuthenticated, user])

  // 배치 처리 실행
  const processBatch = useCallback(async () => {
    const readIds = Array.from(pendingReadIds)
    const deleteIds = Array.from(pendingDeleteIds)

    // 처리할 알림이 없으면 배치 처리 건너뛰기
    if (readIds.length === 0 && deleteIds.length === 0) {
      return
    }

    try {
      logger.log(`배치 처리 시작 - 읽음: ${readIds.length}개, 삭제: ${deleteIds.length}개`)

      const response = await notificationQuery.getAll()

      if (response.success) {
        logger.log("배치 처리 완료")
        // 처리 완료된 ID들 제거
        setPendingReadIds(new Set())
        setPendingDeleteIds(new Set())
      } else {
        logger.error("배치 처리 실패:", response.error)
      }
    } catch (error) {
      logger.error("배치 처리 중 오류:", error)
    }
  }, [pendingReadIds, pendingDeleteIds])

  // 5분마다 배치 처리 실행
  useEffect(() => {
    if (canConnectSSE()) {
      // 기존 타이머 정리
      if (batchTimerRef.current) {
        clearInterval(batchTimerRef.current)
      }

      // 5분(300초)마다 배치 처리 실행
      batchTimerRef.current = setInterval(() => {
        processBatch()
      }, 5 * 60 * 1000) // 5분

      logger.log("알림 배치 처리 타이머 시작 (5분 간격)")

      return () => {
        if (batchTimerRef.current) {
          clearInterval(batchTimerRef.current)
          batchTimerRef.current = null
          logger.log("알림 배치 처리 타이머 정리")
        }
      }
    }
  }, [canConnectSSE, processBatch])

  // 컴포넌트 언마운트 시 남은 배치 처리
  useEffect(() => {
    return () => {
      if (batchTimerRef.current) {
        clearInterval(batchTimerRef.current)
      }
      // 언마운트 시 남은 배치 처리 실행
      if (pendingReadIds.size > 0 || pendingDeleteIds.size > 0) {
        processBatch()
      }
    }
  }, [pendingReadIds.size, pendingDeleteIds.size, processBatch])

  // 알림 목록 조회
  const fetchNotifications = async () => {
    if (!canConnectSSE()) return

    setIsLoading(true)
    try {
      const response = await notificationQuery.getAll()
      
      if (response.success && response.data) {
        setNotifications(response.data)
        setUnreadCount(response.data.filter((n) => !n.isRead).length)  // v2: read → isRead
        logger.log(`알림 ${response.data.length}개 조회됨 (읽지 않음: ${response.data.filter((n) => !n.isRead).length}개)`)
      }
    } catch (error) {
      logger.error("알림 조회 실패:", error)
    } finally {
      setIsLoading(false)
    }
  }


  // SSE 알림 리스너 등록 (AuthContext에서 연결 관리)
  useEffect(() => {
    if (canConnectSSE()) {
      logger.log(`사용자 인증 완료 (${user?.userName}) - 알림 리스너 등록`)

      // 기존 리스너 제거 후 새로 등록
      sseManager.removeEventListener("notification")

      // 새 알림 수신 리스너 등록
      sseManager.addEventListener("notification", (data) => {
        logger.log("새 알림 수신:", data)
        
        // SSEManager에서 이미 변환된 Notification 객체를 직접 사용 (서버 재조회 불필요)
        const newNotification: Notification = {
          id: data.id,
          content: data.content,
          url: data.url,
          notificationType: data.notificationType,
          createdAt: data.createdAt,
          isRead: data.isRead,
        }

        setNotifications((prev) => [newNotification, ...prev])
        if (!data.isRead) {
          setUnreadCount((prev) => prev + 1)
        }

        // 브라우저 알림 표시
        if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "granted") {
          new Notification(data.content, {
            body: data.url,
            icon: "/favicon.ico",
          })
        }
      })

      // 연결 상태 모니터링
      const checkConnection = () => {
        const state = sseManager.getConnectionState()
        const connected = sseManager.isConnected()

        logger.log(`SSE 연결 상태: ${state}, 연결됨: ${connected}`)
        setConnectionState(state)
        setIsSSEConnected(connected)
      }

      // 주기적 연결 상태 확인 (30초마다)
      const intervalId = setInterval(checkConnection, 30000)
      
      // 초기 연결 상태 확인
      setTimeout(checkConnection, 1000)
      
      return () => {
        clearInterval(intervalId)
        sseManager.removeEventListener("notification")
      }
    } else {
      // 인증이 해제된 경우 리스너만 제거 (연결 해제는 AuthContext에서 처리)
      sseManager.removeEventListener("notification")
      setIsSSEConnected(false)
      setConnectionState("DISCONNECTED")
    }
  }, [canConnectSSE, user])

  // 개별 알림 읽음 처리 (배치에 추가)
  const markAsRead = useCallback(async (notificationId: number) => {
    logger.log(`알림 ${notificationId} 읽음 처리 - 배치에 추가`)
    
    // UI 즉시 업데이트
    setNotifications((prev) =>
      prev.map((notification) =>
        notification.id === notificationId ? { ...notification, isRead: true } : notification,  // v2: read → isRead
      ),
    )
    setUnreadCount((prev) => Math.max(0, prev - 1))
    
    // 배치에 추가
    setPendingReadIds((prev) => new Set([...prev, notificationId]))
  }, [])

  // 개별 알림 삭제 (배치에 추가)
  const deleteNotification = useCallback(async (notificationId: number) => {
    logger.log(`알림 ${notificationId} 삭제 - 배치에 추가`)
    
    const notification = notifications.find((n) => n.id === notificationId)
    
    // UI 즉시 업데이트
    setNotifications((prev) => prev.filter((n) => n.id !== notificationId))
    if (notification && !notification.isRead) {  // v2: read → isRead
      setUnreadCount((prev) => Math.max(0, prev - 1))
    }
    
    // 배치에 추가
    setPendingDeleteIds((prev) => new Set([...prev, notificationId]))
  }, [notifications])

  // 모든 알림 읽음 처리 (즉시 실행)
  const markAllAsRead = async () => {
    try {
      const unreadIds = notifications.filter((n) => !n.isRead).map((n) => n.id)  // v2: read → isRead
      if (unreadIds.length === 0) return

      logger.log(`모든 알림 읽음 처리 - 즉시 실행 (${unreadIds.length}개)`)
      await notificationCommand.markAllAsRead()
      const response = await notificationQuery.getAll()
      if (response.success) {
        setNotifications((prev) => prev.map((notification) => ({ ...notification, isRead: true })))  // v2: read → isRead
        setUnreadCount(0)
        
        // 배치에서 해당 ID들 제거 (이미 처리됨)
        setPendingReadIds((prev) => {
          const newSet = new Set(prev)
          unreadIds.forEach(id => newSet.delete(id))
          return newSet
        })
        
        logger.log("모든 알림 읽음 처리 완료")
      }
    } catch (error) {
      logger.error("모든 알림 읽음 처리 실패:", error)
    }
  }

  // 모든 알림 삭제 (즉시 실행)
  const deleteAllNotifications = async () => {
    try {
      const allIds = notifications.map((n) => n.id)
      if (allIds.length === 0) return

      logger.log(`모든 알림 삭제 - 즉시 실행 (${allIds.length}개)`)
      for (const id of allIds) {
        await notificationCommand.delete(id)
      }
      const response = await notificationQuery.getAll()
      if (response.success) {
        setNotifications([])
        setUnreadCount(0)
        
        // 배치에서 해당 ID들 제거 (이미 처리됨)
        setPendingDeleteIds((prev) => {
          const newSet = new Set(prev)
          allIds.forEach(id => newSet.delete(id))
          return newSet
        })
        
        logger.log("모든 알림 삭제 완료")
      }
    } catch (error) {
      logger.error("모든 알림 삭제 실패:", error)
    }
  }

  // 브라우저 알림 권한 요청
  const requestNotificationPermission = async () => {
    if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "default") {
      const permission = await Notification.requestPermission()
      logger.log("브라우저 알림 권한:", permission)
      return permission
    }
    return Notification.permission
  }

  return {
    notifications,
    unreadCount,
    isLoading,
    isSSEConnected,
    connectionState,
    canConnectSSE,
    fetchNotifications,
    markAsRead,
    deleteNotification,
    markAllAsRead,
    deleteAllNotifications,
    requestNotificationPermission,
    // 배치 상태 (디버깅용)
    pendingReadIds: Array.from(pendingReadIds),
    pendingDeleteIds: Array.from(pendingDeleteIds),
    processBatch, // 수동 배치 처리 (테스트용)
  }
}
