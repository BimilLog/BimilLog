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
    search: (keyword: string, page?: number, size?: number) => [...queryKeys.user.all, 'search', keyword, page, size] as const,
    settings: () => [...queryKeys.user.all, 'settings'] as const,
    friendList: () => [...queryKeys.user.all, 'friendList'] as const,
  },

  // MyPage
  mypage: {
    all: ['mypage'] as const,
    info: (page?: number, size?: number) => [...queryKeys.mypage.all, 'info', { page, size }] as const,
  },

  // Post
  post: {
    all: ['post'] as const,
    lists: () => [...queryKeys.post.all, 'list'] as const,
    list: (filters?: Record<string, string | number | boolean | null | undefined>) => [...queryKeys.post.lists(), filters] as const,
    infiniteList: () => [...queryKeys.post.all, 'infinite'] as const, // 커서 기반 무한 스크롤용
    details: () => [...queryKeys.post.all, 'detail'] as const,
    detail: (postId: number) => [...queryKeys.post.details(), postId] as const,
    // F-BUG-1 (round-6): searchType 누락 시 type 토글 시 stale 캐시 hit → race condition.
    // type 을 키에 포함시켜 type 별 캐시 분리.
    search: (query: string, type: string, page?: number) =>
      [...queryKeys.post.all, 'search', query, type, page] as const,
    realtimePopular: (params?: { page?: number; size?: number }) => [...queryKeys.post.all, 'popular', 'realtime', params] as const,
    weeklyPopular: (params?: { page?: number; size?: number }) => [...queryKeys.post.all, 'popular', 'weekly', params] as const,
    legend: (filters?: Record<string, string | number | boolean | null | undefined>) => [...queryKeys.post.all, 'legend', filters] as const,
    notices: (params?: { page?: number; size?: number }) => [...queryKeys.post.all, 'notices', params] as const,
  },

  // Comment
  comment: {
    all: ['comment'] as const,
    list: (postId: number) => [...queryKeys.comment.all, 'list', postId] as const,
  },

  // Paper (Rolling Paper)
  paper: {
    all: ['paper'] as const,
    my: ['paper', 'my'] as const,
    detail: (userName: string) => [...queryKeys.paper.all, 'detail', userName] as const,
    popular: (size?: number) => [...queryKeys.paper.all, 'popular', size] as const,
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

  // Friend
  // 라운드 9: lists() 가 page/size 무시 → useMyFriends(0,20) 와
  // useFriendRelationshipCheck 의 useMyFriends(0,100) 가 같은 캐시를 덮어써
  // 친구 100명 초과 case 누락(B-009-B 부분 완화). page/size 를 키에 포함시켜
  // 페이지네이션·관계 체크 호출이 분리된 캐시 슬롯을 갖도록 한다.
  friend: {
    all: ['friend'] as const,
    listsAll: () => [...queryKeys.friend.all, 'list'] as const,
    lists: (page: number = 0, size: number = 20) =>
      [...queryKeys.friend.all, 'list', page, size] as const,
    sentRequests: (page: number, size: number) => [...queryKeys.friend.all, 'sent', page, size] as const,
    receivedRequests: (page: number, size: number) => [...queryKeys.friend.all, 'received', page, size] as const,
    recommended: (page: number, size: number) => [...queryKeys.friend.all, 'recommended', page, size] as const,
  },

  // Blacklist
  // 라운드 15 F-15-BUG-1/2: useBlacklistQueries 와 invalidate 키가 어긋나 옵티미스틱 적용 후
  // invalidate 가 실제 캐시 슬롯을 건드리지 못하던 회귀를 수정. lists() 가 page/size 무시하는
  // 부모 키이고, list(p,s) 는 페이지별 캐시 슬롯.
  blacklist: {
    all: ['blacklist'] as const,
    lists: () => [...queryKeys.blacklist.all, 'list'] as const,
    list: (page: number = 0, size: number = 20) =>
      [...queryKeys.blacklist.all, 'list', page, size] as const,
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

  // Friend
  friend: {
    sendRequest: ['friend', 'sendRequest'] as const,
    cancelRequest: ['friend', 'cancelRequest'] as const,
    acceptRequest: ['friend', 'acceptRequest'] as const,
    rejectRequest: ['friend', 'rejectRequest'] as const,
    removeFriend: ['friend', 'removeFriend'] as const,
  },
} as const;