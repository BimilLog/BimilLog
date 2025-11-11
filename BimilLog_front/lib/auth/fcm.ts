import { isMobileOrTablet, isKakaoInAppBrowser, logger } from "@/lib/utils";

/**
 * FCM 토큰 가져오기 (모바일/태블릿에서만)
 */
export async function getFCMToken(): Promise<string | null> {
  // 모바일/태블릿이 아니면 FCM 토큰을 가져오지 않음
  if (!isMobileOrTablet()) {
    logger.log('데스크톱 환경 - FCM 토큰 건너뛰기');
    return null
  }

  if (!('serviceWorker' in navigator)) {
    logger.log('서비스워커 미지원 환경 - FCM 토큰 생략');
    return null;
  }

  try {
    const { getMessaging, getToken, isSupported } = await import('firebase/messaging');
    const messagingSupported = await isSupported();

    if (!messagingSupported) {
      logger.log('브라우저가 FCM을 지원하지 않습니다.');
      return null;
    }

    const { initializeApp, getApps } = await import('firebase/app');

    let app;
    if (getApps().length === 0) {
      const firebaseConfig = {
        apiKey: "AIzaSyDQHWI_zhIjqp_SJz0Fdv7xtG6mIZfwBhU",
        authDomain: "growfarm-6cd79.firebaseapp.com",
        projectId: "growfarm-6cd79",
        storageBucket: "growfarm-6cd79.firebasestorage.app",
        messagingSenderId: "763805350293",
        appId: "1:763805350293:web:68b1b3ca3a294b749b1e9c",
        measurementId: "G-G9C4KYCEEJ"
      };
      app = initializeApp(firebaseConfig);
    } else {
      app = getApps()[0];
    }

    const messaging = getMessaging(app);

    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');

    // 서비스 워커가 active 상태가 될 때까지 대기
    await navigator.serviceWorker.ready;

    const token = await getToken(messaging, {
      serviceWorkerRegistration: registration
    });

    if (token) {
      logger.log('FCM 토큰 획득 성공:', token.substring(0, 20) + '...');
      return token;
    }

    logger.log('FCM 토큰을 가져올 수 없습니다. 브라우저 알림 권한을 확인해주세요.');
    return null;
  } catch (error) {
    logger.error('FCM 토큰 발급 실패:', error);
    return null;
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

  // 싱글톤 패턴으로 FCMManager 인스턴스를 하나만 유지
  static getInstance(): FCMManager {
    if (!FCMManager.instance) {
      FCMManager.instance = new FCMManager();
    }
    return FCMManager.instance;
  }

  async getToken(): Promise<FCMTokenResult> {
    // 1단계: 디바이스 지원 여부 우선 판단
    const isSupported = this.isSupported();

    if (!isSupported) {
      return {
        token: null,
        isSupported: false,
        error: new Error("FCM is not supported on this device")
      };
    }

    try {
      // 2단계: 캐시된 토큰이 유효한지 확인 (30분 캐시)
      if (this.isCachedTokenValid()) {
        // 캐시 히트 시 서버 요청 없이 바로 반환
        return {
          token: this.cachedToken,
          isSupported: true
        };
      }

      // 3단계: 캐시 미스 시 새로운 토큰 요청
      const token = await this.fetchNewToken();
      // 4단계: 새 토큰 캐싱으로 향후 요청 최적화
      this.cacheToken(token);

      return {
        token,
        isSupported: true
      };
    } catch (error) {
      logger.error("FCM 토큰 획득 중 오류 발생:", error);
      return {
        token: null,
        isSupported: true,
        error: error instanceof Error ? error : new Error("FCM 토큰 획득 실패")
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
      logger.error("FCM 토큰 획득 실패:", error);
      return null;
    }
  }

  // FCM 지원 여부 판단 조건들을 체크
  isSupported(): boolean {
    // SSR 환경에서는 window 객체가 없으므로 지원하지 않음
    if (typeof window === "undefined") {
      return false;
    }

    // 모바일/태블릿 환경이면서 브라우저가 알림 API를 지원하는 경우만 FCM 지원
    return isMobileOrTablet() && "Notification" in window && !isKakaoInAppBrowser();
  }

  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported()) {
      return "denied";
    }

    try {
      const permission = await Notification.requestPermission();
      return permission;
    } catch (error) {
      logger.error("알림 권한 요청 실패:", error);
      return "denied";
    }
  }

  getPermissionStatus(): NotificationPermission {
    if (!this.isSupported()) {
      return "denied";
    }
    
    return Notification.permission;
  }

  // 캐시 메커니즘: 토큰과 만료 시간을 동시에 체크
  private isCachedTokenValid(): boolean {
    // 토큰이나 만료 시간이 없으면 캐시 무효
    if (!this.cachedToken || !this.tokenExpiryTime) {
      return false;
    }

    // 현재 시간이 만료 시간보다 이전이면 캐시 유효 (30분 캐시)
    return Date.now() < this.tokenExpiryTime;
  }

  // 권한-토큰 발급 비동기 체인: 권한 획득 → 토큰 발급 순서로 진행
  private async fetchNewToken(): Promise<string | null> {
    // 1단계: 알림 권한 요청
    const permission = await this.requestPermission();

    // 2단계: 권한이 승인되지 않으면 토큰 발급 중단
    if (permission !== "granted") {
      logger.log(`알림 권한이 거부되었습니다 (상태: ${permission})`);
      return null;
    }

    // 3단계: 권한 승인 후 FCM 토큰 발급 요청
    return await getFCMToken();
  }

  // 토큰 캐싱: 토큰과 만료 시간을 함께 저장
  private cacheToken(token: string | null): void {
    this.cachedToken = token;
    // 토큰이 있을 때만 만료 시간 설정 (현재 시간 + 30분)
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
