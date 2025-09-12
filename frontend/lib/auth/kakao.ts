export interface KakaoAuthConfig {
  authUrl: string;
  clientId: string;
  redirectUri: string;
}

export class KakaoAuthManager {
  private config: KakaoAuthConfig;

  constructor(config?: Partial<KakaoAuthConfig>) {
    this.config = {
      authUrl: config?.authUrl || process.env.NEXT_PUBLIC_KAKAO_AUTH_URL || "",
      clientId: config?.clientId || process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID || "",
      redirectUri: config?.redirectUri || process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI || ""
    };
    
    this.validateConfig();
  }

  private validateConfig(): void {
    if (!this.config.authUrl || !this.config.clientId || !this.config.redirectUri) {
      console.error("Kakao Auth configuration is incomplete");
    }
  }

  buildAuthUrl(postAuthRedirectUrl?: string): string {
    const params = new URLSearchParams({
      response_type: "code",
      client_id: this.config.clientId,
      redirect_uri: this.config.redirectUri
    });

    if (postAuthRedirectUrl) {
      params.append("state", encodeURIComponent(postAuthRedirectUrl));
    }

    return `${this.config.authUrl}?${params.toString()}`;
  }

  extractCodeFromUrl(searchParams: URLSearchParams): string | null {
    return searchParams.get("code");
  }

  extractErrorFromUrl(searchParams: URLSearchParams): string | null {
    return searchParams.get("error");
  }

  extractStateFromUrl(searchParams: URLSearchParams): string | null {
    const state = searchParams.get("state");
    return state ? decodeURIComponent(state) : null;
  }

  isCallbackUrl(pathname: string): boolean {
    return pathname === "/auth/callback";
  }

  redirectToKakaoAuth(postAuthRedirectUrl?: string): void {
    const authUrl = this.buildAuthUrl(postAuthRedirectUrl);
    window.location.href = authUrl;
  }
}

export const kakaoAuthManager = new KakaoAuthManager();