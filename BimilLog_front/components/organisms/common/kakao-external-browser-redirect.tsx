"use client";

import { useEffect } from "react";
import { isAndroid, isIOS, isKakaoInAppBrowser, logger } from "@/lib/utils";

const REDIRECT_ATTEMPT_KEY = "kakao_intent_redirect_attempt";
const CHROME_PACKAGE = "com.android.chrome";
const CHROME_FALLBACK_URL = "https://play.google.com/store/apps/details?id=com.android.chrome";

export function KakaoExternalBrowserRedirect() {
  useEffect(() => {
    if (typeof window === "undefined") return;
    
    // 카카오톡 인앱 브라우저만 체크 (Android + iOS 모두)
    if (!isKakaoInAppBrowser()) return;

    const currentUrl = window.location.href;
    if (!currentUrl.startsWith("http")) return;

    try {
      const lastAttempt = sessionStorage.getItem(REDIRECT_ATTEMPT_KEY);
      if (lastAttempt === currentUrl) {
        return;
      }
      sessionStorage.setItem(REDIRECT_ATTEMPT_KEY, currentUrl);
    } catch (error) {
      logger.warn("세션 스토리지 접근에 실패했습니다:", error);
    }

    // Android: Chrome 인텐트
    if (isAndroid()) {
      const scheme = window.location.protocol.replace(":", "") || "https";
      const sanitizedTarget = currentUrl.replace(/^https?:\/\//i, "");
      const intentUrl = `intent://${sanitizedTarget}#Intent;scheme=${scheme};package=${CHROME_PACKAGE};S.browser_fallback_url=${encodeURIComponent(
        CHROME_FALLBACK_URL
      )};end`;

      try {
        window.location.replace(intentUrl);
      } catch (error) {
        logger.error("Chrome 인텐트 호출 실패:", error);
        window.location.href = CHROME_FALLBACK_URL;
      }
    } 
    // iOS: Chrome URL Scheme
    else if (isIOS()) {
      const url = currentUrl.replace(/^https?:\/\//i, '');
      const scheme = window.location.protocol === 'https:' ? 'googlechromes' : 'googlechrome';
      const chromeUrl = `${scheme}://${url}`;

      try {
        window.location.href = chromeUrl;
        
        // Chrome 미설치 시 Safari에서 계속 (폴백)
        // Chrome이 설치되어 있으면 이 코드는 실행되지 않음
        setTimeout(() => {
          // 500ms 후에도 페이지가 여전히 열려 있으면 Safari에서 계속
          logger.info("Chrome 미설치, Safari에서 계속");
        }, 500);
      } catch (error) {
        logger.error("Chrome URL Scheme 호출 실패:", error);
        // Safari에서 계속 진행
      }
    }
  }, []);

  return null;
}
