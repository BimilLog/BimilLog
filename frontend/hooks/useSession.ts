"use client";

import { useCallback } from "react";

export interface SessionData {
  tempUserUuid?: string;
  returnUrl?: string;
  kakaoConsentUrl?: string;
  lastAuthTime?: number;
  authRetryCount?: number;
}

interface SessionConfig {
  maxSessionAgeMs: number;
  maxRetryCount: number;
  storagePrefix: string;
}

const DEFAULT_CONFIG: SessionConfig = {
  maxSessionAgeMs: 60 * 60 * 1000, // 1 hour
  maxRetryCount: 3,
  storagePrefix: "bimillog_auth_"
};

const SESSION_KEYS = {
  TEMP_USER_UUID: "temp_user_uuid",
  RETURN_URL: "return_url", 
  KAKAO_CONSENT_URL: "kakao_consent_url",
  LAST_AUTH_TIME: "last_auth_time",
  AUTH_RETRY_COUNT: "auth_retry_count"
} as const;

export function useSession(config: Partial<SessionConfig> = {}) {
  const fullConfig = { ...DEFAULT_CONFIG, ...config };

  const getKey = useCallback((key: string): string => {
    return `${fullConfig.storagePrefix}${key}`;
  }, [fullConfig.storagePrefix]);

  const set = useCallback((key: keyof typeof SESSION_KEYS, value: string | number): void => {
    try {
      const storageKey = getKey(SESSION_KEYS[key]);
      sessionStorage.setItem(storageKey, String(value));
    } catch (error) {
      console.error(`Failed to set session data for ${key}:`, error);
    }
  }, [getKey]);

  const get = useCallback((key: keyof typeof SESSION_KEYS): string | null => {
    try {
      const storageKey = getKey(SESSION_KEYS[key]);
      return sessionStorage.getItem(storageKey);
    } catch (error) {
      console.error(`Failed to get session data for ${key}:`, error);
      return null;
    }
  }, [getKey]);

  const remove = useCallback((key: keyof typeof SESSION_KEYS): void => {
    try {
      const storageKey = getKey(SESSION_KEYS[key]);
      sessionStorage.removeItem(storageKey);
    } catch (error) {
      console.error(`Failed to remove session data for ${key}:`, error);
    }
  }, [getKey]);

  const clear = useCallback((): void => {
    try {
      Object.values(SESSION_KEYS).forEach(key => {
        const storageKey = getKey(key);
        sessionStorage.removeItem(storageKey);
      });
    } catch (error) {
      console.error("Failed to clear session data:", error);
    }
  }, [getKey]);

  const setTempUserUuid = useCallback((uuid: string): void => {
    set("TEMP_USER_UUID", uuid);
    set("LAST_AUTH_TIME", Date.now());
  }, [set]);

  const getTempUserUuid = useCallback((): string | null => {
    return get("TEMP_USER_UUID");
  }, [get]);

  const clearTempUserUuid = useCallback((): void => {
    remove("TEMP_USER_UUID");
  }, [remove]);

  const setReturnUrl = useCallback((url: string): void => {
    set("RETURN_URL", url);
  }, [set]);

  const getReturnUrl = useCallback((): string | null => {
    return get("RETURN_URL");
  }, [get]);

  const clearReturnUrl = useCallback((): void => {
    remove("RETURN_URL");
  }, [remove]);

  const setKakaoConsentUrl = useCallback((url: string): void => {
    set("KAKAO_CONSENT_URL", url);
  }, [set]);

  const getKakaoConsentUrl = useCallback((): string | null => {
    return get("KAKAO_CONSENT_URL");
  }, [get]);

  const clearKakaoConsentUrl = useCallback((): void => {
    remove("KAKAO_CONSENT_URL");
  }, [remove]);

  const updateLastAuthTime = useCallback((): void => {
    set("LAST_AUTH_TIME", Date.now());
  }, [set]);

  const getLastAuthTime = useCallback((): number | null => {
    const time = get("LAST_AUTH_TIME");
    return time ? parseInt(time, 10) : null;
  }, [get]);

  const isSessionValid = useCallback((): boolean => {
    const lastAuthTime = getLastAuthTime();
    if (!lastAuthTime) return false;
    
    const now = Date.now();
    return (now - lastAuthTime) < fullConfig.maxSessionAgeMs;
  }, [getLastAuthTime, fullConfig.maxSessionAgeMs]);

  const incrementRetryCount = useCallback((): number => {
    const currentCount = getRetryCount();
    const newCount = currentCount + 1;
    set("AUTH_RETRY_COUNT", newCount);
    return newCount;
  }, [set]);

  const getRetryCount = useCallback((): number => {
    const count = get("AUTH_RETRY_COUNT");
    return count ? parseInt(count, 10) : 0;
  }, [get]);

  const resetRetryCount = useCallback((): void => {
    remove("AUTH_RETRY_COUNT");
  }, [remove]);

  const canRetry = useCallback((): boolean => {
    return getRetryCount() < fullConfig.maxRetryCount;
  }, [getRetryCount, fullConfig.maxRetryCount]);

  const isKakaoFriendConsentFlow = useCallback((): boolean => {
    return !!(getReturnUrl() && getKakaoConsentUrl());
  }, [getReturnUrl, getKakaoConsentUrl]);

  const clearKakaoFriendConsentData = useCallback((): void => {
    clearReturnUrl();
    clearKakaoConsentUrl();
  }, [clearReturnUrl, clearKakaoConsentUrl]);

  const getAllSessionData = useCallback((): SessionData => {
    return {
      tempUserUuid: getTempUserUuid() || undefined,
      returnUrl: getReturnUrl() || undefined,
      kakaoConsentUrl: getKakaoConsentUrl() || undefined,
      lastAuthTime: getLastAuthTime() || undefined,
      authRetryCount: getRetryCount() || undefined
    };
  }, [getTempUserUuid, getReturnUrl, getKakaoConsentUrl, getLastAuthTime, getRetryCount]);

  const handleKakaoFriendConsentComplete = useCallback((): string => {
    const returnUrl = getReturnUrl() || "/";
    clearKakaoFriendConsentData();
    return returnUrl;
  }, [getReturnUrl, clearKakaoFriendConsentData]);

  return {
    // Basic operations
    set,
    get,
    remove,
    clear,
    
    // Temp user UUID
    setTempUserUuid,
    getTempUserUuid,
    clearTempUserUuid,
    
    // Return URL
    setReturnUrl,
    getReturnUrl,
    clearReturnUrl,
    
    // Kakao consent URL
    setKakaoConsentUrl,
    getKakaoConsentUrl,
    clearKakaoConsentUrl,
    
    // Auth time
    updateLastAuthTime,
    getLastAuthTime,
    isSessionValid,
    
    // Retry count
    incrementRetryCount,
    getRetryCount,
    resetRetryCount,
    canRetry,
    
    // Kakao friend consent flow
    isKakaoFriendConsentFlow,
    clearKakaoFriendConsentData,
    handleKakaoFriendConsentComplete,
    
    // Utils
    getAllSessionData
  };
}