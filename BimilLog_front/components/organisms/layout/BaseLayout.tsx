"use client";

import React from "react";
import { AuthHeader } from "@/components/organisms/common/AuthHeader";
import { HomeFooter } from "@/components/organisms/home/HomeFooter";
import {
  ResponsiveAdFitBanner,
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
} from "@/components";

interface BaseLayoutProps {
  children: React.ReactNode;
  showTopAd?: boolean;
  showBottomAd?: boolean;
  showFooter?: boolean;
  showHeader?: boolean;
  className?: string;
  containerClassName?: string;
}

export const BaseLayout: React.FC<BaseLayoutProps> = ({
  children,
  showTopAd = true,
  showBottomAd = true,
  showFooter = true,
  showHeader = true,
  className = "",
  containerClassName = "",
}) => {
  return (
    <div
      className={`relative min-h-screen bg-paper dark:bg-gradient-to-br dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 text-ink dark:text-gray-100 transition-colors duration-300 overflow-hidden ${className}`}
    >
      {/* Atmosphere: 좌우 floating 종이/편지 decoration (데스크톱에만, 인쇄 제외) */}
      <div
        aria-hidden="true"
        className="no-print pointer-events-none hidden lg:block absolute -top-10 -left-16 w-72 h-72 opacity-[0.18] animate-paper-float select-none"
      >
        <svg viewBox="0 0 200 200" className="w-full h-full">
          <rect x="20" y="30" width="160" height="120" rx="6" fill="#FFFDF7" stroke="#2A1F1A" strokeWidth="2" />
          <line x1="40" y1="60" x2="160" y2="60" stroke="#A89084" strokeWidth="1.5" />
          <line x1="40" y1="80" x2="150" y2="80" stroke="#A89084" strokeWidth="1.5" />
          <line x1="40" y1="100" x2="160" y2="100" stroke="#A89084" strokeWidth="1.5" />
          <line x1="40" y1="120" x2="120" y2="120" stroke="#A89084" strokeWidth="1.5" />
          <circle cx="160" cy="40" r="10" fill="none" stroke="#C73E3E" strokeWidth="2" strokeDasharray="2 3" />
        </svg>
      </div>
      <div
        aria-hidden="true"
        className="no-print pointer-events-none hidden lg:block absolute top-32 -right-12 w-64 h-64 opacity-[0.16] select-none"
        style={{ animation: "paper-float 9s ease-in-out infinite reverse" }}
      >
        <svg viewBox="0 0 200 200" className="w-full h-full">
          {/* 봉투 */}
          <rect x="20" y="50" width="160" height="110" rx="4" fill="#FFFDF7" stroke="#2A1F1A" strokeWidth="2" />
          <polyline points="20,50 100,120 180,50" fill="none" stroke="#2A1F1A" strokeWidth="2" />
          <circle cx="150" cy="90" r="14" fill="#C73E3E" opacity="0.85" />
        </svg>
      </div>

      {/* Header (인쇄 제외) */}
      {showHeader && (
        <div className="no-print">
          <AuthHeader />
        </div>
      )}

      {/* Top Banner Advertisement (인쇄 제외) */}
      {showTopAd && (
        <div className={`no-print container-paper px-4 py-2 ${containerClassName}`}>
          <div className="flex justify-center">
            <ResponsiveAdFitBanner
              position="페이지 상단"
              className="max-w-full"
            />
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className={`relative flex-1 ${containerClassName}`}>
        {children}
      </main>

      {/* Bottom Advertisement (인쇄 제외) */}
      {showBottomAd && (
        <div className={`no-print container-paper px-4 py-3 ${containerClassName}`}>
          <div className="flex justify-center px-2">
            {(() => {
              const adUnit = getAdUnit("MOBILE_BANNER");
              return adUnit ? (
                <AdFitBanner
                  adUnit={adUnit}
                  width={AD_SIZES.BANNER_320x50.width}
                  height={AD_SIZES.BANNER_320x50.height}
                />
              ) : null;
            })()}
          </div>
        </div>
      )}

      {/* Footer (인쇄 제외) */}
      {showFooter && (
        <div className="no-print">
          <div className="border-t border-ink-soft dark:border-gray-700"></div>
          <HomeFooter />
        </div>
      )}
    </div>
  );
};

// 사전 정의된 레이아웃 타입들
export const MainLayout = React.memo(({ children, ...props }: Omit<BaseLayoutProps, 'showTopAd' | 'showBottomAd'>) => (
  <BaseLayout showTopAd={true} showBottomAd={true} {...props}>
    {children}
  </BaseLayout>
));

export const CleanLayout = React.memo(({ children, ...props }: Omit<BaseLayoutProps, 'showTopAd' | 'showBottomAd'>) => (
  <BaseLayout showTopAd={false} showBottomAd={false} {...props}>
    {children}
  </BaseLayout>
));

export const AuthLayout = React.memo(({ children, ...props }: Omit<BaseLayoutProps, 'showHeader' | 'showFooter'>) => (
  <BaseLayout showHeader={false} showFooter={false} {...props}>
    {children}
  </BaseLayout>
));

export const ContentLayout = React.memo(({ children, ...props }: BaseLayoutProps) => (
  <BaseLayout containerClassName="container mx-auto px-4" {...props}>
    {children}
  </BaseLayout>
));
