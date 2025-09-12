"use client";

import { useCallback } from "react";

export interface AuthSession {
  tempUserUuid?: string;
  returnUrl?: string;
  kakaoConsentUrl?: string;
  lastAuthTime?: number;
}

const SESSION_KEYS = {
  TEMP_USER_UUID: "tempUserUuid",
  RETURN_URL: "returnUrl",
  KAKAO_CONSENT_URL: "kakaoConsentUrl",
  LAST_AUTH_TIME: "lastAuthTime"
} as const;

export function useAuthSession() {
  const setTempUserUuid = useCallback((uuid: string) => {
    sessionStorage.setItem(SESSION_KEYS.TEMP_USER_UUID, uuid);
  }, []);

  const getTempUserUuid = useCallback((): string | null => {
    return sessionStorage.getItem(SESSION_KEYS.TEMP_USER_UUID);
  }, []);

  const clearTempUserUuid = useCallback(() => {
    sessionStorage.removeItem(SESSION_KEYS.TEMP_USER_UUID);
  }, []);

  const setReturnUrl = useCallback((url: string) => {
    sessionStorage.setItem(SESSION_KEYS.RETURN_URL, url);
  }, []);

  const getReturnUrl = useCallback((): string | null => {
    return sessionStorage.getItem(SESSION_KEYS.RETURN_URL);
  }, []);

  const clearReturnUrl = useCallback(() => {
    sessionStorage.removeItem(SESSION_KEYS.RETURN_URL);
  }, []);

  const setKakaoConsentUrl = useCallback((url: string) => {
    sessionStorage.setItem(SESSION_KEYS.KAKAO_CONSENT_URL, url);
  }, []);

  const getKakaoConsentUrl = useCallback((): string | null => {
    return sessionStorage.getItem(SESSION_KEYS.KAKAO_CONSENT_URL);
  }, []);

  const clearKakaoConsentUrl = useCallback(() => {
    sessionStorage.removeItem(SESSION_KEYS.KAKAO_CONSENT_URL);
  }, []);

  const setLastAuthTime = useCallback(() => {
    sessionStorage.setItem(SESSION_KEYS.LAST_AUTH_TIME, Date.now().toString());
  }, []);

  const getLastAuthTime = useCallback((): number | null => {
    const time = sessionStorage.getItem(SESSION_KEYS.LAST_AUTH_TIME);
    return time ? parseInt(time, 10) : null;
  }, []);

  const isSessionExpired = useCallback((expiryMinutes: number = 60): boolean => {
    const lastAuthTime = getLastAuthTime();
    if (!lastAuthTime) return true;
    
    const now = Date.now();
    const timeDiff = now - lastAuthTime;
    const minutesPassed = timeDiff / (1000 * 60);
    
    return minutesPassed > expiryMinutes;
  }, [getLastAuthTime]);

  const getFullSession = useCallback((): AuthSession => {
    return {
      tempUserUuid: getTempUserUuid() || undefined,
      returnUrl: getReturnUrl() || undefined,
      kakaoConsentUrl: getKakaoConsentUrl() || undefined,
      lastAuthTime: getLastAuthTime() || undefined
    };
  }, [getTempUserUuid, getReturnUrl, getKakaoConsentUrl, getLastAuthTime]);

  const clearSession = useCallback(() => {
    Object.values(SESSION_KEYS).forEach(key => {
      sessionStorage.removeItem(key);
    });
  }, []);

  const clearAuthRelatedSession = useCallback(() => {
    clearTempUserUuid();
    clearReturnUrl();
    clearKakaoConsentUrl();
  }, [clearTempUserUuid, clearReturnUrl, clearKakaoConsentUrl]);

  const isKakaoFriendConsentFlow = useCallback((): boolean => {
    return !!(getReturnUrl() && getKakaoConsentUrl());
  }, [getReturnUrl, getKakaoConsentUrl]);

  const handleKakaoFriendConsentComplete = useCallback((): string => {
    const returnUrl = getReturnUrl() || "/";
    clearReturnUrl();
    clearKakaoConsentUrl();
    return returnUrl;
  }, [getReturnUrl, clearReturnUrl, clearKakaoConsentUrl]);

  return {
    setTempUserUuid,
    getTempUserUuid,
    clearTempUserUuid,
    setReturnUrl,
    getReturnUrl,
    clearReturnUrl,
    setKakaoConsentUrl,
    getKakaoConsentUrl,
    clearKakaoConsentUrl,
    setLastAuthTime,
    getLastAuthTime,
    isSessionExpired,
    getFullSession,
    clearSession,
    clearAuthRelatedSession,
    isKakaoFriendConsentFlow,
    handleKakaoFriendConsentComplete
  };
}