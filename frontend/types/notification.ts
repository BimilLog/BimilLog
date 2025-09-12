// Notification-related type definitions

// 알림 타입 - v2 백엔드 NotificationDTO 호환
export interface Notification {
  id: number
  content: string  // v2: data → content
  url: string
  notificationType: "PAPER" | "COMMENT" | "POST_FEATURED" | "INITIATE" | "ADMIN"  // v2: type → notificationType, updated enum values
  createdAt: string
  isRead: boolean  // v2: read → isRead
}