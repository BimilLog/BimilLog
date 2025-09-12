const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

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

// 응답 후 CSRF 토큰 상태를 확인하는 헬퍼 함수
function logCsrfTokenUpdate(method: string, endpoint: string): void {
  if (process.env.NODE_ENV === 'development' && typeof document !== 'undefined') {
    // POST/PUT/DELETE/PATCH 요청 후 토큰 변경 확인
    if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      setTimeout(() => {
        const currentToken = getCookie('XSRF-TOKEN');
        console.log(`[${method}] ${endpoint} - Current CSRF token:`, currentToken?.substring(0, 8) + '...');
      }, 100);
    }
  }
}

// API 응답 타입 정의
export interface ApiResponse<T = any> {
  success: boolean
  data?: T | null
  message?: string
  error?: string
  needsRelogin?: boolean // 다른 기기에서 로그아웃된 경우
}

// 사용자 정보 타입 (백엔드 UserInfoResponseDTO와 일치)
export interface User {
  userId: number
  settingId: number
  socialNickname: string
  thumbnailImage: string
  userName: string
  role: "USER" | "ADMIN"
}

// 백엔드 v2 Auth API 타입 정의
export interface AuthResponse {
  status: "NEW_USER" | "EXISTING_USER" | "SUCCESS"
  uuid?: string
  data: Record<string, any>
}

export interface SocialLoginRequest {
  provider: string
  code: string
  fcmToken?: string
}

export interface SignUpRequest {
  userName: string
  uuid: string
}

// DecoType enum - 백엔드 DecoType과 완전 일치
export type DecoType = 
  // 과일
  | "POTATO" | "CARROT" | "CABBAGE" | "TOMATO" | "STRAWBERRY" | "BLUEBERRY"
  | "WATERMELON" | "PUMPKIN" | "APPLE" | "GRAPE" | "BANANA"
  
  // 이상한 장식  
  | "GOBLIN" | "SLIME" | "ORC" | "DRAGON" | "PHOENIX"
  | "WEREWOLF" | "ZOMBIE" | "KRAKEN" | "CYCLOPS" | "DEVIL" | "ANGEL"
  
  // 음료
  | "COFFEE" | "MILK" | "WINE" | "SOJU" | "BEER" | "BUBBLETEA" | "SMOOTHIE"
  | "BORICHA" | "STRAWBERRYMILK" | "BANANAMILK"
  
  // 음식
  | "BREAD" | "BURGER" | "CAKE" | "SUSHI" | "PIZZA" | "CHICKEN" | "NOODLE" | "EGG"
  | "SKEWER" | "KIMBAP" | "SUNDAE" | "MANDU" | "SAMGYEOPSAL" | "FROZENFISH" | "HOTTEOK"
  | "COOKIE" | "PICKLE"
  
  // 동물
  | "CAT" | "DOG" | "RABBIT" | "FOX" | "TIGER" | "PANDA" | "LION" | "ELEPHANT"
  | "SQUIRREL" | "HEDGEHOG" | "CRANE" | "SPARROW" | "CHIPMUNK" | "GIRAFFE" | "HIPPO" | "POLARBEAR" | "BEAR"
  
  // 자연
  | "STAR" | "SUN" | "MOON" | "VOLCANO" | "CHERRY" | "MAPLE" | "BAMBOO" | "SUNFLOWER"
  | "STARLIGHT" | "CORAL" | "ROCK" | "WATERDROP" | "WAVE" | "RAINBOW"
  
  // 기타
  | "DOLL" | "BALLOON" | "SNOWMAN" | "FAIRY" | "BUBBLE"

// 롤링페이퍼 메시지 타입 - v2 백엔드 MessageDTO 완전 호환
export interface RollingPaperMessage {
  id: number
  userId: number
  decoType: DecoType
  anonymity: string
  content: string
  x: number  // 그리드 X 좌표 (1-based)
  y: number  // 그리드 Y 좌표 (1-based)
  createdAt: string // ISO 8601 string format - 백엔드 Instant는 ISO string으로 변환됨
}

// 방문용 메시지 타입 - v2 백엔드 VisitMessageDTO 완전 호환
export interface VisitMessage {
  id: number
  userId: number
  decoType: DecoType
  x: number  // 그리드 X 좌표 (1-based)
  y: number  // 그리드 Y 좌표 (1-based)
}

