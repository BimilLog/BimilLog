const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "https://www.grow-farm.com"

// API 응답 타입 정의
export interface ApiResponse<T = any> {
  success: boolean
  data?: T
  message?: string
  error?: string
}

// 사용자 정보 타입
export interface User {
  userId: number
  userName: string
  kakaoId?: string
  createdAt?: string
}

// 롤링페이퍼 메시지 타입
export interface RollingPaperMessage {
  id: number
  userId: number
  decoType: string
  anonymity: string
  content: string
  width: number
  height: number
  createdAt?: string
  isDeleted?: boolean
}

// 방문용 메시지 타입 (공개 롤링페이퍼용)
export interface VisitMessage {
  id: number
  userId: number
  decoType: string
  width: number
  height: number
}

// 게시글 타입
export interface Post {
  postId: number
  userId: number
  userName: string
  title: string
  content: string
  views: number
  likes: number
  popularFlag?: "REALTIME" | "WEEKLY" | "LEGEND"
  createdAt: string
  userLike: boolean
  password?: number
  notice: boolean
}

// 간단한 게시글 타입 (목록용)
export interface SimplePost {
  postId: number
  userId: number
  userName: string
  title: string
  commentCount: number
  likes: number
  views: number
  createdAt: string
  popularFlag?: "REALTIME" | "WEEKLY" | "LEGEND"
  is_notice: boolean
}

// 댓글 타입
export interface Comment {
  id: number
  parentId?: number
  postId: number
  userName: string
  content: string
  popular: boolean
  deleted: boolean
  password?: number
  likes: number
  createdAt: string
  userLike: boolean
}

// 간단한 댓글 타입 (목록용)
export interface SimpleComment {
  id: number
  postId: number
  userName: string
  content: string
  likes: number
  userLike: boolean
  createdAt: string
}

// 알림 타입
export interface Notification {
  id: number
  data: string
  url: string
  type: "ADMIN" | "FARM" | "COMMENT" | "POST_FEATURED" | "COMMENT_FEATURED" | "INITIATE"
  createdAt: string
  read: boolean
}

// 설정 타입
export interface Setting {
  settingId: number
  messageNotification: boolean
  commentNotification: boolean
  postFeaturedNotification: boolean
}

// 페이지네이션 타입
export interface PageResponse<T> {
  totalPages: number
  totalElements: number
  size: number
  content: T[]
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

// HTTP 클라이언트 설정
class ApiClient {
  private baseURL: string

  constructor(baseURL: string) {
    this.baseURL = baseURL
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}`

    const defaultHeaders = {
      "Content-Type": "application/json",
    }

    const config: RequestInit = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers,
      },
      credentials: "include", // httpOnly 쿠키 자동 포함
    }

    try {
      const response = await fetch(url, config)

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()
      return {
        success: true,
        data,
      }
    } catch (error) {
      console.error("API request failed:", error)
      return {
        success: false,
        error: error instanceof Error ? error.message : "Unknown error",
      }
    }
  }

  // GET 요청
  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: "GET" })
  }

  // POST 요청
  async post<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  // PUT 요청
  async put<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    })
  }

  // DELETE 요청
  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: "DELETE" })
  }

  // PATCH 요청
  async patch<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    })
  }
}

// API 클라이언트 인스턴스
export const apiClient = new ApiClient(API_BASE_URL)

// 인증 관련 API
export const authApi = {
  // 카카오 로그인 URL 가져오기
  kakaoLogin: (code: string, fcmToken?: string) => {
    const queryParams = new URLSearchParams({ code })
    if (fcmToken) queryParams.append("fcmToken", fcmToken)
    return apiClient.get(`/auth/login?${queryParams.toString()}`)
  },

  // 현재 사용자 정보 조회 (httpOnly 쿠키 자동 포함)
  getCurrentUser: () => apiClient.post<User>("/auth/me"),

  // 로그아웃
  logout: () => apiClient.post("/auth/logout"),

  // 회원 탈퇴
  deleteAccount: () => apiClient.post("/auth/withdraw"),

  // 회원가입 (닉네임 설정)
  signUp: (userName: string, tempCookie: string) =>
    apiClient.get(`/auth/signUp?userName=${encodeURIComponent(userName)}&temp=${tempCookie}`),

  // 서버 상태 확인
  healthCheck: () => apiClient.get<string>("/auth/health"),
}

// 사용자 관련 API
export const userApi = {
  // 닉네임 변경
  updateUserName: (userName: string) => apiClient.post("/user/username", { userName }),

  // 사용자 설정 조회
  getUserSettings: () => apiClient.get<Setting>("/user/setting"),

  // 사용자 설정 수정
  updateUserSettings: (settings: Partial<Setting>) => apiClient.post("/user/setting", settings),

  // 건의사항 제출
  submitSuggestion: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    targetId?: number
    content: string
  }) => apiClient.post("/user/suggestion", report),

  // 카카오 친구 목록 조회
  getFriendList: (offset: number) => apiClient.post(`/user/friendlist?offset=${offset}`),

  // 사용자가 작성한 글 목록
  getUserPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/user/posts?page=${page}&size=${size}`),

