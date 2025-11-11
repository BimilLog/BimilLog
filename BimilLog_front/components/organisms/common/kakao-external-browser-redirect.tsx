"use client";

import { useEffect } from "react";
import { isAndroid, isKakaoInAppBrowser, logger } from "@/lib/utils";

const REDIRECT_ATTEMPT_KEY = "kakao_intent_redirect_attempt";

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

    const target = currentUrl.replace(/^https?:\/\//i, "");
    const chromeUrl = `googlechrome://${target}`;

    try {
      window.location.href = chromeUrl;
    } catch (error) {
      logger.error("Chrome URL 스킴 호출에 실패했습니다:", error);
    }
  }, []);

  return null;
}
