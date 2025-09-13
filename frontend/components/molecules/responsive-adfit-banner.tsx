"use client";

import React from "react";
import { AdFitBanner, AD_SIZES, getAdUnit } from "./adfit-banner";
import { logger } from '@/lib/utils/logger';

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

  const handleAdFail = (type: "mobile" | "pc") => {
    if (process.env.NODE_ENV === 'development') {
      logger.log(`${position} ${type} 광고 로딩 실패`);
    }
    onAdFail?.(type);
  };

  // 환경변수가 설정되지 않은 경우 렌더링하지 않음
  const mobileAdUnit = getAdUnit("MOBILE_BANNER");
  const pcAdUnit = getAdUnit("PC_BANNER");

  if (!mobileAdUnit || !pcAdUnit) {
    return null;
  }

  return (
    <div className={`responsive-adfit-banner ${className}`}>
      {/* Mobile Banner (320x50) */}
      <div className="block md:hidden">
        <AdFitBanner
          adUnit={mobileAdUnit}
          width={AD_SIZES.BANNER_320x50.width}
          height={AD_SIZES.BANNER_320x50.height}
          onAdFail={() => handleAdFail("mobile")}
        />
      </div>

      {/* PC Banner (728x90) */}
      <div className="hidden md:block">
        <AdFitBanner
          adUnit={pcAdUnit}
          width={AD_SIZES.BANNER_728x90.width}
          height={AD_SIZES.BANNER_728x90.height}
          onAdFail={() => handleAdFail("pc")}
        />
      </div>
    </div>
  );
}
