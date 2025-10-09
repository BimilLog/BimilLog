import { Notification } from '@/types/domains/notification'
import { logger } from '@/lib/utils'

export class SSEManager {
  private eventSource: EventSource | null = null
  private listeners: Map<string, (data: Notification) => void> = new Map()
  private statusListeners: Set<(status: 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting') => void> = new Set()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private baseReconnectDelay = 1000
  private maxReconnectDelay = 10000
  private wasConnected = false
  private networkListenersAdded = false

  constructor() {
    this.setupNetworkListeners()
  }

  private setupNetworkListeners() {
    if (typeof window === 'undefined' || this.networkListenersAdded) return

    // 네트워크 복구 감지 시 자동 재연결
    window.addEventListener('online', () => {
      logger.log('네트워크 복구 감지 - SSE 재연결 시도')
      this.reconnectAttempts = 0 // 재시도 카운터 초기화
      this.connect()
    })

    // 네트워크 끊김 감지
    window.addEventListener('offline', () => {
      logger.log('네트워크 연결 끊김 감지')
      this.notifyStatusListeners('disconnected')
    })

    this.networkListenersAdded = true
  }

  connect() {
    if (this.eventSource) {
      this.disconnect()
    }

    try {
      const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
      const sseUrl = `${API_BASE_URL}/api/notification/subscribe`

      logger.log("SSE 연결 시도:", sseUrl)
      this.notifyStatusListeners('connecting')

      this.eventSource = new EventSource(sseUrl, {
        withCredentials: true,
      })

      this.eventSource.onopen = (event) => {
        logger.log("SSE connection opened successfully", event)
        this.reconnectAttempts = 0

        // 재연결 성공인지 최초 연결 성공인지 구분
        if (this.wasConnected) {
          this.notifyStatusListeners('connected')
        }
        this.wasConnected = true
      }

      const handleSSEEvent = (event: MessageEvent) => {
        logger.log(`SSE ${event.type} event received:`, event.data)

        try {
          const data = JSON.parse(event.data)

          logger.log("Parsed SSE data:", data)

          if (event.type === "INITIATE") {
            logger.log("SSE 연결 초기화 완료:", data.message)
            // INITIATE 메시지는 연결 완료 시점
            this.notifyStatusListeners('connected')
            return
          }

          const notificationData = {
            id: data.id || Date.now() + Math.random(),
            content: data.message || data.content || data.data || "새로운 알림",
            url: data.url || "",
            notificationType: (event.type || "ADMIN") as "PAPER" | "COMMENT" | "POST_FEATURED" | "INITIATE" | "ADMIN",
            createdAt: data.createdAt || new Date().toISOString(),
            read: false
          }

          const listener = this.listeners.get("notification")
          if (listener) {
            logger.log("SSE 알림 리스너 실행:", notificationData)
            listener(notificationData)
          } else {
            logger.warn("No listener found for SSE message")
          }
        } catch (error) {
          logger.error(`Failed to parse SSE ${event.type} event:`, error, "Raw data:", event.data)
        }
      }

      this.eventSource.onmessage = handleSSEEvent

      const eventTypes = ["COMMENT", "PAPER", "POST_FEATURED", "ADMIN", "INITIATE"]
      eventTypes.forEach(type => {
        if (this.eventSource) {
          this.eventSource.addEventListener(type, handleSSEEvent)
        }
      })

      this.eventSource.onerror = (error) => {
        logger.error("SSE connection error:", error)
        logger.log("EventSource readyState:", this.eventSource?.readyState)

        this.notifyStatusListeners('error')

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++

          // Exponential backoff: 1s, 2s, 4s, 8s, 10s (max)
          const delay = Math.min(
            this.baseReconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
            this.maxReconnectDelay
          )

          logger.log(`SSE 재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts} (${delay}ms 후)`)
          this.notifyStatusListeners('reconnecting')

          setTimeout(() => {
            this.connect()
          }, delay)
        } else {
          logger.error("SSE 재연결 시도 초과됨")
          this.notifyStatusListeners('disconnected')
        }
      }
    } catch (error) {
      logger.error("SSE 연결 실패:", error)
      this.notifyStatusListeners('error')
    }
  }
  
  disconnect() {
    if (this.eventSource) {
      logger.log("SSE 연결을 종료합니다.")
      this.eventSource.close()
      this.eventSource = null
      this.reconnectAttempts = 0
      this.wasConnected = false
      this.notifyStatusListeners('disconnected')
    }
  }

  addEventListener(type: string, listener: (data: Notification) => void) {
    logger.log(`SSE 리스너 등록: ${type}`)
    this.listeners.set(type, listener)
  }

  removeEventListener(type: string) {
    logger.log(`SSE 리스너 제거: ${type}`)
    this.listeners.delete(type)
  }

  addStatusListener(listener: (status: 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting') => void) {
    this.statusListeners.add(listener)
  }

  removeStatusListener(listener: (status: 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting') => void) {
    this.statusListeners.delete(listener)
  }

  private notifyStatusListeners(status: 'connecting' | 'connected' | 'disconnected' | 'error' | 'reconnecting') {
    this.statusListeners.forEach(listener => {
      try {
        listener(status)
      } catch (error) {
        logger.error('Status listener error:', error)
      }
    })
  }

  isConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN
  }

  getConnectionState(): string {
    if (!this.eventSource) return "DISCONNECTED"

    switch (this.eventSource.readyState) {
      case EventSource.CONNECTING: return "CONNECTING"
      case EventSource.OPEN: return "OPEN"
      case EventSource.CLOSED: return "CLOSED"
      default: return "UNKNOWN"
    }
  }
}

export const sseManager = new SSEManager()