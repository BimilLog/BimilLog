"use client";

import { useEffect, useState } from "react";
import {
  isIOS,
  isSafari,
  isAndroid,
  isKakaoInAppBrowser,
} from "@/lib/utils";

/**
 * 디바이스/브라우저 감지 결과 — SSR 가드 적용.
 *
 * 사용 패턴:
 *   const detection = useBrowserDetection();
 *   if (!detection.isClient) return <Spinner />;
 *   if (detection.isInApp) return <InAppGuide />;
 *
 * - 모든 boolean 은 hydration 전에 false 로 시작 (SSR 안전).
 * - hydration 후 useEffect 에서 한 번만 측정 → setState → re-render.
 * - hooks/common/useBrowserGuide 가 PWA prompt + 모달 상태를 관리한다면,
 *   본 훅은 단순 device classification 만 담당한다.
 */

export type BrowserKind =
  | "kakao-inapp"
  | "line-inapp"
  | "instagram-inapp"
  | "facebook-inapp"
  | "ios-safari"
  | "ios-chrome"
  | "android-chrome"
  | "desktop"
  | "unknown";

export interface BrowserDetection {
  /** hydration 완료 전에는 false. 분기 렌더링 시 가드로 사용. */
  isClient: boolean;
  /** iPhone/iPad/iPod */
  isIOSDevice: boolean;
  /** Safari (Chrome 제외) */
  isSafariBrowser: boolean;
  /** Android */
  isAndroidDevice: boolean;
  /** 카카오톡 인앱 브라우저 */
  isKakaoInApp: boolean;
  /** 모든 종류의 인앱 브라우저 (KakaoTalk/Line/Instagram/Facebook) */
  isInApp: boolean;
  /** 데스크톱 (모바일/태블릿/InApp 모두 false) */
  isDesktop: boolean;
  /** 분기용 단일 분류값 */
  kind: BrowserKind;
  /** 사용자 표시용 브라우저 이름 */
  name: string;
}

const SSR_DEFAULT: BrowserDetection = {
  isClient: false,
  isIOSDevice: false,
  isSafariBrowser: false,
  isAndroidDevice: false,
  isKakaoInApp: false,
  isInApp: false,
  isDesktop: false,
  kind: "unknown",
  name: "브라우저",
};

function detect(): BrowserDetection {
  if (typeof navigator === "undefined") return SSR_DEFAULT;

  const ua = navigator.userAgent;
  const iOSDevice = isIOS();
  const safariBrowser = isSafari();
  const androidDevice = isAndroid();
  const kakaoInApp = isKakaoInAppBrowser();

  const lineInApp = /Line/i.test(ua);
  const instaInApp = /Instagram/i.test(ua);
  const fbInApp = /FBAN|FBAV|Facebook/i.test(ua);
  const inApp = kakaoInApp || lineInApp || instaInApp || fbInApp;

  const isMobile = iOSDevice || androidDevice;
  const isDesktop = !isMobile && !inApp;

  let kind: BrowserKind = "unknown";
  let name = "알 수 없는 브라우저";

  if (kakaoInApp) {
    kind = "kakao-inapp";
    name = "카카오톡 인앱 브라우저";
  } else if (lineInApp) {
    kind = "line-inapp";
    name = "라인 인앱 브라우저";
  } else if (instaInApp) {
    kind = "instagram-inapp";
    name = "인스타그램 인앱 브라우저";
  } else if (fbInApp) {
    kind = "facebook-inapp";
    name = "페이스북 인앱 브라우저";
  } else if (iOSDevice && safariBrowser) {
    kind = "ios-safari";
    name = "Safari";
  } else if (iOSDevice) {
    kind = "ios-chrome";
    name = "iOS Chrome";
  } else if (androidDevice) {
    kind = "android-chrome";
    name = "Android Chrome";
  } else if (isDesktop) {
    kind = "desktop";
    if (/Edg/i.test(ua)) name = "Edge";
    else if (/Chrome/i.test(ua)) name = "Chrome";
    else if (/Firefox/i.test(ua)) name = "Firefox";
    else if (/Safari/i.test(ua)) name = "Safari";
    else name = "데스크톱 브라우저";
  }

  return {
    isClient: true,
    isIOSDevice: iOSDevice,
    isSafariBrowser: safariBrowser,
    isAndroidDevice: androidDevice,
    isKakaoInApp: kakaoInApp,
    isInApp: inApp,
    isDesktop,
    kind,
    name,
  };
}

export function useBrowserDetection(): BrowserDetection {
  const [detection, setDetection] = useState<BrowserDetection>(SSR_DEFAULT);

  useEffect(() => {
    setDetection(detect());
  }, []);

  return detection;
}
