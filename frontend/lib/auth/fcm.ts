import { getFCMToken as getUtilFCMToken, isMobileOrTablet } from "@/lib/utils";

export interface FCMTokenResult {
  token: string | null;
  error?: Error;
  isSupported: boolean;
}

export class FCMManager {
  private static instance: FCMManager;
  private cachedToken: string | null = null;
  private tokenExpiryTime: number | null = null;
  private static readonly TOKEN_CACHE_DURATION_MS = 30 * 60 * 1000; // 30 minutes

  private constructor() {}

  static getInstance(): FCMManager {
    if (!FCMManager.instance) {
      FCMManager.instance = new FCMManager();
    }
    return FCMManager.instance;
  }

  async getToken(): Promise<FCMTokenResult> {
    const isSupported = this.isSupported();
    
    if (!isSupported) {
      return {
        token: null,
        isSupported: false,
        error: new Error("FCM is not supported on this device")
      };
    }

    try {
      if (this.isCachedTokenValid()) {
        return {
          token: this.cachedToken,
          isSupported: true
        };
      }

      const token = await this.fetchNewToken();
      this.cacheToken(token);
      
      return {
        token,
        isSupported: true
      };
    } catch (error) {
      console.error("Failed to get FCM token:", error);
      return {
        token: null,
        isSupported: true,
        error: error instanceof Error ? error : new Error("Unknown FCM error")
      };
    }
  }

  async tryGetToken(): Promise<string | null> {
    if (!this.isSupported()) {
      return null;
    }

    try {
      const result = await this.getToken();
      return result.token;
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error("FCM token acquisition failed:", error);
      }
      return null;
    }
  }

  isSupported(): boolean {
    if (typeof window === "undefined") {
      return false;
    }
    
    return isMobileOrTablet() && "Notification" in window;
  }

  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported()) {
      return "denied";
    }

    try {
      const permission = await Notification.requestPermission();
      return permission;
    } catch (error) {
      console.error("Failed to request notification permission:", error);
      return "denied";
    }
  }

  getPermissionStatus(): NotificationPermission {
    if (!this.isSupported()) {
      return "denied";
    }
    
    return Notification.permission;
  }

  private isCachedTokenValid(): boolean {
    if (!this.cachedToken || !this.tokenExpiryTime) {
      return false;
    }
    
    return Date.now() < this.tokenExpiryTime;
  }

  private async fetchNewToken(): Promise<string | null> {
    const permission = await this.requestPermission();
    
    if (permission !== "granted") {
      return null;
    }

    return await getUtilFCMToken();
  }

  private cacheToken(token: string | null): void {
    this.cachedToken = token;
    this.tokenExpiryTime = token ? Date.now() + FCMManager.TOKEN_CACHE_DURATION_MS : null;
  }

  clearCache(): void {
    this.cachedToken = null;
    this.tokenExpiryTime = null;
  }

  async ensureTokenForLogin(): Promise<string | undefined> {
    if (!this.isSupported()) {
      return undefined;
    }

    const token = await this.tryGetToken();
    return token || undefined;
  }
}

export const fcmManager = FCMManager.getInstance();