"use client";

import { useEffect } from "react";
import { isAndroid, isKakaoInAppBrowser, logger } from "@/lib/utils";

const REDIRECT_ATTEMPT_KEY = "kakao_intent_redirect_attempt";
const CHROME_PACKAGE = "com.android.chrome";
const CHROME_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.android.chrome";

export function KakaoExternalBrowserRedirect() {
  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!isAndroid() || !isKakaoInAppBrowser()) return;

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

    const scheme = window.location.protocol.replace(":", "") || "https";
    const sanitizedTarget = currentUrl.replace(/^https?:\/\//i, "");
    const intentUrl = `intent://${sanitizedTarget}#Intent;scheme=${scheme};package=${CHROME_PACKAGE};end`;

    let fallbackTimer: ReturnType<typeof setTimeout> | null = null;

    const fallback = () => {
      window.location.href = CHROME_PLAY_STORE_URL;
    };

    try {
      fallbackTimer = setTimeout(fallback, 1500);
      window.location.replace(intentUrl);
    } catch (error) {
      logger.error("Chrome 인텐트 호출에 실패했습니다:", error);
      if (fallbackTimer) {
        clearTimeout(fallbackTimer);
        fallbackTimer = null;
      }
      fallback();
    }

    return () => {
      if (fallbackTimer) {
        clearTimeout(fallbackTimer);
      }
    };
  }, []);

  return null;
}
