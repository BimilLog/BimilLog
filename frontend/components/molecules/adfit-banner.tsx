"use client";

import { useEffect, useRef } from "react";

interface AdFitBannerProps {
  /**
   * AdFit에서 발급받은 광고단위 ID
   */
  adUnit: string;
  /**
   * 광고 가로 사이즈
   */
  width: number;
  /**
   * 광고 세로 사이즈
   */
  height: number;
  /**
   * 광고 로딩 실패 시 실행될 콜백 함수
   */
  onAdFail?: () => void;
  /**
   * 추가 CSS 클래스
   */
  className?: string;
}

export function AdFitBanner({
  adUnit,
  width,
  height,
  onAdFail,
  className = "",
}: AdFitBannerProps) {
  const adRef = useRef<HTMLDivElement>(null);
  const scriptLoadedRef = useRef(false);

  useEffect(() => {
    // SSR 환경에서는 실행하지 않음
    if (typeof window === "undefined") return;

    // 이미 스크립트가 로딩되었다면 재실행하지 않음
    if (scriptLoadedRef.current) return;

    const loadAdScript = () => {
      // AdFit 스크립트가 이미 있는지 확인
      if (document.querySelector('script[src*="kas/static/ba.min.js"]')) {
        scriptLoadedRef.current = true;
        return;
      }

      const script = document.createElement("script");
      script.type = "text/javascript";
      script.src = "//t1.daumcdn.net/kas/static/ba.min.js";
      script.async = true;

      script.onload = () => {
        scriptLoadedRef.current = true;
      };

      script.onerror = () => {
        console.error("AdFit 스크립트 로딩에 실패했습니다.");
        onAdFail?.();
      };

      document.head.appendChild(script);
    };

    loadAdScript();
  }, [onAdFail]);

  // NO-AD 콜백 함수
  const handleAdFail = (element: HTMLElement) => {
    console.log("AdFit 광고 로딩 실패:", element);
    onAdFail?.();

    // 광고 영역 숨기기
    if (element) {
      element.style.display = "none";
    }
  };

  // 전역 콜백 함수 등록
  useEffect(() => {
    if (typeof window !== "undefined") {
      (window as any)[`adfit_fail_${adUnit}`] = handleAdFail;
    }
  }, [adUnit, onAdFail]);

  return (
    <div
      ref={adRef}
      className={`adfit-banner-container ${className}`}
      style={{
        width: "100%",
        maxWidth: `${width}px`,
        height: `${height}px`,
        margin: "0 auto",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <ins
        className="kakao_ad_area"
        style={{
          display: "none",
          width: "100%",
          maxWidth: `${width}px`,
          height: `${height}px`,
        }}
        data-ad-unit={adUnit}
        data-ad-width={width.toString()}
        data-ad-height={height.toString()}
        data-ad-onfail={`adfit_fail_${adUnit}`}
      />
    </div>
  );
}

// 기본 사이즈 상수
export const AD_SIZES = {
  BANNER_320x50: { width: 320, height: 50 },
  BANNER_320x100: { width: 320, height: 100 },
  BANNER_300x250: { width: 300, height: 250 },
  BANNER_728x90: { width: 728, height: 90 },
} as const;

// 안전한 광고 단위 접근 함수 - SSR 안전
export const getAdUnit = (
  type: "MOBILE_BANNER" | "PC_BANNER"
): string | null => {
  try {
    if (type === "MOBILE_BANNER") {
      return process.env.NEXT_PUBLIC_MOBILE_AD || null;
    }
    if (type === "PC_BANNER") {
      return process.env.NEXT_PUBLIC_PC_AD || null;
    }
    return null;
  } catch (error) {
    console.warn("광고 단위 접근 중 오류:", error);
    return null;
  }
};

// 하위 호환성을 위한 AD_UNITS (더 이상 직접 사용 권장하지 않음)
export const AD_UNITS = {
  get MOBILE_BANNER() {
    return getAdUnit("MOBILE_BANNER");
  },
  get PC_BANNER() {
    return getAdUnit("PC_BANNER");
  },
} as const;

// 환경변수 체크 - 더 이상 함수를 export하지 않음
