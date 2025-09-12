// Client
export { apiClient, csrfDebugUtils } from './client'

// Auth
export { authQuery } from './auth/query'
export { authCommand } from './auth/command'

// User
export { userQuery } from './user/query'
export { userCommand } from './user/command'

// Post (Board)
export { postQuery } from './post/query'
export { postCommand } from './post/command'

// Comment
export { commentQuery } from './comment/query'
export { commentCommand } from './comment/command'

// Paper (Rolling Paper)
export { paperQuery } from './paper/query'
export { paperCommand } from './paper/command'

// Notification
export { notificationQuery } from './notification/query'
export { notificationCommand } from './notification/command'

// Admin
export { adminQuery } from './admin/query'
export { adminCommand } from './admin/command'

// SSE
export { SSEManager, sseManager } from './sse'

// Legacy compatibility exports
export const authApi = {
  ...authQuery,
  ...authCommand,
}

export const userApi = {
  ...userQuery,
  ...userCommand,
}

export const boardQueryApi = postQuery
export const boardCommandApi = postCommand
export const boardApi = {
  ...postQuery,
  ...postCommand,
}

export const commentQueryApi = commentQuery
export const commentCommandApi = commentCommand
export const commentApi = {
  ...commentQuery,
  ...commentCommand,
}

export const rollingPaperApi = {
  getMyRollingPaper: paperQuery.getMy,
  getRollingPaper: paperQuery.getByUserName,
  createMessage: paperCommand.createMessage,
  deleteMessage: paperCommand.deleteMessage,
}

export const notificationApi = {
  getNotifications: notificationQuery.getAll,
  subscribeToNotifications: notificationQuery.getSSEUrl,
  markAsRead: notificationCommand.markAsRead,
  markAllAsRead: notificationCommand.markAllAsRead,
  deleteNotification: notificationCommand.delete,
}

export const adminApi = {
  ...adminQuery,
  ...adminCommand,
}

// Utils exports from original api.ts
import { DecoType } from '@/types/domains/paper'

export function getDecoInfo(decoType: DecoType) {
  const decoTypeMap: Record<DecoType, string> = {
    STICKER1: "스티커1",
    STICKER2: "스티커2",
    STICKER3: "스티커3",
    STICKER4: "스티커4",
    STICKER5: "스티커5",
    STICKER6: "스티커6",
    POSTIT1: "포스트잇1",
    POSTIT2: "포스트잇2",
    POSTIT3: "포스트잇3",
    POSTIT4: "포스트잇4",
    POSTIT5: "포스트잇5",
    POSTIT6: "포스트잇6",
  }
  return decoTypeMap[decoType] || "알 수 없음"
}

export { decoTypeMap } from '@/types/domains/paper'

// Re-export types for convenience
export type { 
  ApiResponse, 
  PageResponse,
  ErrorResponse 
} from '@/types/api/common'

export type {
  AuthResponse,
  SocialLoginRequest,
  SignUpRequest,
  LoginStatus
} from '@/types/domains/auth'

export type {
  User,
  Setting,
  KakaoFriend,
  KakaoFriendList
} from '@/types/domains/user'

export type {
  Post,
  SimplePost
} from '@/types/domains/post'

export type {
  Comment,
  SimpleComment
} from '@/types/domains/comment'

export type {
  DecoType,
  RollingPaperMessage,
  VisitMessage
} from '@/types/domains/paper'

export type {
  Notification
} from '@/types/domains/notification'

export type {
  Report
} from '@/types/domains/admin'