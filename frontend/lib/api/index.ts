// Client
export { apiClient, csrfDebugUtils } from './client'

// Auth
export { authQuery } from './auth/query'
export { authCommand } from './auth/command'
import { authQuery } from './auth/query'
import { authCommand } from './auth/command'

// User
export { userQuery } from './user/query'
export { userCommand } from './user/command'
import { userQuery } from './user/query'
import { userCommand } from './user/command'

// Post (Board)
export { postQuery } from './post/query'
export { postCommand } from './post/command'
import { postQuery } from './post/query'
import { postCommand } from './post/command'

// Comment
export { commentQuery } from './comment/query'
export { commentCommand } from './comment/command'
import { commentQuery } from './comment/query'
import { commentCommand } from './comment/command'

// Paper (Rolling Paper)
export { paperQuery } from './paper/query'
export { paperCommand } from './paper/command'
import { paperQuery } from './paper/query'
import { paperCommand } from './paper/command'

// Notification
export { notificationQuery } from './notification/query'
export { notificationCommand } from './notification/command'
import { notificationQuery } from './notification/query'
import { notificationCommand } from './notification/command'

// Admin
export { adminQuery } from './admin/query'
export { adminCommand } from './admin/command'
import { adminQuery } from './admin/query'
import { adminCommand } from './admin/command'

// SSE
export { SSEManager, sseManager } from './sse'

// Legacy compatibility exports
export const authApi = {
  ...authQuery,
  ...authCommand,
  // Legacy method name mappings
  deleteAccount: authCommand.withdraw,
}

export const userApi = {
  ...userQuery,
  ...userCommand,
  // Legacy method name mappings
  getUserSettings: userQuery.getSettings,
  updateUserSettings: userCommand.updateSettings,
}

export const boardQueryApi = postQuery
export const boardCommandApi = postCommand
export const boardApi = {
  ...postQuery,
  ...postCommand,
  // Legacy method name mappings
  getPosts: postQuery.getAll,
  getPost: postQuery.getById,
  searchPosts: postQuery.search,
  getPopularPosts: postQuery.getPopular,
  getLegendPosts: postQuery.getLegend,
  updatePost: postCommand.update,
}

export const commentQueryApi = commentQuery
export const commentCommandApi = commentCommand
export const commentApi = {
  ...commentQuery,
  ...commentCommand,
  // Legacy method name mappings
  createComment: commentCommand.create,
  updateComment: commentCommand.update,
  deleteComment: commentCommand.delete,
  likeComment: commentCommand.like,
  getComments: commentQuery.getByPostId,
  getPopularComments: commentQuery.getPopular,
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
  // Legacy method name mappings
  updateNotifications: notificationQuery.getAll,
}

export const adminApi = {
  ...adminQuery,
  ...adminCommand,
}

// Utils exports from original api.ts
import { DecoType, decoTypeMap } from '@/types/domains/paper'

export function getDecoInfo(decoType: DecoType) {
  const decoInfo = decoTypeMap[decoType]
  return decoInfo?.name || "알 수 없음"
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