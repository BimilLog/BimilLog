"use client";

import React, { useState, useEffect } from "react";
import { AdFitBanner, AD_SIZES, getAdUnit } from "./adfit-banner";
import { logger } from '@/lib/utils/logger';
import { useMediaQuery } from '@/hooks/common/useMediaQuery';

interface ResponsiveAdFitBannerProps {
  /**
   * 추가 CSS 클래스
   */
  className?: string;
  /**
   * 광고 로딩 실패 시 실행될 콜백 함수
   */
  onAdFail?: (type: "mobile" | "pc") => void;
  /**
   * 배치 위치 (로깅용)
   */
  position?: string;
}

export function ResponsiveAdFitBanner({
  className = "",
  onAdFail,
  position = "unknown",
}: ResponsiveAdFitBannerProps) {
  // 모바일 화면 감지 (Tailwind의 md 브레이크포인트: 768px)
  const isMobile = useMediaQuery('(max-width: 767px)');
  const [mounted, setMounted] = useState(false);

  // SSR 불일치 방지
  useEffect(() => {
    setMounted(true);
  }, []);

  const handleAdFail = (type: "mobile" | "pc") => {
    if (process.env.NODE_ENV === 'development') {
      logger.log(`${position} ${type} 광고 로딩 실패`);
    }
    onAdFail?.(type);
  };

  // 환경변수가 설정되지 않은 경우 렌더링하지 않음
  const mobileAdUnit = getAdUnit("MOBILE_BANNER");
  const pcAdUnit = getAdUnit("PC_BANNER");

  // 마운트 전에는 렌더링하지 않음 (SSR 불일치 방지)
  if (!mounted) {
    return null;
  }

  if (!mobileAdUnit && !pcAdUnit) {
    return null;
  }

  return (
    <div className={`responsive-adfit-banner ${className}`}>
      {/* 조건부 렌더링: 모바일 또는 PC 광고 중 하나만 DOM에 렌더링 */}
      {isMobile && mobileAdUnit ? (
        <AdFitBanner
          adUnit={mobileAdUnit}
          width={AD_SIZES.BANNER_320x50.width}
          height={AD_SIZES.BANNER_320x50.height}
          onAdFail={() => handleAdFail("mobile")}
        />
      ) : (
        pcAdUnit && (
          <AdFitBanner
            adUnit={pcAdUnit}
            width={AD_SIZES.BANNER_728x90.width}
            height={AD_SIZES.BANNER_728x90.height}
            onAdFail={() => handleAdFail("pc")}
          />
        )
      )}
    </div>
  );
}
