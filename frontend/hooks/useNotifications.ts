"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import { notificationApi, sseManager, type Notification } from "@/lib/api"
import { useAuth } from "./useAuth"

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

    // 닉네임이 설정되지 않은 신규회원은 연결 불가
    if (!user.userName || user.userName.trim() === "") {
      if (process.env.NODE_ENV === 'development') {
      console.log("닉네임이 설정되지 않은 사용자 - SSE 연결 불가");
    }
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
      console.log("배치 처리할 알림이 없음 - 건너뛰기")
      return
    }

    try {
      console.log(`배치 처리 시작 - 읽음: ${readIds.length}개, 삭제: ${deleteIds.length}개`)
      
      const response = await notificationApi.updateNotifications({
        readIds: readIds.length > 0 ? readIds : undefined,
        deletedIds: deleteIds.length > 0 ? deleteIds : undefined,
      })

      if (response.success) {
        console.log("배치 처리 완료")
        // 처리 완료된 ID들 제거
        setPendingReadIds(new Set())
        setPendingDeleteIds(new Set())
      } else {
        console.error("배치 처리 실패:", response.error)
      }
    } catch (error) {
      console.error("배치 처리 중 오류:", error)
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

      if (process.env.NODE_ENV === 'development') {
        console.log("알림 배치 처리 타이머 시작 (5분 간격)");
      }

      return () => {
        if (batchTimerRef.current) {
          clearInterval(batchTimerRef.current)
          batchTimerRef.current = null
          if (process.env.NODE_ENV === 'development') {
            console.log("알림 배치 처리 타이머 정리");
          }
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
      console.log("알림 목록을 조회합니다...")
      const response = await notificationApi.getNotifications()
      console.log("알림 조회 응답:", response)
      
      if (response.success && response.data) {
        setNotifications(response.data)
        setUnreadCount(response.data.filter((n) => !n.read).length)
        console.log(`알림 ${response.data.length}개 조회됨 (읽지 않음: ${response.data.filter((n) => !n.read).length}개)`)
      }
    } catch (error) {
      console.error("알림 조회 실패:", error)
    } finally {
      setIsLoading(false)
    }
  }

  // SSE 연결 함수 (수동 트리거용)
  const connectSSE = useCallback(() => {
    if (!canConnectSSE()) {
      if (process.env.NODE_ENV === 'development') {
      console.log("SSE 연결 조건 미충족 - 연결 건너뛰기");
    }
      return
    }

    if (isSSEConnected) {
      if (process.env.NODE_ENV === 'development') {
        console.log("이미 SSE가 연결되어 있습니다.");
      }
      return
    }

    if (process.env.NODE_ENV === 'development') {
      console.log(`SSE 연결을 시작합니다 (사용자: ${user?.userName})...`);
    }
    
    // 기존 리스너 제거
    sseManager.removeEventListener("notification")
    
    // 새 알림 수신 리스너 등록
    sseManager.addEventListener("notification", async (data) => {
      if (process.env.NODE_ENV === 'development') {
        console.log("새 알림 수신:", data);
      }
      
      // 임시 알림을 즉시 표시 (사용자 경험 향상)
      const tempNotification: Notification = {
        id: data.id,
        data: data.data,
        url: data.url,
        type: data.type,
        createdAt: data.createdAt,
        read: false,
      }

      setNotifications((prev) => {
        console.log("임시 알림 목록 업데이트:", tempNotification)
        return [tempNotification, ...prev]
      })
      setUnreadCount((prev) => prev + 1)

      // 서버에서 최신 알림 목록을 다시 조회하여 정확한 데이터로 업데이트
      try {
        if (process.env.NODE_ENV === 'development') {
        console.log("SSE 알림 수신 후 서버에서 최신 알림 목록 조회...");
      }
        const response = await notificationApi.getNotifications()
        if (response.success && response.data) {
          setNotifications(response.data)
          setUnreadCount(response.data.filter((n) => !n.read).length)
          if (process.env.NODE_ENV === 'development') {
        console.log("서버에서 최신 알림 목록 업데이트 완료");
      }
        }
      } catch (error) {
        console.error("SSE 알림 수신 후 알림 목록 조회 실패:", error)
      }

      // 브라우저 알림 표시 (권한이 있는 경우)
      if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "granted") {
        new Notification(data.data, {
          body: data.url,
          icon: "/favicon.ico",
        })
      }
    })

    // SSE 연결
    sseManager.connect()

    // 연결 상태 모니터링
    const checkConnection = () => {
      const state = sseManager.getConnectionState()
      const connected = sseManager.isConnected()
      
      if (process.env.NODE_ENV === 'development') {
      console.log(`SSE 연결 상태: ${state}, 연결됨: ${connected}`);
    }
      setConnectionState(state)
      setIsSSEConnected(connected)
      
      if (!connected && canConnectSSE()) {
        // 연결이 끊어진 경우 3초 후 재연결 시도
        setTimeout(() => {
          if (canConnectSSE() && !sseManager.isConnected()) {
            console.log("SSE 재연결 시도...")
            connectSSE()
          }
        }, 3000)
      }
    }

    // 초기 연결 상태 확인
    setTimeout(checkConnection, 1000)
    
    // 주기적 연결 상태 확인 (30초마다)
    const intervalId = setInterval(checkConnection, 30000)
    
    // cleanup 함수 반환
    return () => {
      clearInterval(intervalId)
    }
  }, [canConnectSSE, isSSEConnected, user])

  // SSE 연결 해제 함수
  const disconnectSSE = useCallback(() => {
    console.log("SSE 연결을 해제합니다...")
    sseManager.removeEventListener("notification")
    sseManager.disconnect()
    setIsSSEConnected(false)
    setConnectionState("DISCONNECTED")
  }, [])

  // 인증 상태 및 사용자 정보가 변경될 때 SSE 연결 관리
  useEffect(() => {
    if (canConnectSSE()) {
      if (process.env.NODE_ENV === 'development') {
      console.log(`사용자 인증 완료 (${user?.userName}) - SSE 연결 준비`);
    }
      // 인증된 경우 자동으로 SSE 연결
      const cleanup = connectSSE()
      return cleanup
    } else {
      if (isAuthenticated && user && (!user.userName || user.userName.trim() === "")) {
        console.log("닉네임 미설정 사용자 - SSE 연결 대기")
      } else {
        console.log("사용자 인증 해제됨 - SSE 연결 해제")
      }
      disconnectSSE()
    }
  }, [canConnectSSE, connectSSE, disconnectSSE])

  // 컴포넌트 언마운트 시 SSE 연결 해제
  useEffect(() => {
    return () => {
      if (isSSEConnected) {
        disconnectSSE()
      }
    }
  }, [isSSEConnected, disconnectSSE])

  // 개별 알림 읽음 처리 (배치에 추가)
  const markAsRead = useCallback(async (notificationId: number) => {
    console.log(`알림 ${notificationId} 읽음 처리 - 배치에 추가`)
    
    // UI 즉시 업데이트
    setNotifications((prev) =>
      prev.map((notification) =>
        notification.id === notificationId ? { ...notification, read: true } : notification,
      ),
    )
    setUnreadCount((prev) => Math.max(0, prev - 1))
    
    // 배치에 추가
    setPendingReadIds((prev) => new Set([...prev, notificationId]))
    
    console.log(`알림 ${notificationId} 배치 추가 완료 (다음 배치 처리 시 전송)`)
  }, [])

  // 개별 알림 삭제 (배치에 추가)
  const deleteNotification = useCallback(async (notificationId: number) => {
    console.log(`알림 ${notificationId} 삭제 - 배치에 추가`)
    
    const notification = notifications.find((n) => n.id === notificationId)
    
    // UI 즉시 업데이트
    setNotifications((prev) => prev.filter((n) => n.id !== notificationId))
    if (notification && !notification.read) {
      setUnreadCount((prev) => Math.max(0, prev - 1))
    }
    
    // 배치에 추가
    setPendingDeleteIds((prev) => new Set([...prev, notificationId]))
    
    console.log(`알림 ${notificationId} 배치 추가 완료 (다음 배치 처리 시 전송)`)
  }, [notifications])

  // 모든 알림 읽음 처리 (즉시 실행)
  const markAllAsRead = async () => {
    try {
      const unreadIds = notifications.filter((n) => !n.read).map((n) => n.id)
      if (unreadIds.length === 0) return

      console.log(`모든 알림 읽음 처리 - 즉시 실행 (${unreadIds.length}개)`)
      const response = await notificationApi.updateNotifications({
        readIds: unreadIds,
      })
      if (response.success) {
        setNotifications((prev) => prev.map((notification) => ({ ...notification, read: true })))
        setUnreadCount(0)
        
        // 배치에서 해당 ID들 제거 (이미 처리됨)
        setPendingReadIds((prev) => {
          const newSet = new Set(prev)
          unreadIds.forEach(id => newSet.delete(id))
          return newSet
        })
        
        console.log("모든 알림 읽음 처리 완료")
      }
    } catch (error) {
      console.error("모든 알림 읽음 처리 실패:", error)
    }
  }

  // 모든 알림 삭제 (즉시 실행)
  const deleteAllNotifications = async () => {
    try {
      const allIds = notifications.map((n) => n.id)
      if (allIds.length === 0) return

      console.log(`모든 알림 삭제 - 즉시 실행 (${allIds.length}개)`)
      const response = await notificationApi.updateNotifications({
        deletedIds: allIds,
      })
      if (response.success) {
        setNotifications([])
        setUnreadCount(0)
        
        // 배치에서 해당 ID들 제거 (이미 처리됨)
        setPendingDeleteIds((prev) => {
          const newSet = new Set(prev)
          allIds.forEach(id => newSet.delete(id))
          return newSet
        })
        
        console.log("모든 알림 삭제 완료")
      }
    } catch (error) {
      console.error("모든 알림 삭제 실패:", error)
    }
  }

  // 브라우저 알림 권한 요청
  const requestNotificationPermission = async () => {
    if (typeof window !== "undefined" && "Notification" in window && Notification.permission === "default") {
      const permission = await Notification.requestPermission()
      if (process.env.NODE_ENV === 'development') {
        console.log("브라우저 알림 권한:", permission);
      }
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
    connectSSE,
    disconnectSSE,
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