// 게시글 타입 - v2 백엔드 FullPostResDTO 호환
export interface Post {
  id: number         // v2: postId → id
  userId: number
  userName: string
  title: string
  content: string
  viewCount: number  // v2: views → viewCount
  likeCount: number  // v2: likes → likeCount
  commentCount: number // v2: 추가된 필드
  postCacheFlag?: "REALTIME" | "WEEKLY" | "LEGEND"
  createdAt: string  // v2: Instant → ISO string
  isLiked: boolean   // v2: userLike → isLiked
  isNotice: boolean  // v2: notice → isNotice
  password?: number
}

// 간단한 게시글 타입 (목록용) - v2 백엔드 SimplePostResDTO 호환
export interface SimplePost {
  id: number         // v2: postId → id
  userId: number
  userName: string
  title: string
  content: string    // v2: 추가된 필드 (간단한 내용 미리보기)
  commentCount: number
  likeCount: number  // v2: likes → likeCount
  viewCount: number  // v2: views → viewCount
  createdAt: string  // v2: Instant → ISO string
  postCacheFlag?: "REALTIME" | "WEEKLY" | "LEGEND"
  isNotice: boolean  // v2: _notice → isNotice
}

// 댓글 타입 - v2 백엔드 CommentDTO 호환
export interface Comment {
  id: number
  parentId?: number
  postId: number
  userId?: number
  userName: string
  content: string
  popular: boolean
  deleted: boolean
  likeCount: number  // v2: likes → likeCount
  createdAt: string
  userLike: boolean
}

// 간단한 댓글 타입 (목록용) - v2 백엔드 호환
export interface SimpleComment {
  id: number
  postId: number
  userName: string
  content: string
  likeCount: number  // v2: likes → likeCount
  userLike: boolean
  createdAt: string
}

// 알림 타입 - v2 백엔드 NotificationDTO 호환
export interface Notification {
  id: number
  content: string  // v2: data → content
  url: string
  notificationType: "PAPER" | "COMMENT" | "POST_FEATURED" | "INITIATE" | "ADMIN"  // v2: type → notificationType, updated enum values
  createdAt: string
  isRead: boolean  // v2: read → isRead
}

// 설정 타입 - v2 백엔드 SettingDTO 호환
export interface Setting {
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

// 신고 타입 - v2 백엔드 ReportDTO 호환
export interface Report {
  id: number
  reporterId: number
  reporterName: string
  reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
  targetId: number
  content: string
  createdAt: string
  // 임시 호환용 (나중에 제거 필요)
  targetTitle?: string
  userId?: number // reporterId 대신 사용되는 경우가 있음
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

    // 요청 직전에 매번 최신 CSRF 토큰을 가져옴 (POST 요청 후 업데이트된 토큰 반영)
    const csrfToken = getCookie("XSRF-TOKEN");

    const defaultHeaders: Record<string, string> = {
      "Content-Type": "application/json",
    };

