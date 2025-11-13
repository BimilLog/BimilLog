import { logger } from '@/lib/utils'

export interface NaverAuthConfig {
  authUrl: string;
  clientId: string;
  redirectUri: string;
}

export class NaverAuthManager {
  private config: NaverAuthConfig;

  constructor(config?: Partial<NaverAuthConfig>) {
    this.config = {
      authUrl: config?.authUrl || process.env.NEXT_PUBLIC_NAVER_AUTH_URL || "",
      clientId: config?.clientId || process.env.NEXT_PUBLIC_NAVER_CLIENT_ID || "",
      redirectUri: config?.redirectUri || process.env.NEXT_PUBLIC_NAVER_REDIRECT_URL || ""
    };

    this.validateConfig();
  }

  private validateConfig(): void {
    if (!this.config.authUrl || !this.config.clientId || !this.config.redirectUri) {
      logger.error("Naver Auth configuration is incomplete");
    }
  }

  /**
   * 네이버 OAuth URL 생성
   * state 파라미터는 CSRF 방지 및 로그인 후 리다이렉트 URL 저장용으로 사용
   */
  buildAuthUrl(postAuthRedirectUrl?: string): string {
    const params = new URLSearchParams({
      response_type: "code",
      client_id: this.config.clientId,
      redirect_uri: this.config.redirectUri,
      // 네이버는 state 파라미터가 필수
      state: this.generateState(postAuthRedirectUrl)
    });

    return `${this.config.authUrl}?${params.toString()}`;
  }

  /**
   * state 파라미터 생성
   * CSRF 방지를 위한 랜덤 문자열 + 로그인 후 리다이렉트 URL 포함
   */
  private generateState(postAuthRedirectUrl?: string): string {
    // CSRF 토큰 생성 (랜덤 문자열)
    const csrfToken = Math.random().toString(36).substring(2, 15);

    // postAuthRedirectUrl이 있으면 함께 인코딩
    if (postAuthRedirectUrl) {
      const stateData = {
        csrf: csrfToken,
        redirect: postAuthRedirectUrl
      };
      return encodeURIComponent(JSON.stringify(stateData));
    }

    // postAuthRedirectUrl이 없으면 CSRF 토큰만
    return csrfToken;
  }

  extractCodeFromUrl(searchParams: URLSearchParams): string | null {
    return searchParams.get("code");
  }

  extractErrorFromUrl(searchParams: URLSearchParams): string | null {
    return searchParams.get("error");
  }

  /**
   * state 파라미터에서 리다이렉트 URL 추출
   */
  extractStateFromUrl(searchParams: URLSearchParams): string | null {
    const state = searchParams.get("state");
    if (!state) return null;

    try {
      const decodedState = decodeURIComponent(state);
      const stateData = JSON.parse(decodedState);
      return stateData.redirect || null;
    } catch {
      // JSON 파싱 실패 시 (단순 문자열인 경우) null 반환
      return null;
    }
  }

  isCallbackUrl(pathname: string): boolean {
    return pathname === "/auth/callback/naver";
  }

  redirectToNaverAuth(postAuthRedirectUrl?: string): void {
    const authUrl = this.buildAuthUrl(postAuthRedirectUrl);
    window.location.href = authUrl;
  }
}

export const naverAuthManager = new NaverAuthManager();
