export type AuthStatus = "NEW_USER" | "EXISTING_USER" | "SUCCESS" | "ERROR";

export interface AuthResponse {
  status: AuthStatus;
  uuid?: string;
  data?: {
    userId?: number;
    userName?: string;
    email?: string;
    profileImage?: string;
    socialId?: string;  // Backend StrategyLoginResult.userProfile.socialId
    provider?: SocialProvider;  // Backend StrategyLoginResult.userProfile.provider
    createdAt?: string;
    token?: {  // Backend StrategyLoginResult.token
      accessToken?: string;
      refreshToken?: string;
      expiresAt?: string;
    };
    [key: string]: any;
  };
  error?: string;
  errorCode?: string;
}

export type SocialProvider = "KAKAO" | "GOOGLE" | "NAVER";

export interface SocialLoginRequest {
  provider: SocialProvider;
  code: string;
  fcmToken?: string;
  redirectUri?: string;
}

export interface SignUpRequest {
  userName: string;
  uuid: string;
  marketingConsent?: boolean;
  privacyConsent?: boolean;
}

export interface LoginStatus {
  isLoggedIn: boolean;
  userName?: string;
  lastLoginAt?: string;
  loginMethod?: SocialProvider;
}

export interface AuthTokens {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
}

export interface AuthSession {
  user?: {
    id: number;
    userName: string;
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
  details?: Record<string, any>;
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

// Legacy User type (for compatibility)
export interface LegacyUser {
  id: number;
  userName: string;
  email: string;
  createdAt: string;
  updatedAt: string;
}