    if (csrfToken) {
      defaultHeaders["X-XSRF-TOKEN"] = csrfToken;
      if (process.env.NODE_ENV === 'development') {
        console.log(`[${options.method || 'GET'}] ${endpoint} - Using CSRF token:`, csrfToken.substring(0, 8) + '...');
      }
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
        '/post/manage/like',     // 글 추천
        '/comment/like',  // 댓글 추천
        '/notification',  // 알림
        '/api/auth/logout',   // 로그아웃
        '/api/auth/withdraw'  // 회원탈퇴
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

      // POST/PUT/DELETE/PATCH 요청 후 토큰 변경 로그
      logCsrfTokenUpdate(options.method || 'GET', endpoint);

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
          
          // 다른 기기에서 로그아웃 에러 감지
          const needsRelogin = errorMessage.includes("다른기기에서 로그아웃 하셨습니다");
          
          // needsRelogin이 true이면 전역 이벤트 발생
          if (needsRelogin && typeof window !== 'undefined') {
            const event = new CustomEvent('needsRelogin', {
              detail: {
                title: '로그인이 필요합니다',
                message: '다른기기에서 로그아웃 하셨습니다 다시 로그인해주세요'
              }
            });
            window.dispatchEvent(event);
          }
          
          return {
            success: false,
            error: errorMessage,
            needsRelogin,
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
  // 카카오 로그인 (백엔드 v2 호환)
  kakaoLogin: (code: string, fcmToken?: string) => {
    const requestBody: SocialLoginRequest = {
      provider: 'KAKAO',
      code,
      fcmToken
    }
    return apiClient.post<AuthResponse>("/api/auth/login", requestBody)
  },

  // 현재 사용자 정보 조회 (httpOnly 쿠키 자동 포함)
  getCurrentUser: () => apiClient.get<User>("/api/auth/me"),

  // 로그아웃
  logout: () => apiClient.post("/api/auth/logout"),

  // 회원 탈퇴
  deleteAccount: () => apiClient.delete("/api/user/withdraw"),

  // 회원가입 (백엔드 v2 호환)
  signUp: (userName: string, uuid: string) => {
    const requestBody: SignUpRequest = {
      userName,
      uuid
    }
    return apiClient.post<AuthResponse>("/api/auth/signup", requestBody)
  },

  // 서버 상태 확인
  healthCheck: () => apiClient.get<string>("/api/auth/health"),
}

// 사용자 관련 API
export const userApi = {
  // 닉네임 중복 확인 ✅ 이미 v2 호환
  checkUserName: (userName: string) => apiClient.get<boolean>(`/api/user/username/check?userName=${encodeURIComponent(userName)}`),

  // 닉네임 변경 - v2 마이그레이션
  updateUserName: (userName: string) => apiClient.post("/api/user/username", { userName }),

  // 사용자 설정 조회 - v2 마이그레이션  
  getUserSettings: () => apiClient.get<Setting>("/api/user/setting"),

  // 사용자 설정 수정 - v2 마이그레이션
  updateUserSettings: (settings: Setting) => apiClient.post("/api/user/setting", settings),

  // v2: 신고/건의사항 제출 (UserCommandController.submitReport)
  submitReport: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT"
    targetId?: number
    content: string
  }) => apiClient.post("/api/user/report", report),

  // 레거시 호환용 (v2 신고 API로 직접 연결)
  submitSuggestion: (report: {
    reportType: "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT" | "SUGGESTION"
    userId?: number
    targetId?: number
    content: string
  }) => {
    // SUGGESTION을 IMPROVEMENT로 매핑
    const mappedType = report.reportType === "SUGGESTION" 
      ? "IMPROVEMENT" 
      : report.reportType as "POST" | "COMMENT" | "ERROR" | "IMPROVEMENT";
    
    return apiClient.post("/api/user/report", {
      reportType: mappedType,
      targetId: report.targetId,
      content: report.content
    });
  },

  // 카카오 친구 목록 조회 - v2 마이그레이션 (GET으로 변경, offset/limit 파라미터)
  getFriendList: (offset = 0, limit = 10) => apiClient.get<KakaoFriendList>(`/api/user/friendlist?offset=${offset}&limit=${limit}`),

  // 사용자가 작성한 글 목록 - v2 마이그레이션
  getUserPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/user/posts?page=${page}&size=${size}`),

  // 사용자가 작성한 댓글 목록 - v2 마이그레이션
  getUserComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/user/comments?page=${page}&size=${size}`),

  // 사용자가 추천한 글 목록 - v2 마이그레이션
  getUserLikedPosts: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimplePost>>(`/api/user/likeposts?page=${page}&size=${size}`),

  // 사용자가 추천한 댓글 목록 - v2 마이그레이션
  getUserLikedComments: (page = 0, size = 10) =>
    apiClient.get<PageResponse<SimpleComment>>(`/api/user/likecomments?page=${page}&size=${size}`),

}

// 롤링페이퍼 관련 API - v2 마이그레이션
export const rollingPaperApi = {
  // 내 롤링페이퍼 조회 - v2 마이그레이션
  getMyRollingPaper: () => apiClient.get<RollingPaperMessage[]>("/api/paper"),

  // 다른 사용자의 롤링페이퍼 조회 - v2 마이그레이션
  getRollingPaper: (userName: string) => apiClient.get<VisitMessage[]>(`/api/paper/${encodeURIComponent(userName)}`),

  // 메시지 작성 - v2 마이그레이션
  createMessage: (
    userName: string,
    message: {
      decoType: DecoType
      anonymity: string
      content: string
      x: number  // 그리드 X 좌표 (1-based)
      y: number  // 그리드 Y 좌표 (1-based)
    },
  ) => apiClient.post(`/api/paper/${encodeURIComponent(userName)}`, message),

  // 메시지 삭제 (소유자만) - 최적화: ID만 필요
  deleteMessage: (messageId: number) => apiClient.post("/api/paper/delete", { id: messageId }),
}

