"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AuthHeader } from "@/components/organisms/common";
import { useAuth } from "@/hooks";
import { KakaoFriendsModal } from "@/components";
import {
  ResponsiveAdFitBanner,
  AdFitBanner,
  AD_SIZES,
  getAdUnit,
} from "@/components";

// 분리된 컴포넌트들 import
import { HomeHero, HomeFeatures, HomeFooter } from "@/components/organisms/home";

export default function HomeClient() {
  const { isAuthenticated } = useAuth();
  const [isFriendsModalOpen, setIsFriendsModalOpen] = useState(false);
  const router = useRouter();
  const searchParams = useSearchParams();

  // 프로토콜 URL 처리
  useEffect(() => {
    const url = searchParams.get("url");
    if (url) {
      try {
        const decodedUrl = decodeURIComponent(url);
        const path = decodedUrl.replace(/^web\+bimillog:\/\//, "");
        if (path) {
          router.replace(`/${path}`);
        }
      } catch (error) {
        console.error("Failed to parse protocol URL:", error);
      }
    }
  }, [searchParams, router]);

  const handleOpenFriendsModal = () => {
    if (!isAuthenticated) return;
    setIsFriendsModalOpen(true);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      <AuthHeader />

      {/* Top Banner Advertisement */}
      <div className="container mx-auto px-4 py-1">
        <div className="flex justify-center">
          <ResponsiveAdFitBanner
            position="메인페이지 상단"
            className="max-w-full"
          />
        </div>
      </div>

      {/* Hero Section */}
      <HomeHero
        isAuthenticated={isAuthenticated}
        onOpenFriendsModal={handleOpenFriendsModal}
      />

      {/* Features Section */}
      <HomeFeatures />

      {/* Mobile Advertisement */}
      <section className="container mx-auto px-4 py-3">
        <div className="flex justify-center px-2">
          {(() => {
            const adUnit = getAdUnit("MOBILE_BANNER");
            return adUnit ? (
              <AdFitBanner
                adUnit={adUnit}
                width={AD_SIZES.BANNER_320x50.width}
                height={AD_SIZES.BANNER_320x50.height}
                onAdFail={() => {
                  if (process.env.NODE_ENV === 'development') {
                    console.log("메인 페이지 광고 로딩 실패");
                  }
                }}
              />
            ) : null;
          })()}
        </div>
      </section>

      {/* Footer */}
      <HomeFooter />

      {/* 카카오 친구 모달 */}
      <KakaoFriendsModal
        isOpen={isFriendsModalOpen}
        onClose={() => setIsFriendsModalOpen(false)}
      />
    </div>
  );
}
