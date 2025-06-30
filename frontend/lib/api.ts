const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

// 쿠키를 가져오는 헬퍼 함수
function getCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    const popped = parts.pop();
    if (popped) {
      return popped.split(';').shift() || null;
    }
  }
  return null;
}

// API 응답 타입 정의
export interface ApiResponse<T = any> {
  success: boolean
  data?: T | null
  message?: string
  error?: string
}

// 사용자 정보 타입 (ClientDTO)
export interface User {
  userId: number
  userName: string
  kakaoId?: number
  settingDTO?: {
    settingId: number
    messageNotification: boolean
    commentNotification: boolean
    postFeaturedNotification: boolean
  }
  kakaoNickname?: string
  thumbnailImage?: string
  role?: "USER" | "ADMIN"
  tokenId?: number
  fcmTokenId?: number | null
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
  _notice: boolean
}

// 댓글 타입
export interface Comment {
  id: number
  parentId?: number
  postId: number
  userId?: number
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

// 카카오 친구 타입
export interface KakaoFriend {
  id: number
  uuid: string
  userName: string
  profile_nickname: string
  profile_thumbnail_image: string
}

// 카카오 친구 목록 타입
export interface KakaoFriendList {
  elements: KakaoFriend[]
  total_count: number
}

// 신고 타입
export interface Report {
  reportId: number
  reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
  userId: number
  targetId: number
  content: string
  createdAt?: string
  targetTitle?: string
  targetAuthor?: string
  reporterNickname?: string
  reason?: string
  status?: "pending" | "investigating" | "resolved" | "rejected"
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

    const csrfToken = getCookie("XSRF-TOKEN");

    const defaultHeaders: Record<string, string> = {
      "Content-Type": "application/json",
    };

    if (csrfToken) {
      defaultHeaders["X-XSRF-TOKEN"] = csrfToken;
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

      // 로그인이 반드시 필요한 API 엔드포인트 목록
      const requiredAuthEndpoints = [
        '/user',          // 마이페이지
        '/paper',         // 내 롤링페이퍼
        '/api/admin',     // 관리자
        '/post/like',     // 글 추천
        '/comment/like',  // 댓글 추천
        '/notification',  // 알림
        '/auth/logout',   // 로그아웃
        '/auth/withdraw'  // 회원탈퇴
      ];

      // 현재 요청이 반드시 인증이 필요한지 확인
      const requiresAuth = requiredAuthEndpoints.some(requiredUrl => {
        // 정확히 일치해야 하는 경우 (e.g. '/paper')
        if (requiredUrl === '/paper' || requiredUrl.endsWith('/like')) {
            return endpoint === requiredUrl;
        }
        // 접두사로 일치하는 경우 (e.g. '/user/', '/api/admin/')
        return endpoint.startsWith(requiredUrl);
      });

      if (!response.ok) {
        // 인증이 필수가 아닌 API에서 발생한 401 에러는 정상 흐름으로 간주
        if (!requiresAuth && response.status === 401) {
            return { success: true, data: null };
        }
        
        let errorMessage = `HTTP error! status: ${response.status}`;
        try {
          const errorData = await response.json();
          // 서버에서 보낸 에러 메시지가 있으면 사용
          if (errorData.message) {
            errorMessage = errorData.message;
          } else if (errorData.error) {
            errorMessage = errorData.error;
          }
          return {
            success: false,
            error: errorMessage,
          };
        } catch {
          // JSON 파싱 실패 시 텍스트로 시도
          try {
            const errorText = await response.text();
            if (errorText) {
              errorMessage = errorText;
            }
          } catch {
            // 텍스트도 실패하면 기본 메시지 사용
          }
          return {
            success: false,
            error: errorMessage,
          };
        }
      }
      
      let data;
      try {
        // 먼저 JSON 파싱 시도 (clone 사용)
        data = await response.clone().json();
      } catch {
        try {
          data = await response.text();
        } catch {
          data = null;
        }
      }

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
  getCurrentUser: () => apiClient.get<User>("/auth/me"),

  // 로그아웃
  logout: () => apiClient.post("/auth/logout"),

  // 회원 탈퇴
  deleteAccount: () => apiClient.post("/auth/withdraw"),

  // 회원가입 (닉네임 설정)
  signUp: (userName: string) => apiClient.get(`/auth/signUp?userName=${encodeURIComponent(userName)}`),

  // 서버 상태 확인
  healthCheck: () => apiClient.get<string>("/auth/health"),
}

// 사용자 관련 API
export const userApi = {
  // 닉네임 중복 확인
  checkUserName: (userName: string) => apiClient.get<boolean>(`/user/username/check?userName=${encodeURIComponent(userName)}`),

  // 닉네임 변경
  updateUserName: (userId: number, userName: string) => apiClient.post("/user/username", { userId, userName }),

  // 사용자 설정 조회
  getUserSettings: () => apiClient.get<Setting>("/user/setting"),

  // 사용자 설정 수정
  updateUserSettings: (settings: Setting) => apiClient.post("/user/setting", settings),

  // 건의사항 제출
  submitSuggestion: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    userId?: number
    targetId?: number
    content: string
  }) => apiClient.post("/user/suggestion", report),

