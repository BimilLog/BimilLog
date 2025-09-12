export interface User {
  id: number;
  userName: string;
  email: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  status: "NEW_USER" | "EXISTING_USER";
  uuid?: string;
  user?: User;
}

export interface AuthError {
  code: "NETWORK_ERROR" | "INVALID_CREDENTIALS" | "SESSION_EXPIRED" | "KAKAO_ERROR" | "SERVER_ERROR" | "UNKNOWN";
  message: string;
  details?: unknown;
}

export interface KakaoAuthConfig {
  authUrl: string;
  clientId: string;
  redirectUri: string;
}

export interface SessionData {
  tempUserUuid?: string;
  returnUrl?: string;
  kakaoConsentUrl?: string;
  lastAuthTime?: number;
  authRetryCount?: number;
}

export interface FCMTokenResult {
  token: string | null;
  error?: Error;
  isSupported: boolean;
}

export interface KakaoCallbackState {
  isProcessing: boolean;
  error: string | null;
}

export interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (postAuthRedirectUrl?: string) => void;
  logout: () => Promise<void>;
  signUp: (userName: string, uuid: string) => Promise<{ success: boolean; error?: string }>;
  updateUserName: (userName: string) => Promise<boolean>;
  deleteAccount: () => Promise<boolean>;
  refreshUser: () => Promise<void>;
}

export interface LoginFormData {
  postAuthRedirectUrl?: string;
}

export interface SignUpFormData {
  userName: string;
  uuid: string;
}

export interface SessionConfig {
  maxSessionAgeMs: number;
  maxRetryCount: number;
  storagePrefix: string;
}

export type AuthProviderProps = {
  children: React.ReactNode;
};

export type NotificationPermission = "granted" | "denied" | "default";

export interface AuthLoadingProps {
  message?: string;
  subMessage?: string;
}

export interface AuthLayoutProps {
  children: React.ReactNode;
}

export const AUTH_ERROR_MESSAGES: Record<AuthError["code"], string> = {
  NETWORK_ERROR: "네트워크 연결을 확인해주세요",
  INVALID_CREDENTIALS: "잘못된 인증 정보입니다",
  SESSION_EXPIRED: "세션이 만료되었습니다. 다시 로그인해주세요",
  KAKAO_ERROR: "카카오 로그인 중 오류가 발생했습니다",
  SERVER_ERROR: "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요",
  UNKNOWN: "알 수 없는 오류가 발생했습니다"
};

export const AUTH_CONFIG = {
  SESSION: {
    MAX_AGE_MS: 60 * 60 * 1000, // 1 hour
    MAX_RETRY_COUNT: 3,
    STORAGE_PREFIX: "bimillog_auth_"
  },
  FCM: {
    TOKEN_CACHE_DURATION_MS: 30 * 60 * 1000 // 30 minutes
  },
  KAKAO: {
    RESPONSE_TYPE: "code" as const
  }
} as const;