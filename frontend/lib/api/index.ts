// Client
export { apiClient, csrfDebugUtils } from './client'

// Auth
export { authQuery } from './auth/query'
export { authCommand } from './auth/command'

// User
export { userQuery } from './user/query'
export { userCommand } from './user/command'

// Post
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

// Utils exports from original api.ts
import { DecoType, decoTypeMap } from '@/types/domains/paper'

export function getDecoInfo(decoType: DecoType | string) {
  const decoInfo = decoTypeMap[decoType as DecoType]
  return decoInfo || { name: "알 수 없음", color: "from-gray-100 to-gray-200" }
}

export { decoTypeMap } from '@/types/domains/paper'

// Re-export types for convenience
export type { 
  ApiResponse, 
  PageResponse,
  ErrorResponse 
} from '@/types/common'

export type {
  AuthResponse,
  SocialLoginRequest,
  SignUpRequest,
  LoginStatus
} from '@/types/domains/auth'

export type {
  Member,
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