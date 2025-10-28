// Notification-related type definitions

// 알림 타입 - v2 백엔드 NotificationDTO 호환
export interface Notification {
  id: number
  content: string  // v2: data → content
  url: string
  // 알림 유형별 의미:
  // PAPER: 내 롤링페이퍼에 새 메시지가 작성됨
  // COMMENT: 내가 작성한 게시글/댓글에 댓글이 달림
  // POST_FEATURED: 내가 작성한 게시글이 인기글/레전드글로 선정됨
  // INITIATE: 서비스 초기 안내 메시지 (회원가입 축하 등)
  // ADMIN: 관리자가 보내는 공지사항 또는 경고 메시지
  notificationType: "PAPER" | "COMMENT" | "POST_FEATURED" | "INITIATE" | "ADMIN"  // v2: type → notificationType, updated enum values
  createdAt: string
  read: boolean    // v2: Jackson이 isRead를 read로 직렬화
}