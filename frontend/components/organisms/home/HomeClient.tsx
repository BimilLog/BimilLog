"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MainLayout } from "@/components/organisms/layout/BaseLayout";
import { useAuth } from "@/hooks";
import { logger } from '@/lib/utils/logger';
import { LazyKakaoFriendsModal } from "@/lib/utils/lazy-components";

// 분리된 컴포넌트들 import - 직접 파일에서 import하여 circular dependency 방지
import { HomeHero } from "./HomeHero";
import { HomeFeatures } from "./HomeFeatures";

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
        logger.error("Failed to parse protocol URL:", error);
      }
    }
  }, [searchParams, router]);

  const handleOpenFriendsModal = () => {
    if (!isAuthenticated) return;
    setIsFriendsModalOpen(true);
  };

  return (
    <MainLayout className="bg-gradient-to-br from-pink-50 via-purple-50 to-indigo-50">
      {/* Hero Section */}
      <HomeHero
        isAuthenticated={isAuthenticated}
        onOpenFriendsModal={handleOpenFriendsModal}
      />

      {/* Features Section */}
      <HomeFeatures />

      {/* 카카오 친구 모달 */}
      <LazyKakaoFriendsModal
        isOpen={isFriendsModalOpen}
        onClose={() => setIsFriendsModalOpen(false)}
      />
    </MainLayout>
  );
}
