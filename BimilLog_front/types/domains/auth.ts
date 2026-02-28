// 지원하는 소셜 로그인 제공자
export type SocialProvider = "KAKAO" | "GOOGLE" | "NAVER";

export interface SocialLoginRequest {
  provider: SocialProvider;
  code: string;
  redirectUri?: string;
}

export interface LoginStatus {
  isLoggedIn: boolean;
  memberName?: string;
  lastLoginAt?: string;
  loginMethod?: SocialProvider;
}

// JWT 토큰만 담는 간단한 구조 (쿠키나 로컬 스토리지 저장용)
export interface AuthTokens {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
}

// 사용자 정보 + 토큰을 모두 포함하는 완전한 인증 세션 정보
export interface AuthSession {
  user?: {
    id: number;
    memberName: string;
    email?: string;
    profileImage?: string;
  };
  tokens?: AuthTokens;
  sessionId?: string;
  createdAt: string;
  expiresAt: string;
}

export interface AuthError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
  timestamp: string;
}

// UI Component Props
export interface AuthLayoutProps {
  children: React.ReactNode;
}

export interface AuthProviderProps {
  children: React.ReactNode;
}

export interface AuthLoadingProps {
  message?: string;
  subMessage?: string;
}