  // 사용자가 작성한 댓글 목록
  getUserComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/user/comments?page=${page}&size=${size}`),

  // 사용자가 추천한 글 목록
  getUserLikedPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/user/likeposts?page=${page}&size=${size}`),

  // 사용자가 추천한 댓글 목록
  getUserLikedComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/user/likecomments?page=${page}&size=${size}`),
}

// 롤링페이퍼 관련 API
export const rollingPaperApi = {
  // 내 롤링페이퍼 조회
  getMyRollingPaper: () => apiClient.get<RollingPaperMessage[]>("/paper"),

  // 다른 사용자의 롤링페이퍼 조회
  getRollingPaper: (userName: string) => apiClient.get<VisitMessage[]>(`/paper/${encodeURIComponent(userName)}`),

  // 메시지 작성
  createMessage: (
    userName: string,
    message: {
      decoType: string
      anonymity: string
      content: string
      width: number
      height: number
    },
  ) => apiClient.post(`/paper/${encodeURIComponent(userName)}`, message),

  // 메시지 삭제 (소유자만)
  deleteMessage: (messageData: {
    id: number
    userId?: number
    decoType?: string
    anonymity?: string
    content?: string
    width?: number
    height?: number
  }) => apiClient.post("/paper/delete", messageData),
}

// 게시판 관련 API
export const boardApi = {
  // 게시글 목록 조회
  getPosts: (page = 0, size = 10) => apiClient.get<PageResponse<SimplePost>>(`/post?page=${page}&size=${size}`),

  // 게시글 상세 조회
  getPost: (postId: number) => apiClient.get<Post>(`/post/${postId}`),

  // 게시글 검색
  searchPosts: (type: "title" | "content" | "author", query: string, page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(
      `/post/search?type=${type}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    ),

  // 게시글 작성
  createPost: (post: {
    title: string
    content: string
    password?: number
  }) => apiClient.post<Post>("/post/write", post),

  // 게시글 수정
  updatePost: (post: Post) => apiClient.post("/post/update", post),

  // 게시글 삭제
  deletePost: (post: Post) => apiClient.post("/post/delete", post),

  // 게시글 추천/취소
  likePost: (post: Post) => apiClient.post("/post/like", post),

  // 실시간 인기글 조회
  getRealtimePosts: () => apiClient.get<SimplePost[]>("/post/realtime"),

  // 주간 인기글 조회
  getWeeklyPosts: () => apiClient.get<SimplePost[]>("/post/weekly"),

  // 레전드 인기글 조회
  getLegendPosts: () => apiClient.get<SimplePost[]>("/post/legend"),
}

// 댓글 관련 API
export const commentApi = {
  // 댓글 목록 조회
  getComments: (postId: number, page = 0) => apiClient.get<PageResponse<Comment>>(`/comment/${postId}?page=${page}`),

  // 인기 댓글 조회
  getPopularComments: (postId: number) => apiClient.get<Comment[]>(`/comment/${postId}/popular`),

  // 댓글 작성
  createComment: (comment: {
    postId: number
    userName: string
    content: string
    parentId?: number
    password?: number
  }) => apiClient.post("/comment/write", comment),

  // 댓글 수정
  updateComment: (comment: Comment) => apiClient.post("/comment/update", comment),

  // 댓글 삭제
  deleteComment: (comment: Comment) => apiClient.post("/comment/delete", comment),

  // 댓글 추천/취소
  likeComment: (comment: Comment) => apiClient.post("/comment/like", comment),
}

