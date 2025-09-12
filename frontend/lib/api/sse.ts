import { Notification } from '@/types/domains/notification'

export class SSEManager {
  private eventSource: EventSource | null = null
  private listeners: Map<string, (data: Notification) => void> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  
  connect() {
    if (this.eventSource) {
      this.disconnect()
    }
    
    try {
      const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
      const sseUrl = `${API_BASE_URL}/api/notification/connect`
      
      if (process.env.NODE_ENV === 'development') {
        console.log("SSE 연결 시도:", sseUrl)
      }
      
      this.eventSource = new EventSource(sseUrl, {
        withCredentials: true,
      })
      
      this.eventSource.onopen = (event) => {
        if (process.env.NODE_ENV === 'development') {
          console.log("SSE connection opened successfully", event)
        }
        this.reconnectAttempts = 0
      }
      
      const handleSSEEvent = (event: MessageEvent) => {
        if (process.env.NODE_ENV === 'development') {
          console.log(`SSE ${event.type} event received:`, event.data)
        }
        
        try {
          const data = JSON.parse(event.data)
          
          if (process.env.NODE_ENV === 'development') {
            console.log("Parsed SSE data:", data)
          }
          
          const notificationData = {
            id: data.id || Date.now() + Math.random(),
            content: data.message || data.content || data.data || "새로운 알림",
            url: data.url || "",
            notificationType: (event.type || "ADMIN") as "PAPER" | "COMMENT" | "POST_FEATURED" | "INITIATE" | "ADMIN",
            createdAt: data.createdAt || new Date().toISOString(),
            isRead: false
          }
          
          if (event.type === "INITIATE") {
            if (process.env.NODE_ENV === 'development') {
              console.log("SSE 연결 초기화 완료:", data.message)
            }
            return
          }
          
          const listener = this.listeners.get("notification")
          if (listener) {
            if (process.env.NODE_ENV === 'development') {
              console.log("SSE 알림 리스너 실행:", notificationData)
            }
            listener(notificationData)
          } else {
            console.warn("No listener found for SSE message")
          }
        } catch (error) {
          console.error(`Failed to parse SSE ${event.type} event:`, error, "Raw data:", event.data)
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
        console.error("SSE connection error:", error)
        console.log("EventSource readyState:", this.eventSource?.readyState)
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          console.log(`SSE 재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)
          
          setTimeout(() => {
            this.connect()
          }, this.reconnectDelay * this.reconnectAttempts)
        } else {
          console.error("SSE 재연결 시도 초과됨")
        }
      }
    } catch (error) {
      console.error("SSE 연결 실패:", error)
    }
  }
  
  disconnect() {
    if (this.eventSource) {
      if (process.env.NODE_ENV === 'development') {
        console.log("SSE 연결을 종료합니다.")
      }
      this.eventSource.close()
      this.eventSource = null
      this.reconnectAttempts = 0
    }
  }
  
  addEventListener(type: string, listener: (data: Notification) => void) {
    if (process.env.NODE_ENV === 'development') {
      console.log(`SSE 리스너 등록: ${type}`)
    }
    this.listeners.set(type, listener)
  }
  
  removeEventListener(type: string) {
    if (process.env.NODE_ENV === 'development') {
      console.log(`SSE 리스너 제거: ${type}`)
    }
    this.listeners.delete(type)
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