"use client";

import { useEffect } from "react";
import { isAndroid, isKakaoInAppBrowser, logger } from "@/lib/utils";

const REDIRECT_ATTEMPT_KEY = "kakao_intent_redirect_attempt";
const CHROME_PACKAGE = "com.android.chrome";
const CHROME_FALLBACK_URL = "https://play.google.com/store/apps/details?id=com.android.chrome";

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
    const intentUrl = `intent://${sanitizedTarget}#Intent;scheme=${scheme};package=${CHROME_PACKAGE};S.browser_fallback_url=${encodeURIComponent(
      CHROME_FALLBACK_URL
    )};end`;

    try {
      window.location.replace(intentUrl);
    } catch (error) {
      logger.error("Chrome 인텐트 호출 실패:", error);
      window.location.href = CHROME_FALLBACK_URL;
    }
  }, []);

  return null;
}
