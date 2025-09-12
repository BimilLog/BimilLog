export interface SessionData {
  tempUserUuid?: string;
  returnUrl?: string;
  kakaoConsentUrl?: string;
  lastAuthTime?: number;
  authRetryCount?: number;
}

export class SessionManager {
  private static readonly STORAGE_PREFIX = "bimillog_auth_";
  
  private static readonly KEYS = {
    TEMP_USER_UUID: "temp_user_uuid",
    RETURN_URL: "return_url",
    KAKAO_CONSENT_URL: "kakao_consent_url",
    LAST_AUTH_TIME: "last_auth_time",
    AUTH_RETRY_COUNT: "auth_retry_count"
  } as const;

  private static readonly MAX_SESSION_AGE_MS = 60 * 60 * 1000; // 1 hour
  private static readonly MAX_RETRY_COUNT = 3;

  private getKey(key: string): string {
    return `${SessionManager.STORAGE_PREFIX}${key}`;
  }

  set(key: keyof typeof SessionManager.KEYS, value: string | number): void {
    try {
      const storageKey = this.getKey(SessionManager.KEYS[key]);
      sessionStorage.setItem(storageKey, String(value));
    } catch (error) {
      console.error(`Failed to set session data for ${key}:`, error);
    }
  }

  get(key: keyof typeof SessionManager.KEYS): string | null {
    try {
      const storageKey = this.getKey(SessionManager.KEYS[key]);
      return sessionStorage.getItem(storageKey);
    } catch (error) {
      console.error(`Failed to get session data for ${key}:`, error);
      return null;
    }
  }

  remove(key: keyof typeof SessionManager.KEYS): void {
    try {
      const storageKey = this.getKey(SessionManager.KEYS[key]);
      sessionStorage.removeItem(storageKey);
    } catch (error) {
      console.error(`Failed to remove session data for ${key}:`, error);
    }
  }

  clear(): void {
    try {
      Object.values(SessionManager.KEYS).forEach(key => {
        const storageKey = this.getKey(key);
        sessionStorage.removeItem(storageKey);
      });
    } catch (error) {
      console.error("Failed to clear session data:", error);
    }
  }

  setTempUserUuid(uuid: string): void {
    this.set("TEMP_USER_UUID", uuid);
    this.updateLastAuthTime();
  }

  getTempUserUuid(): string | null {
    return this.get("TEMP_USER_UUID");
  }

  setReturnUrl(url: string): void {
    this.set("RETURN_URL", url);
  }

  getReturnUrl(): string | null {
    return this.get("RETURN_URL");
  }

  setKakaoConsentUrl(url: string): void {
    this.set("KAKAO_CONSENT_URL", url);
  }

  getKakaoConsentUrl(): string | null {
    return this.get("KAKAO_CONSENT_URL");
  }

  updateLastAuthTime(): void {
    this.set("LAST_AUTH_TIME", Date.now());
  }

  getLastAuthTime(): number | null {
    const time = this.get("LAST_AUTH_TIME");
    return time ? parseInt(time, 10) : null;
  }

  isSessionValid(): boolean {
    const lastAuthTime = this.getLastAuthTime();
    if (!lastAuthTime) return false;
    
    const now = Date.now();
    return (now - lastAuthTime) < SessionManager.MAX_SESSION_AGE_MS;
  }

  incrementRetryCount(): number {
    const currentCount = this.getRetryCount();
    const newCount = currentCount + 1;
    this.set("AUTH_RETRY_COUNT", newCount);
    return newCount;
  }

  getRetryCount(): number {
    const count = this.get("AUTH_RETRY_COUNT");
    return count ? parseInt(count, 10) : 0;
  }

  resetRetryCount(): void {
    this.remove("AUTH_RETRY_COUNT");
  }

  canRetry(): boolean {
    return this.getRetryCount() < SessionManager.MAX_RETRY_COUNT;
  }

  isKakaoFriendConsentFlow(): boolean {
    return !!(this.getReturnUrl() && this.getKakaoConsentUrl());
  }

  clearKakaoFriendConsentData(): void {
    this.remove("RETURN_URL");
    this.remove("KAKAO_CONSENT_URL");
  }

  getAllSessionData(): SessionData {
    return {
      tempUserUuid: this.getTempUserUuid() || undefined,
      returnUrl: this.getReturnUrl() || undefined,
      kakaoConsentUrl: this.getKakaoConsentUrl() || undefined,
      lastAuthTime: this.getLastAuthTime() || undefined,
      authRetryCount: this.getRetryCount() || undefined
    };
  }
}

export const sessionManager = new SessionManager();