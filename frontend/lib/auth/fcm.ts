import { isMobileOrTablet, logger } from "@/lib/utils";

/**
 * FCM 토큰 가져오기 (모바일/태블릿에서만)
 */
export async function getFCMToken(): Promise<string | null> {
  // 모바일/태블릿이 아니면 FCM 토큰을 가져오지 않음
  if (!isMobileOrTablet()) {
    logger.log('데스크톱 환경 - FCM 토큰 건너뛰기');
    return null
  }

  try {
    // Firebase 관련 모듈 동적 import (SSR 안전)
    const { getMessaging, getToken } = await import('firebase/messaging')
    const { initializeApp, getApps } = await import('firebase/app')

    // Firebase 앱이 이미 초기화되었는지 확인
    let app
    if (getApps().length === 0) {
      // Firebase 설정
      const firebaseConfig = {
        apiKey: "AIzaSyDQHWI_zhIjqp_SJz0Fdv7xtG6mIZfwBhU",
        authDomain: "growfarm-6cd79.firebaseapp.com",
        projectId: "growfarm-6cd79",
        storageBucket: "growfarm-6cd79.firebasestorage.app",
        messagingSenderId: "763805350293",
        appId: "1:763805350293:web:68b1b3ca3a294b749b1e9c",
        measurementId: "G-G9C4KYCEEJ"
      }
      app = initializeApp(firebaseConfig)
    } else {
      app = getApps()[0]
    }

    const messaging = getMessaging(app)

    // 서비스워커 등록
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js')

    // FCM 토큰 가져오기 (서버->클라이언트 일방향이므로 VAPID 키 불필요)
    const token = await getToken(messaging, {
      serviceWorkerRegistration: registration
    })

    if (token) {
      logger.log('FCM 토큰 획득 성공:', token.substring(0, 20) + '...');
      return token
    } else {
      logger.log('FCM 토큰 획득 실패 - 브라우저 알림 권한을 확인해주세요.');
      return null
    }
  } catch (error) {
    logger.error('FCM 토큰 가져오기 실패:', error)
    return null
  }
}

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
      logger.error("Failed to get FCM token:", error);
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
      logger.error("FCM token acquisition failed:", error);
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
      logger.error("Failed to request notification permission:", error);
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

    return await getFCMToken();
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