// 게시판 관련 API - v2 백엔드 CQRS 패턴 완전 연동
export const boardApi = {
  // 게시글 목록 조회 - v2 마이그레이션 (PostQueryController)
  getPosts: (page = 0, size = 10) => apiClient.get<PageResponse<SimplePost>>(`/api/post?page=${page}&size=${size}`),

  // 게시글 상세 조회 - v2 마이그레이션 (PostQueryController)
  getPost: (postId: number) => apiClient.get<Post>(`/api/post/${postId}`),

  // 게시글 검색 - v2 마이그레이션 (PostQueryController) - 백엔드 검색 타입 형식에 맞춤
  searchPosts: (type: "TITLE" | "TITLE_CONTENT" | "AUTHOR", query: string, page = 0, size = 10) => {
    // Frontend 타입을 Backend 타입으로 변환
    const typeMap: Record<string, string> = {
      "TITLE": "title",
      "AUTHOR": "writer",
      "TITLE_CONTENT": "title_content"
    };
    const backendType = typeMap[type] || "title";
    
    return apiClient.get<PageResponse<SimplePost>>(
      `/api/post/search?type=${backendType}&query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
    );
  },

  // 게시글 작성 - v2 마이그레이션 (PostCommandController)
  createPost: (post: {
    userName: string | null
    title: string
    content: string
    password?: number
  }) => {
    // v2 백엔드 PostCreateDTO 형식에 맞춤 - password는 4자리 숫자 문자열
    const payload: any = {
      title: post.title,
      content: post.content
    };
    
    // 익명 사용자일 경우 password 필드 추가
    if (post.password !== undefined) {
      payload.password = post.password.toString().padStart(4, '0'); // 4자리 문자열로 변환
    }
    
    return apiClient.post<{ id: number }>("/api/post", payload).then(response => {
      // 생성된 게시글 ID만 반환되므로 전체 Post 객체로 래핑
      if (response.success && response.data) {
        return { ...response, data: { ...post, id: response.data.id } as Post };
      }
      return response as ApiResponse<Post>;
    });
  },

  // 게시글 수정 - v2 마이그레이션 (PostCommandController)
  updatePost: (post: Post) => {
    // v2 백엔드 PostUpdateDTO 형식에 맞춤 - password 필드 없음
    const payload = {
      title: post.title,
      content: post.content
    };
    return apiClient.put(`/api/post/${post.id}`, payload);
  },

  // 게시글 삭제 - v2 마이그레이션 (PostCommandController)
  deletePost: (postId: number, userId?: number, password?: string, content?: string, title?: string) => {
    // v2에서는 인증된 사용자는 DELETE 메서드만 사용
    // 익명 게시글의 경우 별도 처리 필요 (백엔드에서 처리)
    return apiClient.delete(`/api/post/${postId}`);
  },

  // 게시글 추천/취소 - v2 마이그레이션 (PostCommandController)
  likePost: (postId: number) => apiClient.post(`/api/post/${postId}/like`),

  // 인기글 조회 - v2 백엔드 PostCacheController 연동
  // 실시간 + 주간 인기글 한 번에 조회
  getPopularPosts: () => apiClient.get<{ realtime: SimplePost[], weekly: SimplePost[] }>("/api/post/popular"),
  
  // 레전드 인기글 조회 (페이지네이션 지원)
  getLegendPosts: (page = 0, size = 10) => 
    apiClient.get<PageResponse<SimplePost>>(`/api/post/legend?page=${page}&size=${size}`),
  
  // 공지사항 조회
  getNoticePosts: () => apiClient.get<SimplePost[]>("/api/post/notice"),
}

// 댓글 관련 API - v2 백엔드 CQRS 마이그레이션
export const commentApi = {
  // 댓글 목록 조회 - v2 마이그레이션 (CommentQueryController)
  getComments: (postId: number, page = 0) => 
    apiClient.get<PageResponse<Comment>>(`/api/comment/${postId}?page=${page}`),

  // 인기 댓글 조회 - v2 마이그레이션 (CommentQueryController)
  getPopularComments: (postId: number) => 
    apiClient.get<Comment[]>(`/api/comment/${postId}/popular`),

  // 댓글 작성 - v2 마이그레이션 (CommentCommandController)
  createComment: (comment: {
    postId: number
    content: string
    parentId?: number
    password?: number
  }) => {
    // v2 백엔드 CommentReqDTO 형식에 맞춤 (userName은 백엔드에서 userDetails로 자동 처리)
    const payload = {
      postId: comment.postId,
      content: comment.content,
      parentId: comment.parentId,
      password: comment.password
    };
    return apiClient.post("/api/comment/write", payload);
  },

  // 댓글 수정 - v2 마이그레이션 (CommentCommandController)
  updateComment: (commentId: number, data: { content: string; password?: number }) => {
    // v2 백엔드 CommentReqDTO 형식에 맞춤
    const payload: any = {
      id: commentId,
      content: data.content
    };
    if (data.password !== undefined) {
      payload.password = data.password;
    }
    return apiClient.post("/api/comment/update", payload);
  },

  // 댓글 삭제 - v2 마이그레이션 (CommentCommandController)  
  deleteComment: (commentId: number, password?: number) => {
    // v2 백엔드 CommentReqDTO 형식에 맞춤
    const payload: any = { id: commentId };
    if (password !== undefined) {
      payload.password = password;
    }
    return apiClient.post("/api/comment/delete", payload);
  },

  // 댓글 추천/취소 - v2 마이그레이션 (CommentCommandController)
  likeComment: (commentId: number) => 
    apiClient.post("/api/comment/like", { commentId }),
}

// 알림 관련 API - v2 백엔드 CQRS 마이그레이션
export const notificationApi = {
  // 알림 목록 조회 - v2 마이그레이션 (NotificationQueryController)
  getNotifications: () => apiClient.get<Notification[]>("/api/notification/list"),

  // 알림 읽음/삭제 처리 - v2 마이그레이션 (NotificationCommandController)
  updateNotifications: (data: {
    readIds?: number[]
    deletedIds?: number[]  // v2: deletedIds는 그대로 유지
  }) => {
    // v2 백엔드 UpdateNotificationDTO 형식에 맞춤
    const payload: any = {};
    if (data.readIds && data.readIds.length > 0) {
      payload.readIds = data.readIds;
    }
    if (data.deletedIds && data.deletedIds.length > 0) {
      payload.deletedIds = data.deletedIds;
    }
    return apiClient.post("/api/notification/update", payload);
  },

  // SSE 알림 구독 - v2 마이그레이션 (NotificationSseController)
  subscribeToNotifications: () => `${API_BASE_URL}/api/notification/subscribe`,
}

// 관리자 관련 API - v2 백엔드 연결
export const adminApi = {
  // v2: 신고 목록 조회 (AdminQueryController.getReportList)
  getReports: (page = 0, size = 20, reportType?: string) => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() })
    if (reportType && reportType !== "all") {
      // v2에서는 POST/COMMENT/ERROR/IMPROVEMENT 모두 지원
      const mappedType = reportType
      params.append("reportType", mappedType)
    }
    return apiClient.get(`/api/admin/reports?${params.toString()}`)
  },

  // v2: 신고 상세 조회 (현재 v2에 없음, 목록에서 충분한 정보 제공)
  getReport: async (reportId: number) => {
    // v2에서는 별도 상세 조회 없이 목록에서 모든 정보 제공
    // 레거시 호환을 위해 목록 조회로 대체
    try {
      const response = await apiClient.get(`/api/admin/reports?page=0&size=1000`);
      if (response.success && response.data && (response.data as any)?.content) {
        const report = ((response.data as any).content as any[]).find((r: any) => r.id === reportId);
        return { ...response, data: report };
      }
      return response;
    } catch (error) {
      return { success: false, error: '신고 내역 조회에 실패했습니다.' };
    }
  },

  // v2: 사용자 차단 (AdminCommandController.banUser)
  banUser: (reportData: { reporterId?: number; reporterName?: string; reportType: string; targetId: number; content: string }) =>
    apiClient.post("/api/admin/ban", {
      reporterId: reportData.reporterId,
      reporterName: reportData.reporterName,
      reportType: reportData.reportType,
      targetId: reportData.targetId,
      content: reportData.content
    }),

  // v2: 사용자 강제 탈퇴 (AdminCommandController.forceWithdrawUser)
  forceWithdrawUser: (reportData: { targetId: number; reportType: string; content: string }) =>
    apiClient.post('/api/admin/withdraw', {
      reportType: reportData.reportType,
      targetId: reportData.targetId,
      content: reportData.content
    }),

  // 레거시 호환용
  banUserByReport: (reportData: { targetId: number; reportType: string; content: string }) =>
    adminApi.banUser({
      reportType: reportData.reportType,
      targetId: reportData.targetId,
      content: reportData.content
    }),
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
      // HttpOnly 쿠키가 자동으로 전송되므로 토큰 확인 불필요
      const sseUrl = notificationApi.subscribeToNotifications()

      if (process.env.NODE_ENV === 'development') {
        console.log("SSE 연결 시도:", sseUrl);
      }

      this.eventSource = new EventSource(sseUrl, {
        withCredentials: true, // httpOnly 쿠키 포함
      })

      this.eventSource.onopen = (event) => {
        if (process.env.NODE_ENV === 'development') {
          console.log("SSE connection opened successfully", event);
        }
        this.reconnectAttempts = 0 // 성공 시 재연결 시도 횟수 초기화
      }

      // SSE 이벤트 리스너 등록 (백엔드에서 .name(type.toString())으로 전송하는 이벤트들)
      const handleSSEEvent = (event: MessageEvent) => {
        if (process.env.NODE_ENV === 'development') {
          console.log(`SSE ${event.type} event received:`, event.data);
        }
        try {
          const data = JSON.parse(event.data)
          if (process.env.NODE_ENV === 'development') {
            console.log("Parsed SSE data:", data);
          }
          
          // 백엔드 v2 SSE 메시지 구조를 프론트엔드 Notification 인터페이스로 변환
          const notificationData = {
            id: data.id || Date.now() + Math.random(), // 서버 ID 우선, 없으면 임시 ID
            content: data.message || data.content || data.data || "새로운 알림",
            url: data.url || "",
            notificationType: (event.type || "ADMIN") as "PAPER" | "COMMENT" | "POST_FEATURED" | "INITIATE" | "ADMIN",
            createdAt: data.createdAt || new Date().toISOString(),
            isRead: false
          }
          
          // INITIATE 이벤트는 연결 확인용이므로 알림으로 처리하지 않음
          if (event.type === "INITIATE") {
            if (process.env.NODE_ENV === 'development') {
          console.log("SSE 연결 초기화 완료:", data.message);
        }
            return
          }
          
          // 리스너 실행
          const listener = this.listeners.get("notification")
          if (listener) {
            if (process.env.NODE_ENV === 'development') {
          console.log("SSE 알림 리스너 실행:", notificationData);
        }
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

      // 특정 타입별 이벤트 리스너 등록 - v2 백엔드 NotificationType과 일치
      const eventTypes = ["COMMENT", "PAPER", "POST_FEATURED", "ADMIN", "INITIATE"]
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
      if (process.env.NODE_ENV === 'development') {
      console.log("SSE 연결을 종료합니다.");
    }
      this.eventSource.close()
      this.eventSource = null
      this.reconnectAttempts = 0
    }
  }

  addEventListener(type: string, listener: (data: any) => void) {
    if (process.env.NODE_ENV === 'development') {
      console.log(`SSE 리스너 등록: ${type}`);
    }
    this.listeners.set(type, listener)
  }

  removeEventListener(type: string) {
    if (process.env.NODE_ENV === 'development') {
      console.log(`SSE 리스너 제거: ${type}`);
    }
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
  // 과일
  POTATO: { name: "감자", color: "from-yellow-100 to-amber-100" },
  CARROT: { name: "당근", color: "from-orange-100 to-red-100" },
  CABBAGE: { name: "양배추", color: "from-green-100 to-emerald-100" },
  TOMATO: { name: "토마토", color: "from-red-100 to-pink-100" },
  STRAWBERRY: { name: "딸기", color: "from-pink-100 to-red-100" },
  WATERMELON: { name: "수박", color: "from-green-100 to-red-100" },
  PUMPKIN: { name: "호박", color: "from-orange-100 to-yellow-100" },
  APPLE: { name: "사과", color: "from-red-100 to-pink-100" },
  GRAPE: { name: "포도", color: "from-purple-100 to-violet-100" },
  BANANA: { name: "바나나", color: "from-yellow-100 to-amber-100" },
  BLUEBERRY: { name: "블루베리", color: "from-blue-100 to-indigo-100" },

  // 몬스터
  GOBLIN: { name: "고블린", color: "from-green-100 to-emerald-100" },
  SLIME: { name: "슬라임", color: "from-blue-100 to-indigo-100" },
  ORC: { name: "오크", color: "from-gray-100 to-slate-100" },
  DRAGON: { name: "드래곤", color: "from-red-100 to-orange-100" },
  PHOENIX: { name: "피닉스", color: "from-orange-100 to-red-100" },
  WEREWOLF: { name: "늑대인간", color: "from-gray-100 to-brown-100" },
  ZOMBIE: { name: "좀비", color: "from-gray-100 to-green-100" },
  KRAKEN: { name: "크라켄", color: "from-blue-100 to-purple-100" },
  CYCLOPS: { name: "사이클롭스", color: "from-purple-100 to-indigo-100" },
  DEVIL: { name: "악마", color: "from-red-100 to-orange-100" },
  ANGEL: { name: "천사", color: "from-white to-yellow-100" },

  // 음료
  COFFEE: { name: "커피", color: "from-amber-100 to-brown-100" },
  MILK: { name: "우유", color: "from-white to-gray-100" },
  WINE: { name: "와인", color: "from-purple-100 to-red-100" },
  SOJU: { name: "소주", color: "from-blue-50 to-slate-100" },
  BEER: { name: "맥주", color: "from-yellow-100 to-amber-100" },
  BUBBLETEA: { name: "버블티", color: "from-pink-100 to-purple-100" },
  SMOOTHIE: { name: "스무디", color: "from-pink-100 to-red-100" },
  BORICHA: { name: "보리차", color: "from-amber-100 to-yellow-100" },
  STRAWBERRYMILK: { name: "딸기우유", color: "from-pink-100 to-red-100" },
  BANANAMILK: { name: "바나나우유", color: "from-yellow-100 to-amber-100" },

  // 음식
  BREAD: { name: "빵", color: "from-amber-100 to-yellow-100" },
  BURGER: { name: "햄버거", color: "from-yellow-100 to-red-100" },
  CAKE: { name: "케이크", color: "from-pink-100 to-yellow-100" },
  SUSHI: { name: "스시", color: "from-orange-100 to-red-100" },
  PIZZA: { name: "피자", color: "from-red-100 to-yellow-100" },
  CHICKEN: { name: "치킨", color: "from-yellow-100 to-orange-100" },
  NOODLE: { name: "라면", color: "from-yellow-100 to-red-100" },
  EGG: { name: "계란", color: "from-yellow-100 to-white" },
  SKEWER: { name: "꼬치", color: "from-red-100 to-orange-100" },
  KIMBAP: { name: "김밥", color: "from-green-100 to-yellow-100" },
  SUNDAE: { name: "순대", color: "from-gray-100 to-red-100" },
  MANDU: { name: "만두", color: "from-white to-yellow-100" },
  SAMGYEOPSAL: { name: "삼겹살", color: "from-pink-100 to-red-100" },
  FROZENFISH: { name: "동상걸린 붕어", color: "from-yellow-100 to-brown-100" },
  HOTTEOK: { name: "호떡", color: "from-brown-100 to-amber-100" },
  COOKIE: { name: "쿠키", color: "from-brown-100 to-yellow-100" },
  PICKLE: { name: "피클", color: "from-green-100 to-yellow-100" },

  // 동물
  CAT: { name: "고양이", color: "from-gray-100 to-orange-100" },
  DOG: { name: "강아지", color: "from-yellow-100 to-brown-100" },
  RABBIT: { name: "토끼", color: "from-pink-100 to-white" },
  FOX: { name: "여우", color: "from-orange-100 to-red-100" },
  TIGER: { name: "호랑이", color: "from-orange-100 to-yellow-100" },
  PANDA: { name: "판다", color: "from-gray-100 to-white" },
  LION: { name: "사자", color: "from-yellow-100 to-amber-100" },
  ELEPHANT: { name: "코끼리", color: "from-gray-100 to-slate-100" },
  SQUIRREL: { name: "다람쥐", color: "from-brown-100 to-orange-100" },
  HEDGEHOG: { name: "고슴도치", color: "from-brown-100 to-gray-100" },
  CRANE: { name: "두루미", color: "from-white to-gray-100" },
  SPARROW: { name: "참새", color: "from-brown-100 to-yellow-100" },
  CHIPMUNK: { name: "청설모", color: "from-gray-100 to-brown-100" },
  GIRAFFE: { name: "기린", color: "from-yellow-100 to-orange-100" },
  HIPPO: { name: "하마", color: "from-gray-100 to-purple-100" },
  POLARBEAR: { name: "북극곰", color: "from-white to-blue-100" },
  BEAR: { name: "곰", color: "from-red-100 to-rainbow-100" },

  // 자연
  STAR: { name: "별", color: "from-yellow-100 to-amber-100" },
  SUN: { name: "태양", color: "from-yellow-100 to-orange-100" },
  MOON: { name: "달", color: "from-blue-100 to-indigo-100" },
  VOLCANO: { name: "화산", color: "from-red-100 to-orange-100" },
  CHERRY: { name: "벚꽃", color: "from-pink-100 to-white" },
  MAPLE: { name: "단풍", color: "from-red-100 to-orange-100" },
  BAMBOO: { name: "대나무", color: "from-green-100 to-emerald-100" },
  SUNFLOWER: { name: "해바라기", color: "from-yellow-100 to-orange-100" },
  STARLIGHT: { name: "별빛", color: "from-yellow-100 to-blue-100" },
  CORAL: { name: "산호", color: "from-orange-100 to-pink-100" },
  ROCK: { name: "바위", color: "from-gray-100 to-slate-100" },
  WATERDROP: { name: "물방울", color: "from-blue-100 to-white" },
  WAVE: { name: "파도", color: "from-blue-100 to-cyan-100" },
  RAINBOW: { name: "무지개", color: "from-pink-100 to-purple-100" },

  // 기타
  DOLL: { name: "인형", color: "from-pink-100 to-purple-100" },
  BALLOON: { name: "풍선", color: "from-red-100 to-rainbow-100" },
  SNOWMAN: { name: "눈사람", color: "from-white to-blue-100" },
  FAIRY: { name: "요정", color: "from-pink-100 to-purple-100" },
  BUBBLE: { name: "비눗방울", color: "from-blue-100 to-white" }
}

// 헬퍼 함수들 - icon mapping 추가
import { getIconMapping } from './icon-mappings';

export const getDecoInfo = (decoType: DecoType | string) => {
  const baseInfo = decoTypeMap[decoType as keyof typeof decoTypeMap] || {
    name: "기본",
    color: "from-gray-100 to-slate-100"
  };
  
  const iconMapping = getIconMapping(decoType as DecoType);
  
  return {
    ...baseInfo,
    iconMapping
  };
}

// CSRF 토큰 디버깅용 유틸리티
export const csrfDebugUtils = {
  getCurrentToken: () => getCookie("XSRF-TOKEN"),
  logCurrentToken: () => {
    const token = getCookie("XSRF-TOKEN");
    console.log("Current CSRF token:", token);
    return token;
  },
  testTokenRotation: async () => {
    console.log("=== CSRF Token Rotation Test ===");
    const beforeToken = getCookie("XSRF-TOKEN");
    console.log("Before POST:", beforeToken?.substring(0, 8) + "...");
    
    // 테스트 POST 요청
    const response = await apiClient.post("/api/auth/health");
    
    // 짧은 지연 후 토큰 확인
    setTimeout(() => {
      const afterToken = getCookie("XSRF-TOKEN");
      console.log("After POST:", afterToken?.substring(0, 8) + "...");
      console.log("Token changed:", beforeToken !== afterToken);
    }, 200);
    
    return response;
  }
};

// 개발 환경에서 전역 접근을 위해 window 객체에 추가
if (typeof window !== 'undefined' && process.env.NODE_ENV === 'development') {
  (window as any).csrfDebug = csrfDebugUtils;
}
