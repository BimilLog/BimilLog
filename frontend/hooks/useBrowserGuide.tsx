"use client";

import { useState, useEffect } from "react";
import { storage } from "@/lib/storage";

export function useBrowserGuide() {
  const [isKakaoInApp, setIsKakaoInApp] = useState(false);
  const [showGuide, setShowGuide] = useState(false);
  const [isPWAInstallable, setIsPWAInstallable] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);

  useEffect(() => {
    // 클라이언트 환경에서만 실행
    if (typeof window === "undefined" || typeof navigator === "undefined") {
      return;
    }

      // 카카오톡 인앱 브라우저 감지
  const detectKakaoInApp = () => {
    const userAgent = navigator.userAgent;
    if (process.env.NODE_ENV === 'development') {
      console.log("User Agent:", userAgent);
    }

      // 더 정확한 카카오톡 인앱 브라우저 감지
      const isKakao =
        /KAKAOTALK/i.test(userAgent) ||
        /KAKAO/i.test(userAgent) ||
        userAgent.includes("KAKAO");

      // 개발 환경에서 테스트를 위한 강제 표시 (URL 파라미터 확인)
      const urlParams = new URLSearchParams(window.location.search);
      const forceShow = urlParams.get("show-guide") === "true";

      setIsKakaoInApp(isKakao);

      // 로컬 스토리지에서 가이드 숨김 상태 확인
      const guideHidden = storage.local.getBrowserGuideHidden();
      const hideUntil = storage.local.getBrowserGuideHideUntil();

          if (process.env.NODE_ENV === 'development') {
      console.log("Browser detection:", {
        isKakao,
        forceShow,
        guideHidden,
        hideUntil,
      });
    }

      if ((isKakao || forceShow) && !guideHidden) {
        // 숨김 기간 확인
        if (hideUntil && new Date().getTime() < hideUntil) {
          if (process.env.NODE_ENV === 'development') {
            console.log("Guide hidden until:", new Date(hideUntil));
          }
          return;
        }
        setShowGuide(true);
      }
    };

    // PWA 설치 가능 여부 감지
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setIsPWAInstallable(true);
    };

    // PWA 설치 완료 감지
    const handleAppInstalled = () => {
      setIsPWAInstallable(false);
      setDeferredPrompt(null);
      if (process.env.NODE_ENV === 'development') {
        console.log("PWA가 설치되었습니다!");
      }
    };

    detectKakaoInApp();

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    window.addEventListener("appinstalled", handleAppInstalled);

    return () => {
      window.removeEventListener(
        "beforeinstallprompt",
        handleBeforeInstallPrompt
      );
      window.removeEventListener("appinstalled", handleAppInstalled);
    };
  }, []);

  const installPWA = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
              if (process.env.NODE_ENV === 'development') {
          console.log(`User response to the install prompt: ${outcome}`);
        }
      setDeferredPrompt(null);
      setIsPWAInstallable(false);
    }
  };

  const hideGuide = (
    duration: "session" | "1h" | "24h" | "7d" | "forever" = "24h"
  ) => {
    setShowGuide(false);

    switch (duration) {
      case "session":
        // 세션 동안만 숨김 (새로고침 시 다시 표시)
        break;
      case "1h":
        // 1시간 동안 숨김
        const hideUntil1h = new Date().getTime() + 1 * 60 * 60 * 1000;
        storage.local.setBrowserGuideHideUntil(hideUntil1h);
        break;
      case "24h":
        // 24시간 동안 숨김
        const hideUntil24h = new Date().getTime() + 24 * 60 * 60 * 1000;
        storage.local.setBrowserGuideHideUntil(hideUntil24h);
        break;
      case "7d":
        // 7일 동안 숨김
        const hideUntil7d = new Date().getTime() + 7 * 24 * 60 * 60 * 1000;
        storage.local.setBrowserGuideHideUntil(hideUntil7d);
        break;
      case "forever":
        // 영원히 숨김
        storage.local.setBrowserGuideHidden(true);
        break;
    }
  };

  const getBrowserInfo = () => {
    // SSR 환경에서는 기본값 반환
    if (typeof window === "undefined" || typeof navigator === "undefined") {
      return { name: "브라우저", isInApp: false };
    }

    const userAgent = navigator.userAgent;

    if (/KAKAO/i.test(userAgent) || /KakaoTalk/i.test(userAgent)) {
      return { name: "카카오톡 인앱 브라우저", isInApp: true };
    }
    if (/Line/i.test(userAgent)) {
      return { name: "라인 인앱 브라우저", isInApp: true };
    }
    if (/Instagram/i.test(userAgent)) {
      return { name: "인스타그램 인앱 브라우저", isInApp: true };
    }
    if (/Facebook/i.test(userAgent)) {
      return { name: "페이스북 인앱 브라우저", isInApp: true };
    }
    if (/Safari/i.test(userAgent) && !/Chrome/i.test(userAgent)) {
      return { name: "Safari", isInApp: false };
    }
    if (/Chrome/i.test(userAgent)) {
      return { name: "Chrome", isInApp: false };
    }
    if (/Samsung/i.test(userAgent)) {
      return { name: "삼성 인터넷", isInApp: false };
    }

    return { name: "알 수 없는 브라우저", isInApp: false };
  };

  return {
    isKakaoInApp,
    showGuide,
    isPWAInstallable,
    installPWA,
    hideGuide,
    getBrowserInfo,
  };
}
