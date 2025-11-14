import { logger } from "@/lib/utils";

export interface GoogleAuthConfig {
  authUrl: string;
  clientId: string;
  redirectUri: string;
}

export class GoogleAuthManager {
  private config: GoogleAuthConfig;

  constructor(config?: Partial<GoogleAuthConfig>) {
    this.config = {
      authUrl: config?.authUrl || process.env.NEXT_PUBLIC_GOOGLE_AUTH_URL || "",
      clientId: config?.clientId || process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "",
      redirectUri: config?.redirectUri || process.env.NEXT_PUBLIC_GOOGLE_REDIRECT_URL || "",
    };

    this.validateConfig();
  }

  private validateConfig() {
    if (!this.config.authUrl || !this.config.clientId || !this.config.redirectUri) {
      logger.error("구글 인증 설정 실패");
    }
  }

  /**
   * Google OAuth URL 생성
   * scope: openid profile email
   */
  buildAuthUrl(postAuthRedirectUrl?: string): string {
    const params = new URLSearchParams({
      response_type: "code",
      client_id: this.config.clientId,
      redirect_uri: this.config.redirectUri,
      scope: "openid profile email",
      access_type: "offline",
      include_granted_scopes: "true",
      prompt: "consent",
      state: this.generateState(postAuthRedirectUrl),
    });

    return `${this.config.authUrl}?${params.toString()}`;
  }

  private generateState(postAuthRedirectUrl?: string): string {
    const csrfToken = Math.random().toString(36).slice(2);
    if (!postAuthRedirectUrl) return csrfToken;

    return encodeURIComponent(
      JSON.stringify({
        csrf: csrfToken,
        redirect: postAuthRedirectUrl,
      }),
    );
  }

  isCallbackUrl(pathname: string): boolean {
    return pathname === "/auth/callback/google";
  }

  redirectToGoogleAuth(postAuthRedirectUrl?: string): void {
    const authUrl = this.buildAuthUrl(postAuthRedirectUrl);
    window.location.href = authUrl;
  }
}

export const googleAuthManager = new GoogleAuthManager();
