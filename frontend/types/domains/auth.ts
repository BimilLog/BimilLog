// 카카오 OAuth 로그인 결과 상태
// NEW_USER: 첫 로그인, 회원가입 필요 | EXISTING_USER: 기존 회원, 로그인 완료
export type AuthStatus = "NEW_USER" | "EXISTING_USER";

// 백엔드 AuthController에서 반환하는 응답 (문자열 직접 반환)
export type AuthResponse = AuthStatus;

// 지원하는 소셜 로그인 제공자 (현재는 KAKAO만 구현됨)
export type SocialProvider = "KAKAO" | "GOOGLE" | "NAVER";

export interface SocialLoginRequest {
  provider: SocialProvider;
  code: string;
  fcmToken?: string;
  redirectUri?: string;
}

export interface SignUpRequest {
  memberName: string;
  marketingConsent?: boolean;
  privacyConsent?: boolean;
  // UUID는 HttpOnly 쿠키로 전달되어 요청 본문에 포함하지 않음
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