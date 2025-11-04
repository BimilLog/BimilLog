/**
 * TanStack Query 키 팩토리
 * 일관된 쿼리 키 관리를 위한 중앙화된 키 생성 시스템
 */

export const queryKeys = {
  // Auth
  auth: {
    all: ['auth'] as const,
    me: () => [...queryKeys.auth.all, 'me'] as const,
  },

  // User
  user: {
    all: ['user'] as const,
    detail: (userId: number) => [...queryKeys.user.all, 'detail', userId] as const,
    list: (page?: number, size?: number) => [...queryKeys.user.all, 'list', page, size] as const,
    posts: (page?: number, pageSize?: number) => [...queryKeys.user.all, 'posts', page, pageSize] as const,
    comments: (page?: number, pageSize?: number) => [...queryKeys.user.all, 'comments', page, pageSize] as const,
    likePosts: (page?: number, pageSize?: number) => [...queryKeys.user.all, 'likePosts', page, pageSize] as const,
    likeComments: (page?: number, pageSize?: number) => [...queryKeys.user.all, 'likeComments', page, pageSize] as const,
    settings: () => [...queryKeys.user.all, 'settings'] as const,
    friendList: () => [...queryKeys.user.all, 'friendList'] as const,
  },

  // Post
  post: {
    all: ['post'] as const,
    lists: () => [...queryKeys.post.all, 'list'] as const,
    list: (filters?: Record<string, string | number | boolean | null | undefined>) => [...queryKeys.post.lists(), filters] as const,
    details: () => [...queryKeys.post.all, 'detail'] as const,
    detail: (postId: number) => [...queryKeys.post.details(), postId] as const,
    search: (query: string, page?: number) => [...queryKeys.post.all, 'search', query, page] as const,
    realtimePopular: () => [...queryKeys.post.all, 'popular', 'realtime'] as const,
    weeklyPopular: () => [...queryKeys.post.all, 'popular', 'weekly'] as const,
    legend: (filters?: Record<string, string | number | boolean | null | undefined>) => [...queryKeys.post.all, 'legend', filters] as const,
    notices: () => [...queryKeys.post.all, 'notices'] as const,
  },

  // Comment
  comment: {
    all: ['comment'] as const,
    list: (postId: number) => [...queryKeys.comment.all, 'list', postId] as const,
    popular: (postId: number) => [...queryKeys.comment.all, 'popular', postId] as const,
  },

  // Paper (Rolling Paper)
  paper: {
    all: ['paper'] as const,
    my: ['paper', 'my'] as const,
    detail: (userName: string) => [...queryKeys.paper.all, 'detail', userName] as const,
    popular: (page?: number, size?: number) => [...queryKeys.paper.all, 'popular', page, size] as const,
  },

  // Notification
  notification: {
    all: ['notification'] as const,
    list: () => [...queryKeys.notification.all, 'list'] as const,
  },

  // Admin
  admin: {
    all: ['admin'] as const,
    reports: (page?: number) => [...queryKeys.admin.all, 'reports', page] as const,
  },
} as const;

/**
 * Mutation 키 팩토리
 */
export const mutationKeys = {
  // Auth
  auth: {
    login: ['auth', 'login'] as const,
    signup: ['auth', 'signup'] as const,
    logout: ['auth', 'logout'] as const,
  },

  // User
  user: {
    updateUsername: ['user', 'updateUsername'] as const,
    updateSettings: ['user', 'updateSettings'] as const,
    report: ['user', 'report'] as const,
    withdraw: ['user', 'withdraw'] as const,
  },

  // Post
  post: {
    create: ['post', 'create'] as const,
    update: ['post', 'update'] as const,
    delete: ['post', 'delete'] as const,
    like: ['post', 'like'] as const,
    toggleNotice: ['post', 'toggleNotice'] as const,
  },

  // Comment
  comment: {
    write: ['comment', 'write'] as const,
    update: ['comment', 'update'] as const,
    delete: ['comment', 'delete'] as const,
    like: ['comment', 'like'] as const,
  },

  // Paper
  paper: {
    write: ['paper', 'write'] as const,
    delete: ['paper', 'delete'] as const,
  },

  // Notification
  notification: {
    markAsRead: ['notification', 'markAsRead'] as const,
    delete: ['notification', 'delete'] as const,
    markAllAsRead: ['notification', 'markAllAsRead'] as const,
    deleteAll: ['notification', 'deleteAll'] as const,
  },

  // Admin
  admin: {
    ban: ['admin', 'ban'] as const,
    withdraw: ['admin', 'withdraw'] as const,
  },
} as const;