// 알림 관련 API
export const notificationApi = {
  // 알림 목록 조회
  getNotifications: () => apiClient.get<Notification[]>("/notification/list"),

  // 알림 읽음/삭제 처리
  updateNotifications: (data: {
    readIds?: number[]
    deletedIds?: number[]
  }) => apiClient.post("/notification/update", data),

  // SSE 알림 구독
  subscribeToNotifications: () => `${API_BASE_URL}/notification/subscribe`,
}

// 관리자 관련 API
export const adminApi = {
  // 신고 목록 조회
  getReports: (page = 0, size = 20, reportType?: string) => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() })
    if (reportType) params.append("reportType", reportType)
    return apiClient.get(`/api/admin/report?${params.toString()}`)
  },

  // 신고 상세 조회
  getReport: (reportId: number) => apiClient.get(`/api/admin/report/${reportId}`),

  // 사용자 차단
  banUser: (userId: number) => apiClient.post(`/api/admin/user/ban?userId=${userId}`),
}

// SSE 연결 관리
export class SSEManager {
  private eventSource: EventSource | null = null
  private listeners: Map<string, (data: any) => void> = new Map()

  connect() {
    if (this.eventSource) {
      this.disconnect()
    }

    this.eventSource = new EventSource(notificationApi.subscribeToNotifications(), {
      withCredentials: true, // httpOnly 쿠키 포함
    })

    this.eventSource.onopen = () => {
      console.log("SSE connection opened")
    }

    this.eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        const listener = this.listeners.get(data.type)
        if (listener) {
          listener(data)
        }
      } catch (error) {
        console.error("Failed to parse SSE message:", error)
      }
    }

    this.eventSource.onerror = (error) => {
      console.error("SSE connection error:", error)
    }
  }

  disconnect() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }

  addEventListener(type: string, listener: (data: any) => void) {
    this.listeners.set(type, listener)
  }

  removeEventListener(type: string) {
    this.listeners.delete(type)
  }
}

// SSE 매니저 인스턴스
export const sseManager = new SSEManager()

// 데코레이션 타입 매핑
export const decoTypeMap = {
  // 농작물
  POTATO: { name: "감자", color: "from-yellow-100 to-amber-100", emoji: "🥔" },
  CARROT: { name: "당근", color: "from-orange-100 to-red-100", emoji: "🥕" },
  CABBAGE: { name: "양배추", color: "from-green-100 to-emerald-100", emoji: "🥬" },
  TOMATO: { name: "토마토", color: "from-red-100 to-pink-100", emoji: "🍅" },

  // 과일
  STRAWBERRY: { name: "딸기", color: "from-pink-100 to-red-100", emoji: "🍓" },
  WATERMELON: { name: "수박", color: "from-green-100 to-red-100", emoji: "🍉" },
  PUMPKIN: { name: "호박", color: "from-orange-100 to-yellow-100", emoji: "🎃" },
  APPLE: { name: "사과", color: "from-red-100 to-pink-100", emoji: "🍎" },
  GRAPE: { name: "포도", color: "from-purple-100 to-violet-100", emoji: "🍇" },
  BANANA: { name: "바나나", color: "from-yellow-100 to-amber-100", emoji: "🍌" },

  // 몬스터
  GOBLIN: { name: "고블린", color: "from-green-100 to-emerald-100", emoji: "👹" },
  SLIME: { name: "슬라임", color: "from-blue-100 to-indigo-100", emoji: "🟢" },
  ORC: { name: "오크", color: "from-gray-100 to-slate-100", emoji: "👺" },
  DRAGON: { name: "드래곤", color: "from-red-100 to-orange-100", emoji: "🐉" },
  PHOENIX: { name: "피닉스", color: "from-orange-100 to-red-100", emoji: "🔥" },
  WEREWOLF: { name: "늑대인간", color: "from-gray-100 to-brown-100", emoji: "🐺" },
  ZOMBIE: { name: "좀비", color: "from-gray-100 to-green-100", emoji: "🧟" },
  KRAKEN: { name: "크라켄", color: "from-blue-100 to-purple-100", emoji: "🐙" },
  CYCLOPS: { name: "사이클롭스", color: "from-purple-100 to-indigo-100", emoji: "👁️" },
}

// 헬퍼 함수들
export const getDecoInfo = (decoType: string) => {
  return (
    decoTypeMap[decoType as keyof typeof decoTypeMap] || {
      name: "기본",
      color: "from-gray-100 to-slate-100",
      emoji: "📝",
    }
  )
}
