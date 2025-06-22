"use client"

import { useState, useEffect } from "react"
import { notificationApi, sseManager, type Notification } from "@/lib/api"
import { useAuth } from "./useAuth"

export function useNotifications() {
  const { isAuthenticated } = useAuth()
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isLoading, setIsLoading] = useState(false)

  // 알림 목록 조회
  const fetchNotifications = async () => {
    if (!isAuthenticated) return

    setIsLoading(true)
    try {
      const response = await notificationApi.getNotifications()
      if (response.success && response.data) {
        setNotifications(response.data)
        setUnreadCount(response.data.filter((n) => !n.read).length)
      }
    } catch (error) {
      console.error("Failed to fetch notifications:", error)
    } finally {
      setIsLoading(false)
    }
  }

  // 알림 읽음 처리
  const markAsRead = async (notificationId: number) => {
    try {
      const response = await notificationApi.updateNotifications({
        readIds: [notificationId],
      })
      if (response.success) {
        setNotifications((prev) =>
          prev.map((notification) =>
            notification.id === notificationId ? { ...notification, read: true } : notification,
          ),
        )
        setUnreadCount((prev) => Math.max(0, prev - 1))
      }
    } catch (error) {
      console.error("Failed to mark notification as read:", error)
    }
  }

  // 알림 삭제
  const deleteNotification = async (notificationId: number) => {
    try {
      const response = await notificationApi.updateNotifications({
        deletedIds: [notificationId],
      })
      if (response.success) {
        const notification = notifications.find((n) => n.id === notificationId)
        setNotifications((prev) => prev.filter((n) => n.id !== notificationId))
        if (notification && !notification.read) {
          setUnreadCount((prev) => Math.max(0, prev - 1))
        }
      }
    } catch (error) {
      console.error("Failed to delete notification:", error)
    }
  }

  // 모든 알림 읽음 처리
  const markAllAsRead = async () => {
    try {
      const unreadIds = notifications.filter((n) => !n.read).map((n) => n.id)
      if (unreadIds.length === 0) return

      const response = await notificationApi.updateNotifications({
        readIds: unreadIds,
      })
      if (response.success) {
        setNotifications((prev) => prev.map((notification) => ({ ...notification, read: true })))
        setUnreadCount(0)
      }
    } catch (error) {
      console.error("Failed to mark all notifications as read:", error)
    }
  }

  // SSE 연결 및 실시간 알림 수신
  useEffect(() => {
    if (!isAuthenticated) return

    // 초기 알림 목록 조회
    fetchNotifications()

    // SSE 연결
    sseManager.connect()

    // 새 알림 수신 리스너
    sseManager.addEventListener("notification", (data) => {
      const newNotification: Notification = {
        id: data.id,
        data: data.data,
        url: data.url,
        type: data.type,
        createdAt: data.createdAt,
        read: false,
      }

      setNotifications((prev) => [newNotification, ...prev])
      setUnreadCount((prev) => prev + 1)

      // 브라우저 알림 표시 (권한이 있는 경우)
      if (Notification.permission === "granted") {
        new Notification(data.data, {
          body: data.url,
          icon: "/favicon.ico",
        })
      }
    })

    return () => {
      sseManager.disconnect()
    }
  }, [isAuthenticated])

  // 브라우저 알림 권한 요청
  const requestNotificationPermission = async () => {
    if ("Notification" in window && Notification.permission === "default") {
      await Notification.requestPermission()
    }
  }

  return {
    notifications,
    unreadCount,
    isLoading,
    fetchNotifications,
    markAsRead,
    deleteNotification,
    markAllAsRead,
    requestNotificationPermission,
  }
}