  // 카카오 친구 목록 조회
  getFriendList: (offset: number) => apiClient.post<KakaoFriendList>(`/user/friendlist?offset=${offset}`),

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
  searchPosts: (type: "TITLE" | "TITLE_CONTENT" | "AUTHOR", query: string, page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(
      `/post/search?type=${type}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    ),

  // 게시글 작성
  createPost: (post: {
    userName: string | null
    title: string
    content: string
    password?: number
  }) => apiClient.post<Post>("/post/write", post),

  // 게시글 수정
  updatePost: (post: Post) => apiClient.post("/post/update", post),

  // 게시글 삭제
  deletePost: (postId: number, userId?: number, password?: string, content?: string, title?: string) => {
    const payload: any = { postId };
    if (userId !== undefined) payload.userId = userId;
    if (password) payload.password = Number(password);
    if (content) payload.content = content;
    if (title) payload.title = title;
    return apiClient.post("/post/delete", payload);
  },

  // 게시글 추천/취소
  likePost: (postId: number) => apiClient.post("/post/like", { postId }),

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
  updateComment: (commentId: number, data: { content: string; password?: string }) => {
    const payload = data.password 
      ? { id: commentId, content: data.content, password: Number(data.password) }
      : { id: commentId, content: data.content };
    return apiClient.post("/comment/update", payload);
  },

  // 댓글 삭제
  deleteComment: (commentId: number, userId?: number, password?: number, content?: string) => {
    const payload: any = { id: commentId };
    if (userId !== undefined) payload.userId = userId;
    if (password !== undefined) payload.password = Number(password);
    if (content) payload.content = content;
    return apiClient.post("/comment/delete", payload);
  },

  // 댓글 추천/취소
  likeComment: (commentId: number) => apiClient.post("/comment/like", { id: commentId }),
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
    return apiClient.get(`/admin/report?${params.toString()}`)
  },

  // 신고 상세 조회
  getReport: (reportId: number) => apiClient.get(`/admin/report/${reportId}`),

  // 사용자 차단
  banUser: (userId: number) => apiClient.post(`/admin/user/ban?userId=${userId}`),

  // 회원탈퇴 (관리자)
  banUserByReport: (reportData: { targetId: number; reportType: string; content: string }) =>
    apiClient.post("/admin/user/ban", reportData),
}

// SSE 연결 관리
export class SSEManager {
  private eventSource: EventSource | null = null
  private listeners: Map<string, (data: any) => void> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000

  connect() {
    if (this.eventSource) {
      this.disconnect()
    }

    try {
      // JWT 토큰을 URL 파라미터로 전달
      const token = getCookie("accessToken")
      const sseUrl = token 
        ? `${notificationApi.subscribeToNotifications()}?token=${encodeURIComponent(token)}`
        : notificationApi.subscribeToNotifications()

      console.log("SSE 연결 시도:", sseUrl.replace(token || '', '[TOKEN]'))

      this.eventSource = new EventSource(sseUrl, {
        withCredentials: true, // httpOnly 쿠키 포함
      })

      this.eventSource.onopen = (event) => {
        console.log("SSE connection opened successfully", event)
        this.reconnectAttempts = 0 // 성공 시 재연결 시도 횟수 초기화
      }

      // SSE 이벤트 리스너 등록 (백엔드에서 .name(type.toString())으로 전송하는 이벤트들)
      const handleSSEEvent = (event: MessageEvent) => {
        console.log(`SSE ${event.type} event received:`, event.data)
        try {
          const data = JSON.parse(event.data)
          console.log("Parsed SSE data:", data)
          
          // 백엔드 메시지 구조: { message: "내용", url: "URL" }
          // 프론트엔드 알림 구조로 변환
          const notificationData = {
            // 임시 ID (실제로는 서버에서 받아야 함 - 추후 개선 필요)
            id: Date.now() + Math.random(), // 중복 방지
            data: data.message || data.data || "새로운 알림",
            url: data.url || "",
            type: event.type || "NOTIFICATION",
            createdAt: new Date().toISOString(),
            read: false
          }
          
          // INITIATE 이벤트는 연결 확인용이므로 알림으로 처리하지 않음
          if (event.type === "INITIATE") {
            console.log("SSE 연결 초기화 완료:", data.message)
            return
          }
          
          // 리스너 실행
          const listener = this.listeners.get("notification")
          if (listener) {
            console.log("SSE 알림 리스너 실행:", notificationData)
            listener(notificationData)
          } else {
            console.warn("No listener found for SSE message")
          }
        } catch (error) {
          console.error(`Failed to parse SSE ${event.type} event:`, error, "Raw data:", event.data)
        }
      }

      // 기본 메시지 이벤트
      this.eventSource.onmessage = handleSSEEvent

      // 특정 타입별 이벤트 리스너 등록
      const eventTypes = ["COMMENT", "FARM", "POST_FEATURED", "COMMENT_FEATURED", "ADMIN", "INITIATE"]
      eventTypes.forEach(type => {
        if (this.eventSource) {
          this.eventSource.addEventListener(type, handleSSEEvent)
        }
      })

      this.eventSource.onerror = (error) => {
        console.error("SSE connection error:", error)
        console.log("EventSource readyState:", this.eventSource?.readyState)
        
        // 연결 실패 시 재연결 시도
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
      console.log("SSE 연결을 종료합니다.")
      this.eventSource.close()
      this.eventSource = null
      this.reconnectAttempts = 0
    }
  }

  addEventListener(type: string, listener: (data: any) => void) {
    console.log(`SSE 리스너 등록: ${type}`)
    this.listeners.set(type, listener)
  }

  removeEventListener(type: string) {
    console.log(`SSE 리스너 제거: ${type}`)
    this.listeners.delete(type)
  }

  // 연결 상태 확인
  isConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN
  }

  // 연결 상태 반환
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
