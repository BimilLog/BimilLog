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
      className={`min-h-screen bg-brand-gradient text-brand-primary transition-colors duration-300 dark:bg-gradient-to-br dark:from-[#0d0f1a] dark:via-[#121327] dark:to-[#0b0c1c] dark:text-brand-primary ${className}`}
    >
      {/* Header */}
      {showHeader && <AuthHeader />}

      {/* Top Banner Advertisement */}
      {showTopAd && (
        <div className={`container mx-auto px-4 py-2 ${containerClassName}`}>
          <div className="flex justify-center">
            <ResponsiveAdFitBanner
              position="페이지 상단"
              className="max-w-full"
            />
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className={`flex-1 ${containerClassName}`}>
        {children}
      </main>

      {/* Bottom Advertisement */}
      {showBottomAd && (
        <div className={`container mx-auto px-4 py-3 ${containerClassName}`}>
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

      {/* Footer */}
      {showFooter && (
        <>
          <div className="border-t-2 border-black"></div>
          <HomeFooter />
        </>
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
