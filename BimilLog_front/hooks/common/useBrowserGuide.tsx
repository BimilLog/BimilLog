"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { storage, logger } from "@/lib/utils";

type HideDuration = "session" | "1h" | "24h" | "7d" | "forever";

interface BrowserInfo {
  name: string;
  isInApp: boolean;
}

interface BrowserGuideContextValue {
  isKakaoInApp: boolean;
  showGuide: boolean;
  isPWAInstallable: boolean;
  installPWA: () => Promise<void>;
  hideGuide: (duration?: HideDuration) => void;
  getBrowserInfo: () => BrowserInfo;
}

const BrowserGuideContext = createContext<BrowserGuideContextValue | null>(
  null
);

const isBrowserEnvironment = () =>
  typeof window !== "undefined" && typeof navigator !== "undefined";

interface BrowserGuideProviderProps {
  children: ReactNode;
}

export function BrowserGuideProvider({ children }: BrowserGuideProviderProps) {
  const [isKakaoInApp, setIsKakaoInApp] = useState(false);
  const [showGuide, setShowGuide] = useState(false);
  const [isPWAInstallable, setIsPWAInstallable] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);

  useEffect(() => {
    if (!isBrowserEnvironment()) {
      return;
    }

    const detectKakaoInApp = () => {
      const userAgent = navigator.userAgent;
      logger.log("User Agent:", userAgent);

      const isKakao =
        /KAKAOTALK/i.test(userAgent) ||
        /KAKAO/i.test(userAgent) ||
        userAgent.includes("KAKAO");

      const urlParams = new URLSearchParams(window.location.search);
      const forceShow = urlParams.get("show-guide") === "true";

      setIsKakaoInApp(isKakao);

      const guideHidden = storage.local.getBrowserGuideHidden();
      const hideUntil = storage.local.getBrowserGuideHideUntil();

      logger.log("Browser detection:", {
        isKakao,
        forceShow,
        guideHidden,
        hideUntil,
      });

      if ((isKakao || forceShow) && !guideHidden) {
        if (hideUntil && new Date().getTime() < hideUntil) {
          logger.log("Guide hidden until:", new Date(hideUntil));
          return;
        }
        setShowGuide(true);
      }
    };

    const handleBeforeInstallPrompt = (event: Event) => {
      event.preventDefault();
      setDeferredPrompt(event);
      setIsPWAInstallable(true);
    };

    const handleAppInstalled = () => {
      setIsPWAInstallable(false);
      setDeferredPrompt(null);
      logger.log("PWA가 설치되었어요.");
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

  const installPWA = useCallback(async () => {
    if (!deferredPrompt) {
      return;
    }

    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    logger.log(`User response to the install prompt: ${outcome}`);
    setDeferredPrompt(null);
    setIsPWAInstallable(false);
  }, [deferredPrompt]);

  const hideGuide = useCallback(
    (duration: HideDuration = "24h") => {
      setShowGuide(false);

      switch (duration) {
        case "session":
          break;
        case "1h": {
          const hideUntil1h = new Date().getTime() + 1 * 60 * 60 * 1000;
          storage.local.setBrowserGuideHideUntil(hideUntil1h);
          break;
        }
        case "24h": {
          const hideUntil24h = new Date().getTime() + 24 * 60 * 60 * 1000;
          storage.local.setBrowserGuideHideUntil(hideUntil24h);
          break;
        }
        case "7d": {
          const hideUntil7d = new Date().getTime() + 7 * 24 * 60 * 60 * 1000;
          storage.local.setBrowserGuideHideUntil(hideUntil7d);
          break;
        }
        case "forever":
          storage.local.setBrowserGuideHidden(true);
          break;
      }
    },
    []
  );

  const getBrowserInfo = useCallback((): BrowserInfo => {
    if (!isBrowserEnvironment()) {
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
      return { name: "Samsung Internet", isInApp: false };
    }

    return { name: "알 수 없는 브라우저", isInApp: false };
  }, []);

  const contextValue = useMemo(
    () => ({
      isKakaoInApp,
      showGuide,
      isPWAInstallable,
      installPWA,
      hideGuide,
      getBrowserInfo,
    }),
    [getBrowserInfo, hideGuide, installPWA, isKakaoInApp, isPWAInstallable, showGuide]
  );

  return (
    <BrowserGuideContext.Provider value={contextValue}>
      {children}
    </BrowserGuideContext.Provider>
  );
}

export function useBrowserGuide(): BrowserGuideContextValue {
  const context = useContext(BrowserGuideContext);

  if (context === null) {
    throw new Error("useBrowserGuide must be used within a BrowserGuideProvider");
  }

  return context;
}
