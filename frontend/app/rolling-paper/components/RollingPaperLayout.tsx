"use client";

import React from "react";
import { AuthHeader } from "@/components/organisms/auth-header";
import {
  ResponsiveAdFitBanner,
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
} from "@/components";

interface RollingPaperLayoutProps {
  children: React.ReactNode;
  adPosition: string;
}

export const RollingPaperLayout: React.FC<RollingPaperLayoutProps> = ({
  children,
  adPosition,
}) => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-cyan-50 to-teal-50">
      {/* Auth Header */}
      <AuthHeader />

      {/* Top Banner Advertisement */}
      <div className="container mx-auto px-4 py-2">
        <div className="flex justify-center">
          <ResponsiveAdFitBanner position={adPosition} className="max-w-full" />
        </div>
      </div>

      {/* PC Header Advertisement */}
      <div className="hidden md:block bg-white border-b">
        <div className="container mx-auto px-4 py-3">
          <div className="flex justify-center">
            {(() => {
              const adUnit = getAdUnit("PC_BANNER");
              return adUnit ? (
                <AdFitBanner
                  adUnit={adUnit}
                  width={AD_SIZES.BANNER_728x90.width}
                  height={AD_SIZES.BANNER_728x90.height}
                  className="max-w-full"
                />
              ) : null;
            })()}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="container mx-auto px-2 md:px-4 py-4 md:py-8">
        {children}
      </div>
    </div>
  );
};
